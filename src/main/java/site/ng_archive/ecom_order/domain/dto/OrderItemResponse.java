package site.ng_archive.ecom_order.domain.dto;

import site.ng_archive.ecom_order.domain.OrderItem;

public record OrderItemResponse(
    Long id,
    Long productId,
    String productName,
    Long productPrice,
    Long productQuantity
) {
    public static OrderItemResponse from(OrderItem orderItem) {
        return new OrderItemResponse(orderItem.id(),
            orderItem.productId(),
            orderItem.productName(),
            orderItem.productPrice(),
            orderItem.productQuantity()
        );
    }
}
