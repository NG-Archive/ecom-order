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

import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static io.restassured.module.webtestclient.RestAssuredWebTestClient.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

public class OrderControllerTest extends AcceptedTest {

    @MockitoBean
    private MemberRequester memberRequester;

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    private static final Long TEST_MEMBER_ID = 100L;
    private static final String TEST_ROLE = "USER";

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll().block();
        orderItemRepository.deleteAll().block();
    }

    @Test
    void 주문목록조회() {
        createTestOrder(TEST_MEMBER_ID);
        String token = createTestToken(TEST_MEMBER_ID, TEST_ROLE);
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
                        field(OrderListResponse.class, "[].createdDate", "주문 생성 시간")
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
            .body("[0].createdDate", notNullValue())
            .log().all();
    }

    @Test
    void 주문목록조회_탈퇴한회원오류() {
        String token = createTestToken(TEST_MEMBER_ID, TEST_ROLE);
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
    void 주문상세조회() {
        Long orderId = createTestOrder(TEST_MEMBER_ID);
        String token = createTestToken(TEST_MEMBER_ID, TEST_ROLE);
        mockGetDeliveryInfo(TEST_MEMBER_ID,10L);

        OrderDetailResponse response = given()
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
                        field(OrderDetailResponse.class, "id", "주문 ID"),
                        field(OrderDetailResponse.class, "totalPrice", "주문 최종 금액"),
                        field(OrderDetailResponse.class, "status", "주문 상태"),
                        field(OrderDetailResponse.class, "statusName", "주문 상태 설명"),
                        field(OrderDetailResponse.class, "memberId", "회원 ID"),
                        field(OrderDetailResponse.class, "deliveryId", "배송지 ID"),
                        field(OrderDetailResponse.class, "updatedDate", "주문 수정 시간"),
                        field(OrderDetailResponse.class, "orderItems", "주문 상품 목록"),
                        field(OrderDetailResponse.class, "orderItems[].id", "주문 상품 ID"),
                        field(OrderDetailResponse.class, "orderItems[].productId", "주문한 상품 ID"),
                        field(OrderDetailResponse.class, "orderItems[].productName", "주문 시 상품 이름"),
                        field(OrderDetailResponse.class, "orderItems[].productPrice", "주문 시 상품 가격"),
                        field(OrderDetailResponse.class, "address", "배송지 주소")
                    )
                    .responseSchema(Schema.schema("OrderDetail"))
            ))
            .get("/order/{id}")
            .then()
            .status(HttpStatus.OK)
            .contentType(ContentType.JSON)
            .log().all()
            .extract().body().as(OrderDetailResponse.class);

        Assertions.assertThat(orderId).isEqualTo(response.id());
    }

    @Test
    void 주문상세조회_타인주문조회오류() {
        Long orderId = createTestOrder(TEST_MEMBER_ID);
        String token = createTestToken(-1L, TEST_ROLE);
        mockGetDeliveryInfo(TEST_MEMBER_ID,10L);

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

    private String createTestToken(Long memberId, String role) {
        return TokenUtil.getSign(UserContext.of(memberId, role));
    }

    private Long createTestOrder(Long memberId) {
        CreateOrderCommand orderCommand = new CreateOrderCommand(10000L, OrderStatus.ORDERED, memberId, 10L);
        OrderResponse order = orderService.createOrder(orderCommand).block();

        CreateOrderItemCommand orderItemCommand = new CreateOrderItemCommand(order.id(), 100L, "상품A", 10000L);
        orderService.createOrderItem(orderItemCommand).block();

        return order.id();
    }

    private void mockGetMember(Long memberId, String status) {
        BDDMockito.given(memberRequester.getMember(memberId))
            .willReturn(Mono.just(new MemberResponse(memberId, "USER", status)));
    }

    private void mockGetDeliveryInfo(Long memberId, Long deliveryId) {
        BDDMockito.given(memberRequester.getDeliveryInfo(memberId, deliveryId))
            .willReturn(Mono.just(new DeliveryInfoResponse(deliveryId, memberId, "주소")));
    }

}
