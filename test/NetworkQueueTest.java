package network;

import interfaces.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import network.io.Message;
import network.io.MessageSendCollect;

/** Local memory-only queue and wire tests, no listeners, DB or game startup. */
public final class NetworkQueueTest {
    static int checks;
    static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    static final class SessionFixture implements InvocationHandler {
        volatile boolean connected = true; boolean encrypted;
        final byte[] key; final ISession session;
        SessionFixture(byte... key) { this.key = key; session = (ISession)Proxy.newProxyInstance(ISession.class.getClassLoader(), new Class[]{ISession.class}, this); }
        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "isConnected" -> connected;
                case "sentKey" -> encrypted;
                case "getKey" -> key;
                case "disconnect" -> { connected = false; yield null; }
                case "toString" -> "fixture";
                default -> null;
            };
        }
    }
    static final class MemorySocket extends Socket {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        @Override public OutputStream getOutputStream() { return output; }
    }
    static class TrackedMessage extends Message {
        int cleaned;
        TrackedMessage(int n) throws IOException { super(1); writer().writeInt(n); }
        @Override public void cleanup() { cleaned++; super.cleanup(); }
    }
    @SuppressWarnings("unchecked") static BlockingDeque<Message> queue(Object o) throws Exception {
        Field f = o.getClass().getDeclaredField("messages"); f.setAccessible(true); return (BlockingDeque<Message>)f.get(o);
    }
    static void burst(java.util.function.IntConsumer enqueue, int count) throws Exception {
        int threads = 24; ExecutorService pool = Executors.newFixedThreadPool(threads); CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> tasks = new ArrayList<>();
        try {
            for (int t = 0; t < threads; t++) { int n = t; tasks.add(pool.submit(() -> { start.await(); for (int i = n; i < count; i += threads) enqueue.accept(i); return null; })); }
            start.countDown(); for (Future<?> task : tasks) task.get(10, TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
    }
    public static void main(String[] args) throws Exception {
        SessionFixture f = new SessionFixture((byte)3, (byte)15, (byte)-77); AtomicInteger disconnects = new AtomicInteger();
        Network.gI().setAcceptHandler(new ISessionAcceptHandler() {
            public void sessionInit(ISession s) {}
            public void sessionDisconnect(ISession s) { disconnects.incrementAndGet(); }
        });
        QueueHandler handler = new QueueHandler(f.session); Sender sender = new Sender(f.session, new MemorySocket());
        for (int i = 0; i < 500; i++) handler.addMessage(new TrackedMessage(i));
        handler.addMessage(new TrackedMessage(501)); check(queue(handler).size() == 500, "Inbound limit drops overflow without exceeding 500");
        for (int i = 0; i < 500; i++) check(new DataInputStream(new ByteArrayInputStream(queue(handler).poll().getData())).readInt() == i, "Inbound accepted messages remain FIFO");
        for (int i = 0; i < 3000; i++) sender.sendMessage(new TrackedMessage(i));
        TrackedMessage rejected = new TrackedMessage(3001); sender.sendMessage(rejected);
        check(queue(sender).size() == 3000 && disconnects.get() == 1 && rejected.cleaned == 1, "Outbound overflow retains disconnect and cleanup policy");
        for (int i = 0; i < 3000; i++) check(new DataInputStream(new ByteArrayInputStream(queue(sender).poll().getData())).readInt() == i, "Outbound accepted messages remain FIFO");
        f.connected = false; handler.addMessage(new Message(1)); sender.sendMessage(new Message(1));
        check(queue(handler).isEmpty() && queue(sender).isEmpty(), "Disconnected sessions reject new enqueue"); f.connected = true;
        // Race producers for the final free slot repeatedly, exercising actual admission methods.
        for (int round = 0; round < 6; round++) {
            queue(handler).clear(); queue(sender).clear();
            for (int i = 0; i < 499; i++) handler.addMessage(new Message(1));
            for (int i = 0; i < 2999; i++) sender.sendMessage(new Message(1));
            burst(n -> { handler.addMessage(new Message(1)); sender.sendMessage(new Message(1)); }, 2400);
            check(queue(handler).size() == 500 && queue(sender).size() == 3000, "Concurrent producers cannot overrun either queue limit");
        }
        handler.close(); sender.close(); check(queue(handler).isEmpty() && queue(sender).isEmpty(), "Close releases queued messages");

        QueueHandler consumer = new QueueHandler(f.session); List<Integer> seen = new ArrayList<>();
        consumer.setMessageHandler((session, message) -> { seen.add(new DataInputStream(new ByteArrayInputStream(message.getData())).readInt()); if (seen.size() == 50) f.connected = false; });
        for (int i = 0; i < 50; i++) consumer.addMessage(new TrackedMessage(i));
        Thread worker = new Thread(consumer); worker.setDaemon(true); worker.start(); worker.join(3000);
        check(!worker.isAlive() && seen.size() == 50 && seen.get(0) == 0 && seen.get(49) == 49, "Actual handler drains accepted messages in order");

        // Compare real serializer output with an independent protocol encoder. The same message is broadcast with different keys.
        for (int cmd : new int[]{1, -5, -32, -66, -74, 11, -67, -87, 66, -28}) for (boolean encrypted : new boolean[]{false,true}) {
            Message shared = new Message(cmd); byte[] payload = new byte[513]; for (int i = 0; i < payload.length; i++) payload[i] = (byte)(i * 17);
            shared.writer().write(payload);
            for (byte[] key : new byte[][]{{3,15,-77}, {1,-8,11,90,5}}) {
                SessionFixture session = new SessionFixture(key); session.encrypted = encrypted;
                MemorySocket socket = new MemorySocket(); Sender send = new Sender(session.session, socket); send.setSend(new MessageSendCollect());
                send.doSendMessage(shared);
                check(Arrays.equals(socket.output.toByteArray(), encode(cmd, payload, key, encrypted)), "Packet bytes preserve framing and XOR for command " + cmd);
                check(Arrays.equals(shared.getData(), payload), "Broadcast encryption cannot mutate shared payload"); send.close();
            }
        }
        System.out.println("PASS: " + checks + " bounded queue, concurrent admission, FIFO, overflow, consumer and broadcast wire checks.");
    }
    static byte[] encode(int cmd, byte[] bytes, byte[] key, boolean encrypted) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(buffer); int cursor = 0;
        out.writeByte(encrypted ? ((byte)cmd ^ key[cursor++ % key.length]) : cmd);
        boolean big = Set.of(-32,-66,-74,11,-67,-87,66,-28).contains(cmd);
        if (big) { for (int shift : new int[]{0,8,16}) out.writeByte(((byte)((byte)(bytes.length >> shift) ^ key[cursor++ % key.length])) - 128); }
        else if (encrypted) { out.writeByte((byte)(bytes.length >> 8) ^ key[cursor++ % key.length]); out.writeByte((byte)bytes.length ^ key[cursor++ % key.length]); }
        else out.writeShort(bytes.length);
        for (byte b : bytes) out.writeByte(encrypted ? b ^ key[cursor++ % key.length] : b);
        return buffer.toByteArray();
    }
}
