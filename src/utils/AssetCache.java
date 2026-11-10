package utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Bounded cache for immutable file contents. Returned arrays must not be modified. */
final class AssetCache {

    private final long maxBytes;
    private final Function<String, byte[]> loader;
    private volatile State state = new State();

    AssetCache(long maxBytes, Function<String, byte[]> loader) {
        if (maxBytes < 0) {
            throw new IllegalArgumentException("Negative cache size");
        }
        this.maxBytes = maxBytes;
        this.loader = loader;
    }

    byte[] get(String path) {
        State current = state;
        byte[] cached = current.entries.get(path);
        if (cached != null) {
            return cached;
        }

        CompletableFuture<byte[]> pending = new CompletableFuture<>();
        CompletableFuture<byte[]> loading = current.loads.putIfAbsent(path, pending);
        if (loading != null) {
            try {
                return loading.join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof RuntimeException cause) {
                    throw cause;
                }
                if (e.getCause() instanceof Error cause) {
                    throw cause;
                }
                throw e;
            }
        }

        try {
            // Another load may have completed between the first lookup and registration.
            byte[] data = current.entries.get(path);
            if (data == null) {
                data = loader.apply(path);
                if (data != null) {
                    synchronized (current) {
                        // Keep accounting and admission atomic across different file loads.
                        if (data.length <= maxBytes - current.bytes) {
                            current.entries.put(path, data);
                            current.bytes += data.length;
                        }
                    }
                }
            }
            pending.complete(data);
            return data;
        } catch (RuntimeException | Error e) {
            pending.completeExceptionally(e);
            throw e;
        } finally {
            current.loads.remove(path, pending);
        }
    }

    void clear() {
        // In-flight loads belong to the old state and cannot refill the cleared cache.
        state = new State();
    }

    private static final class State {

        final ConcurrentHashMap<String, byte[]> entries = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, CompletableFuture<byte[]>> loads = new ConcurrentHashMap<>();
        long bytes;
    }
}
