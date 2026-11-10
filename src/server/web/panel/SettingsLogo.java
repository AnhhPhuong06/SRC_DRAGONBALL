package server.web.panel;

import com.sun.net.httpserver.HttpExchange;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import static server.web.panel.PanelSupport.*;

/** Multipart image upload, with decoding before any bytes are persisted. */
final class SettingsLogo {
    private static final int MAX_IMAGE = 2 * 1024 * 1024;
    private static final Pattern BOUNDARY = Pattern.compile("^multipart/form-data\\s*;\\s*boundary=(?:\"([^\"]+)\"|([^;\\s]+))\\s*$", Pattern.CASE_INSENSITIVE);
    private SettingsLogo() {}

    static void upload(HttpExchange exchange) throws IOException {
        String type = exchange.getRequestHeaders().getFirst("Content-Type");
        Matcher boundary = BOUNDARY.matcher(type == null ? "" : type);
        if (!boundary.matches()) throw error(415, "Yêu cầu multipart/form-data với một ảnh file");
        String value = boundary.group(1) == null ? boundary.group(2) : boundary.group(1);
        if (!value.matches("[0-9A-Za-z'()+_,./:=?-]{1,70}")) throw error(400, "Multipart boundary không hợp lệ");
        byte[] body = readBytes(exchange, MAX_IMAGE + 8192);
        byte[] opening = ("--" + value + "\r\n").getBytes(StandardCharsets.US_ASCII);
        if (!starts(body, opening, 0)) throw error(400, "Multipart không hợp lệ");
        int headerEnd = index(body, "\r\n\r\n".getBytes(StandardCharsets.US_ASCII), opening.length);
        if (headerEnd < 0 || headerEnd - opening.length > 4096) throw error(400, "Multipart header không hợp lệ");
        String headers = new String(body, opening.length, headerEnd - opening.length, StandardCharsets.ISO_8859_1);
        String declaredType = null;
        boolean filePart = false;
        for (String header : headers.split("\r\n")) {
            int colon = header.indexOf(':');
            if (colon <= 0) throw error(400, "Multipart header không hợp lệ");
            String key = header.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String headerValue = header.substring(colon + 1).trim();
            if ("content-disposition".equals(key)) {
                if (filePart || !headerValue.toLowerCase(Locale.ROOT).startsWith("form-data;")
                        || !Pattern.compile("(?:^|;)\\s*name=\"file\"(?:;|$)").matcher(headerValue).find()) {
                    throw error(400, "Chỉ chấp nhận một trường multipart tên file");
                }
                filePart = true;
            } else if ("content-type".equals(key)) {
                if (declaredType != null) throw error(400, "Multipart header trùng lặp");
                declaredType = headerValue.toLowerCase(Locale.ROOT);
            }
        }
        if (!filePart) throw error(400, "Thiếu trường file");
        byte[] closing = ("\r\n--" + value + "--").getBytes(StandardCharsets.US_ASCII);
        int end = index(body, closing, headerEnd + 4);
        int suffix = end + closing.length;
        if (end < 0 || !(suffix == body.length || suffix + 2 == body.length
                && body[suffix] == '\r' && body[suffix + 1] == '\n')) {
            throw error(400, "Chỉ chấp nhận một ảnh, multipart phải kết thúc đúng");
        }
        byte[] image = Arrays.copyOfRange(body, headerEnd + 4, end);
        if (image.length == 0 || image.length > MAX_IMAGE) throw error(413, "Ảnh phải có kích thước từ 1 byte đến 2MB");
        String actualType = mime(image);
        if (actualType == null || !actualType.equals(declaredType)) throw error(415, "Chỉ nhận ảnh PNG/JPEG/WebP, kiểu MIME phải khớp nội dung");
        PanelFiles.write("logo.bin", decode(image, actualType));
    }

    private static byte[] decode(byte[] bytes, String type) throws IOException {
        if ("image/webp".equals(type)) {
            long size = Integer.toUnsignedLong((bytes[4] & 255) | (bytes[5] & 255) << 8
                    | (bytes[6] & 255) << 16 | (bytes[7] & 255) << 24);
            if (size + 8 != bytes.length) throw error(400, "Ảnh WebP bị cắt hoặc sai container");
        }
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw error(415, "image/webp".equals(type)
                    ? "Java hiện chưa có bộ giải mã WebP; hãy dùng PNG/JPEG" : "Không giải mã được ảnh");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 4096 || height > 4096 || (long) width * height > 16000000) {
                    throw error(413, "Logo tối đa 4096px mỗi chiều và 16 triệu điểm ảnh");
                }
                BufferedImage image = reader.read(0);
                if (image == null) throw error(400, "Ảnh không hợp lệ");
                if ("image/webp".equals(type)) return bytes;
                ByteArrayOutputStream normalized = new ByteArrayOutputStream();
                if (!ImageIO.write(image, "image/png".equals(type) ? "png" : "jpeg", normalized)) {
                    throw error(415, "Không thể chuẩn hóa ảnh");
                }
                if (normalized.size() > MAX_IMAGE) throw error(413, "Ảnh sau chuẩn hóa vượt quá 2MB");
                return normalized.toByteArray();
            } finally { reader.dispose(); }
        } catch (PanelException e) { throw e; }
        catch (IOException | RuntimeException e) { throw error(400, "Nội dung ảnh không hợp lệ"); }
    }

    static void serve(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET", "HEAD");
        byte[] bytes = PanelFiles.read("logo.bin", MAX_IMAGE);
        if (bytes == null) throw error(404, "Chưa có logo");
        String type = mime(bytes);
        if (type == null) throw error(500, "Logo lưu không hợp lệ");
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Cache-Control", "private, no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Content-Security-Policy", "default-src 'none'; sandbox");
        exchange.getResponseHeaders().set("Content-Disposition", "inline; filename=\"logo." + ("image/png".equals(type) ? "png" : "image/jpeg".equals(type) ? "jpg" : "webp") + "\"");
        if ("HEAD".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Content-Length", Integer.toString(bytes.length));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        } else {
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        }
    }

    private static String mime(byte[] bytes) {
        if (starts(bytes, new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}, 0)) return "image/png";
        if (starts(bytes, new byte[] {(byte) 255, (byte) 216, (byte) 255}, 0)) return "image/jpeg";
        if (bytes.length >= 12 && starts(bytes, "RIFF".getBytes(StandardCharsets.US_ASCII), 0)
                && starts(bytes, "WEBP".getBytes(StandardCharsets.US_ASCII), 8)) return "image/webp";
        return null;
    }

    private static boolean starts(byte[] bytes, byte[] part, int offset) {
        if (offset < 0 || offset + part.length > bytes.length) return false;
        for (int i = 0; i < part.length; i++) if (bytes[offset + i] != part[i]) return false;
        return true;
    }
    private static int index(byte[] bytes, byte[] part, int offset) {
        for (int i = offset; i <= bytes.length - part.length; i++) if (starts(bytes, part, i)) return i;
        return -1;
    }
}
