package cn.njust.campusexpress.review;

import cn.njust.campusexpress.IntegrationTestSupport;
import cn.njust.campusexpress.common.enums.*;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.order.dto.*;
import cn.njust.campusexpress.model.order.service.OrderService;
import cn.njust.campusexpress.model.review.dto.*;
import cn.njust.campusexpress.model.review.entity.ReviewAppeal;
import cn.njust.campusexpress.model.review.mapper.*;
import cn.njust.campusexpress.model.review.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.*;
import static cn.njust.campusexpress.common.enums.UserRoleEnum.*;
import static cn.njust.campusexpress.common.enums.OrderActionEnum.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//验证评价、申诉和异常枚举的完整业务流程
@SpringBootTest
@AutoConfigureMockMvc
class ReviewFlowTest extends IntegrationTestSupport {
    @Autowired ReviewService service;
    @Autowired OrderService orders;
    @Autowired ServiceReviewMapper reviews;
    @MockitoSpyBean ReviewAppealMapper appeals;
    @Autowired tools.jackson.databind.json.JsonMapper json;
    Long sender, recipient, courier, otherCourier, admin, stranger;

    @BeforeEach void setup() {
        sender = createUser(CUSTOMER, "评价测试"); recipient = createUser(CUSTOMER, "评价测试");
        courier = createUser(COURIER, "评价测试");
        otherCourier = createUser(COURIER, "评价测试"); admin = createUser(ADMIN, "评价测试");
        stranger = createUser(CUSTOMER, "评价测试");
    }
    @AfterEach void cleanup() {
        reset(appeals);
        cleanupCreatedData();
    }
    Long order(Long receiver, boolean complete) {
        var dto = new CreateOrderDTO();
        dto.setPickupAddress("快递站"); dto.setPickupName("寄件人"); dto.setPickupPhone("13800000001");
        dto.setDeliveryAddress("宿舍"); dto.setDeliveryName("收件人");
        dto.setDeliveryPhone(users.getById(receiver).getPhone());
        dto.setItemDescription("书籍"); dto.setFee(new BigDecimal("5.00"));
        var id = orders.create(sender, CUSTOMER, dto);
        createdOrderIds.add(id);
        if (complete) {
            orders.act(sender, CUSTOMER, id, PAY, null);
            orders.act(courier, COURIER, id, ACCEPT, null);
            orders.act(courier, COURIER, id, PICKUP, null);
            orders.act(courier, COURIER, id, DELIVER, null);
            orders.act(receiver, CUSTOMER, id, COMPLETE, null);
        }
        return id;
    }
    ReviewDTO reviewDto(Integer rating, String content) {
        var dto = new ReviewDTO();
        dto.setRating(rating);
        dto.setContent(content);
        return dto;
    }
    AppealDTO appealDto(String reason) {
        var dto = new AppealDTO();
        dto.setReason(reason);
        return dto;
    }
    ResolveAppealDTO resolveAppealDto(AppealStatusEnum status, String reason) {
        var dto = new ResolveAppealDTO();
        dto.setStatus(status);
        dto.setReason(reason);
        return dto;
    }
    Long review(Long orderId) { return service.create(sender, CUSTOMER, orderId, reviewDto(4, " 服务及时 ")); }

    @Test void bothParticipantsCanReviewAndSamePersonOnlyOnce() {
        var id = order(recipient, true);
        assertTrue(service.list(sender, CUSTOMER, id).isCanReview());
        review(id);
        service.create(recipient, CUSTOMER, id, reviewDto(5, "满意"));
        assertEquals(2, service.list(courier, COURIER, id).getReviews().size());
        assertFalse(service.list(sender, CUSTOMER, id).isCanReview());
        assertThrows(BusinessException.class, () -> review(id));
        var own = order(sender, true);
        review(own);
        assertThrows(BusinessException.class, () -> review(own));
        assertEquals(1, service.list(sender, CUSTOMER, own).getReviews().size());
    }

