package cn.njust.campusexpress.model.user.dto;

import cn.njust.campusexpress.common.util.StringUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserUsernameDTO {
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-\\u4e00-\\u9fa5]{1,10}$", message = "用户名必须是1-10位的中文、字母、数字或下划线")
    private String username;

    public void setUsername(String username) {
        this.username = StringUtil.clean(username);
    }
}
