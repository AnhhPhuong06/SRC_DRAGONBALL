import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import network.io.Message;
import network.io.MessageSendCollect;
import network.session.MySession;
import server.Controller;
import services.AccountRegistrationRequest;
import sun.misc.Unsafe;

/** Packet parsing and malformed request handling only; never creates a database account. */
public class AccountRegistrationPacketTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        byte[] compact = payload("user123", "pass123");
        byte[] legacy = payload("device", "platform", "version", "locale", "model", "region", "extra", "user123", "pass123");
        for (byte[] bytes : new byte[][]{compact, legacy}) {
            AccountRegistrationRequest request = parse(bytes);
            check(request.username().equals("user123") && request.password().equals("pass123"), "Decode correct credentials");
        }
        AccountRegistrationRequest unicode = parse(payload("người", " pấss\u0000 "));
        check(unicode.username().equals("người") && unicode.password().equals(" pấss\u0000 "),
                "Preserve modified UTF, NUL and password whitespace");

        List<byte[]> invalid = new ArrayList<>();
        invalid.add(new byte[0]);
        invalid.add(new byte[]{0});
        invalid.add(payload("user123"));
        invalid.add(concat(compact, new byte[]{0}));
        invalid.add(concat(compact, new byte[]{0, 5, 'a'}));
        invalid.add(new byte[]{0, 2, (byte) 0xc0});
        invalid.add(new byte[]{0, 1, (byte) 0x80});
        for (int fieldCount = 3; fieldCount <= 10; fieldCount++) {
            if (fieldCount == 9) continue;
            String[] fields = new String[fieldCount];
            Arrays.fill(fields, "unwanted");
            invalid.add(payload(fields));
        }
        for (byte[] bytes : invalid) {
            try {
                parse(bytes);
                throw new AssertionError("Malformed payload accepted");
            } catch (IOException expected) {
                checks++;
            }
        }
        try {
            AccountRegistrationRequest.read(null);
            throw new AssertionError("Missing reader accepted");
        } catch (IOException expected) { checks++; }

        for (boolean encrypted : new boolean[]{false, true}) {
            byte[] key = {7, 19, -3};
            byte[] frames = concat(frame(compact), frame(legacy));
            if (encrypted) frames = encrypt(frames, key);
            FakeSession session = session(encrypted, key);
            MessageSendCollect collector = new MessageSendCollect();
            DataInputStream input = new DataInputStream(new FragmentedInput(frames));
            for (int index = 0; index < 2; index++) {
                Message message = collector.readMessage(session, input);
                check(message.command == 42, "Framing preserves registration command");
                AccountRegistrationRequest request = AccountRegistrationRequest.read(message.reader());
                check(request.username().equals("user123") && request.password().equals("pass123"),
                        "Read fragmented " + (encrypted ? "encrypted" : "plain") + " payload");
            }
            check(input.available() == 0, "Read two frames without losing boundaries");
            byte[] complete = frame(legacy);
            if (encrypted) complete = encrypt(complete, key);
            for (int cut = 0; cut < complete.length; cut++) {
                try {
                    new MessageSendCollect().readMessage(session(encrypted, key),
                            new DataInputStream(new FragmentedInput(Arrays.copyOf(complete, cut))));
                    throw new AssertionError("Truncated frame dispatched");
                } catch (EOFException expected) { checks++; }
            }
        }

        FakeSession session = session(false, new byte[]{0});
        for (byte[] bytes : invalid) {
            session.lastNotification = null;
            Controller.gI().onMessage(session, new Message((byte) 42, bytes));
            check(session.lastNotification != null && session.lastNotification.contains("Dữ liệu đăng ký không hợp lệ"),
                    "Controller returns a clear error for malformed registration");
        }
        Controller.gI().onMessage(session, new Message((byte) 42, payload("abc", "pass123")));
        check(session.lastNotification.contains("4-18"), "Compact format reaches existing username validation");
        Controller.gI().onMessage(session, new Message((byte) 42,
                payload("", "", "", "", "", "", "", "user123", "12345")));
        check(session.lastNotification.contains("6-18"), "Legacy format reaches existing password validation");
        Controller.gI().onMessage(session, new Message((byte) 42, compactWithInvalidUser()));
        check(session.lastNotification.contains("4-18"), "Queue can process another request after malformed packets");
        System.out.println("PASS: " + checks + " registration formats, malformed payload, framing and controller checks");
    }

    private static AccountRegistrationRequest parse(byte[] bytes) throws IOException {
        return AccountRegistrationRequest.read(new DataInputStream(new ByteArrayInputStream(bytes)));
    }
    private static byte[] compactWithInvalidUser() throws IOException { return payload("x", "validpass"); }
    private static byte[] payload(String... fields) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream writer = new DataOutputStream(bytes);
        for (String field : fields) writer.writeUTF(field);
        return bytes.toByteArray();
    }
    private static byte[] frame(byte[] payload) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream writer = new DataOutputStream(bytes);
        writer.writeByte(42);
        writer.writeShort(payload.length);
        writer.write(payload);
        return bytes.toByteArray();
    }
    private static byte[] concat(byte[] first, byte[] second) {
        byte[] joined = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, joined, first.length, second.length);
        return joined;
    }
    private static byte[] encrypt(byte[] plain, byte[] key) {
        byte[] encoded = plain.clone();
        for (int index = 0; index < encoded.length; index++) encoded[index] ^= key[index % key.length];
        return encoded;
    }
    private static FakeSession session(boolean encrypted, byte[] key) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        FakeSession session = (FakeSession) ((Unsafe) field.get(null)).allocateInstance(FakeSession.class);
        session.encrypted = encrypted;
        session.key = key;
        return session;
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static class FragmentedInput extends ByteArrayInputStream {
        FragmentedInput(byte[] bytes) { super(bytes); }
        @Override public synchronized int read(byte[] target, int offset, int length) {
            return super.read(target, offset, Math.min(length, 2));
        }
    }
    private static class FakeSession extends MySession {
        boolean encrypted;
        byte[] key;
        String lastNotification;
        private FakeSession() { super(null); }
        @Override public boolean sentKey() { return encrypted; }
        @Override public byte[] getKey() { return key; }
        @Override public void sendMessage(Message message) {
            try {
                if (message.command == -26) lastNotification = new DataInputStream(new ByteArrayInputStream(message.getData())).readUTF();
            } catch (IOException e) { throw new RuntimeException(e); }
        }
    }
}
