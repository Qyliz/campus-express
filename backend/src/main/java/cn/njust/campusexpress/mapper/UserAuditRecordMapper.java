package cn.njust.campusexpress.mapper;

import cn.njust.campusexpress.entity.UserAuditRecord;
import cn.njust.campusexpress.vo.UserAuditRecordVO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;


public interface UserAuditRecordMapper extends BaseMapper<UserAuditRecord> {
    Page<UserAuditRecordVO> selectAuditRecord(
            Page<UserAuditRecordVO> page,
            @Param("ew") Wrapper<UserAuditRecordVO> wrapper);

}




