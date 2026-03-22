package site.ng_archive.ecom_order.domain;

import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import io.restassured.http.ContentType;
import org.assertj.core.api.Assertions;
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
import site.ng_archive.ecom_order.domain.dto.CreateOrderCommand;
import site.ng_archive.ecom_order.domain.dto.MemberResponse;
import site.ng_archive.ecom_order.domain.dto.OrderListResponse;
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

    private static final Long TEST_MEMBER_ID = 100L;
    private static final String TEST_ROLE = "USER";

    @Test
    void 주문목록조회() {
        createTestOrder(TEST_MEMBER_ID);
        String token = createTestToken(TEST_MEMBER_ID, TEST_ROLE);
        mockMemberRequester(TEST_MEMBER_ID, "NORMAL");

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
        mockMemberRequester(TEST_MEMBER_ID, "WITHDRAWN");

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

    private String createTestToken(Long memberId, String role) {
        return TokenUtil.getSign(UserContext.of(memberId, role));
    }

    private void mockMemberRequester(Long memberId, String status) {
        BDDMockito.given(memberRequester.getMember(memberId))
            .willReturn(Mono.just(new MemberResponse(memberId, "USER", status)));
    }

    private void createTestOrder(Long memberId) {
        CreateOrderCommand command = new CreateOrderCommand(10000L, OrderStatus.ORDERED, memberId, 10L);
        orderService.createOrder(command).block();
    }

}
