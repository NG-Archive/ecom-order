package site.ng_archive.ecom_order.domain.dto;

public record DeliveryInfoResponse(
    Long id,
    Long memberId,
    String address
) {
}
