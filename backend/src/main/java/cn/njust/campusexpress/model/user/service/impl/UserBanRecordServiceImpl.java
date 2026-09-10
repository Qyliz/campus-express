package cn.njust.campusexpress.model.user.service.impl;

import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.user.dto.UserBanDTO;
import cn.njust.campusexpress.model.user.dto.UserBanQueryDTO;
import cn.njust.campusexpress.model.user.entity.RoleAccount;
import cn.njust.campusexpress.model.user.entity.UserBanRecord;
import cn.njust.campusexpress.model.user.mapper.UserBanRecordMapper;
import cn.njust.campusexpress.model.user.service.RoleAccountService;
import cn.njust.campusexpress.model.user.service.UserBanRecordService;
import cn.njust.campusexpress.model.user.vo.UserBanRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UserBanRecordServiceImpl extends CrudRepository<UserBanRecordMapper, UserBanRecord>
        implements UserBanRecordService {

    private final UserBanRecordMapper mapper;
    private final RoleAccountService roleAccountService;

    @Override
    @Transactional
    public void banUser(Long userId, UserRoleEnum role, UserBanDTO dto) {
        RoleAccount account = roleAccountService.getByUserAndRole(userId, role);
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        if (account.getStatus() == UserStatusEnum.DISABLED) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_DISABLED);
        }
        roleAccountService.updateStatus(account, role, UserStatusEnum.DISABLED);
        UserBanRecord record = new UserBanRecord();
        record.setUserId(userId);
        record.setRole(role);
        record.setReason(dto.getReason());
        save(record);
    }

    @Override
    @Transactional
    public void unbanUser(Long userId, UserRoleEnum role) {
        RoleAccount account = roleAccountService.getByUserAndRole(userId, role);
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }
        //账号当前未处于封禁状态
        if (account.getStatus() != UserStatusEnum.DISABLED) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_BANNED);
        }
        //解封该账号最近一条生效中的封禁记录（unbanned=0）
        UserBanRecord record = lambdaQuery()
                .eq(UserBanRecord::getUserId, userId)
                .eq(UserBanRecord::getRole, role)
                .eq(UserBanRecord::getUnbanned, 0)
                .orderByDesc(UserBanRecord::getId)
                .last("LIMIT 1")
                .one();
        if (record != null) {
            record.setUnbanned(1);
            updateById(record);
        }
        //恢复账号状态为正常
        roleAccountService.updateStatus(account, role, UserStatusEnum.NORMAL);
    }

    @Override
    public Page<UserBanRecordVO> getBanRecordPage(UserBanQueryDTO dto) {
        //页码为空时默认为第1页
        int currentPage = dto.getCurrentPage() == null ? 1 : dto.getCurrentPage();
        Page<UserBanRecordVO> page = new Page<>(currentPage, 10);
        //关联 user_ban_record 与 user 表分页查询（角色是记录表自己的列），查询条件与排序在 UserBanRecordMapper.xml 中动态拼接
        mapper.selectBanPage(page, dto);
        return page;
    }
}
