package network.session;

import java.net.InetSocketAddress;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import interfaces.IKeySessionHandler;
import interfaces.IMessageHandler;
import interfaces.IMessageSendCollect;

import interfaces.ISession;
import consts.SocketType;
import network.Collector;
import network.QueueHandler;
import network.Sender;
import network.io.Message;

public class Session implements ISession {

    private static ISession instance;
    private static int ID_INIT;
    private SocketType socketType = SocketType.SERVER;
    private byte[] KEYS;
    private volatile boolean sentKey;
    public int id;
    private Socket socket;
    private volatile boolean connected;
    private Sender sender;
    private Collector collector;
    private QueueHandler queueHandler;
    private final Thread tSender;
    private final Thread tCollector;
    private final Thread tQueueHandler;
    private IKeySessionHandler keyHandler;
    private String ip;

    public static ISession gI() throws Exception {
        if (instance == null) {
            throw new Exception("Instance has not been initialized!");
        }
        return instance;
    }
    
    public Session(String host, int port) throws IOException {
        this.id = 31072002;
        this.socket = new Socket(host, port);
        this.socket.setTcpNoDelay(true);
        this.socket.setSendBufferSize(0x100000);
        this.socket.setReceiveBufferSize(0x100000);
        this.socketType = SocketType.CLIENT;
        this.connected = true;
        this.sender = this.sender != null ? this.sender.setSocket(this.socket) : new Sender(this, this.socket);
        this.collector = this.collector != null ? this.collector.setSocket(this.socket) : new Collector(this, this.socket);
        this.queueHandler = new QueueHandler(this);
        this.tSender = new Thread(this.sender, "Sender - IP : " + this.ip);
        this.tCollector = new Thread(this.collector, "Collector - IP : " + this.ip);
        this.tQueueHandler = new Thread(this.queueHandler);
    }

    public Session(Socket socket) {
        this.KEYS = "NguyenDucVuEntertainment".getBytes();
        this.id = ID_INIT++;
        this.socket = socket;
        try {
            this.socket.setTcpNoDelay(true);
            this.socket.setSendBufferSize(0x100000);
            this.socket.setReceiveBufferSize(0x100000);
        } catch (SocketException ignored) {
        }
        this.socketType = SocketType.SERVER;
        this.connected = true;
        this.ip = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().toString().replace("/", "");
        this.sender = this.sender != null ? this.sender.setSocket(this.socket) : new Sender(this, this.socket);
        this.collector = this.collector != null ? this.collector.setSocket(this.socket) : new Collector(this, this.socket);
        this.queueHandler = new QueueHandler(this);
        this.tSender = new Thread(this.sender, "Sender - IP : " + this.ip);
        this.tCollector = new Thread(this.collector, "Collector - IP : " + this.ip);
        this.tQueueHandler = new Thread(this.queueHandler);
    }

    @Override
    public void sendMessage(Message msg) {
        if (this.isConnected() && msg != null) {
            this.sender.sendMessage(msg);
        }
    }

    @Override
    public ISession setSendCollect(IMessageSendCollect collect) {
        this.sender.setSend(collect);
        this.collector.setCollect(collect);
        return this;
    }

    @Override
    public ISession setMessageHandler(IMessageHandler handler) {
//        this.collector.setMessageHandler(handler);
        this.queueHandler.setMessageHandler(handler);
        return this;
    }

    @Override
    public ISession setKeyHandler(IKeySessionHandler handler) {
        this.keyHandler = handler;
        return this;
    }

    @Override
    public synchronized ISession startSend() {
        if (connected && this.tSender.getState() == Thread.State.NEW) this.tSender.start();
        return this;
    }

    @Override
    public ISession startCollect() {
        this.tCollector.start();
        return this;
    }

    @Override
    public ISession startQueueHandler() {
        this.tQueueHandler.start();
        return this;
    }

    @Override
    public ISession start() {
        this.tSender.start();
        this.tCollector.start();
        this.tQueueHandler.start();
        return this;
    }

    @Override
    public String getIP() {
        return this.ip;
    }

    @Override
    public long getID() {
        return this.id;
    }

    @Override
    public synchronized void disconnect() {
        if (!this.connected) return;
        this.connected = false;
        this.sentKey = false;
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException ignored) {
            }
        }
        if (this.sender != null) {
            this.sender.close();
        }
        if (this.collector != null) {
            this.collector.close();
        }
        if (this.queueHandler != null) {
            this.queueHandler.close();
        }
        this.dispose();
    }

    @Override
    public void dispose() {
        if (this.sender != null) {
            this.sender.dispose();
        }
        if (this.collector != null) {
            this.collector.dispose();
        }
        if (this.queueHandler != null) {
            this.queueHandler.dispose();
        }
        this.socket = null;
        this.sender = null;
        this.collector = null;
        this.queueHandler = null;
        this.ip = null;
        SessionManager.gI().removeSession(this);
    }

    @Override
    public void sendKey() throws Exception {
        if (this.keyHandler == null) {
            throw new Exception("Key handler has not been initialized!");
        }
        this.keyHandler.sendKey(this);
    }

    @Override
    public void setKey(Message message) throws Exception {
        if (this.keyHandler == null) {
            throw new Exception("Key handler has not been initialized!");
        }
        this.keyHandler.setKey(this, message);
    }

    @Override
    public void setKey(byte[] key) {
        this.KEYS = key;
    }

    @Override
    public boolean sentKey() {
        return this.sentKey;
    }

    @Override
    public void setSentKey(boolean sent) {
        this.sentKey = sent;
    }

    @Override
    public void doSendMessage(Message msg) throws Exception {
        this.sender.doSendMessage(msg);
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public byte[] getKey() {
        return this.KEYS;
    }

    @Override
    public SocketType getSocketType() {
        return this.socketType;
    }

    @Override
    public QueueHandler getQueueHandler() {
        return this.queueHandler;
    }

}
