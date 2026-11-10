package utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileIO {

    /**
     * Cache cho tài nguyên tĩnh (icon, mob, effect, tile...). Các file này chỉ đọc và
     * không đổi lúc chạy, nhưng trước đây mỗi yêu cầu của client là một lần mở file
     * ngay trên luồng xử lý gói tin — vào map lạ/đông người là client bắn hàng loạt
     * yêu cầu như vậy. Có trần dung lượng để không phình bộ nhớ vô hạn.
     */
    private static final long CACHE_MAX_BYTES = 128L * 1024 * 1024;
    private static final AssetCache CACHE = new AssetCache(CACHE_MAX_BYTES, FileIO::readFile);

    public static byte[] readFileCached(String url) {
        return CACHE.get(url);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static byte[] readFile(String url) {
        try {
            return Files.readAllBytes(Path.of(url));
        } catch (IOException | InvalidPathException e) {
        }
        return null;
    }

    public static ByteArrayOutputStream loadFile(String url) {
        // try-with-resources: trước đây stream bị rò rỉ khi read() ném IOException
        try (FileInputStream openFileInput = new FileInputStream(url)) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            openFileInput.transferTo(byteArrayOutputStream);
            return byteArrayOutputStream;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void writeFile(String url, byte[] ab) {
        try {
            File f = new File(url);
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            try (FileOutputStream output = new FileOutputStream(url)) {
                output.write(ab);
            }
        } catch (IOException e) {
        }
    }

    public static void writeFile(String url, String content) {
        try {
            File file = new File(url);

            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();

            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file))) {
                writer.write(content);
            }
        } catch (IOException e) {
        }
    }

    public static List<File> getFiles(String folderPath) {
        List<File> fileList = new ArrayList<>();
        File folder = new File(folderPath);

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        fileList.add(file);
                    }
                }
            }
        }

        return fileList;
    }
}
