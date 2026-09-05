package cn.njust.campusexpress;

import cn.njust.campusexpress.mapper.UserAuditRecordMapper;
import cn.njust.campusexpress.vo.UserAuditRecordVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TempTest {

    @Autowired
    private UserAuditRecordMapper mapper;

    @Test
    void test01() {
        Page<UserAuditRecordVO> page = new Page<>();
        LambdaQueryWrapper<UserAuditRecordVO> wrapper = new LambdaQueryWrapper<>();
        mapper.selectAuditRecord(page, wrapper);
    }
}
