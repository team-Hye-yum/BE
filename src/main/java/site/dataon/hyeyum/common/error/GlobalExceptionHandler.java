package site.dataon.hyeyum.common.error;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OpenAiApiException.class)
    public ProblemDetail handleOpenAiApiException(OpenAiApiException exception) {
        HttpStatus status = resolveStatus(exception);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, resolveDetail(exception));
        problemDetail.setTitle("OpenAI API request failed");
        problemDetail.setProperty("code", "OPENAI_API_FAILED");
        problemDetail.setProperty("openAiStatusCode", exception.getStatusCode());
        problemDetail.setProperty("openAiType", exception.getType());
        problemDetail.setProperty("openAiCode", exception.getCode());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        ProblemDetail problemDetail = badRequest("Validation failed", "VALIDATION_FAILED", "요청 값이 올바르지 않습니다.");
        List<ApiFieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException exception) {
        ProblemDetail problemDetail = badRequest("Validation failed", "VALIDATION_FAILED", "요청 값이 올바르지 않습니다.");
        List<ApiFieldError> errors = exception.getConstraintViolations().stream()
                .map(violation -> new ApiFieldError(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        MissingServletRequestPartException.class,
        HttpMessageNotReadableException.class
    })
    public ProblemDetail handleMalformedRequestException(Exception exception) {
        return badRequest("Bad request", "BAD_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "업로드 가능한 파일 크기를 초과했습니다.");
        problemDetail.setTitle("Payload too large");
        problemDetail.setProperty("code", "PAYLOAD_TOO_LARGE");
        return problemDetail;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException exception) {
        HttpStatusCode status = exception.getStatusCode();
        String detail = exception.getReason() == null ? "요청을 처리할 수 없습니다." : exception.getReason();
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle("Request failed");
        problemDetail.setProperty("code", "REQUEST_FAILED");
        return problemDetail;
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ProblemDetail handleErrorResponseException(ErrorResponseException exception) {
        ProblemDetail problemDetail = exception.getBody();
        problemDetail.setProperty("code", "REQUEST_FAILED");
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException exception) {
        return badRequest("Bad request", "BAD_REQUEST", exception.getMessage());
    }

    private ProblemDetail badRequest(String title, String code, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("code", code);
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

    private record ApiFieldError(String field, String message) {}
}
