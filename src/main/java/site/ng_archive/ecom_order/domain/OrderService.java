package site.ng_archive.ecom_order.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.ng_archive.ecom_common.auth.UserContext;
import site.ng_archive.ecom_common.auth.exception.ForbiddenException;
import site.ng_archive.ecom_order.domain.dto.*;
import site.ng_archive.ecom_order.domain.requester.MemberRequester;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberRequester memberRequester;

    public Flux<OrderListResponse> readAllOrders(UserContext user, long offset, int size) {
        return memberRequester.getMember(user.id())
            .filter(member -> "NORMAL".equals(member.status()))
            .switchIfEmpty(Mono.defer(() -> Mono.error(new ForbiddenException("member.status.invalid"))))
            .flatMapMany(member -> orderRepository.findByAll(offset, size, member.id()))
            .map(OrderListResponse::from);
    }

    public Mono<OrderDetailResponse> readOrder(UserContext user, Long id) {
       return orderRepository.findById(id)
            .filter(order -> user.id().equals(order.memberId()))
            .switchIfEmpty(Mono.defer(() -> Mono.error(new ForbiddenException("auth.forbidden"))))
            .flatMap(order -> {
                Mono<List<OrderItemResponse>> orderItemsMono = orderItemRepository.findByOrderId(id)
                    .map(OrderItemResponse::from)
                    .collectList();
                Mono<DeliveryInfoResponse> deliveryInfoMono = memberRequester.getDeliveryInfo(order.memberId(), order.deliveryId());

                return Mono.zip(orderItemsMono, deliveryInfoMono, (orderItems, delivery) ->
                    OrderDetailResponse.of(order, orderItems, delivery)
                );
            });
    }

    public Mono<OrderResponse> createOrder(CreateOrderCommand command) {
        return orderRepository.save(command.toEntity())
            .map(OrderResponse::from);
    }

    public Mono<OrderItemResponse> createOrderItem(CreateOrderItemCommand command) {
        return orderItemRepository.save(command.toEntity())
            .map(OrderItemResponse::from);
    }

}
