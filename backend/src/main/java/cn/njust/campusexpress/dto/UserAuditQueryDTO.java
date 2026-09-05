package cn.njust.campusexpress.dto;

import cn.njust.campusexpress.common.enums.OrderEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserAuditQueryDTO {
    //搜索字段
    private String username;
    private String phone;
    private String email;
    //审核状态
    private UserStatusEnum auditStatus;

    @NotNull(message = "页数不能为空")
    private Integer currentPage;

    @NotNull(message = "排序方式不能为空")
    private OrderEnum order;
}
