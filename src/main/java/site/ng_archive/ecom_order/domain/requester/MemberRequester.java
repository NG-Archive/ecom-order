package site.ng_archive.ecom_order.domain.requester;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import site.ng_archive.ecom_common.webclient.WebClientErrorHandler;
import site.ng_archive.ecom_order.domain.dto.DeliveryInfoResponse;
import site.ng_archive.ecom_order.domain.dto.MemberResponse;

@Component
public class MemberRequester {

    private final WebClient webClient;

    public MemberRequester(@Qualifier("memberClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<MemberResponse> getMember(Long memberId) {
        return webClient.get()
            .uri("/member/{id}", memberId)
            .retrieve()
            .onStatus(HttpStatusCode::isError, WebClientErrorHandler::handle)
            .bodyToMono(MemberResponse.class);
    }

    public Mono<DeliveryInfoResponse> getDeliveryInfo(Long memberId, Long deliveryId) {
        return webClient.get()
            .uri("/{memberId}/delivery-info/{deliveryId}", memberId, deliveryId)
            .retrieve()
            .onStatus(HttpStatusCode::isError, WebClientErrorHandler::handle)
            .bodyToMono(DeliveryInfoResponse.class);
    }

}