    @Test void invalidStageAndUnauthorizedRolesCannotReadOrWrite() {
        var unfinished = order(recipient, false);
        assertThrows(BusinessException.class, () -> review(unfinished));
        orders.act(sender, CUSTOMER, unfinished, CANCEL, "取消");
        assertThrows(BusinessException.class, () -> review(unfinished));
        var id = order(recipient, true);
        assertThrows(BusinessException.class, () -> service.create(stranger, CUSTOMER, id, reviewDto(4, "评价")));
        assertThrows(BusinessException.class, () -> service.list(stranger, CUSTOMER, id));
        assertThrows(BusinessException.class, () -> service.list(otherCourier, COURIER, id));
        assertThrows(BusinessException.class, () -> service.create(courier, COURIER, id, reviewDto(4, "评价")));
        var reviewId = review(id);
        assertThrows(BusinessException.class, () -> service.appeal(otherCourier, COURIER, reviewId, appealDto("理由")));
        assertThrows(BusinessException.class, () -> service.appeal(sender, CUSTOMER, reviewId, appealDto("理由")));
        var appealId = service.appeal(courier, COURIER, reviewId, appealDto("理由"));
        assertThrows(BusinessException.class, () -> service.resolve(sender, CUSTOMER, appealId, resolveAppealDto(AppealStatusEnum.UPHELD, "理由")));
        assertThrows(BusinessException.class, () -> service.adminList(courier, COURIER, new AppealQueryDTO()));
        var account = accounts.getByUserAndRole(sender, CUSTOMER);
        accounts.updateStatus(account, CUSTOMER, UserStatusEnum.DISABLED);
        assertThrows(BusinessException.class, () -> service.list(sender, CUSTOMER, id));
    }

    @Test void upheldAndRejectedAppealsRetainHistoryAndCannotBeRepeated() {
        var id = order(recipient, true);
        var first = review(id);
        var second = service.create(recipient, CUSTOMER, id, reviewDto(2, "送达较晚"));
        var a = service.appeal(courier, COURIER, first, appealDto(" 已及时送达 "));
        var b = service.appeal(courier, COURIER, second, appealDto("解释"));
        assertThrows(BusinessException.class, () -> service.appeal(courier, COURIER, first, appealDto("重复")));
        service.resolve(admin, ADMIN, a, resolveAppealDto(AppealStatusEnum.UPHELD, " 核实成立 "));
        service.resolve(admin, ADMIN, b, resolveAppealDto(AppealStatusEnum.REJECTED, "评价合理"));
        assertEquals(ReviewStatusEnum.VOID, reviews.selectById(first).getStatus());
        assertEquals(ReviewStatusEnum.VALID, reviews.selectById(second).getStatus());
        assertEquals("服务及时", reviews.selectById(first).getContent());
        assertEquals("核实成立", appeals.selectById(a).getResolutionReason());
        assertNotNull(appeals.selectById(a).getResolvedTime());
        assertEquals(accounts.getByUserAndRole(admin, ADMIN).getId(), appeals.selectById(a).getAdminId());
        assertThrows(BusinessException.class, () -> service.resolve(admin, ADMIN, a, resolveAppealDto(AppealStatusEnum.REJECTED, "再处理")));
        assertThrows(BusinessException.class, () -> service.appeal(courier, COURIER, first, appealDto("作废后")));
        assertThrows(BusinessException.class, () -> service.appeal(courier, COURIER, second, appealDto("驳回后")));
        assertTrue(service.list(courier, COURIER, id).getReviews().stream().noneMatch(r -> r.isCanAppeal()));
        assertEquals(2, service.list(admin, ADMIN, id).getReviews().size());
        //按订单筛选申诉以隔离种子数据
        var query = new AppealQueryDTO(); query.setOrderId(id);
        assertEquals(2, service.adminList(admin, ADMIN, query).getTotal());
        query.setStatus(AppealStatusEnum.PENDING); assertEquals(0, service.adminList(admin, ADMIN, query).getTotal());
        query.setStatus(AppealStatusEnum.UPHELD); assertEquals(1, service.adminList(admin, ADMIN, query).getTotal());
        query.setStatus(null); assertEquals(2, service.adminList(admin, ADMIN, query).getTotal());
    }

