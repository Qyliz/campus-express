package cn.njust.campusexpress.model.user.dto;

import cn.njust.campusexpress.common.enums.VerifySceneEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@SuppressWarnings("unused")
@Data
public class SendCodeDTO {

    @NotBlank(message = "账号不能为空")
    private String account;

    @NotNull(message = "场景不能为空")
    private VerifySceneEnum scene;

    public void setAccount(String account) {
        this.account = account == null ? null : account.trim();
    }
}
