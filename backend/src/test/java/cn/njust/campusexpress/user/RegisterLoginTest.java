package cn.njust.campusexpress.user;

import cn.dev33.satoken.stp.StpUtil;
import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class RegisterLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void RegisterTest() throws Exception {
        //注册成功
        String Json1 = """
                {
                    "username":"zhangsan",
                    "password":"1234567",
                    "role":"CUSTOMER",
                    "gender":"MALE",
                    "phone":"13788888888",
                    "email":"qwert@email.com"
                }
                """;
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
        //手机号已绑定
        String Json2 = """
                {
                    "username":"lisi",
                    "password":"1234567",
                    "role":"COURIER",
                    "gender":"MALE",
                    "phone":"13788888888"
                }
                """;
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PHONE_ALREADY_BIND.getCode()));
        //不能注册管理员
        String Json3 = """
                {
                    "username":"lisi",
                    "password":"1234567",
                    "role":"ADMIN",
                    "gender":"MALE",
                    "phone":"13888888888"
                }
                """;
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
    }

    @Test
    public void LoginTest() throws Exception {
        //注册
        String Json1 = """
                {
                    "username":"zhangsan",
                    "password":"1234567",
                    "role":"CUSTOMER",
                    "gender":"MALE",
                    "phone":"13788888888",
                    "email":"qwert@email.com"
                }
                """;
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
        //登录成功
        String Json2 = """
                {
                    "account":"13788888888",
                    "password":"1234567",
                    "role":"CUSTOMER"
                }
                """;
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
        //参数错误
        String Json3 = """
                {
                    "account":"",
                    "password":"1234567",
                    "role":"CUSTOMER"
                }
                """;
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
        //账号不存在、身份不正确
        String Json4 = """
                {
                    "account":"13788888888",
                    "password":"1234567",
                    "role":"COURIER"
                }
                """;
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.LOGIN_ERROR.getCode()));
        //密码错误
        String Json5 = """
                {
                    "account":"13788888888",
                    "password":"12345678",
                    "role":"CUSTOMER"
                }
                """;
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.LOGIN_ERROR.getCode()));
        //账号状态异常
        String Json6 = """
                {
                    "username":"lisi",
                    "password":"1234567",
                    "role":"COURIER",
                    "gender":"MALE",
                    "phone":"13888888888"
                }
                """;
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json6))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
        String Json7 = """
                {
                    "account":"13888888888",
                    "password":"1234567",
                    "role":"COURIER"
                }
                """;
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json7))
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
