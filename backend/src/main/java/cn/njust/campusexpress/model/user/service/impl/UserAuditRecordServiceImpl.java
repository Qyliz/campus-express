package cn.njust.campusexpress.model.user.service.impl;

import cn.njust.campusexpress.common.PageResult;
import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.common.enums.AuditStatusEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.user.dto.UserAuditDTO;
import cn.njust.campusexpress.model.user.dto.UserAuditQueryDTO;
import cn.njust.campusexpress.model.user.entity.Courier;
import cn.njust.campusexpress.model.user.entity.UserAuditRecord;
import cn.njust.campusexpress.model.user.mapper.UserAuditRecordMapper;
import cn.njust.campusexpress.model.user.service.CourierService;
import cn.njust.campusexpress.model.user.service.UserAuditRecordService;
import cn.njust.campusexpress.model.user.vo.UserAuditRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

//配送员审核模块ServiceImpl
@Service
@RequiredArgsConstructor
public class UserAuditRecordServiceImpl extends CrudRepository<UserAuditRecordMapper, UserAuditRecord>
        implements UserAuditRecordService {

    private final UserAuditRecordMapper mapper;
    private final CourierService courierService;

    //分页查询配送员审核记录
    @Override
    public Page<UserAuditRecordVO> getRecordPage(UserAuditQueryDTO dto) {
        Page<UserAuditRecordVO> page = PageResult.pageOf(dto.getCurrentPage());
        //关联 user_audit_record、courier 与 user 表分页查询，查询条件与排序在 UserAuditRecordMapper.xml 中动态拼接
        mapper.selectAuditPage(page, dto);
        return page;
    }

    //审核配送员申请
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditUser(Long recordId, UserAuditDTO dto) {
        //审核结果只能是通过或驳回
        AuditStatusEnum result = dto.getStatus();
        if (result != AuditStatusEnum.NORMAL && result != AuditStatusEnum.REJECTED) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "审核结果只能为通过或驳回");
        }
        UserAuditRecord record = getById(recordId);
        if (record == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        //账号已审核
        AuditStatusEnum oldState1 = record.getStatus();
        if (Objects.requireNonNull(oldState1) == AuditStatusEnum.NORMAL) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_REVIEWED);
        }
        //审核记录与配送员账户状态在同一事务内同步更新
        record.setStatus(dto.getStatus());
        record.setReason(dto.getReason());
        updateById(record);
        //根据审核记录读取对应配送员账户
        Courier courier = courierService.getById(record.getCourierId());
        if (courier == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        //只允许审核仍处于待审核或已驳回状态的配送员
        UserStatusEnum oldState2 = courier.getStatus();
        switch (oldState2) {
            case NORMAL ->
                    throw new BusinessException(ResultCodeEnum.ACCOUNT_REVIEWED);
            case DISABLED ->
                    throw new BusinessException(ResultCodeEnum.ACCOUNT_DISABLED);
        }
        //审核结果同步到配送员账户状态
        courier.setStatus(result == AuditStatusEnum.NORMAL ? UserStatusEnum.NORMAL : UserStatusEnum.REJECTED);
        courierService.updateById(courier);
    }
}
