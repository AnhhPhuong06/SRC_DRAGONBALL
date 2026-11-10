package services;

/** Release versions are independent of the existing 2.4.0 packet layout. */
public final class ClientVersionPolicy {
    public static final String REQUIRED_VERSION = "0.0.3";
    public static final int PROTOCOL_VERSION = 240;
    public static final String DENIED_MESSAGE = "Máy chủ chỉ hỗ trợ phiên bản 0.0.3. Vui lòng cập nhật game tại https://nroha.fun";

    private ClientVersionPolicy() {}

    public static boolean isAllowed(String version) {
        return REQUIRED_VERSION.equals(version);
    }
}
