package site.ng_archive.ecom_order.domain.dto;

public record DeductStockRequest(
    Long productId,
    Long orderId,
    Long quantity
) {
}
