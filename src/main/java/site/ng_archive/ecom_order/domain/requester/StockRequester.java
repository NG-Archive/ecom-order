package site.ng_archive.ecom_order.domain.requester;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import site.ng_archive.ecom_common.webclient.WebClientErrorHandler;
import site.ng_archive.ecom_order.domain.dto.DeductStockRequest;
import site.ng_archive.ecom_order.domain.dto.StockResponse;

@Component
public class StockRequester {

    private final WebClient webClient;

    public StockRequester(@Qualifier("stockClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<StockResponse> getStock(Long productId) {
        return webClient.get()
            .uri("/{productId}/stock", productId)
            .retrieve()
            .onStatus(HttpStatusCode::isError, WebClientErrorHandler::handle)
            .bodyToMono(StockResponse.class);
    }

    public Mono<Void> deductStock(Long productId, Long orderId, Long quantity) {
        DeductStockRequest request = new DeductStockRequest(productId, orderId, quantity);

        return webClient.patch()
            .uri("/{productId}/stock/deduct", productId)
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatusCode::isError, WebClientErrorHandler::handle)
            .bodyToMono(Void.class);
    }

}
