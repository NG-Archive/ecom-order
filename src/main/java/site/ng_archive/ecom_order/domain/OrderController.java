package site.ng_archive.ecom_order.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.ng_archive.ecom_common.auth.UserContext;
import site.ng_archive.ecom_common.auth.aspect.LoginUser;
import site.ng_archive.ecom_common.auth.aspect.RequireRoles;
import site.ng_archive.ecom_order.domain.dto.*;

import java.util.UUID;

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
        return orderService.readAllOrders(user.id(), offset, size);
    }

    @RequireRoles
    @GetMapping("/order/{id}")
    public Mono<OrderDetailResponse> readOrder(
        @LoginUser UserContext user,
        @PathVariable Long id) {
        return orderService.readOrder(user.id(), id);
    }

    @RequireRoles
    @PostMapping("/order/token")
    public Mono<OrderTokenResponse> generateOrderToken() {
        return Mono.fromSupplier(() -> new OrderTokenResponse(UUID.randomUUID().toString()));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequireRoles
    @PostMapping("/order")
    public Mono<OrderResponse> createOrder(
        @LoginUser UserContext user,
        @RequestHeader("X-Order-Token") @NotBlank @Size(max = 50) String orderToken,
        @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request.toCommand(user.id()), orderToken);
    }

}
