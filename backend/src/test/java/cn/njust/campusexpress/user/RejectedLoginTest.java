package cn.njust.campusexpress.user;
import cn.njust.campusexpress.common.enums.*;
import cn.njust.campusexpress.common.exception.BusinessException;
import cn.njust.campusexpress.model.user.dto.UserLoginDTO;
import cn.njust.campusexpress.model.user.entity.*;
import cn.njust.campusexpress.model.user.service.*;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RejectedLoginTest {
    @Autowired UserService users;
    @Autowired RoleAccountService accounts;
    @Autowired UserAuditRecordService audits;

    @Test
    void reasonFallbackAndPasswordProtection() {
        User user = new User();
        user.setUsername("驳回测试");
        user.setGender(UserGenderEnum.UNKNOWN);
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setPassword(BCrypt.hashpw("1234567", BCrypt.gensalt()));
        users.save(user);
        RoleAccount account = accounts.createAccount(user.getId(), UserRoleEnum.COURIER, UserStatusEnum.REJECTED);
        UserLoginDTO login = new UserLoginDTO();
        login.setAccount(user.getEmail());
        login.setPassword("1234567");
        login.setRole(UserRoleEnum.COURIER);
        assertTrue(assertThrows(BusinessException.class, () -> users.login(login)).getMessage().contains("未填写驳回原因"));
        UserAuditRecord record = new UserAuditRecord();
        record.setCourierId(account.getId());
        record.setStatus(AuditStatusEnum.REJECTED);
        record.setReason("材料照片不清晰");
        audits.save(record);
        BusinessException failure = assertThrows(BusinessException.class, () -> users.login(login));
        assertEquals(2006, failure.getCode());
        assertTrue(failure.getMessage().contains("材料照片不清晰"));
        record.setReason(" ");
        audits.updateById(record);
        assertTrue(assertThrows(BusinessException.class, () -> users.login(login)).getMessage().contains("未填写驳回原因"));
        login.setPassword("wrongpass");
        assertEquals(2002, assertThrows(BusinessException.class, () -> users.login(login)).getCode());
    }
}
