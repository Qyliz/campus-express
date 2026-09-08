package cn.njust.campusexpress.model.user.dto;

import cn.njust.campusexpress.common.enums.SortEnum;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UserBanQueryDTO {
    //搜索字段
    private String username;
    private String phone;
    private String email;
    //封禁状态筛选：null=全部，false=仅封禁中(未解封)，true=仅已解封
    private Boolean unbanned;
    //删除状态筛选：null=全部（完整留痕），false=仅未删除，true=仅已删除
    private Boolean deleted;

    @Min(value = 1, message = "页码必须大于0")
    private Integer currentPage;

    //排序方式
    private SortEnum sort;
}
