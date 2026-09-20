package cn.njust.campusexpress.model.user.service;

import cn.njust.campusexpress.model.user.dto.UserAuditDTO;
import cn.njust.campusexpress.model.user.dto.UserAuditQueryDTO;
import cn.njust.campusexpress.model.user.entity.UserAuditRecord;
import cn.njust.campusexpress.model.user.vo.UserAuditRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;

//配送员审核模块Service
public interface UserAuditRecordService extends IRepository<UserAuditRecord> {

    //分页查询配送员审核记录
    Page<UserAuditRecordVO> getRecordPage(UserAuditQueryDTO dto);

    //审核配送员申请
    void auditUser(Long recordId, UserAuditDTO dto);
}
