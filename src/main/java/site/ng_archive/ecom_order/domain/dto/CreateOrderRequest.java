package site.ng_archive.ecom_order.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
    @NotNull(message = "delivery.id.null")
    Long deliveryId,

    @Valid
    @NotNull(message = "orderitem.null")
    OrderItemRequest orderItem
) {
    public record OrderItemRequest(
        @NotNull(message = "product.id.null")
        Long productId,

        @NotNull(message = "product.quantity.null")
        @Min(value = 1, message = "product.quantity.min")
        Long quantity
    ) {}

    public CreateOrderCommand toCommand(Long memberId) {
        return new CreateOrderCommand(
            memberId,
            deliveryId,
            new CreateOrderItemCommand(orderItem().productId, orderItem().quantity())
        );
    }
}
