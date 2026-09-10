package cn.njust.campusexpress.model.user.service;

import cn.njust.campusexpress.model.user.dto.UserAuditDTO;
import cn.njust.campusexpress.model.user.dto.UserAuditQueryDTO;
import cn.njust.campusexpress.model.user.entity.UserAuditRecord;
import cn.njust.campusexpress.model.user.vo.UserAuditRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;


public interface UserAuditRecordService extends IRepository<UserAuditRecord> {

    Page<UserAuditRecordVO> getRecordPage(UserAuditQueryDTO dto);

    void auditUser(Long recordId, UserAuditDTO dto);
}
