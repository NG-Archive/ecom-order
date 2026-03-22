package site.ng_archive.ecom_order.domain;

import org.springframework.data.annotation.Id;
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
    LocalDateTime createdDate,
    LocalDateTime updatedDate
) {
}
