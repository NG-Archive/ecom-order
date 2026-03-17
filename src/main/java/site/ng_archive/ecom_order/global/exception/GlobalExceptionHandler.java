package site.ng_archive.ecom_order.global.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import site.ng_archive.ecom_order.global.error.ErrorMessageUtils;
import site.ng_archive.ecom_order.global.error.ErrorResponse;

import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorMessageUtils errorMessageUtils;

    @ExceptionHandler(EntityNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleEntityNotFoundException(EntityNotFoundException ex) {
        return Mono.just(ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorMessageUtils.getErrorResult(ex)));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebExchangeBindException(WebExchangeBindException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();

        if (fieldErrors.isEmpty()) {
            return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorMessageUtils.getErrorResult("error.input.unknown")));
        }

        FieldError error = fieldErrors.getFirst();

        return Mono.just(ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorMessageUtils.getErrorResult(error.getDefaultMessage(), error.getArguments())));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleServerWebInputException(ServerWebInputException ex) {
        return Mono.just(ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorMessageUtils.getErrorResult("error.input.unknown")));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneralException(Exception ex) {
        log.error("handleGeneralException: ", ex);

        String errorCode = "error.internal.server";
        return Mono.just(ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorMessageUtils.getErrorResult(errorCode)));
    }

}
