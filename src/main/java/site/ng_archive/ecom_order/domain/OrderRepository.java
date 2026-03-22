package site.ng_archive.ecom_order.domain;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    @Query("""
        SELECT o.*
        FROM orders o
        JOIN (
            SELECT id FROM orders WHERE member_id = :memberId
            ORDER BY id DESC LIMIT :size OFFSET :offset) temp
        ON o.id = temp.id
        ORDER BY o.id DESC
        """)
    Flux<Order> findByAll(long offset, int size, Long memberId);

}
