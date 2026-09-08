package cn.njust.campusexpress.model.user.mapper;

import cn.njust.campusexpress.model.user.dto.UserBanQueryDTO;
import cn.njust.campusexpress.model.user.entity.UserBanRecord;
import cn.njust.campusexpress.model.user.vo.UserBanRecordVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;


public interface UserBanRecordMapper extends BaseMapper<UserBanRecord> {

    /**
     * 分页查询封禁记录列表
     * <p>关联 user_ban_record、user_role 与 user 表，按 {@link UserBanQueryDTO} 中的条件动态过滤与排序。</p>
     *
     * @param page 分页参数
     * @param dto  查询条件（用户名、手机号、邮箱、解封状态、删除状态、排序方式）
     * @return 填充了封禁记录的分页对象
     */
    Page<UserBanRecordVO> selectBanPage(Page<UserBanRecordVO> page,
                                        @Param("dto") UserBanQueryDTO dto);
}




