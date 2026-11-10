package network;

import lombok.NonNull;
import lombok.Setter;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import interfaces.IMessageHandler;
import interfaces.ISession;
import network.io.Message;

public class QueueHandler implements Runnable {
    private static final int MAX_PENDING_MESSAGES = 500;

    private ISession session;
    private BlockingDeque<Message> messages;
    @Setter
    private IMessageHandler messageHandler;

    public QueueHandler(@NonNull ISession session) {
        try {
            this.session = session;
            this.messages = new LinkedBlockingDeque<>(MAX_PENDING_MESSAGES);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void run() {
        while (session != null && session.isConnected()) {
            try {
                Message message = messages.poll(200L, TimeUnit.MILLISECONDS);
                if (message != null) {
                    try {
                        this.messageHandler.onMessage(this.session, message);
                    } catch (Exception e) {
                        // Lưới chót: Controller đã catch Exception - đây là phòng thủ cho Error/lỗi ngoài
                        utils.Logger.logException(QueueHandler.class, e);
                    }
                    message.cleanup();
                }
            } catch (Exception e) {
                utils.Logger.logException(QueueHandler.class, e);
            }
        }
    }

    public void addMessage(Message msg) {
        try {
            if (session == null || !session.isConnected() || !messages.offer(msg)) {
                if (msg != null) msg.cleanup();
            }
        } catch (Exception ignored) {
        }
    }

    public void close() {
        if (messages != null) {
            messages.clear();
        }
    }

    public void dispose() {
        this.session = null;
        this.messages = null;
    }
}
