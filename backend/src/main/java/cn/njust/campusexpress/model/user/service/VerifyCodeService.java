package cn.njust.campusexpress.model.user.service;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.njust.campusexpress.common.enums.ResultCodeEnum;
import cn.njust.campusexpress.common.enums.VerifySceneEnum;
import cn.njust.campusexpress.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * 验证码服务：基于 Sa-Token 的 SaTokenDao（自带 TTL 的 KV 存储）保存验证码。
 * 键格式 vc:{scene}:{account}，与登录态无关，登录/未登录场景通用。
 */
@SuppressWarnings("unused")
@Service
public class VerifyCodeService {

    private static final int CODE_LENGTH = 6;
    private static final long EXPIRE_SECONDS = 300; // 5 分钟
    private static final String KEY_PREFIX = "vc:";

    private final SecureRandom random = new SecureRandom();

    /**
     * 生成并存储验证码。桩版直接返回该码，由前端展示以模拟"发送成功"。
     */
    public String send(String account, VerifySceneEnum scene) {
        String code = generate();
        dao().set(key(scene, account), code, EXPIRE_SECONDS);
        return code;
    }

    /**
     * 校验验证码，通过后立即删除（一次性）。不存在/过期抛 EXPIRED，不匹配抛 ERROR。
     */
    public void verify(String account, VerifySceneEnum scene, String code) {
        String saved = dao().get(key(scene, account));
        if (saved == null) {
            throw new BusinessException(ResultCodeEnum.VERIFY_CODE_EXPIRED);
        }
        if (!saved.equals(code)) {
            throw new BusinessException(ResultCodeEnum.VERIFY_CODE_ERROR);
        }
        dao().delete(key(scene, account));
    }

    private SaTokenDao dao() {
        return SaManager.getSaTokenDao();
    }

    private String key(VerifySceneEnum scene, String account) {
        return KEY_PREFIX + scene.name() + ":" + account;
    }

    private String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
