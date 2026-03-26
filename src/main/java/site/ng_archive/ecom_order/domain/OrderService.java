package site.ng_archive.ecom_order.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public Flux<OrderListResponse> readAllOrders(Long memberId, long offset, int size) {
        return memberRequester.getMember(memberId)
            .filter(member -> "NORMAL".equals(member.status()))
            .switchIfEmpty(Mono.defer(() -> Mono.error(new ForbiddenException("member.status.invalid"))))
            .flatMapMany(member -> orderRepository.findByAll(offset, size, member.id()))
            .map(OrderListResponse::from);
    }

    public Mono<OrderDetailResponse> readOrder(Long memberId, Long orderId) {
       return orderRepository.findById(orderId)
           .switchIfEmpty(Mono.defer(() -> Mono.error(new EntityNotFoundException("order.notfound"))))
           .filter(order -> memberId.equals(order.memberId()))
           .switchIfEmpty(Mono.defer(() -> Mono.error(new ForbiddenException("auth.forbidden"))))
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

    @Transactional
    public Mono<OrderResponse> createOrder(CreateOrderCommand command) {
        return prepareOrderContext(command)
            .flatMap(ctx -> saveOrderAndItems(command, ctx))
            .flatMap(orderResponse -> deductStock(command, orderResponse));
    }

    private Mono<OrderContext> prepareOrderContext(CreateOrderCommand command) {
        CreateOrderItemCommand itemCommand = command.orderItem();

        return Mono.zip(
                productRequester.getProduct(itemCommand.productId()),
                stockRequester.getStock(itemCommand.productId()),
                memberRequester.getDeliveryInfo(command.memberId(), command.deliveryId())
            ).map(t -> new OrderContext(t.getT1(), t.getT2(), t.getT3()))
            .filter(ctx -> ctx.stock().quantity() >= itemCommand.quantity())
            .switchIfEmpty(Mono.defer(() -> Mono.error(new IllegalArgumentException("stock.insufficient"))));
    }

    private Mono<OrderResponse> saveOrderAndItems(CreateOrderCommand command, OrderContext ctx) {
        Order order = Order.create(
            command.memberId(),
            command.calculateTotalPrice(ctx.product().price()),
            command.deliveryId());

        return orderRepository.save(order)
            .flatMap(savedOrder -> {
                OrderItem orderItem = OrderItem.create(savedOrder.id(), ctx.product().id(), ctx.product().name(), ctx.product().price());
                return orderItemRepository.save(orderItem)
                    .map(savedOrderItem -> OrderResponse.of(savedOrder, savedOrderItem, ctx.delivery().address()));
            });
    }

    private Mono<OrderResponse> deductStock(CreateOrderCommand command, OrderResponse orderResponse) {
        return stockRequester.deductStock(
            command.orderItem().productId(),
            orderResponse.id(),
            command.orderItem().quantity()
        ).thenReturn(orderResponse);
    }

}
