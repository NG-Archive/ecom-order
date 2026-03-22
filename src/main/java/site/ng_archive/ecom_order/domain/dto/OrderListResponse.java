package site.ng_archive.ecom_order.domain.dto;

import site.ng_archive.ecom_order.domain.Order;

import java.time.LocalDateTime;

public record OrderListResponse(
    Long id,
    Long totalPrice,
    String status,
    String statusName,
    LocalDateTime createdDate
) {
    public static OrderListResponse from(Order order) {
        return new OrderListResponse(
            order.id(),
            order.totalPrice(),
            order.status().name(),
            order.status().getDesc(),
            order.createdDate());
    }
}
