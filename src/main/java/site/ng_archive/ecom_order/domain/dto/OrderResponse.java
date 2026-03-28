package site.ng_archive.ecom_order.domain.dto;

import site.ng_archive.ecom_order.domain.Order;
import site.ng_archive.ecom_order.domain.OrderItem;
import site.ng_archive.ecom_order.domain.OrderStatus;

import java.time.LocalDateTime;

public record OrderResponse(
   Long id,
   Long totalPrice,
   String status,
   String statusName,
   OrderItemResponse orderItem,
   String deliveryAddress,
   LocalDateTime createdDate
) {
    public static OrderResponse of(Order order, OrderItem orderItem, String deliveryAddress) {
        return new OrderResponse(order.id(),
            order.totalPrice(),
            order.status().name(),
            order.status().getDesc(),
            OrderItemResponse.from(orderItem),
            deliveryAddress,
            order.createdDate()
        );
    }

    public OrderResponse withStatus(OrderStatus newStatus) {
        return new OrderResponse(id, totalPrice, newStatus.name(), newStatus.getDesc(), orderItem, deliveryAddress, createdDate);
    }
}
