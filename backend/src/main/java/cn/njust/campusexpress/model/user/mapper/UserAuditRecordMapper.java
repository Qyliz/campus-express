package cn.njust.campusexpress.model.user.mapper;

import cn.njust.campusexpress.model.user.dto.UserAuditQueryDTO;
import cn.njust.campusexpress.model.user.entity.UserAuditRecord;
import cn.njust.campusexpress.model.user.vo.UserAuditRecordVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

public interface UserAuditRecordMapper extends BaseMapper<UserAuditRecord> {

    /**
     * 分页查询审核记录列表
     * <p>关联 user_audit_record、courier 与 user 表（审核只针对配送员），
     * 按 {@link UserAuditQueryDTO} 中的条件动态过滤与排序。</p>
     *
     * @param page 分页参数
     * @param dto  查询条件（用户名、手机号、邮箱、审核状态、删除状态、排序方式）
     * @return 填充了审核记录的分页对象
     */
    Page<UserAuditRecordVO> selectAuditPage(Page<UserAuditRecordVO> page,
                                            @Param("dto") UserAuditQueryDTO dto);
}




