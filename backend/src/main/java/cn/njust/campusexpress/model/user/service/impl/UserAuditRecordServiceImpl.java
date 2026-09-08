package cn.njust.campusexpress.model.user.service.impl;

import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.user.dto.UserAuditDTO;
import cn.njust.campusexpress.model.user.dto.UserAuditQueryDTO;
import cn.njust.campusexpress.model.user.entity.UserAuditRecord;
import cn.njust.campusexpress.model.user.entity.UserRole;
import cn.njust.campusexpress.model.user.mapper.UserAuditRecordMapper;
import cn.njust.campusexpress.model.user.service.UserAuditRecordService;
import cn.njust.campusexpress.model.user.service.UserRoleService;
import cn.njust.campusexpress.model.user.vo.UserAuditRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;


@Service
@RequiredArgsConstructor
public class UserAuditRecordServiceImpl extends CrudRepository<UserAuditRecordMapper, UserAuditRecord>
        implements UserAuditRecordService {

    private final UserAuditRecordMapper mapper;
    private final UserRoleService userRoleService;

    //获取审核记录
    @Override
    public Page<UserAuditRecordVO> getRecordPage(UserAuditQueryDTO dto) {
        //页码为空时默认为第1页
        int currentPage = dto.getCurrentPage() == null ? 1 : dto.getCurrentPage();
        Page<UserAuditRecordVO> page = new Page<>(currentPage, 10);
        //关联 user_audit_record、user_role 与 user 表分页查询，查询条件与排序在 UserAuditRecordMapper.xml 中动态拼接
        mapper.selectAuditPage(page, dto);
        return page;
    }

    //审核账号
    @Override
    @Transactional
    public void auditUser(UserAuditDTO dto) {
        //审核结果只能是通过或驳回
        UserStatusEnum result = dto.getStatus();
        if (result != UserStatusEnum.NORMAL && result != UserStatusEnum.REJECTED) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "审核结果只能为通过或驳回");
        }
        UserAuditRecord record = getById(dto.getUserAuditRecordId());
        if (record == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        //账号已审核
        UserStatusEnum oldState1 = record.getStatus();
        if (Objects.requireNonNull(oldState1) == UserStatusEnum.NORMAL) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_REVIEWED);
        }
        //更新user_audit_record表
        record.setStatus(dto.getStatus());
        record.setReason(dto.getReason());
        updateById(record);
        //获取user_role表数据
        UserRole userRole = userRoleService.getById(record.getUserRoleId());
        if (userRole == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        //检查账号状态
        UserStatusEnum oldState2 = userRole.getStatus();
        switch (oldState2) {
            case NORMAL ->
                    throw new BusinessException(ResultCodeEnum.ACCOUNT_REVIEWED);
            case DISABLED ->
                    throw new BusinessException(ResultCodeEnum.ACCOUNT_DISABLED);
        }
        //更新user_role表
        userRole.setStatus(dto.getStatus());
        userRoleService.updateById(userRole);
    }
}




