package services;

import java.io.IOException;
import java.nio.file.*;

/** Replace a complete configuration before publishing it to gameplay threads. */
final class DropConfigFiles {
    private DropConfigFiles() {}
    static void write(Path path, byte[] bytes) throws IOException {
        path = path.toAbsolutePath().normalize();
        Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(), "drop-", ".tmp");
        try {
            Files.write(temp, bytes);
            try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
