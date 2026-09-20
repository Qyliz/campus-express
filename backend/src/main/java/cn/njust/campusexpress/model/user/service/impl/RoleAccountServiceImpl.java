package cn.njust.campusexpress.model.user.service.impl;

import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import cn.njust.campusexpress.model.user.entity.Admin;
import cn.njust.campusexpress.model.user.entity.Customer;
import cn.njust.campusexpress.model.user.entity.Courier;
import cn.njust.campusexpress.model.user.entity.RoleAccount;
import cn.njust.campusexpress.model.user.service.AdminService;
import cn.njust.campusexpress.model.user.service.CustomerService;
import cn.njust.campusexpress.model.user.service.CourierService;
import cn.njust.campusexpress.model.user.service.RoleAccountService;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

//角色账户路由ServiceImpl
@Service
@RequiredArgsConstructor
public class RoleAccountServiceImpl implements RoleAccountService {

    private final CustomerService customerService;
    private final CourierService courierService;
    private final AdminService adminService;

    //查询用户的指定角色账户
    @Override
    public RoleAccount getByUserAndRole(Long userId, UserRoleEnum role) {
        return switch (role) {
            case CUSTOMER -> customerService.lambdaQuery().eq(Customer::getUserId, userId).one();
            case COURIER -> courierService.lambdaQuery().eq(Courier::getUserId, userId).one();
            case ADMIN -> adminService.lambdaQuery().eq(Admin::getUserId, userId).one();
        };
    }

    //为用户创建指定角色账户
    @Override
    public RoleAccount createAccount(Long userId, UserRoleEnum role, UserStatusEnum status) {
        return switch (role) {
            case CUSTOMER -> saveNew(new Customer(), customerService, userId, status);
            case COURIER -> saveNew(new Courier(), courierService, userId, status);
            case ADMIN -> saveNew(new Admin(), adminService, userId, status);
        };
    }

    //更新指定角色账户状态
    @Override
    public void updateStatus(RoleAccount account, UserRoleEnum role, UserStatusEnum status) {
        account.setStatus(status);
        switch (role) {
            case CUSTOMER -> customerService.updateById((Customer) account);
            case COURIER -> courierService.updateById((Courier) account);
            case ADMIN -> adminService.updateById((Admin) account);
        }
    }

    //逻辑删除用户的指定角色账户
    @Override
    public boolean deleteByUserAndRole(Long userId, UserRoleEnum role) {
        RoleAccount account = getByUserAndRole(userId, role);
        if (account == null) {
            return false;
        }
        return switch (role) {
            case CUSTOMER -> customerService.removeById(account.getId());
            case COURIER -> courierService.removeById(account.getId());
            case ADMIN -> adminService.removeById(account.getId());
        };
    }

    //复用三类角色账户共有的初始化与保存步骤
    private <T extends RoleAccount> T saveNew(T account, IRepository<T> service,
                                              Long userId, UserStatusEnum status) {
        account.setUserId(userId);
        account.setStatus(status);
        service.save(account);
        return account;
    }
}
