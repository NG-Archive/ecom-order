package site.ng_archive.ecom_order.domain.dto;

import site.ng_archive.ecom_order.domain.Order;
import site.ng_archive.ecom_order.domain.OrderStatus;

import java.time.LocalDateTime;

public record OrderResponse(
   Long id,
   Long totalPrice,
   OrderStatus status,
   Long memberId,
   Long deliveryId,
   LocalDateTime createdDate
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.id(), order.totalPrice(), order.status(), order.memberId(), order.deliveryId(), order.createdDate());
    }
}
