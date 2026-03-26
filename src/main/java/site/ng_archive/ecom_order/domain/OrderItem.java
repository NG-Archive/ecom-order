package site.ng_archive.ecom_order.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("order_item")
public record OrderItem(
    @Id
    Long id,
    Long orderId,
    Long productId,
    String productName,
    Long productPrice
) {
    public static OrderItem create(Long orderId, Long productId, String productName, Long productPrice) {
        return new OrderItem(null, orderId, productId, productName, productPrice);
    }
}
