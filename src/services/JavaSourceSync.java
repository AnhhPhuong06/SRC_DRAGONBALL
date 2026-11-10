package services;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import utils.Logger;

/**
 * Bộ đồng bộ tự động giữa cấu hình hệ thống và mã nguồn Java (.java).
 * Khi chỉnh sửa tỉ lệ/mốc thưởng từ Admin Panel, hệ thống sẽ tự động cập nhật
 * trực tiếp giá trị vào file mã nguồn .java trong thư mục src/ để code luôn khớp với cấu hình mới nhất.
 */
public class JavaSourceSync {

    /**
     * Tìm file mã nguồn Java trên ổ đĩa.
     * Kiểm tra thư mục làm việc hiện tại và các thư mục cha/con tương ứng.
     */
    public static File findSourceFile(String relativePath) {
        String cleanPath = relativePath.replace('\\', '/');
        String[] candidatePaths = {
            cleanPath,
            "../" + cleanPath,
            "../../" + cleanPath,
            System.getProperty("user.dir", ".") + "/" + cleanPath,
            System.getProperty("user.dir", ".") + "/../" + cleanPath
        };
        for (String p : candidatePaths) {
            File f = new File(p);
            if (f.exists() && f.isFile()) {
                return f;
            }
        }
        return null;
    }

    /**
     * Cập nhật giá trị trường số nguyên (int, long) trong mã nguồn Java
     */
    public static String updateIntField(String content, String fieldName, long value) {
        Pattern p = Pattern.compile("(?m)^(\\s*public\\s+static\\s+(?:int|long)\\s+" + Pattern.quote(fieldName) + "\\s*=\\s*)[^;]+(;.*)$");
        Matcher m = p.matcher(content);
        if (m.find()) {
            return m.replaceFirst("$1" + value + "$2");
        }
        return content;
    }

    /**
     * Cập nhật giá trị trường double trong mã nguồn Java
     */
    public static String updateDoubleField(String content, String fieldName, double value) {
        Pattern p = Pattern.compile("(?m)^(\\s*public\\s+static\\s+double\\s+" + Pattern.quote(fieldName) + "\\s*=\\s*)[^;]+(;.*)$");
        Matcher m = p.matcher(content);
        if (m.find()) {
            return m.replaceFirst("$1" + value + "$2");
        }
        return content;
    }

    /**
     * Cập nhật giá trị trường float trong mã nguồn Java
     */
    public static String updateFloatField(String content, String fieldName, float value) {
        Pattern p = Pattern.compile("(?m)^(\\s*public\\s+static\\s+float\\s+" + Pattern.quote(fieldName) + "\\s*=\\s*)[^;]+(;.*)$");
        Matcher m = p.matcher(content);
        if (m.find()) {
            return m.replaceFirst("$1" + value + "f$2");
        }
        return content;
    }

    /**
     * Cập nhật giá trị trường String trong mã nguồn Java
     */
    public static String updateStringField(String content, String fieldName, String value) {
        Pattern p = Pattern.compile("(?m)^(\\s*public\\s+static\\s+String\\s+" + Pattern.quote(fieldName) + "\\s*=\\s*\")(?:\\\\.|[^\"\\\\])*(\";.*)$");
        Matcher m = p.matcher(content);
        if (m.find()) {
            String escaped = (value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\"");
            return m.replaceFirst(Matcher.quoteReplacement(m.group(1) + escaped + m.group(2)));
        }
        return content;
    }

    /**
     * Cập nhật giá trị trường mảng số nguyên int[] { ... } trong mã nguồn Java
     */
    public static String updateIntArrayField(String content, String fieldName, int[] values) {
        Pattern p = Pattern.compile("(?m)^(\\s*public\\s+static\\s+int\\[\\]\\s+" + Pattern.quote(fieldName) + "\\s*=\\s*\\{)[^}]*(\\};.*)$");
        Matcher m = p.matcher(content);
        if (m.find()) {
            StringBuilder sb = new StringBuilder();
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(values[i]);
                }
            }
            return m.replaceFirst(Matcher.quoteReplacement(m.group(1) + sb.toString() + m.group(2)));
        }
        return content;
    }

    /**
     * Cập nhật giá trị trường mảng double[] { ... } trong mã nguồn Java
     */
    public static String updateDoubleArrayField(String content, String fieldName, double[] values) {
        Pattern p = Pattern.compile("(?m)^(\\s*public\\s+static\\s+double\\[\\]\\s+" + Pattern.quote(fieldName) + "\\s*=\\s*\\{)[^}]*(\\};.*)$");
        Matcher m = p.matcher(content);
        if (m.find()) {
            StringBuilder sb = new StringBuilder();
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(values[i]);
                }
            }
            return m.replaceFirst(Matcher.quoteReplacement(m.group(1) + sb.toString() + m.group(2)));
        }
        return content;
    }

    /**
     * Cập nhật giá trị trường mảng float[] { ... } trong mã nguồn Java
     */
    public static String updateFloatArrayField(String content, String fieldName, float[] values) {
        Pattern p = Pattern.compile("(?m)^(\\s*public\\s+static\\s+float\\[\\]\\s+" + Pattern.quote(fieldName) + "\\s*=\\s*\\{)[^}]*(\\};.*)$");
        Matcher m = p.matcher(content);
        if (m.find()) {
            StringBuilder sb = new StringBuilder();
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(values[i]).append("f");
                }
            }
            return m.replaceFirst(Matcher.quoteReplacement(m.group(1) + sb.toString() + m.group(2)));
        }
        return content;
    }
}
