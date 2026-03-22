package site.ng_archive.ecom_order.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import site.ng_archive.ecom_common.auth.UserContext;
import site.ng_archive.ecom_common.auth.exception.ForbiddenException;
import site.ng_archive.ecom_common.handler.EntityNotFoundException;
import site.ng_archive.ecom_order.domain.dto.CreateOrderCommand;
import site.ng_archive.ecom_order.domain.dto.OrderListResponse;
import site.ng_archive.ecom_order.domain.dto.OrderResponse;
import site.ng_archive.ecom_order.domain.requester.MemberRequester;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRequester memberRequester;

    public Flux<OrderListResponse> readAllOrders(UserContext user, long offset, int size) {
        return memberRequester.getMember(user.id())
            .filter(member -> "NORMAL".equals(member.status()))
            .switchIfEmpty(Mono.defer(() -> Mono.error(new ForbiddenException("member.status.invalid"))))
            .flatMapMany(member -> orderRepository.findByAll(offset, size, member.id()))
            .map(OrderListResponse::from);
    }

    public Mono<OrderResponse> createOrder(CreateOrderCommand command) {
        return orderRepository.save(command.toEntity())
            .map(OrderResponse::from);
    }

}
