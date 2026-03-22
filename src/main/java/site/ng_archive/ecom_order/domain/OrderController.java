package site.ng_archive.ecom_order.domain;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import site.ng_archive.ecom_common.auth.UserContext;
import site.ng_archive.ecom_common.auth.aspect.LoginUser;
import site.ng_archive.ecom_common.auth.aspect.RequireRoles;
import site.ng_archive.ecom_order.domain.dto.OrderListResponse;

@RestController
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @RequireRoles
    @GetMapping("/orders")
    public Flux<OrderListResponse> readAllOrders(
        @LoginUser UserContext user,
        @RequestParam(defaultValue = "0") @Min(0) long offset,
        @RequestParam(defaultValue = "10") @Min(1) int size) {

        return orderService.readAllOrders(user, offset, size);
    }

}
