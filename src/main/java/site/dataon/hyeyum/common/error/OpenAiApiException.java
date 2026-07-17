package site.dataon.hyeyum.common.error;

public class OpenAiApiException extends RuntimeException {

    private final int statusCode;
    private final String type;
    private final String code;

    public OpenAiApiException(int statusCode, String message, String type, String code) {
        super(message);
        this.statusCode = statusCode;
        this.type = type;
        this.code = code;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getType() {
        return type;
    }

    public String getCode() {
        return code;
    }
}
