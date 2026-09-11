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
    public synchronized String send(String account, VerifySceneEnum scene) {
        if (scene == VerifySceneEnum.REGISTER && (account == null ||
                !(account.matches("^1[3-9]\\d{9}$") ||
                        (account.length() <= 254 && account.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))))) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "请填写正确的手机号或邮箱");
        }
        String code = generate();
        dao().set(key(scene, account), code, EXPIRE_SECONDS);
        return code;
    }

    /**
     * 校验验证码，通过后立即删除（一次性）。不存在/过期抛 EXPIRED，不匹配抛 ERROR。
     */
    public synchronized void verify(String account, VerifySceneEnum scene, String code) {
        check(account, scene, code);
        dao().delete(key(scene, account));
    }

    /** 校验验证码但不消费，供忘记密码的分步表单进入下一步前使用。 */
    public synchronized void validate(String account, VerifySceneEnum scene, String code) {
        check(account, scene, code);
    }

    /** 注册时先检查全部验证码，再统一消费，避免第二项错误导致第一项验证码丢失。 */
    public synchronized void verifyRegistration(String phone, String phoneCode, String email, String emailCode) {
        if (phone != null) check(phone, VerifySceneEnum.REGISTER, phoneCode);
        if (email != null) check(email, VerifySceneEnum.REGISTER, emailCode);
        if (phone != null) dao().delete(key(VerifySceneEnum.REGISTER, phone));
        if (email != null) dao().delete(key(VerifySceneEnum.REGISTER, email));
    }

    // 校验但不消费验证码，由调用方决定何时删除。
    private void check(String account, VerifySceneEnum scene, String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ResultCodeEnum.PARAM_MISSING, "请填写验证码");
        }
        String saved = dao().get(key(scene, account));
        if (saved == null) {
            throw new BusinessException(ResultCodeEnum.VERIFY_CODE_EXPIRED);
        }
        if (!saved.equals(code)) {
            throw new BusinessException(ResultCodeEnum.VERIFY_CODE_ERROR);
        }
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
