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
    @CreatedDate
    LocalDateTime createdDate,
    @LastModifiedDate
    LocalDateTime updatedDate
) {
    public static Order create(Long memberId, Long totalPrice, Long deliveryId) {
        return new Order(null, totalPrice, OrderStatus.ORDERED, memberId, deliveryId, null, null);
    }
}
