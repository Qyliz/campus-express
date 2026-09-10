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

    private MockMultipartHttpServletRequestBuilder registration(String email) {
        return multipart("/api/user/register").param("username", "验证码测试")
                .param("password", "1234567").param("role", "CUSTOMER")
                .param("gender", "UNKNOWN").param("email", email);
    }

    private String email() { return UUID.randomUUID() + "@example.com"; }

    @Test
    void emailRegistrationUsesPublicSendEndpointAndCodeIsSingleUse() throws Exception {
        String account = email();
        String response = mvc.perform(post("/api/user/verify-code").contentType(MediaType.APPLICATION_JSON)
                .content("{\"account\":\"" + account + "\",\"scene\":\"REGISTER\"}"))
                .andExpect(jsonPath("$.code").value(0)).andReturn().getResponse().getContentAsString();
        String code = JsonMapper.builder().build().readTree(response).get("data").asString();
        assertTrue(code.matches("\\d{6}"));
        mvc.perform(registration(account).param("emailCode", code)).andExpect(jsonPath("$.code").value(0));
        mvc.perform(registration(account).param("emailCode", code)).andExpect(jsonPath("$.code").value(2012));
    }

    @Test
    void missingWrongExpiredAndOtherSceneCodesCannotRegister() throws Exception {
        String account = email();
        mvc.perform(registration(account)).andExpect(jsonPath("$.code").value(2));
        String code = codes.send(account, VerifySceneEnum.REGISTER);
        String wrong = "000000".equals(code) ? "111111" : "000000";
        mvc.perform(registration(account).param("emailCode", wrong)).andExpect(jsonPath("$.code").value(2011));
        SaManager.getSaTokenDao().delete("vc:REGISTER:" + account);
        mvc.perform(registration(account).param("emailCode", code)).andExpect(jsonPath("$.code").value(2012));
        String otherSceneCode = codes.send(account, VerifySceneEnum.FORGOT_PASSWORD);
        mvc.perform(registration(account).param("emailCode", otherSceneCode)).andExpect(jsonPath("$.code").value(2012));
    }

    @Test
    void bothContactsMustBeVerifiedWithoutConsumingFirstOnSecondFailure() throws Exception {
        String account = email(), phone = "13900000071";
        String phoneCode = codes.send(phone, VerifySceneEnum.REGISTER);
        String emailCode = codes.send(account, VerifySceneEnum.REGISTER);
        mvc.perform(registration(account).param("phone", phone).param("phoneCode", phoneCode))
                .andExpect(jsonPath("$.code").value(2));
        mvc.perform(registration(account).param("phone", phone).param("phoneCode", phoneCode).param("emailCode", emailCode))
                .andExpect(jsonPath("$.code").value(0));
        assertThrows(BusinessException.class, () -> codes.verify(phone, VerifySceneEnum.REGISTER, phoneCode));
    }

    @Test
    void changingAccountCannotReusePreviousCodeAndInvalidTargetIsRejected() throws Exception {
        String first = email(), second = email();
        String code = codes.send(first, VerifySceneEnum.REGISTER);
        mvc.perform(registration(second).param("emailCode", code)).andExpect(jsonPath("$.code").value(2012));
        mvc.perform(post("/api/user/verify-code").contentType(MediaType.APPLICATION_JSON)
                .content("{\"account\":\"not-an-account\",\"scene\":\"REGISTER\"}"))
                .andExpect(jsonPath("$.code").value(1));
    }
}
