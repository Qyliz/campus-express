package cn.njust.campusexpress.user;

import cn.dev33.satoken.stp.StpUtil;
import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class RegisterLoginTest {

    @Autowired
    private MockMvc mockMvc;

    //构造注册（multipart）请求；配送员需带审核材料，收寄件人不带
    private MockMultipartHttpServletRequestBuilder register(String username, String phone, String email, String role, boolean withMaterial) {
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/user/register");
        builder.param("username", username);
        builder.param("password", "1234567");
        builder.param("role", role);
        builder.param("gender", "MALE");
        builder.param("phone", phone);
        if (email != null) {
            builder.param("email", email);
        }
        if (withMaterial) {
            builder.file(new MockMultipartFile("material", "m.png", "image/png", new byte[]{1, 2, 3}));
        }
        return builder;
    }

    private MockHttpServletRequestBuilder login(String account, String password, String role) {
        String body = String.format("{\"account\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}", account, password, role);
        return post("/api/user/login").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void RegisterTest() throws Exception {
        //注册成功（收寄件人，无需材料）
        mockMvc.perform(register("zhangsan", "13788888888", "qwert@email.com", "CUSTOMER", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
        //手机号已绑定（配送员，但在手机号查重处即失败，无需材料）
        mockMvc.perform(register("lisi", "13788888888", null, "COURIER", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PHONE_ALREADY_BIND.getCode()));
        //不能注册管理员
        mockMvc.perform(register("lisi", "13888888888", null, "ADMIN", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
    }

    @Test
    public void LoginTest() throws Exception {
        //注册
        mockMvc.perform(register("zhangsan", "13788888888", "qwert@email.com", "CUSTOMER", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
        //登录成功
        mockMvc.perform(login("13788888888", "1234567", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
        //参数错误
        mockMvc.perform(login("", "1234567", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
        //账号不存在、身份不正确
        mockMvc.perform(login("13788888888", "1234567", "COURIER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.LOGIN_ERROR.getCode()));
        //密码错误
        mockMvc.perform(login("13788888888", "12345678", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.LOGIN_ERROR.getCode()));
        //注册配送员（需材料）-> 审核中
        mockMvc.perform(register("lisi", "13888888888", null, "COURIER", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
        //账号状态异常（审核中不可登录）
        mockMvc.perform(login("13888888888", "1234567", "COURIER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.ACCOUNT_REVIEWING.getCode()));
    }

    @Test
    void LogoutTest() throws Exception {
        //未登录
        mockMvc.perform(post("/api/user/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.UNAUTHORIZED.getCode()));
        //已登录
        String token = StpUtil.createLoginSession(10001L);
        Assertions.assertEquals("10001", StpUtil.getLoginIdByToken(token));
        mockMvc.perform(post("/api/user/logout")
                        .header(StpUtil.getTokenName(), token))
                .andExpect(status().isOk());
        Assertions.assertNull(StpUtil.getLoginIdByToken(token));
    }
}
