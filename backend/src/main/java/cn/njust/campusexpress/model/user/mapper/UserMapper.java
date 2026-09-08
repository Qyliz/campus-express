package cn.njust.campusexpress.model.user.mapper;

import cn.njust.campusexpress.model.user.dto.UserQueryDTO;
import cn.njust.campusexpress.model.user.entity.User;
import cn.njust.campusexpress.model.user.vo.UserProfileAdminVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<User> {

    /**
     * 分页查询管理端用户列表
     * <p>关联 user 与 user_role 表，按 {@link UserQueryDTO} 中的条件动态过滤与排序。</p>
     *
     * @param page 分页参数
     * @param dto  查询条件（用户名、手机号、邮箱、状态、删除状态、排序方式）
     * @return 填充了用户信息的分页对象
     */
    Page<UserProfileAdminVO> selectUserPage(Page<UserProfileAdminVO> page,
                                            @Param("dto") UserQueryDTO dto);
}




