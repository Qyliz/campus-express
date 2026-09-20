package cn.njust.campusexpress.user;

import cn.njust.campusexpress.IntegrationTestSupport;
import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SelfServiceTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    //验证修改密码、会话登出以及新旧密码登录结果
    @Test
    void changePassword() throws Exception {
        registerCustomer("pwd_user", "13900000201");
        Cookie c = login("13900000201", "1234567", "CUSTOMER");

        //旧密码不正确
        mockMvc.perform(put("/api/user/password")
                        .cookie(c)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"wrong1\",\"newPassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.OLD_PASSWORD_ERROR.getCode()));

        //旧密码正确时修改成功
        mockMvc.perform(put("/api/user/password")
                        .cookie(c)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"1234567\",\"newPassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        //改密成功后原会话已登出
        mockMvc.perform(get("/api/user/profile").cookie(c))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.UNAUTHORIZED.getCode()));

        //新密码可登录
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"13900000201\",\"password\":\"654321\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        //旧密码已失效
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"13900000201\",\"password\":\"1234567\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.LOGIN_ERROR.getCode()));
    }

    //新密码长度不合法时返回参数错误
    @Test
    void changePasswordParamInvalid() throws Exception {
        registerCustomer("pwd_user2", "13900000202");
        Cookie c = login("13900000202", "1234567", "CUSTOMER");
        mockMvc.perform(put("/api/user/password")
                        .cookie(c)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"1234567\",\"newPassword\":\"12\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
    }

    //注销当前角色账号后该角色无法再次登录
    @Test
    void deleteAccountThenLoginFails() throws Exception {
        registerCustomer("del_user", "13900000203");
        Cookie c = login("13900000203", "1234567", "CUSTOMER");

        mockMvc.perform(delete("/api/user/account").cookie(c))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        //角色逻辑删除后登录失败
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"13900000203\",\"password\":\"1234567\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.LOGIN_ERROR.getCode()));
    }
}
