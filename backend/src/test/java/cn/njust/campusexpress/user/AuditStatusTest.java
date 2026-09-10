package cn.njust.campusexpress.user;

import cn.njust.campusexpress.common.enums.*;
import cn.njust.campusexpress.model.user.dto.UserAuditQueryDTO;
import cn.njust.campusexpress.model.user.entity.*;
import cn.njust.campusexpress.model.user.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuditStatusTest {
    @Autowired UserService users;
    @Autowired RoleAccountService accounts;
    @Autowired UserAuditRecordService audits;

    @Test
    void filterAndListUseAuditRecordStatusEvenWhenAccountIsDisabled() {
        User user = new User();
        user.setUsername("审核" + UUID.randomUUID().toString().substring(0, 8));
        user.setGender(UserGenderEnum.UNKNOWN);
        user.setPassword("unused");
        users.save(user);
        RoleAccount courier = accounts.createAccount(user.getId(), UserRoleEnum.COURIER, UserStatusEnum.DISABLED);
        for (AuditStatusEnum state : AuditStatusEnum.values()) {
            UserAuditRecord record = new UserAuditRecord();
            record.setCourierId(courier.getId());
            record.setStatus(state);
            audits.save(record);
        }
        for (AuditStatusEnum state : AuditStatusEnum.values()) {
            UserAuditQueryDTO query = new UserAuditQueryDTO();
            query.setUsername(user.getUsername());
            query.setAuditStatus(state);
            var page = audits.getRecordPage(query);
            assertEquals(1, page.getTotal());
            assertEquals(state, page.getRecords().get(0).getStatus());
        }
        assertEquals(UserStatusEnum.DISABLED, accounts.getByUserAndRole(user.getId(), UserRoleEnum.COURIER).getStatus());
    }
}
