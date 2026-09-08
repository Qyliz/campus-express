package cn.njust.campusexpress.model.user.service.impl;

import cn.njust.campusexpress.model.user.entity.UserRole;
import cn.njust.campusexpress.model.user.mapper.UserRoleMapper;
import cn.njust.campusexpress.model.user.service.UserRoleService;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public class UserRoleServiceImpl extends CrudRepository<UserRoleMapper, UserRole>
        implements UserRoleService {

}




