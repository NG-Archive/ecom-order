package site.ng_archive.ecom_order.domain.dto;

public record OrderContext(
    ProductResponse product,
    StockResponse stock,
    DeliveryInfoResponse delivery
) {
    public boolean hasEnoughStock(Long quantity) {
        return stock().quantity() >= quantity;
    }
}
