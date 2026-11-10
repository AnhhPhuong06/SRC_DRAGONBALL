package utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

/** Isolated file/cache regressions: no game startup, database or network. */
public final class AssetCacheTest {

    private static int checks;

    public static void main(String[] args) throws Exception {
        concurrentReadsShareOneLoad();
        concurrentAdmissionRespectsBudget();
        oversizedAndMissingFilesRemainReadable();
        clearDiscardsInFlightLoads();
        failedLoadsCanRetry();
        fileIOReadsCompleteFilesAndReloads();
        System.out.println("PASS: " + checks + " asset cache checks");
    }

    private static void concurrentReadsShareOneLoad() throws Exception {
        int readers = 24;
        AtomicInteger reads = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(readers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch requested = new CountDownLatch(readers);
        byte[] expected = {1, 2, 3};
        AssetCache cache = new AssetCache(1024, path -> {
            reads.incrementAndGet();
            await(requested);
            return expected.clone();
        });
        ExecutorService executor = Executors.newFixedThreadPool(readers);
        try {
            List<Future<byte[]>> results = new ArrayList<>();
            for (int i = 0; i < readers; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    await(start);
                    requested.countDown();
                    return cache.get("same-icon");
                }));
            }
            await(ready);
            start.countDown();
            byte[] shared = results.get(0).get(5, TimeUnit.SECONDS);
            for (Future<byte[]> result : results) {
                check(result.get(5, TimeUnit.SECONDS) == shared,
                        "Concurrent clients share the loaded byte array");
            }
            check(Arrays.equals(shared, expected), "Cached data stays complete");
            check(reads.get() == 1, "One disk load serves simultaneous requests for the same file");
        } finally {
            executor.shutdownNow();
        }
    }

    private static void concurrentAdmissionRespectsBudget() throws Exception {
        int files = 24;
        AtomicIntegerArray reads = new AtomicIntegerArray(files);
        CountDownLatch loading = new CountDownLatch(files);
        AssetCache cache = new AssetCache(16, path -> {
            int id = Integer.parseInt(path);
            if (reads.incrementAndGet(id) == 1) {
                loading.countDown();
                await(loading);
            }
            byte[] data = new byte[8];
            Arrays.fill(data, (byte) id);
            return data;
        });
        ExecutorService executor = Executors.newFixedThreadPool(files);
        try {
            List<Future<byte[]>> results = new ArrayList<>();
            for (int i = 0; i < files; i++) {
                final String path = Integer.toString(i);
                results.add(executor.submit(() -> cache.get(path)));
            }
            for (int i = 0; i < files; i++) {
                byte[] data = results.get(i).get(5, TimeUnit.SECONDS);
                check(data.length == 8 && data[0] == i,
                        "Cache budget never drops a client's requested file");
            }
            int retainedBytes = 0;
            for (int i = 0; i < files; i++) {
                cache.get(Integer.toString(i));
                if (reads.get(i) == 1) {
                    retainedBytes += 8;
                }
            }
            check(retainedBytes == 16, "Concurrent cache admission respects the exact byte budget");
        } finally {
            executor.shutdownNow();
        }
    }

    private static void oversizedAndMissingFilesRemainReadable() {
        AtomicInteger reads = new AtomicInteger();
        AtomicBoolean exists = new AtomicBoolean();
        AssetCache cache = new AssetCache(2, path -> {
            reads.incrementAndGet();
            return switch (path) {
                case "large" -> new byte[3];
                case "empty" -> new byte[0];
                case "missing" -> exists.get() ? new byte[]{9} : null;
                default -> new byte[1];
            };
        });
        check(cache.get("large").length == 3 && cache.get("large").length == 3,
                "Oversized assets are still returned to clients");
        check(reads.get() == 2, "Oversized assets are not cached");
        check(cache.get("missing") == null, "Missing file returns null");
        exists.set(true);
        check(cache.get("missing")[0] == 9, "Missing file can appear later without cache reset");
        byte[] empty = cache.get("empty");
        check(empty == cache.get("empty"), "Empty existing files can be cached");
        byte[] small = cache.get("small");
        check(small == cache.get("small"), "Rejected oversized files do not consume cache budget");
        AssetCache disabled = new AssetCache(0, path -> new byte[]{1});
        check(disabled.get("file") != disabled.get("file"), "Zero budget retains no file payload");
    }

    private static void clearDiscardsInFlightLoads() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        CountDownLatch oldLoadStarted = new CountDownLatch(1);
        CountDownLatch finishOldLoad = new CountDownLatch(1);
        AssetCache cache = new AssetCache(1, path -> {
            int version = reads.incrementAndGet();
            if (version == 1) {
                oldLoadStarted.countDown();
                await(finishOldLoad);
            }
            return new byte[]{(byte) version};
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<byte[]> oldRead = executor.submit(() -> cache.get("icon"));
            await(oldLoadStarted);
            cache.clear();
            Future<byte[]> newRead = executor.submit(() -> cache.get("icon"));
            check(newRead.get(5, TimeUnit.SECONDS)[0] == 2,
                    "Post-clear reader starts a fresh load without waiting for old I/O");
            finishOldLoad.countDown();
            check(oldRead.get(5, TimeUnit.SECONDS)[0] == 1,
                    "An earlier request can finish using its own loaded data");
            check(cache.get("icon")[0] == 2 && reads.get() == 2,
                    "An old in-flight load cannot overwrite or consume the new cache budget");
            cache.clear();
            check(cache.get("icon")[0] == 3 && cache.get("icon")[0] == 3,
                    "Repeated clear resets both contents and admission capacity");
        } finally {
            finishOldLoad.countDown();
            executor.shutdownNow();
        }
    }

    private static void failedLoadsCanRetry() {
        AtomicInteger reads = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("Fixture load failure");
        AssetCache cache = new AssetCache(2, path -> {
            if (reads.incrementAndGet() == 1) {
                throw failure;
            }
            return new byte[]{5};
        });
        try {
            cache.get("file");
            throw new AssertionError("Loader failure must propagate");
        } catch (IllegalStateException actual) {
            check(actual == failure, "Loader failure preserves its original exception");
        }
        check(cache.get("file")[0] == 5 && cache.get("file")[0] == 5 && reads.get() == 2,
                "Failed load releases the in-flight entry so a later request can retry");
    }

    private static void fileIOReadsCompleteFilesAndReloads() throws Exception {
        Path file = Files.createTempFile("asset-cache-test-", ".bin");
        try {
            byte[] original = new byte[64 * 1024 + 17];
            for (int i = 0; i < original.length; i++) {
                original[i] = (byte) (i * 31);
            }
            Files.write(file, original);
            FileIO.clearCache();
            check(Arrays.equals(FileIO.readFile(file.toString()), original),
                    "Uncached reads return all bytes including the final partial block");
            byte[] cached = FileIO.readFileCached(file.toString());
            check(Arrays.equals(cached, original), "FileIO cached API returns the actual file contents");
            Files.write(file, new byte[]{7, 8});
            check(Arrays.equals(FileIO.readFile(file.toString()), new byte[]{7, 8}),
                    "Uncached API reads file edits immediately");
            check(FileIO.readFileCached(file.toString()) == cached,
                    "Static assets reuse cached data until explicitly cleared");
            FileIO.clearCache();
            check(Arrays.equals(FileIO.readFileCached(file.toString()), new byte[]{7, 8}),
                    "Explicit cache clear reloads changed assets");
            Files.delete(file);
            FileIO.clearCache();
            check(FileIO.readFile(file.toString()) == null && FileIO.readFileCached(file.toString()) == null,
                    "Both file APIs preserve null for a missing file");
            check(FileIO.readFile("invalid\u0000path") == null
                    && FileIO.readFileCached("invalid\u0000path") == null,
                    "Invalid file paths retain the null-on-read-failure behavior");
        } finally {
            FileIO.clearCache();
            Files.deleteIfExists(file);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for concurrent cache fixture");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for cache fixture", e);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
