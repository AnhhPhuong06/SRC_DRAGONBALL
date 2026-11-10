package network;

import java.net.Socket;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import lombok.NonNull;
import interfaces.IMessageSendCollect;
import interfaces.ISession;
import network.io.Message;

public final class Sender implements Runnable {

    private static final int MAX_PENDING_MESSAGES = 3000;

    private ISession session;
    private BlockingDeque<Message> messages;
    private DataOutputStream dos;
    private IMessageSendCollect sendCollect;

    public Sender(@NonNull ISession session, @NonNull Socket socket) {
        try {
            this.session = session;
            this.messages = new LinkedBlockingDeque<>(MAX_PENDING_MESSAGES);
            this.setSocket(socket);
        } catch (Exception ignored) {
        }
    }

    public Sender setSocket(@NonNull Socket socket) {
        try {
            this.dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 8192));
        } catch (IOException ignored) {
        }
        return this;
    }

    @Override
    public void run() {
        boolean writeFailed = false;
        try {
            while (session.isConnected()) {
                Message message = messages.poll(5, TimeUnit.SECONDS);
                if (message == null) {
                    continue;
                }
                try {
                    synchronized (this) {
                        while (message != null) {
                            this.sendCollect.doSendMessage(this.session, this.dos, message, false);
                            message.cleanup();
                            message = messages.poll();
                        }
                        this.dos.flush();
                    }
                } catch (Exception io) {

                    writeFailed = true;
                }
                if (writeFailed) {
                    break;
                }
            }
        } catch (Exception ignored) {
            writeFailed = true;
        }
        if (writeFailed) {
            ISession s = this.session;
            try {
                Network.gI().getAcceptHandler().sessionDisconnect(s);
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void doSendMessage(Message message) throws Exception {
        this.sendCollect.doSendMessage(this.session, this.dos, message);
    }

    public void sendMessage(Message msg) {
        try {
            if (session.isConnected()) {
                // Admission and the capacity check share one queue lock.
                if (!messages.offer(msg)) {
                    ISession s = this.session;
                    try {
                        Network.gI().getAcceptHandler().sessionDisconnect(s);
                    } catch (Exception ignored) {
                    }
                    msg.cleanup();
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void setSend(IMessageSendCollect sendCollect) {
        this.sendCollect = sendCollect;
    }

    public void close() {
        this.messages.clear();
        if (this.dos != null) {
            try {
                this.dos.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void dispose() {
        this.session = null;
        this.messages = null;
        this.sendCollect = null;
        this.dos = null;
    }
}
