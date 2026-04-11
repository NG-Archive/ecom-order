package site.ng_archive.ecom_order.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
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
            .switchIfEmpty(Mono.error(()-> new EntityNotFoundException("member.notfound")))
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
                   .switchIfEmpty(Flux.error(() -> new EntityNotFoundException("orderitem.notfound")))
                   .map(OrderItemResponse::from)
                   .collectList();
               Mono<DeliveryInfoResponse> deliveryInfoMono = memberRequester.getDeliveryInfo(order.memberId(), order.deliveryId())
                   .switchIfEmpty(Mono.error(() -> new EntityNotFoundException("deliveryInfo.notfound")));

               return Mono.zip(orderItemsMono, deliveryInfoMono, (orderItems, delivery) ->
                   OrderDetailResponse.of(order, orderItems, delivery)
               );
            });
    }

    public Mono<OrderResponse> createOrder(CreateOrderCommand command, String orderToken) {
        return createOrderIfNotExists(command, orderToken)
            .flatMap(initialOrder ->
                prepareOrderContext(command)
                    .flatMap(ctx -> processOrderPersistence(initialOrder, ctx, command.orderItem().quantity()))
                    .flatMap(this::processStockDeduction)
                    .onErrorResume(e -> updateOrderStatus(initialOrder.id(), OrderStatus.FAILED)
                        .then(Mono.error(e)))
            );
    }

    private Mono<Order> createOrderIfNotExists(CreateOrderCommand command, String orderToken) {
        Order order = Order.createInitial(command.memberId(), command.deliveryId(), orderToken);

        return orderRepository.save(order)
            .onErrorMap(DuplicateKeyException.class, e -> new IllegalStateException("order.already.exists"));
    }

    private Mono<OrderContext> prepareOrderContext(CreateOrderCommand command) {
        CreateOrderItemCommand itemCommand = command.orderItem();

        return Mono.zip(
                productRequester.getProduct(itemCommand.productId())
                    .switchIfEmpty(Mono.error(() -> new EntityNotFoundException("product.notfound"))),
                stockRequester.getStock(itemCommand.productId())
                    .switchIfEmpty(Mono.error(() -> new EntityNotFoundException("stock.notfound"))),
                memberRequester.getDeliveryInfo(command.memberId(), command.deliveryId())
                    .switchIfEmpty(Mono.error(() -> new EntityNotFoundException("deliveryInfo.notfound")))
            )
            .map(t -> new OrderContext(t.getT1(), t.getT2(), t.getT3()))
            .filter(ctx -> ctx.hasEnoughStock(itemCommand.quantity()))
            .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("stock.insufficient")));
    }

    private Mono<OrderResponse> processOrderPersistence(Order order, OrderContext ctx, Long quantity) {
        long totalPrice = quantity * ctx.product().price();
        Order updatedOrder = order.withDetails(totalPrice);

        return orderRepository.save(updatedOrder)
            .flatMap(savedOrder -> {
                OrderItem orderItem = OrderItem.create(
                    savedOrder.id(), ctx.product().id(), ctx.product().name(),
                    ctx.product().price(), quantity);
                return orderItemRepository.save(orderItem)
                    .map(savedOrderItem -> OrderResponse.of(savedOrder, savedOrderItem, ctx.delivery().address()));
            })
            .as(transactionalOperator::transactional);
    }

    private Mono<OrderResponse> processStockDeduction(OrderResponse response) {
        return stockRequester.deductStock(
                response.orderItem().productId(),
                response.id(),
                response.orderItem().productQuantity()
            )
            .then(Mono.defer(() -> updateOrderStatus(response.id(), OrderStatus.COMPLETED)))
            .thenReturn(response.withStatus(OrderStatus.COMPLETED));
    }

    private Mono<Void> updateOrderStatus(Long orderId, OrderStatus status) {
        return orderRepository.findById(orderId)
            .map(order -> order.withStatus(status))
            .flatMap(orderRepository::save)
            .then();
    }

}
