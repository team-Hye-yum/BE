package site.dataon.hyeyum.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OpenAiApiException.class)
    public ProblemDetail handleOpenAiApiException(OpenAiApiException exception) {
        HttpStatus status = resolveStatus(exception);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, resolveDetail(exception));
        problemDetail.setTitle("OpenAI API request failed");
        problemDetail.setProperty("openAiStatusCode", exception.getStatusCode());
        problemDetail.setProperty("openAiType", exception.getType());
        problemDetail.setProperty("openAiCode", exception.getCode());
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problemDetail.setTitle("Bad request");
        return problemDetail;
    }

    private HttpStatus resolveStatus(OpenAiApiException exception) {
        if ("insufficient_quota".equals(exception.getCode())) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (exception.getStatusCode() == 429) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (exception.getStatusCode() >= 500) {
            return HttpStatus.BAD_GATEWAY;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private String resolveDetail(OpenAiApiException exception) {
        if ("insufficient_quota".equals(exception.getCode())) {
            return "OpenAI API quota is exhausted. Check the OpenAI project billing, limits, or API key project.";
        }
        return exception.getMessage();
    }
}
