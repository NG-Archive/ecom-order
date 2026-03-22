package site.ng_archive.ecom_order.domain.dto;

import site.ng_archive.ecom_order.domain.Order;
import site.ng_archive.ecom_order.domain.OrderStatus;

public record CreateOrderCommand(
    Long totalPrice,
    OrderStatus status,
    Long memberId,
    Long deliveryId
) {
    public Order toEntity() {
        return new Order(null, totalPrice, status, memberId, deliveryId, null, null);
    }
}
