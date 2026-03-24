package site.ng_archive.ecom_order.domain.dto;

import site.ng_archive.ecom_order.domain.OrderItem;

public record CreateOrderItemCommand(
    Long orderId,
    Long productId,
    String productName,
    Long productPrice
) {
    public OrderItem toEntity() {
        return new OrderItem(null, orderId, productId, productName, productPrice);
    }
}
