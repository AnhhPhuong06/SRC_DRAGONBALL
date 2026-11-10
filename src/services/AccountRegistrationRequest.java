package services;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Command 42 payload: username/password, or seven legacy fields followed by username/password. */
public record AccountRegistrationRequest(String username, String password) {
    public static AccountRegistrationRequest read(DataInputStream payload) throws IOException {
        if (payload == null) throw new IOException("Missing registration payload");
        List<String> fields = new ArrayList<>(9);
        // Message.reader() wraps a complete byte-array payload, so available() marks the packet boundary.
        while (payload.available() > 0) {
            if (fields.size() == 9) throw new IOException("Unexpected registration fields");
            fields.add(payload.readUTF());
        }
        if (fields.size() == 2) return new AccountRegistrationRequest(fields.get(0), fields.get(1));
        if (fields.size() == 9) return new AccountRegistrationRequest(fields.get(7), fields.get(8));
        throw new IOException("Incomplete registration payload");
    }
}