    @Test void validationCoversRatingsWhitespaceAndLength() {
        var id = order(recipient, true);
        for (Integer rating : Arrays.asList(null, 0, 6)) {
            assertThrows(BusinessException.class, () -> service.create(sender, CUSTOMER, id, reviewDto(rating, "内容")));
        }
        for (String content : Arrays.asList(null, " \n ", "字".repeat(501))) {
            assertThrows(BusinessException.class, () -> service.create(sender, CUSTOMER, id, reviewDto(3, content)));
        }
        var reviewId = service.create(sender, CUSTOMER, id, reviewDto(1, "字".repeat(500)));
        for (String reason : Arrays.asList(null, " ", "字".repeat(501))) {
            assertThrows(BusinessException.class, () -> service.appeal(courier, COURIER, reviewId, appealDto(reason)));
        }
        var appealId = service.appeal(courier, COURIER, reviewId, appealDto("字".repeat(500)));
        for (String reason : Arrays.asList(null, " ", "字".repeat(501))) {
            assertThrows(BusinessException.class, () -> service.resolve(admin, ADMIN, appealId, resolveAppealDto(AppealStatusEnum.UPHELD, reason)));
        }
        //申诉处理结果不能重新设为待处理
        assertThrows(BusinessException.class, () -> service.resolve(admin, ADMIN, appealId, resolveAppealDto(AppealStatusEnum.PENDING, "理由")));
        assertEquals(AppealStatusEnum.PENDING, appeals.selectById(appealId).getStatus());
    }

    @Test void failedResolutionRollsBackReviewInvalidation() {
        var id = order(recipient, true);
        var reviewId = review(id);
        var appealId = service.appeal(courier, COURIER, reviewId, appealDto("理由"));
        doThrow(new IllegalStateException("模拟保存失败")).when(appeals).updateById(any(ReviewAppeal.class));
        assertThrows(IllegalStateException.class, () -> service.resolve(admin, ADMIN, appealId, resolveAppealDto(AppealStatusEnum.UPHELD, "成立")));
        assertEquals(ReviewStatusEnum.VALID, reviews.selectById(reviewId).getStatus());
        assertEquals(AppealStatusEnum.PENDING, appeals.selectById(appealId).getStatus());
        assertNull(appeals.selectById(appealId).getAdminId());
    }

