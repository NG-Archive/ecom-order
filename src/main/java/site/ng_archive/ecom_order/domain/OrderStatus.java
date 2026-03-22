package site.ng_archive.ecom_order.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    ORDERED("주문 완료"),
    SHIPPING("배송 중"),
    DELIVERED("배송 완료"),
    CANCELED("주문 취소");

    private final String desc;
}
