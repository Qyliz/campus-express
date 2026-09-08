package cn.njust.campusexpress.model.user.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@SuppressWarnings("unused")
@Data
public class UserUsernameDTO {
    @Pattern(regexp = "^[a-zA-Z0-9_\\-\\u4e00-\\u9fa5]{1,10}$", message = "用户名必须是1-10位的中文、字母、数字或下划线")
    private String username;

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }
}
