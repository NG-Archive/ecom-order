package site.ng_archive.ecom_order.domain.requester;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import site.ng_archive.ecom_common.webclient.WebClientErrorHandler;
import site.ng_archive.ecom_order.domain.dto.ProductResponse;

@Component
public class ProductRequester {

    private final WebClient webClient;

    public ProductRequester(@Qualifier("productClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<ProductResponse> getProduct(Long productId) {
        return webClient.get()
            .uri("/product/{id}", productId)
            .retrieve()
            .onStatus(HttpStatusCode::isError, WebClientErrorHandler::handle)
            .bodyToMono(ProductResponse.class);
    }

}
