package site.ng_archive.ecom_order.domain.dto;

public record CreateOrderCommand(
    Long memberId,
    Long deliveryId,
    CreateOrderItemCommand orderItem
) {
}
