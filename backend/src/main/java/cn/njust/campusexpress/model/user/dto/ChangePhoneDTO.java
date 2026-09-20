package cn.njust.campusexpress.model.user.dto;

import cn.njust.campusexpress.common.util.StringUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

//换绑手机号DTO
@SuppressWarnings("unused")
@Data
public class ChangePhoneDTO {

    @NotBlank(message = "新手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String newPhone;

    @NotBlank(message = "验证码不能为空")
    private String code;

    public void setNewPhone(String newPhone) {
        this.newPhone = StringUtil.clean(newPhone);
    }

    public void setCode(String code) {
        this.code = StringUtil.clean(code);
    }
}
