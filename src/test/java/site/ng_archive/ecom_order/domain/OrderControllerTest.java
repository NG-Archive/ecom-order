package site.ng_archive.ecom_order.domain;

import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import io.restassured.http.ContentType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import site.ng_archive.ecom_common.auth.UserContext;
import site.ng_archive.ecom_common.auth.token.TokenUtil;
import site.ng_archive.ecom_common.error.ErrorResponse;
import site.ng_archive.ecom_order.config.AcceptedTest;
import site.ng_archive.ecom_order.domain.dto.*;
import site.ng_archive.ecom_order.domain.requester.MemberRequester;
import site.ng_archive.ecom_order.domain.requester.ProductRequester;
import site.ng_archive.ecom_order.domain.requester.StockRequester;

import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;

public class OrderControllerTest extends AcceptedTest {

    @MockitoBean
    private MemberRequester memberRequester;
    @MockitoBean
    private ProductRequester productRequester;
    @MockitoBean
    private StockRequester stockRequester;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    private static final Long TEST_MEMBER_ID = 10L;
    private static final String TEST_MEMBER_ROLE = "USER";
    private static final Long TEST_PRODUCT_ID = 100L;
    private static final Long TEST_DELIVERY_ID = 1000L;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll().block();
        orderItemRepository.deleteAll().block();
    }

    @Test
    void 주문목록조회_성공() {
        createTestOrder(TEST_MEMBER_ID, TEST_DELIVERY_ID);
        String token = createTestToken(TEST_MEMBER_ID, TEST_MEMBER_ROLE);
        mockGetMember(TEST_MEMBER_ID, "NORMAL");

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("offset", 0)
            .queryParam("size", 10)
            .consumeWith(document(
                info()
                    .tag("Order")
                    .summary("주문 목록 조회")
                    .description("토큰에 포함된 회원 정보를 기준으로 해당 사용자의 주문 목록을 페이징으로 조회합니다.")
                    .queryParameters(
                        parameterWithName("offset").description("페이지 오프셋").type(SimpleType.INTEGER).defaultValue(0),
                        parameterWithName("size").description("페이지 크기").type(SimpleType.INTEGER).defaultValue(10)
                    )
                    .responseFields(
                        field(OrderListResponse.class, "[].id", "주문 ID"),
                        field(OrderListResponse.class, "[].totalPrice", "주문 최종 금액"),
                        field(OrderListResponse.class, "[].status", "주문 상태"),
                        field(OrderListResponse.class, "[].statusName", "주문 상태 설명"),
                        field(OrderListResponse.class, "[].updatedDate", "최종 수정 시간")
                    )
                    .responseSchema(Schema.schema("OrderList"))
            ))
            .get("/orders")
            .then()
            .status(HttpStatus.OK)
            .contentType(ContentType.JSON)
            .body("size()", greaterThanOrEqualTo(1))
            .body("[0].id", notNullValue())
            .body("[0].totalPrice", greaterThanOrEqualTo(0))
            .body("[0].status", notNullValue())
            .body("[0].statusName", notNullValue())
            .body("[0].updatedDate", notNullValue())
            .log().all();
    }

    @Test
    void 주문목록조회_실패_탈퇴한회원조회() {
        String token = createTestToken(TEST_MEMBER_ID, TEST_MEMBER_ROLE);
        mockGetMember(TEST_MEMBER_ID, "WITHDRAWN");

        ErrorResponse errorResponse = given()
            .header("Authorization", "Bearer " + token)
            .queryParam("offset", 0)
            .queryParam("size", 10)
            .consumeWith(document(
                info()
                    .tag("Order")
                    .summary("주문 목록 조회")
                    .description("토큰에 포함된 회원 정보를 기준으로 해당 사용자의 주문 목록을 페이징으로 조회합니다.")
                    .queryParameters(
                        parameterWithName("offset").description("페이지 오프셋").type(SimpleType.INTEGER).defaultValue(0),
                        parameterWithName("size").description("페이지 크기").type(SimpleType.INTEGER).defaultValue(10)
                    )
                    .responseFields(
                        field(ErrorResponse.class, "errorCode", "오류 코드"),
                        field(ErrorResponse.class, "message", "오류 메시지")
                    )
                    .responseSchema(Schema.schema("ErrorResponse"))
            ))
            .get("/orders")
            .then()
            .status(HttpStatus.FORBIDDEN)
            .log().all()
            .extract().body().as(ErrorResponse.class);

        Assertions.assertThat(errorResponse.errorCode()).isEqualTo("member.status.invalid");
        Assertions.assertThat(errorResponse.message()).isEqualTo("유효하지 않은 회원 상태입니다.");
    }

    @Test
    void 주문상세조회_성공() {
        Order createdOrder = createTestOrder(TEST_MEMBER_ID, TEST_DELIVERY_ID);
        createTestOrderItem(createdOrder.id());

        String token = createTestToken(TEST_MEMBER_ID, TEST_MEMBER_ROLE);
        mockGetDeliveryInfo(TEST_MEMBER_ID, TEST_DELIVERY_ID);

        OrderDetailResponse response = given()
            .header("Authorization", "Bearer " + token)
            .pathParam("id", createdOrder.id())
            .consumeWith(document(
                info()
                    .tag("Order")
                    .summary("주문 상세 조회")
                    .description("주문 ID를 사용하여 상세 정보를 조회합니다.")
                    .pathParameters(
                        parameterWithName("id").description("주문 ID").type(SimpleType.INTEGER)
                    )
                    .responseFields(
                        field(OrderDetailResponse.class, "id", "주문 ID"),
                        field(OrderDetailResponse.class, "totalPrice", "주문 최종 금액"),
                        field(OrderDetailResponse.class, "status", "주문 상태"),
                        field(OrderDetailResponse.class, "statusName", "주문 상태 설명"),
                        field(OrderDetailResponse.class, "memberId", "회원 ID"),
                        field(OrderDetailResponse.class, "deliveryId", "배송지 ID"),
                        field(OrderDetailResponse.class, "updatedDate", "최종 수정 시간"),
                        field(OrderDetailResponse.class, "orderItems", "주문 상품 목록"),
                        field(OrderDetailResponse.class, "orderItems[].id", "주문 상품 ID"),
                        field(OrderDetailResponse.class, "orderItems[].productId", "주문한 상품 ID"),
                        field(OrderDetailResponse.class, "orderItems[].productName", "주문 시 상품 이름"),
                        field(OrderDetailResponse.class, "orderItems[].productPrice", "주문 시 상품 가격"),
                        field(OrderDetailResponse.class, "deliveryAddress", "배송지 주소")
                    )
                    .responseSchema(Schema.schema("OrderDetail"))
            ))
            .get("/order/{id}")
            .then()
            .status(HttpStatus.OK)
            .contentType(ContentType.JSON)
            .log().all()
            .extract().body().as(OrderDetailResponse.class);

        Assertions.assertThat(createdOrder.id()).isEqualTo(response.id());
    }

    @Test
    void 주문상세조회_실패_미존재주문() {
        String token = createTestToken(TEST_MEMBER_ID, TEST_MEMBER_ROLE);

        ErrorResponse errorResponse = given()
            .header("Authorization", "Bearer " + token)
            .pathParam("id", -1L)
            .consumeWith(document(
                info()
                    .tag("Order")
                    .summary("주문 상세 조회")
                    .description("주문 ID를 사용하여 상세 정보를 조회합니다.")
                    .pathParameters(
                        parameterWithName("id").description("주문 ID").type(SimpleType.INTEGER)
                    )
                    .responseFields(
                        field(ErrorResponse.class, "errorCode", "오류 코드"),
                        field(ErrorResponse.class, "message", "오류 메시지")
                    )
                    .responseSchema(Schema.schema("ErrorResponse"))
            ))
            .get("/order/{id}")
            .then()
            .status(HttpStatus.NOT_FOUND)
            .log().all()
            .extract().body().as(ErrorResponse.class);

        Assertions.assertThat(errorResponse.errorCode()).isEqualTo("order.notfound");
        Assertions.assertThat(errorResponse.message()).isEqualTo("주문이 존재하지 않습니다.");
    }

    @Test
    void 주문상세조회_실패_타인주문조회() {
        Long orderId = createTestOrder(TEST_MEMBER_ID, TEST_DELIVERY_ID).id();
        String token = createTestToken(-1L, TEST_MEMBER_ROLE);

        ErrorResponse errorResponse = given()
            .header("Authorization", "Bearer " + token)
            .pathParam("id", orderId)
            .consumeWith(document(
                info()
                    .tag("Order")
                    .summary("주문 상세 조회")
                    .description("주문 ID를 사용하여 상세 정보를 조회합니다.")
                    .pathParameters(
                        parameterWithName("id").description("주문 ID").type(SimpleType.INTEGER)
                    )
                    .responseFields(
                        field(ErrorResponse.class, "errorCode", "오류 코드"),
                        field(ErrorResponse.class, "message", "오류 메시지")
                    )
                    .responseSchema(Schema.schema("ErrorResponse"))
            ))
            .get("/order/{id}")
            .then()
            .status(HttpStatus.FORBIDDEN)
            .log().all()
            .extract().body().as(ErrorResponse.class);

        Assertions.assertThat(errorResponse.errorCode()).isEqualTo("auth.forbidden");
        Assertions.assertThat(errorResponse.message()).isEqualTo("권한이 필요합니다.");
    }

    @Test
    void 주문상세조회_실패_미존재주문상품() {
        Order createdOrder = createTestOrder(TEST_MEMBER_ID, TEST_DELIVERY_ID);
        String token = createTestToken(TEST_MEMBER_ID, TEST_MEMBER_ROLE);
        mockGetDeliveryInfo(TEST_MEMBER_ID, TEST_DELIVERY_ID);

        ErrorResponse errorResponse = given()
            .header("Authorization", "Bearer " + token)
            .pathParam("id", createdOrder.id())
            .consumeWith(document(
                info()
                    .tag("Order")
                    .summary("주문 상세 조회")
                    .description("주문 ID를 사용하여 상세 정보를 조회합니다.")
                    .pathParameters(
                        parameterWithName("id").description("주문 ID").type(SimpleType.INTEGER)
                    )
                    .responseFields(
                        field(ErrorResponse.class, "errorCode", "오류 코드"),
                        field(ErrorResponse.class, "message", "오류 메시지")
                    )
                    .responseSchema(Schema.schema("ErrorResponse"))
            ))
            .get("/order/{id}")
            .then()
            .status(HttpStatus.NOT_FOUND)
            .log().all()
            .extract().body().as(ErrorResponse.class);

        Assertions.assertThat(errorResponse.errorCode()).isEqualTo("orderitem.notfound");
        Assertions.assertThat(errorResponse.message()).isEqualTo("주문상품이 존재하지 않습니다.");
    }

    @Test
    void 주문생성_성공() {
        CreateOrderRequest request = new CreateOrderRequest(
            TEST_DELIVERY_ID,
            new CreateOrderRequest.OrderItemRequest(TEST_PRODUCT_ID, 2L)
        );
        String token = createTestToken(TEST_MEMBER_ID, TEST_MEMBER_ROLE);

        mockGetProduct(TEST_PRODUCT_ID);
        mockGetStock(TEST_PRODUCT_ID);
        mockGetDeliveryInfo(TEST_MEMBER_ID, TEST_DELIVERY_ID);
        mockDeductStock();

        OrderResponse response = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(request)
            .consumeWith(document(
                info()
                    .tag("Order")
                    .summary("주문 생성")
                    .description("주문 정보를 입력해 주문을 생성합니다.")
                    .requestFields(
                        field(CreateOrderRequest.class, "deliveryId", "배송지 ID"),
                        field(CreateOrderRequest.class, "orderItem", "주문 상품 정보"),
                        field(CreateOrderRequest.class, "orderItem.productId", "주문한 상품 ID"),
                        field(CreateOrderRequest.class, "orderItem.quantity", "주문 상품 수량")
                    )
                    .requestSchema(Schema.schema("OrderCreateRequest"))
                    .responseFields(
                        field(OrderResponse.class, "id", "주문 ID"),
                        field(OrderResponse.class, "totalPrice", "주문 최종 금액"),
                        field(OrderResponse.class, "status", "주문 상태"),
                        field(OrderResponse.class, "statusName", "주문 상태 설명"),
                        field(OrderResponse.class, "deliveryAddress", "배송지 주소"),
                        field(OrderResponse.class, "createdDate", "주문 생성 시간"),
                        field(OrderResponse.class, "orderItem", "주문 상품 목록"),
                        field(OrderResponse.class, "orderItem.id", "주문 상품 ID"),
                        field(OrderResponse.class, "orderItem.productId", "주문한 상품 ID"),
                        field(OrderResponse.class, "orderItem.productName", "주문 시 상품 이름"),
                        field(OrderResponse.class, "orderItem.productPrice", "주문 시 상품 가격")
                    )
                    .responseSchema(Schema.schema("OrderCreatedResponse"))
            ))
            .post("/order")
            .then()
            .status(HttpStatus.CREATED)
            .contentType(ContentType.JSON)
            .log().all()
            .extract().body().as(OrderResponse.class);

        Order savedOrder = orderRepository.findById(response.id()).block();
        Assertions.assertThat(savedOrder.id()).isEqualTo(response.id());
        Assertions.assertThat(savedOrder.status()).isEqualTo(OrderStatus.ORDERED);
    }

    @Test
    void 주문생성_실패_재고부족() {
        CreateOrderRequest request = new CreateOrderRequest(
            TEST_DELIVERY_ID,
            new CreateOrderRequest.OrderItemRequest(TEST_PRODUCT_ID, 20L)
        );
        String token = createTestToken(TEST_MEMBER_ID, TEST_MEMBER_ROLE);

        mockGetProduct(TEST_PRODUCT_ID);
        mockGetStock(TEST_PRODUCT_ID);
        mockGetDeliveryInfo(TEST_MEMBER_ID, TEST_DELIVERY_ID);

        ErrorResponse errorResponse = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body(request)
            .consumeWith(document(
                info()
                    .tag("Order")
                    .summary("주문 생성")
                    .description("주문 정보를 입력해 주문을 생성합니다.")
                    .requestFields(
                        field(CreateOrderRequest.class, "deliveryId", "배송지 ID"),
                        field(CreateOrderRequest.class, "orderItem", "주문 상품 정보"),
                        field(CreateOrderRequest.class, "orderItem.productId", "주문한 상품 ID"),
                        field(CreateOrderRequest.class, "orderItem.quantity", "주문 상품 수량")
                    )
                    .requestSchema(Schema.schema("OrderCreateRequest"))
                    .responseFields(
                        field(ErrorResponse.class, "errorCode", "오류 코드"),
                        field(ErrorResponse.class, "message", "오류 메시지")
                    )
                    .responseSchema(Schema.schema("ErrorResponse"))
            ))
            .post("/order")
            .then()
            .status(HttpStatus.BAD_REQUEST)
            .log().all()
            .extract().body().as(ErrorResponse.class);

        Assertions.assertThat(errorResponse.errorCode()).isEqualTo("stock.insufficient");
        Assertions.assertThat(errorResponse.message()).isEqualTo("상품 재고가 부족합니다.");
    }


    private String createTestToken(Long memberId, String role) {
        return TokenUtil.getSign(UserContext.of(memberId, role));
    }

    private Order createTestOrder(Long memberId, Long deliveryId) {
        Order order = new Order(null, 10000L, OrderStatus.ORDERED, memberId, deliveryId, null, null);
        return orderRepository.save(order).block();
    }

    private OrderItem createTestOrderItem(Long orderId) {
        OrderItem orderItem = new OrderItem(null, orderId, 100L, "테스트 상품", 10000L);
        return orderItemRepository.save(orderItem).block();
    }

    private void mockGetMember(Long memberId, String status) {
        BDDMockito.given(memberRequester.getMember(any()))
            .willReturn(Mono.just(new MemberResponse(memberId, "회원A", status)));
    }

    private void mockGetDeliveryInfo(Long memberId, Long deliveryId) {
        BDDMockito.given(memberRequester.getDeliveryInfo(any(), any()))
            .willReturn(Mono.just(new DeliveryInfoResponse(deliveryId, memberId, "배송 주소")));
    }

    private void mockGetProduct(Long productId) {
        BDDMockito.given(productRequester.getProduct(any()))
            .willReturn(Mono.just(new ProductResponse(productId, "테스트 상품", 10000L)));
    }

    private void mockGetStock(Long productId) {
        BDDMockito.given(stockRequester.getStock(any()))
            .willReturn(Mono.just(new StockResponse(productId, 10L)));
    }

    private void mockDeductStock() {
        BDDMockito.given(stockRequester.deductStock(any(), any(), any()))
            .willReturn(Mono.empty());
    }

}
