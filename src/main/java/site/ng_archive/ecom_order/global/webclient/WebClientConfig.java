package site.ng_archive.ecom_order.global.webclient;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import site.ng_archive.ecom_common.auth.UserContext;
import site.ng_archive.ecom_common.auth.token.TokenUtil;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient memberClient(
        WebClient.Builder builder,
        @Value("${app.member-api.url:http://localhost:8081}") String url,
        @Value("${app.member-api.pool-size:100}") Integer poolSize,
        @Value("${app.member-api.pool-acquire-time:3}") Integer acquireTime,
        @Value("${app.member-api.pool-idle-time:30}") Integer idleTime,
        @Value("${app.member-api.pool-max-time:60}") Integer maxTime,
        @Value("${app.member-api.pool-evict-time:30}") Integer evictTime,
        @Value("${app.member-api.time-out:3}") Integer timeout
    ) {
        return createWebClient(
            builder,
            "member-api",
            url,
            poolSize,
            acquireTime,
            idleTime,
            maxTime,
            evictTime,
            timeout
        );
    }

    @Bean
    public WebClient productClient(
        WebClient.Builder builder,
        @Value("${app.product-api.url:http://localhost:8081}") String url,
        @Value("${app.product-api.pool-size:100}") Integer poolSize,
        @Value("${app.product-api.pool-acquire-time:3}") Integer acquireTime,
        @Value("${app.product-api.pool-idle-time:30}") Integer idleTime,
        @Value("${app.product-api.pool-max-time:60}") Integer maxTime,
        @Value("${app.product-api.pool-evict-time:30}") Integer evictTime,
        @Value("${app.product-api.time-out:3}") Integer timeout
    ) {
        return createWebClient(
            builder,
            "product-api",
            url,
            poolSize,
            acquireTime,
            idleTime,
            maxTime,
            evictTime,
            timeout
        );
    }

    @Bean
    public WebClient stockClient(
        WebClient.Builder builder,
        @Value("${app.stock-api.url:http://localhost:8081}") String url,
        @Value("${app.stock-api.pool-size:100}") Integer poolSize,
        @Value("${app.stock-api.pool-acquire-time:3}") Integer acquireTime,
        @Value("${app.stock-api.pool-idle-time:30}") Integer idleTime,
        @Value("${app.stock-api.pool-max-time:60}") Integer maxTime,
        @Value("${app.stock-api.pool-evict-time:30}") Integer evictTime,
        @Value("${app.stock-api.time-out:3}") Integer timeout
    ) {
        return createWebClient(
            builder,
            "stock-api",
            url,
            poolSize,
            acquireTime,
            idleTime,
            maxTime,
            evictTime,
            timeout
        );
    }

    private WebClient createWebClient(
        WebClient.Builder builder,
        String poolName,
        String baseUrl,
        Integer poolSize,
        Integer acquireTime,
        Integer idleSeconds,
        Integer maxTime,
        Integer evictTime,
        Integer timeout) {

        // 1. Connection Pool 설정
        ConnectionProvider provider = ConnectionProvider.builder(poolName)
            .maxConnections(poolSize)
            // 연결을 가져오기 위해 기다리는 시간
            .pendingAcquireTimeout(Duration.ofSeconds(acquireTime))
            // 유휴 연결 유지 시간
            .maxIdleTime(Duration.ofSeconds(idleSeconds))
            // 연결의 총 수명
            .maxLifeTime(Duration.ofSeconds(maxTime))
            // 백그라운드에서 유휴 연결 제거 시간
            .evictInBackground(Duration.ofSeconds(evictTime))
            .build();

        // 2. HttpClient 설정
        HttpClient httpClient = HttpClient.create(provider)
            // 커넥션 타임아웃
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.min(timeout * 1000, 3000))
            // 응답 타임아웃
            .responseTimeout(Duration.ofSeconds(timeout));

        // 3. WebClient 빌드
        return builder.clone()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .filter(addAuthHeader())
            .build();
    }

    private ExchangeFilterFunction addAuthHeader() {
        // 1. request: 현재 나가는 요청 정보 / next: 다음 필터 혹은 네트워크 호출
        return (request, next) -> Mono.deferContextual(ctx -> {
            // 2. Reactor Context에서 userContext 정보 찾기
            return ctx.getOrEmpty("userContext")
                // 3. userContext 정보가 있다면 해당 유저 정보 기반으로 새로운 JWT 토큰 생성
                .map(user -> {
                    UserContext userContext = (UserContext) user;
                    String token = TokenUtil.getSign(userContext);
                    // 리액티브 객체는 불변이므로 기존 request를 복사해서 Authorization 헤더만 추가된 새로운 ClientRequest를 빌드
                    ClientRequest filteredRequest = ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();

                    return next.exchange(filteredRequest);
                })
                // 4. userContext 정보가 없다면 원본 요청(request)을 수정 없이 전달
                .orElseGet(() -> next.exchange(request));
        });
    }

}
