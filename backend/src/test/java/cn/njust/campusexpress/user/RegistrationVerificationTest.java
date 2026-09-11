package cn.njust.campusexpress.user;

import cn.dev33.satoken.SaManager;
import cn.njust.campusexpress.common.enums.*;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.user.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistrationVerificationTest {
    @Autowired MockMvc mvc;
    @Autowired VerifyCodeService codes;

    /** 手机号现在是注册必填项，所以本类的每个请求都必须带 phone；验证码仍然只在 email 那一项上做文章。 */
    private MockMultipartHttpServletRequestBuilder registration(String email, String phone) {
        return multipart("/api/user/register").param("username", "验证码测试")
                .param("password", "1234567").param("role", "CUSTOMER")
                .param("gender", "UNKNOWN").param("email", email).param("phone", phone);
    }

    private String email() { return UUID.randomUUID() + "@example.com"; }

    // verifyRegistration 先校验手机再校验邮箱，两项全过才统一消费。
    // 因此凡是「期望失败来自邮箱分支」的用例，都必须先给一个有效的手机验证码，否则错误会来自手机分支、测试只是碰巧过了。
    @Test
    void emailRegistrationUsesPublicSendEndpointAndCodeIsSingleUse() throws Exception {
        String account = email(), phone = "13900000081";
        String response = mvc.perform(post("/api/user/verify-code").contentType(MediaType.APPLICATION_JSON)
                .content("{\"account\":\"" + account + "\",\"scene\":\"REGISTER\"}"))
                .andExpect(jsonPath("$.code").value(0)).andReturn().getResponse().getContentAsString();
        String code = JsonMapper.builder().build().readTree(response).get("data").asString();
        assertTrue(code.matches("\\d{6}"));
        mvc.perform(registration(account, phone).param("phoneCode", codes.send(phone, VerifySceneEnum.REGISTER))
                .param("emailCode", code)).andExpect(jsonPath("$.code").value(0));
        // 上一次成功已经把手机验证码也消费掉了，这里必须重新发一个，
        // 才能让 2012 确实来自「邮箱验证码被复用」，而不是「手机验证码不存在」。
        mvc.perform(registration(account, phone).param("phoneCode", codes.send(phone, VerifySceneEnum.REGISTER))
                .param("emailCode", code)).andExpect(jsonPath("$.code").value(2012));
    }

    @Test
    void missingWrongExpiredAndOtherSceneCodesCannotRegister() throws Exception {
        String account = email(), phone = "13900000082";
        // 这四次调用没有一次会成功，而验证码是「全部校验通过后才统一删除」，所以同一个手机验证码始终有效。
        String phoneCode = codes.send(phone, VerifySceneEnum.REGISTER);
        mvc.perform(registration(account, phone).param("phoneCode", phoneCode))
                .andExpect(jsonPath("$.code").value(2));
        String code = codes.send(account, VerifySceneEnum.REGISTER);
        String wrong = "000000".equals(code) ? "111111" : "000000";
        mvc.perform(registration(account, phone).param("phoneCode", phoneCode).param("emailCode", wrong))
                .andExpect(jsonPath("$.code").value(2011));
        SaManager.getSaTokenDao().delete("vc:REGISTER:" + account);
        mvc.perform(registration(account, phone).param("phoneCode", phoneCode).param("emailCode", code))
                .andExpect(jsonPath("$.code").value(2012));
        String otherSceneCode = codes.send(account, VerifySceneEnum.FORGOT_PASSWORD);
        mvc.perform(registration(account, phone).param("phoneCode", phoneCode).param("emailCode", otherSceneCode))
                .andExpect(jsonPath("$.code").value(2012));
    }

    @Test
    void bothContactsMustBeVerifiedWithoutConsumingFirstOnSecondFailure() throws Exception {
        String account = email(), phone = "13900000071";
        String phoneCode = codes.send(phone, VerifySceneEnum.REGISTER);
        String emailCode = codes.send(account, VerifySceneEnum.REGISTER);
        mvc.perform(registration(account, phone).param("phoneCode", phoneCode))
                .andExpect(jsonPath("$.code").value(2));
        mvc.perform(registration(account, phone).param("phoneCode", phoneCode).param("emailCode", emailCode))
                .andExpect(jsonPath("$.code").value(0));
        assertThrows(BusinessException.class, () -> codes.verify(phone, VerifySceneEnum.REGISTER, phoneCode));
    }

    @Test
    void changingAccountCannotReusePreviousCodeAndInvalidTargetIsRejected() throws Exception {
        String first = email(), second = email(), phone = "13900000083";
        String code = codes.send(first, VerifySceneEnum.REGISTER);
        mvc.perform(registration(second, phone).param("phoneCode", codes.send(phone, VerifySceneEnum.REGISTER))
                .param("emailCode", code)).andExpect(jsonPath("$.code").value(2012));
        mvc.perform(post("/api/user/verify-code").contentType(MediaType.APPLICATION_JSON)
                .content("{\"account\":\"not-an-account\",\"scene\":\"REGISTER\"}"))
                .andExpect(jsonPath("$.code").value(1));
    }
}
