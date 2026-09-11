package cn.njust.campusexpress.model.review.dto;
import cn.njust.campusexpress.common.enums.AppealStatusEnum;
import jakarta.validation.constraints.*;
/**
 * 处理申诉的结论。
 * ⚠️ 原来靠 {@code @Min(1)} 挡住「把申诉改回待处理」，换成枚举后 PENDING 也是合法值，
 *    这条规则搬到了 ReviewServiceImpl.resolve 里显式判断，不要两处都删。
 */
public record ResolveAppealDTO(@NotNull AppealStatusEnum status, @NotBlank String reason) {}
