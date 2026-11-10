package network.io;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;

import interfaces.IMessage;

public class Message implements IMessage {

    public byte command;
    private ByteArrayOutputStream os;
    private DataOutputStream dos;
    private ByteArrayInputStream is;
    private DataInputStream dis;

    public Message(int command) {
        this((byte) command);
    }

    public Message(byte command) {
        this.command = command;
        this.os = new ByteArrayOutputStream();
        this.dos = new DataOutputStream(this.os);
    }

    public Message(byte command, byte[] data) {
        this.command = command;
        this.is = new ByteArrayInputStream(data);
        this.dis = new DataInputStream(this.is);
    }
    public static boolean readInt = false;
    public void writeFix(long v) throws IOException {
        if (readInt) {
            writeInt((int) v);
            return;
        }
        writeLong(v);
    }
    public void writeInt(int v) throws IOException {
        /* 219 */ writer().writeInt(v);
    }

    public void writeLong(long v) throws IOException {
        /* 224 */ writer().writeLong(v);
    }

    @Override
    public DataOutputStream writer() {
        return this.dos;
    }

    @Override
    public DataInputStream reader() {
        return this.dis;
    }

    @Override
    public byte[] getData() {
        return this.os.toByteArray();
    }

    @Override
    public void cleanup() {
        try {
            if (this.is != null) {
                this.is.close();
            }
            if (this.os != null) {
                this.os.close();
            }
            if (this.dis != null) {
                this.dis.close();
            }
            if (this.dos != null) {
                this.dos.close();
            }
        } catch (IOException e) {
        }
    }

    @Override
    public void dispose() {
        this.cleanup();
        this.dis = null;
        this.is = null;
        this.dos = null;
        this.os = null;
    }
}
