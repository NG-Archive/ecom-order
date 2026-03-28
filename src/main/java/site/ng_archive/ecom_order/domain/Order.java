package site.ng_archive.ecom_order.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("orders")
public record Order(
    @Id
    Long id,
    Long totalPrice,
    OrderStatus status,
    Long memberId,
    Long deliveryId,
    String orderToken,
    @CreatedDate
    LocalDateTime createdDate,
    @LastModifiedDate
    LocalDateTime updatedDate
) {
    public static Order createInitial(Long memberId, Long deliveryId, String orderToken) {
        return new Order(null, 0L, OrderStatus.PENDING, memberId, deliveryId, orderToken, null, null);
    }

    public Order withDetails(Long totalPrice) {
        return new Order(id, totalPrice, status, memberId, deliveryId, orderToken, createdDate, updatedDate);
    }

    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, totalPrice, newStatus, memberId, deliveryId, orderToken, createdDate, updatedDate);
    }
}
