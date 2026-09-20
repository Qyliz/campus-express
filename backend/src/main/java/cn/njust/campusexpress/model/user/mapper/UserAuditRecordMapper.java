package cn.njust.campusexpress.model.user.mapper;

import cn.njust.campusexpress.model.user.dto.UserAuditQueryDTO;
import cn.njust.campusexpress.model.user.entity.UserAuditRecord;
import cn.njust.campusexpress.model.user.vo.UserAuditRecordVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

@SuppressWarnings("UnusedReturnValue")
public interface UserAuditRecordMapper extends BaseMapper<UserAuditRecord> {

    //分页查询审核记录列表
    Page<UserAuditRecordVO> selectAuditPage(Page<UserAuditRecordVO> page,
                                            @Param("dto") UserAuditQueryDTO dto);
}




