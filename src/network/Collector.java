package network;

import java.net.Socket;
import java.io.BufferedInputStream;
import lombok.Setter;
import java.io.DataInputStream;
import java.io.IOException;
import interfaces.IMessageSendCollect;
import interfaces.ISession;
import consts.Cmd_message;
import consts.SocketType;
import network.io.Message;

public final class Collector implements Runnable {

    private ISession session;
    private DataInputStream dis;
    @Setter
    private IMessageSendCollect collect;
//    @Setter
//    private IMessageHandler messageHandler;

    public Collector(ISession session, Socket socket) {
        this.session = session;
        this.setSocket(socket);
    }

    public Collector setSocket(Socket socket) {
        try {
            dis = new DataInputStream(new BufferedInputStream(socket.getInputStream(), 8192));
        } catch (IOException ignored) {
        }
        return this;
    }

    @Override
    public void run() {
        try {
            while (session != null && session.isConnected()) {
                final Message msg = this.collect.readMessage(this.session, this.dis);
                if (msg.command == Cmd_message.GET_SESSION_ID) {
                    if (session.getSocketType() == SocketType.SERVER) {
                        this.session.sendKey();
                    } else {
                        this.session.setKey(msg);
                    }
                    msg.cleanup();
                } else {
//                    this.messageHandler.onMessage(this.session, msg);
                    this.session.getQueueHandler().addMessage(msg);
                }
//                msg.cleanup();
            }
        } catch (Exception ignored) {
        }
        ISession s = this.session;
        try {
            Network.gI().getAcceptHandler().sessionDisconnect(s);
        } catch (Exception ignored) {
        }
        if (s != null) {
            try {
                s.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    public void close() {
        if (dis != null) {
            try {
                dis.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void dispose() {
        session = null;
        dis = null;
        collect = null;
    }
}
