package cn.njust.campusexpress.user;

import cn.njust.campusexpress.common.enums.VerifySceneEnum;
import cn.njust.campusexpress.model.user.service.VerifyCodeService;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/** 普通业务测试通过真实验证码服务准备注册请求；验证码异常由专门测试覆盖。 */
public final class RegistrationTestSupport {
    private RegistrationTestSupport() {}

    public static MockMultipartHttpServletRequestBuilder registration(VerifyCodeService codes) {
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/user/register");
        builder.with(request -> {
            for (String field : new String[]{"phone", "email"}) {
                String account = request.getParameter(field);
                if (account != null && !account.isBlank()) {
                    request.addParameter(field + "Code", codes.send(account, VerifySceneEnum.REGISTER));
                }
            }
            return request;
        });
        return builder;
    }
}
