package cn.njust.campusexpress.user;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SessionProbeTest {
    @Autowired MockMvc mvc;

    @Test
    void anonymousProbeReturnsNormalEmptyResult() throws Exception {
        mvc.perform(get("/api/user/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void invalidSessionAlsoReturnsEmptyResult() throws Exception {
        mvc.perform(get("/api/user/session").cookie(new Cookie("satoken", "invalid-session-test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void loggedInProbeReturnsProfileAndLogoutClearsIt() throws Exception {
        Cookie cookie = mvc.perform(post("/api/user/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"account\":\"admin@email.com\",\"password\":\"IamADMIN\",\"role\":\"ADMIN\"}"))
                .andExpect(jsonPath("$.code").value(0)).andReturn().getResponse().getCookie("satoken");
        assertNotNull(cookie);
        mvc.perform(get("/api/user/session").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.email").value("admin@email.com"));
        mvc.perform(post("/api/user/logout").cookie(cookie)).andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/api/user/session").cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void protectedEndpointsStillRejectAnonymousRequests() throws Exception {
        mvc.perform(get("/api/user/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/order/mine")).andExpect(status().isUnauthorized());
    }
}
