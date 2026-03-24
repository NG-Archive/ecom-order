package site.ng_archive.ecom_order.domain.dto;

import site.ng_archive.ecom_order.domain.Order;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
    Long id,
    Long totalPrice,
    String status,
    String statusName,
    Long memberId,
    Long deliveryId,
    LocalDateTime updatedDate,
    List<OrderItemResponse> orderItems,
    String address
    ) {
    public static OrderDetailResponse of(Order order, List<OrderItemResponse> orderItems, DeliveryInfoResponse delivery) {
        return new OrderDetailResponse(order.id(),
            order.totalPrice(),
            order.status().name(),
            order.status().getDesc(),
            order.memberId(),
            order.deliveryId(),
            order.updatedDate(),
            orderItems,
            delivery.address());
    }
}
