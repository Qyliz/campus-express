package cn.njust.campusexpress.model.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 强制下线只需要 userId：Sa-Token 的 loginId 就是 user.id，一个用户同一时刻只有一个在线会话。
 */
@Data
public class UserKickoutDTO {
    @NotNull(message = "用户id不能为空")
    private Long userId;
}
