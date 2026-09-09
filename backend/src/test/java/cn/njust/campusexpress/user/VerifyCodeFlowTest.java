package cn.njust.campusexpress.user;

import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.model.user.entity.Customer;
import cn.njust.campusexpress.model.user.entity.User;
import cn.njust.campusexpress.model.user.service.CustomerService;
import cn.njust.campusexpress.model.user.service.UserService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class VerifyCodeFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomerService customerService;

    //注册收寄件人（NORMAL），并校验 user 主表行与 customer 角色账户行都已创建
    private void registerCustomer(String username, String phone) throws Exception {
        mockMvc.perform(multipart("/api/user/register")
                        .param("username", username)
                        .param("password", "1234567")
                        .param("role", "CUSTOMER")
                        .param("gender", "MALE")
                        .param("phone", phone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
        User user = userService.lambdaQuery().eq(User::getUsername, username).one();
        Assertions.assertNotNull(user);
        Customer customer = customerService.lambdaQuery().eq(Customer::getUserId, user.getId()).one();
        Assertions.assertNotNull(customer, "收寄件人账户行应已创建");
    }

    private Cookie login(String account, String password) throws Exception {
        String body = String.format("{\"account\":\"%s\",\"password\":\"%s\",\"role\":\"CUSTOMER\"}", account, password);
        MvcResult result = mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn();
        return result.getResponse().getCookie("satoken");
    }

    private void assertLogin(String account, String password, ResultCodeEnum expected) throws Exception {
        String body = String.format("{\"account\":\"%s\",\"password\":\"%s\",\"role\":\"CUSTOMER\"}", account, password);
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(expected.getCode()));
    }

    //发送验证码并返回码（桩版）
    private String sendCode(String account, String scene) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"account\":\"%s\",\"scene\":\"%s\"}", account, scene)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data");
    }

    @Test
    void sendCodeReturnsSixDigits() throws Exception {
        String code = sendCode("13900000401", "FORGOT_PASSWORD");
        Assertions.assertTrue(code.matches("\\d{6}"), "验证码应为6位数字，实际=" + code);
    }

    @Test
    void forgotPasswordReset() throws Exception {
        registerCustomer("fp_user", "13900000402");
        String code = sendCode("13900000402", "FORGOT_PASSWORD");

        mockMvc.perform(post("/api/user/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"account\":\"13900000402\",\"code\":\"%s\",\"newPassword\":\"newpass1\"}", code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        assertLogin("13900000402", "newpass1", ResultCodeEnum.SUCCESS);
        assertLogin("13900000402", "1234567", ResultCodeEnum.LOGIN_ERROR);
    }

    @Test
    void resetPasswordWrongCode() throws Exception {
        registerCustomer("fp_wrong", "13900000403");
        String real = sendCode("13900000403", "FORGOT_PASSWORD");
        String wrong = real.equals("000000") ? "111111" : "000000";

        mockMvc.perform(post("/api/user/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"account\":\"13900000403\",\"code\":\"%s\",\"newPassword\":\"newpass1\"}", wrong)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.VERIFY_CODE_ERROR.getCode()));
    }

    @Test
    void resetPasswordWithoutCode() throws Exception {
        registerCustomer("fp_nocode", "13900000404");
        //从未发送验证码 -> 存储中不存在 -> EXPIRED
        mockMvc.perform(post("/api/user/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"13900000404\",\"code\":\"123456\",\"newPassword\":\"newpass1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.VERIFY_CODE_EXPIRED.getCode()));
    }

    @Test
    void changePhone() throws Exception {
        registerCustomer("cp_user", "13900000405");
        Cookie c = login("13900000405", "1234567");
        String newPhone = "13900000410";
        String code = sendCode(newPhone, "CHANGE_PHONE");

        mockMvc.perform(put("/api/user/phone")
                        .cookie(c)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"newPhone\":\"%s\",\"code\":\"%s\"}", newPhone, code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        assertLogin(newPhone, "1234567", ResultCodeEnum.SUCCESS);
        assertLogin("13900000405", "1234567", ResultCodeEnum.LOGIN_ERROR);
    }

    @Test
    void changePhoneConflict() throws Exception {
        registerCustomer("cp_a", "13900000407");
        registerCustomer("cp_b", "13900000408");
        Cookie c = login("13900000407", "1234567");
        //试图把 cp_a 的手机号换成 cp_b 已占用的号码
        String code = sendCode("13900000408", "CHANGE_PHONE");

        mockMvc.perform(put("/api/user/phone")
                        .cookie(c)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"newPhone\":\"%s\",\"code\":\"%s\"}", "13900000408", code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PHONE_ALREADY_BIND.getCode()));
    }

    @Test
    void changeEmail() throws Exception {
        registerCustomer("ce_user", "13900000409");
        Cookie c = login("13900000409", "1234567");
        String newEmail = "newbind@email.com";
        String code = sendCode(newEmail, "CHANGE_EMAIL");

        mockMvc.perform(put("/api/user/email")
                        .cookie(c)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"newEmail\":\"%s\",\"code\":\"%s\"}", newEmail, code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        assertLogin(newEmail, "1234567", ResultCodeEnum.SUCCESS);
    }
}
