package cn.njust.campusexpress.model.user.dto;

import cn.njust.campusexpress.common.enums.SortEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UserAuditQueryDTO {
    //搜索字段
    private String username;
    private String phone;
    private String email;
    //审核状态
    private UserStatusEnum auditStatus;
    //删除状态筛选：null=全部（完整留痕），false=仅未删除，true=仅已删除
    private Boolean deleted;

    @Min(value = 1, message = "页码必须大于0")
    private Integer currentPage;

    //排序方式
    private SortEnum sort;
}
