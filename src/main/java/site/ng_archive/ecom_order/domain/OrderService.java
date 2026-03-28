package site.ng_archive.ecom_order.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.ng_archive.ecom_common.auth.exception.ForbiddenException;
import site.ng_archive.ecom_common.handler.EntityNotFoundException;
import site.ng_archive.ecom_order.domain.dto.*;
import site.ng_archive.ecom_order.domain.requester.MemberRequester;
import site.ng_archive.ecom_order.domain.requester.ProductRequester;
import site.ng_archive.ecom_order.domain.requester.StockRequester;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private final MemberRequester memberRequester;
    private final ProductRequester productRequester;
    private final StockRequester stockRequester;

    private final TransactionalOperator transactionalOperator;

    public Flux<OrderListResponse> readAllOrders(Long memberId, long offset, int size) {
        return memberRequester.getMember(memberId)
            .filter(member -> "NORMAL".equals(member.status()))
            .switchIfEmpty(Mono.error(() -> new ForbiddenException("member.status.invalid")))
            .flatMapMany(member -> orderRepository.findByAll(offset, size, member.id()))
            .map(OrderListResponse::from);
    }

    public Mono<OrderDetailResponse> readOrder(Long memberId, Long orderId) {
       return orderRepository.findById(orderId)
           .switchIfEmpty(Mono.error(() -> new EntityNotFoundException("order.notfound")))
           .filter(order -> memberId.equals(order.memberId()))
           .switchIfEmpty(Mono.error(() -> new ForbiddenException("auth.forbidden")))
           .flatMap(order -> {
               Mono<List<OrderItemResponse>> orderItemsMono = orderItemRepository.findByOrderId(orderId)
                   .switchIfEmpty(Flux.defer(() -> Flux.error(new EntityNotFoundException("orderitem.notfound"))))
                   .map(OrderItemResponse::from)
                   .collectList();
               Mono<DeliveryInfoResponse> deliveryInfoMono = memberRequester.getDeliveryInfo(order.memberId(), order.deliveryId());

               return Mono.zip(orderItemsMono, deliveryInfoMono, (orderItems, delivery) ->
                   OrderDetailResponse.of(order, orderItems, delivery)
               );
            });
    }

    public Mono<OrderResponse> createOrder(CreateOrderCommand command, String orderToken) {
        return prepareOrderContext(command)
            .flatMap(ctx -> saveOrderAndItemsPending(command, ctx, orderToken).as(transactionalOperator::transactional))
            .flatMap(orderResponse -> deductStockAndOrderComplete(command, orderResponse));
    }

    private Mono<OrderContext> prepareOrderContext(CreateOrderCommand command) {
        CreateOrderItemCommand itemCommand = command.orderItem();

        return Mono.zip(productRequester.getProduct(itemCommand.productId()),
                stockRequester.getStock(itemCommand.productId()),
                memberRequester.getDeliveryInfo(command.memberId(), command.deliveryId())
            )
            .map(t -> new OrderContext(t.getT1(), t.getT2(), t.getT3()))
            .filter(ctx -> ctx.stock().quantity() >= itemCommand.quantity())
            .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("stock.insufficient")));
    }

    private Mono<OrderResponse> saveOrderAndItemsPending(CreateOrderCommand command, OrderContext ctx, String orderToken) {
        Order order = Order.createPending(
            command.memberId(),
            command.calculateTotalPrice(ctx.product().price()),
            command.deliveryId(),
            orderToken
        );

        return orderRepository.save(order)
            .onErrorMap(e -> new IllegalStateException("order.status.completed"))
            .flatMap(savedOrder -> {
                OrderItem orderItem = OrderItem.create(savedOrder.id(), ctx.product().id(), ctx.product().name(), ctx.product().price());
                return orderItemRepository.save(orderItem)
                    .map(savedOrderItem -> OrderResponse.of(savedOrder, savedOrderItem, ctx.delivery().address()));
            });
    }

    private Mono<OrderResponse> deductStockAndOrderComplete(CreateOrderCommand command, OrderResponse orderResponse) {
        return stockRequester.deductStock(command.orderItem().productId(),
                orderResponse.id(),
                command.orderItem().quantity()
            )
            .then(updateOrderStatus(orderResponse.id(), OrderStatus.COMPLETED))
            .thenReturn(orderResponse.withStatus(OrderStatus.COMPLETED))
            .onErrorResume(e ->
                updateOrderStatus(orderResponse.id(), OrderStatus.FAILED)
                    .then(Mono.error(() -> new IllegalArgumentException("stock.insufficient")))
            );
    }

    private Mono<Void> updateOrderStatus(Long orderId, OrderStatus status) {
        return orderRepository.findById(orderId)
            .map(order -> order.withStatus(status))
            .flatMap(orderRepository::save)
            .then();
    }

}
