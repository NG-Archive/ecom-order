package site.ng_archive.ecom_order.global.error;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ErrorMessageUtils {

    private final MessageSource messageSource;
    private static final String EXCEPTION_ERROR_CODE = "error";

    private String getErrorCode(Exception e) {
        String errorCode = e.getMessage();
        try {
            messageSource.getMessage(errorCode, null, Locale.KOREA);
        } catch (Exception ex) {
            return EXCEPTION_ERROR_CODE;
        }
        return errorCode;
    }

    private String getErrorMessage(String errorCode) {
        try {
            return messageSource.getMessage(errorCode, null, Locale.KOREA);
        } catch (Exception ex) {
            return messageSource.getMessage(EXCEPTION_ERROR_CODE, null, Locale.KOREA);
        }
    }

    private String getErrorMessage(String errorCode, Object[] args) {
        Object[] reversed = null;

        if (args != null && args.length > 1) {
            Object[] copied = Arrays.copyOfRange(args, 1, args.length);
            reversed = Arrays.asList(copied).reversed().toArray();
        }
        try {
            return messageSource.getMessage(errorCode, reversed, Locale.KOREA);
        } catch (Exception ex) {
            return messageSource.getMessage(EXCEPTION_ERROR_CODE, null, Locale.KOREA);
        }
    }

    public ErrorResponse getErrorResult(Exception e) {
        String code = getErrorCode(e);
        String message = getErrorMessage(code);
        return new ErrorResponse(code, message);
    }

    public ErrorResponse getErrorResult(String errorCode) {
        return new ErrorResponse(errorCode, getErrorMessage(errorCode));
    }

    public ErrorResponse getErrorResult(String errorCode, Object[] args) {
        return new ErrorResponse(errorCode, getErrorMessage(errorCode, args));
    }

}
