package site.ng_archive.ecom_order.global.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String code) {
        super(code);
    }

}
