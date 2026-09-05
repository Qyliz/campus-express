package cn.njust.campusexpress.service.impl;

import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.dto.UserBanDTO;
import cn.njust.campusexpress.entity.UserBanRecord;
import cn.njust.campusexpress.entity.UserRole;
import cn.njust.campusexpress.mapper.UserBanRecordMapper;
import cn.njust.campusexpress.service.UserBanRecordService;
import cn.njust.campusexpress.service.UserRoleService;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UserBanRecordServiceImpl extends CrudRepository<UserBanRecordMapper, UserBanRecord>
        implements UserBanRecordService {

    private final UserRoleService userRoleService;

    @Override
    @Transactional
    public void banUser(UserBanDTO dto) {
        Long userRoleId = dto.getUserRoleId();
        UserRole userRole = userRoleService.getById(userRoleId);
        if (userRole.getStatus() == UserStatusEnum.DISABLED) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_DISABLED);
        }
        userRole.setStatus(UserStatusEnum.DISABLED);
        userRoleService.updateById(userRole);
        UserBanRecord record = new UserBanRecord();
        record.setUserRoleId(userRoleId);
        record.setReason(dto.getReason());
        save(record);
    }
}




