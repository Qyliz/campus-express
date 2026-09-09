package cn.njust.campusexpress.user;

import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.model.user.entity.User;
import cn.njust.campusexpress.model.user.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminUserTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    private Cookie adminCookie;

    @BeforeEach
    void adminLogin() throws Exception {
        //管理员由 data.sql 在启动时种子（admin@email.com / IamADMIN）
        adminCookie = login("admin@email.com", "IamADMIN", "ADMIN");
        Assertions.assertNotNull(adminCookie, "管理员登录应返回 satoken");
    }

    //注册收寄件人（初始状态 NORMAL），返回其 userId（管理端接口按 userId + role 定位账号）
    private Long registerCustomer(String username, String phone) throws Exception {
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
        return user.getId();
    }

    private Cookie login(String account, String password, String role) throws Exception {
        String body = String.format("{\"account\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}", account, password, role);
        MvcResult result = mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn();
        return result.getResponse().getCookie("satoken");
    }

    //管理员分页查询所有账号（A1 查询串绑定）
    @Test
    void getAllUsersOk() throws Exception {
        mockMvc.perform(get("/api/user/all-users")
                        .param("currentPage", "1")
                        .param("sort", "CREATE_TIME_DESC")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    //非法页码触发查询参数校验失败 -> PARAM_ERROR（验证 BindException 处理器）
    @Test
    void getAllUsersInvalidPage() throws Exception {
        mockMvc.perform(get("/api/user/all-users")
                        .param("currentPage", "0")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
    }

    //一人持多角色时管理端列表每个角色各占一行；同时验证 UNION 派生表下 MP 自动生成的 COUNT 正确
    @Test
    void getAllUsersCountsOneRowPerRole() throws Exception {
        mockMvc.perform(multipart("/api/user/register")
                        .param("username", "dual_role")
                        .param("password", "1234567")
                        .param("role", "CUSTOMER")
                        .param("gender", "MALE")
                        .param("phone", "13900000601"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        //同手机号 + 同密码追加配送员角色；此处填的用户名/性别应被忽略
        mockMvc.perform(multipart("/api/user/register")
                        .file(new MockMultipartFile("material", "m.png", "image/png", new byte[]{1, 2, 3}))
                        .param("username", "ignored")
                        .param("password", "1234567")
                        .param("role", "COURIER")
                        .param("gender", "FEMALE")
                        .param("phone", "13900000601"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        mockMvc.perform(get("/api/user/all-users")
                        .param("currentPage", "1")
                        .param("username", "dual_role")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[*].role",
                        org.hamcrest.Matchers.containsInAnyOrder("CUSTOMER", "COURIER")))
                .andExpect(jsonPath("$.data.records[*].username",
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("dual_role"))));
    }

    //管理员分页查询审核记录
    @Test
    void getAuditRecordOk() throws Exception {
        mockMvc.perform(get("/api/user/audit")
                        .param("currentPage", "1")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    //非管理员（收寄件人）访问管理端接口 -> 403 NO_PERMISSION
    @Test
    void customerAccessDenied() throws Exception {
        registerCustomer("cus_denied", "13900000101");
        Cookie customerCookie = login("13900000101", "1234567", "CUSTOMER");
        mockMvc.perform(get("/api/user/all-users")
                        .param("currentPage", "1")
                        .cookie(customerCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.NO_PERMISSION.getCode()));
    }

    //封禁 -> 在线会话被踢 -> 解封恢复登录 -> 封禁记录可查
    @Test
    void banKickoutAndUnban() throws Exception {
        Long userId = registerCustomer("cus_ban", "13900000102");
        Cookie customerCookie = login("13900000102", "1234567", "CUSTOMER");

        //封禁前可正常访问个人资料
        mockMvc.perform(get("/api/user/profile").cookie(customerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        //管理员封禁
        mockMvc.perform(post("/api/user/ban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"userId\":%d,\"role\":\"CUSTOMER\",\"reason\":\"违规操作\"}", userId))
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        //A2/SEC1：被禁账号的旧会话被踢出 -> 401 KICKED_OUT
        mockMvc.perform(get("/api/user/profile").cookie(customerCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.KICKED_OUT.getCode()));

        //封禁记录可查（至少一条）
        mockMvc.perform(get("/api/user/ban")
                        .param("currentPage", "1")
                        .param("unbanned", "false")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        //解封
        mockMvc.perform(post("/api/user/unban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"userId\":%d,\"role\":\"CUSTOMER\"}", userId))
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        //解封后可重新登录，说明状态恢复为 NORMAL
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"13900000102\",\"password\":\"1234567\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
    }

    //对未被封禁的账号执行解封 -> ACCOUNT_NOT_BANNED
    @Test
    void unbanNotBannedAccount() throws Exception {
        Long userId = registerCustomer("cus_notban", "13900000103");
        mockMvc.perform(post("/api/user/unban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"userId\":%d,\"role\":\"CUSTOMER\"}", userId))
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.ACCOUNT_NOT_BANNED.getCode()));
    }

    //封禁不存在的账号 -> USER_NOT_FOUND（验证 S1 NPE 修复）
    @Test
    void banNonexistentUser() throws Exception {
        mockMvc.perform(post("/api/user/ban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":999999999999,\"role\":\"CUSTOMER\",\"reason\":\"x\"}")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.USER_NOT_FOUND.getCode()));
    }

    //审核结果非通过/驳回 -> PARAM_ERROR（验证 A3 入口守卫）
    @Test
    void auditInvalidStatus() throws Exception {
        mockMvc.perform(put("/api/user/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAuditRecordId\":12345,\"status\":\"REVIEWING\"}")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()));
    }

    //合法审核结果 + 不存在的记录 -> USER_NOT_FOUND
    @Test
    void auditValidStatusNotFound() throws Exception {
        mockMvc.perform(put("/api/user/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAuditRecordId\":12345,\"status\":\"NORMAL\"}")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.USER_NOT_FOUND.getCode()));
    }

    //C1：收寄件人不产生审核记录，配送员产生一条
    @Test
    void auditRecordOnlyForReviewRoles() throws Exception {
        mockMvc.perform(multipart("/api/user/register")
                        .param("username", "cust_noaud")
                        .param("password", "1234567")
                        .param("role", "CUSTOMER")
                        .param("gender", "MALE")
                        .param("phone", "13900000302"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
        mockMvc.perform(get("/api/user/audit")
                        .param("username", "cust_noaud")
                        .param("currentPage", "1")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(0));

        MockMultipartFile material = new MockMultipartFile("material", "m.png", "image/png", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/user/register")
                        .file(material)
                        .param("username", "cour_audit")
                        .param("password", "1234567")
                        .param("role", "COURIER")
                        .param("gender", "MALE")
                        .param("phone", "13900000303"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));
        mockMvc.perform(get("/api/user/audit")
                        .param("username", "cour_audit")
                        .param("currentPage", "1")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].material").isNotEmpty());
    }

    //F7：配送员注册未提交材料 -> FILE_EMPTY
    @Test
    void courierRegisterRequiresMaterial() throws Exception {
        mockMvc.perform(multipart("/api/user/register")
                        .param("username", "cour_nomat")
                        .param("password", "1234567")
                        .param("role", "COURIER")
                        .param("gender", "MALE")
                        .param("phone", "13900000304"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.FILE_EMPTY.getCode()));
    }

    //D2：管理员强制下线 -> 被踢账号旧 token 返回 KICKED_OUT
    @Test
    void adminKickoutUser() throws Exception {
        Long userId = registerCustomer("cus_kick", "13900000301");
        Cookie customerCookie = login("13900000301", "1234567", "CUSTOMER");

        mockMvc.perform(get("/api/user/profile").cookie(customerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        mockMvc.perform(post("/api/user/kickout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"userId\":%d}", userId))
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        mockMvc.perform(get("/api/user/profile").cookie(customerCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.KICKED_OUT.getCode()));
    }

    //F8：管理员重置他人密码 -> 旧会话被踢、新密码可登、旧密码失效
    @Test
    void adminResetPassword() throws Exception {
        Long userId = registerCustomer("cus_reset", "13900000501");
        Cookie customerCookie = login("13900000501", "1234567", "CUSTOMER");

        mockMvc.perform(post("/api/user/admin/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"userId\":%d,\"newPassword\":\"adminset1\"}", userId))
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        //旧会话被踢
        mockMvc.perform(get("/api/user/profile").cookie(customerCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.KICKED_OUT.getCode()));

        //新密码可登录
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"13900000501\",\"password\":\"adminset1\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.SUCCESS.getCode()));

        //旧密码失效
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"13900000501\",\"password\":\"1234567\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.LOGIN_ERROR.getCode()));
    }
}
