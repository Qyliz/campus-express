package cn.njust.campusexpress.service.impl;

import cn.njust.campusexpress.common.enums.OrderEnum;
import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.dto.UserAuditDTO;
import cn.njust.campusexpress.dto.UserAuditQueryDTO;
import cn.njust.campusexpress.entity.UserAuditRecord;
import cn.njust.campusexpress.entity.UserRole;
import cn.njust.campusexpress.mapper.UserAuditRecordMapper;
import cn.njust.campusexpress.service.UserAuditRecordService;
import cn.njust.campusexpress.service.UserRoleService;
import cn.njust.campusexpress.vo.UserAuditRecordVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    @SuppressWarnings("DuplicatedCode")
    @Override
    public Page<UserAuditRecordVO> getRecordPage(UserAuditQueryDTO dto) {
        Page<UserAuditRecordVO> page = new Page<>(dto.getCurrentPage(), 10);
        LambdaQueryWrapper<UserAuditRecordVO> wrapper = new LambdaQueryWrapper<>();
        //搜索内容
        if (dto.getUsername() != null) {
            wrapper.eq(UserAuditRecordVO::getUsername, dto.getUsername());
        }
        if (dto.getPhone() != null) {
            wrapper.eq(UserAuditRecordVO::getPhone, dto.getPhone());
        }
        if (dto.getEmail() != null) {
            wrapper.eq(UserAuditRecordVO::getEmail, dto.getEmail());
        }
        //筛选审核状态
        UserStatusEnum auditStatus = dto.getAuditStatus();
        if (auditStatus != null) {
            switch (auditStatus) {
                case NORMAL ->
                        wrapper.eq(UserAuditRecordVO::getStatus, UserStatusEnum.NORMAL);
                case REVIEWING ->
                        wrapper.eq(UserAuditRecordVO::getStatus, UserStatusEnum.REVIEWING);
                case REJECTED ->
                        wrapper.eq(UserAuditRecordVO::getStatus, UserStatusEnum.REJECTED);
            }
        }
        //排序方式
        OrderEnum order = dto.getOrder();
        switch (order) {
            case CREATE_TIME_ASC ->
                    wrapper.orderByAsc(UserAuditRecordVO::getCreateTime);
            case CREATE_TIME_DESC ->
                    wrapper.orderByDesc(UserAuditRecordVO::getCreateTime);
            case UPDATE_TIME_ASC ->
                    wrapper.orderByAsc(UserAuditRecordVO::getUpdateTime);
            case UPDATE_TIME_DESC ->
                    wrapper.orderByDesc(UserAuditRecordVO::getUpdateTime);
            default -> throw new BusinessException(ResultCodeEnum.PARAM_ERROR);
        }
        mapper.selectAuditRecord(page, wrapper);
        return page;
    }

    //审核账号
    @Override
    @Transactional
    public void auditUser(UserAuditDTO dto) {
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




