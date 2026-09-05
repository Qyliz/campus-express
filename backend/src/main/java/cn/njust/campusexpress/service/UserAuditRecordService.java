package cn.njust.campusexpress.service;

import cn.njust.campusexpress.dto.UserAuditDTO;
import cn.njust.campusexpress.dto.UserAuditQueryDTO;
import cn.njust.campusexpress.entity.UserAuditRecord;
import cn.njust.campusexpress.vo.UserAuditRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import jakarta.validation.Valid;


public interface UserAuditRecordService extends IRepository<UserAuditRecord> {

    Page<UserAuditRecordVO> getRecordPage(@Valid UserAuditQueryDTO dto);

    void auditUser(@Valid UserAuditDTO dto);
}
