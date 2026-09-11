package cn.njust.campusexpress.model.user.dto;

import cn.njust.campusexpress.common.enums.VerifySceneEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyCodeDTO {

    @NotBlank(message = "账号不能为空")
    private String account;

    @NotNull(message = "场景不能为空")
    private VerifySceneEnum scene;

    @NotBlank(message = "验证码不能为空")
    private String code;

    public void setAccount(String account) {
        this.account = account == null ? null : account.trim();
    }

    public void setCode(String code) {
        this.code = code == null ? null : code.trim();
    }
}