    @Test void orderEnumsUseNamesOverHttpAndNumericDatabaseValues() throws Exception {
        for (var value : OrderStatusEnum.values()) {
            assertEquals("\"" + value.name() + "\"", json.writeValueAsString(value));
            assertEquals(value, json.readValue(json.writeValueAsString(value.name()), OrderStatusEnum.class));
        }
        for (var value : PaymentStatusEnum.values()) {
            assertEquals("\"" + value.name() + "\"", json.writeValueAsString(value));
            assertEquals(value, json.readValue(json.writeValueAsString(value.name()), PaymentStatusEnum.class));
        }
        for (var value : ExceptionStatusEnum.values()) {
            assertEquals("\"" + value.name() + "\"", json.writeValueAsString(value));
            assertEquals(value, json.readValue(json.writeValueAsString(value.name()), ExceptionStatusEnum.class));
        }
        for (var value : ExceptionTypeEnum.values()) {
            assertEquals("\"" + value.name() + "\"", json.writeValueAsString(value));
            assertEquals(value, json.readValue(json.writeValueAsString(value.name()), ExceptionTypeEnum.class));
        }
        for (var value : ExceptionResolutionEnum.values()) {
            assertEquals("\"" + value.name() + "\"", json.writeValueAsString(value));
            assertEquals(value, json.readValue(json.writeValueAsString(value.name()), ExceptionResolutionEnum.class));
        }
        for (var value : AppealStatusEnum.values()) {
            assertEquals("\"" + value.name() + "\"", json.writeValueAsString(value));
            assertEquals(value, json.readValue(json.writeValueAsString(value.name()), AppealStatusEnum.class));
        }
        for (var value : ReviewStatusEnum.values()) {
            assertEquals("\"" + value.name() + "\"", json.writeValueAsString(value));
            assertEquals(value, json.readValue(json.writeValueAsString(value.name()), ReviewStatusEnum.class));
        }
        var id = order(recipient, true);
        var detail = orders.detail(sender, CUSTOMER, id);
        assertEquals(OrderStatusEnum.COMPLETED, detail.getOrder().getOrderStatus());
        assertEquals(PaymentStatusEnum.PAID, detail.getOrder().getPaymentStatus());
        assertNull(detail.getRecords().get(0).getFromStatus());
        assertEquals(OrderStatusEnum.UNPAID, detail.getRecords().get(0).getToStatus());
        assertEquals(OrderStatusEnum.AWAITING_COLLECTION, detail.getRecords().get(5).getFromStatus());
        assertEquals(OrderStatusEnum.COMPLETED, detail.getRecords().get(5).getToStatus());
        assertEquals(5, jdbc.queryForObject("select order_status from express_order where id=?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject("select payment_status from express_order where id=?", Integer.class, id));
        assertEquals(List.of(0, 1, 2, 3, 4, 5), jdbc.queryForList(
            "select to_status from order_status_record where order_id=? order by create_time,id", Integer.class, id));
        var cookie = login(sender, CUSTOMER);
        mvc.perform(get("/api/order/{id}", id).cookie(cookie))
            .andExpect(jsonPath("$.data.order.orderStatus").value("COMPLETED"))
            .andExpect(jsonPath("$.data.order.paymentStatus").value("PAID"))
            .andExpect(jsonPath("$.data.records[0].toStatus").value("UNPAID"))
            .andExpect(jsonPath("$.data.records[5].fromStatus").value("AWAITING_COLLECTION"))
            .andExpect(jsonPath("$.data.records[5].toStatus").value("COMPLETED"));
        mvc.perform(get("/api/order/mine").cookie(cookie).param("orderStatus", "COMPLETED"))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records[0].orderStatus").value("COMPLETED"));
    }

    //校验异常枚举在数据库中存数字码并在接口中返回枚举名
    @Test void exceptionEnumsPersistNumericCodesAndExposeNamesOverHttp() throws Exception {
        var id = order(recipient, false);
        orders.act(sender, CUSTOMER, id, PAY, null);
        orders.act(courier, COURIER, id, ACCEPT, null);
        orders.act(courier, COURIER, id, PICKUP, null);
        var report = new ReportExceptionDTO();
        report.setType(ExceptionTypeEnum.ADDRESS);
        report.setDescription("门禁需要刷卡，无法进入");
        var exceptionId = orders.reportException(courier, COURIER, id, report);
        var resolve = new ResolveExceptionDTO();
        resolve.setResolution(ExceptionResolutionEnum.RESUME);
        resolve.setDescription("已联系收件人下楼取件");
        orders.resolveException(admin, ADMIN, exceptionId, resolve);
        //数据库数值应与各枚举声明的编码一致
        assertEquals(1, jdbc.queryForObject("select type from delivery_exception where id=?", Integer.class, exceptionId));
        assertEquals(0, jdbc.queryForObject("select resolution from delivery_exception where id=?", Integer.class, exceptionId));
        assertEquals(1, jdbc.queryForObject("select status from delivery_exception where id=?", Integer.class, exceptionId));
        mvc.perform(get("/api/order/{id}", id).cookie(login(sender, CUSTOMER)))
            .andExpect(jsonPath("$.data.exceptions[0].type").value("ADDRESS"))
            .andExpect(jsonPath("$.data.exceptions[0].resolution").value("RESUME"))
            .andExpect(jsonPath("$.data.exceptions[0].status").value("RESOLVED"));
    }

    @Test void orderFilterRejectsNumericAndUnknownEnumNames() throws Exception {
        var cookie = login(sender, CUSTOMER);
        for (String value : List.of("5", "INVALID", "completed")) {
            mvc.perform(get("/api/order/mine").cookie(cookie).param("orderStatus", value))
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
        }
        mvc.perform(get("/api/order/mine").cookie(cookie).param("relation", "CREATED"))
            .andExpect(jsonPath("$.code").value(0));
        for (String value : List.of("created", "INVALID")) {
            mvc.perform(get("/api/order/mine").cookie(cookie).param("relation", value))
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
        }
        //状态筛选仅接受枚举名并拒绝旧数字值和未知名称
        var adminCookie = login(admin, ADMIN);
        for (String value : List.of("0", "INVALID")) {
            mvc.perform(get("/api/exception/admin").cookie(adminCookie).param("status", value))
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
            mvc.perform(get("/api/review/appeals/admin").cookie(adminCookie).param("status", value))
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
        }
    }

    @Test void httpFlowIncludesLoginValidationAndRoleBoundaries() throws Exception {
        var id = order(recipient, true);
        mvc.perform(get("/api/order/{id}/reviews", id)).andExpect(status().isUnauthorized());
        var customerCookie = login(sender, CUSTOMER);
        mvc.perform(post("/api/order/{id}/reviews", id).cookie(customerCookie).contentType(MediaType.APPLICATION_JSON)
            .content("{\"rating\":6,\"content\":\"评价\"}")).andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
        mvc.perform(post("/api/order/{id}/reviews", id).cookie(customerCookie).contentType(MediaType.APPLICATION_JSON)
            .content("{\"rating\":5,\"content\":\"服务很好\"}")).andExpect(jsonPath("$.code").value(0));
        var reviewId = service.list(sender, CUSTOMER, id).getReviews().get(0).getReview().getId();
        var courierCookie = login(courier, COURIER);
        mvc.perform(get("/api/order/{id}/reviews", id).cookie(courierCookie))
            .andExpect(jsonPath("$.data.canReview").value(false))
            .andExpect(jsonPath("$.data.reviews[0].canAppeal").value(true))
            .andExpect(jsonPath("$.data.reviews[0].username").value("评价测试"))
            .andExpect(jsonPath("$.data.reviews[0].review.id").value(reviewId.toString()));
        mvc.perform(post("/api/review/{id}/appeals", reviewId).cookie(courierCookie).contentType(MediaType.APPLICATION_JSON)
            .content("{\"reason\":\"请核实\"}")).andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/api/review/appeals/admin").cookie(customerCookie))
            .andExpect(jsonPath("$.code").value(ResultCodeEnum.NO_PERMISSION.getCode()));
        var appealId = service.list(courier, COURIER, id).getReviews().get(0).getAppeal().getId();
        var adminCookie = login(admin, ADMIN);
        mvc.perform(post("/api/review/appeals/{id}/resolve", appealId).cookie(adminCookie).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"UPHELD\",\"reason\":\"申诉成立\"}")).andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/api/review/appeals/admin").param("orderId", id.toString()).cookie(adminCookie))
            .andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.records[0].status").value("UPHELD"));
        mvc.perform(get("/api/order/{id}/reviews", id).cookie(customerCookie))
            .andExpect(jsonPath("$.data.reviews[0].review.status").value("VOID"))
            .andExpect(jsonPath("$.data.reviews[0].review.content").value("服务很好"));
    }
}
