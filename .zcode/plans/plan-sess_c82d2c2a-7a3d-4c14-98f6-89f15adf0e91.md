# campus-express 后端优化计划

## 一、schema.sql 优化(`backend/src/main/resources/db/schema.sql`)

1. **外键策略(核心,每个外键显式写 ON DELETE / ON UPDATE 并说明理由)**:
   - **组成关系(子行离开父行无意义)→ `ON DELETE CASCADE`**:customer/courier/admin 的 user_id→user.id;user_audit_record.courier_id→courier.id;user_ban_record.user_id→user.id;order_status_record.order_id→express_order.id;delivery_exception.order_id→express_order.id;service_review.order_id→express_order.id;review_appeal.review_id→service_review.id。
   - **跨实体引用(子行是独立业务数据,拒绝误删被引用行)→ `ON DELETE NO ACTION`**:express_order.customer_id/courier_id;delivery_exception.courier_id/admin_id;order_status_record.operator_id→user;service_review.user_id/courier_id;review_appeal.admin_id。
   - **review_appeal.order_id、review_appeal.courier_id 保持无外键**(冗余查询列,完整性经 review_id 级联链保证;若加 NO ACTION 外键会与 order→review→appeal 级联链冲突导致删单失败),加注释说明。
   - 所有外键 `ON UPDATE NO ACTION`(主键是雪花/固定 id,不变更);文件头加一段外键策略说明。
2. **时间列策略统一为「数据库默认值」**:全部 11 张表的 create_time / update_time 统一 `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`(customer/courier/admin 等由 timestamp 改 datetime,避免 2038 与时区隐式转换;delivery_exception/service_review/review_appeal 的 create_time 补上默认值;user_ban_record 补 update_time 列)。应用层不再负责插入时间。
3. **统一风格**:标识符统一加反引号、统一小写类型;每张表补表注释(订单 5 张表缺);补列注释(order_status_record 全表、service_review.rating 等);review_appeal 补 `idx_appeal_courier` 索引;user_audit_record/user_ban_record 的普通索引由 `fk_*` 改名 `idx_*`(消除误导)。
4. 文件头注明:改了列类型/外键后需重建库才能生效。

## 二、data.sql 优化(`backend/src/main/resources/db/data.sql`)

1. 头部增加 **id 区段分配总表**(每张表占用的保留 id 范围一览)。
2. **补齐 service_review、review_appeal 种子数据**(当前这两张表是空的):
   - 新增订单 9/10/11:全部 COMPLETED,骑手分别为王五/赵六/王五,version=5,补完整且单调递增的状态流转记录。
   - service_review 4 条:订单6(李四评赵六,5星,有效)、订单9(张三评王五,4星,有效)、订单10(李四评赵六,2星,**已作废**)、订单11(吴九评王五,3星,有效)。
   - review_appeal 2 条:申诉1(对订单10评价,**申诉成立**,admin_id=0 已处理,与评价作废状态自洽)、申诉2(对订单11评价,**待处理**,给管理端申诉页留一条待办)。
3. **格式统一**:11 张表全部统一为"派生表 UNION ALL + LEFT JOIN 反连接"的幂等写法(管理员行也统一);UNION 首行可空列统一 `CAST(NULL AS ...)`;列清单全显式;日期/金额/手机号/邮箱号段约定保持并在注释中说明。
4. 保持幂等:不用 TRUNCATE/DELETE,重启只补缺失行;新种子沿用 136 号段、避开测试号段;时间线自洽(评价/申诉时间晚于对应订单完成时间)。

## 三、模块代码优化(稳妥清理,不改接口 JSON 契约、不动 Date 类型)

1. **动作/范围枚举化**:新增 `OrderActionEnum`(PAY/ACCEPT/PICKUP/DELIVER/COMPLETE/CANCEL/ADMIN_CANCEL,wire code 保留 "pay"/"admin-cancel" 等线上字符串)和 `OrderListScopeEnum`(MINE/AVAILABLE/ASSIGNED/ADMIN)。OrderController 各端点直接传枚举,`OrderServiceImpl.act/list` 的字符串 switch 改枚举 switch,`detail()` 的 allowedActions 输出 code(前端 `OrderDetailView.vue` 依赖 'pay'/'admin-cancel' 字符串,保持不变),删除"未知操作/未知列表"兜底分支。
2. **抽公共校验组件**:user 模块新增 `AccountGuard`,合并 OrderServiceImpl/ReviewServiceImpl 逐字重复的 `requireRole`/`invalid`;`detail`/`exceptionDetail` 的 `requireRole(userId, role, role)` 自证调用改为语义明确的 `requireActiveAccount(userId, role)`。
3. **会话读取收敛**:`"role"` 魔法字符串常量化,4 个 Controller 与 `StpInterfaceImpl` 复用统一的 currentUserId/currentRole 读取。
4. **事务统一**:`UserAuditRecordServiceImpl`、`UserBanRecordServiceImpl` 共 3 处 `@Transactional` 补 `rollbackFor = Exception.class`。
5. **时间字段职责单一化(数据库默认值方案)**:所有实体**不加** INSERT 填充注解,`MPAutoFillHandler` 保持只填 updateTime;删除 OrderServiceImpl/ReviewServiceImpl 里约 9 处手工 `new Date()` 的 createTime 赋值及 `order.setUpdateTime(order.getCreateTime())`(MyBatis-Plus 插入时 null 字段不出现在 INSERT 列清单,由 DB 默认值填充);act/touch 里冗余的 `setUpdateTime(new Date())` 一并删除(update fill 已覆盖)。插入时间=数据库默认值,更新时间=应用 fill,机制单一。
6. **SQL 拼接清理**:2 处 `inSql("select id from customer where user_id = " + userId)` 改为 `apply("...user_id = {0}", userId)` 参数化(保留"包含已注销账户"原语义)。
7. **list() 精简**:mine 范围的 requireRole 只查一次;`recipientQuery` 的 `eq(id, -1L)` 锚点 hack 改为无匹配手机号时直接返回空页。
8. **register 孤儿文件补偿**:事务内 `FileUtil.saveImage` 落盘后,后续步骤失败时删除文件(对齐 updateAvatar 的补偿);`updateAvatar` 中误导性的 `FILE_UPLOAD_ERROR` 改为新错误码 `USER_UPDATE_ERROR(2014)`。
9. **unbanUser 脏状态处理**:账号 DISABLED 但找不到生效封禁记录时抛 `ACCOUNT_NOT_BANNED`,不再静默恢复。
10. **Controller 清理**:UserUsernameDTO 补 `@NotBlank`、UserGenderDTO 补 `@NotNull`,删除 UserController 手工 null 校验;封禁踢人 `StpUtil.kickout` 从 Controller 移入 Service;`admincancel` 方法名改 `adminCancel`;分页魔法 `10` 收敛为常量。

## 四、测试类优化(保持连真实 MySQL 的集成测试方式)

1. **抽公共支撑**:新增 `TestAccounts` 常量(管理员邮箱/密码等)与集成测试支撑(提供 createUser(role)、login(id, role)、cleanupOrders/cleanupUsers 按外键安全顺序清理,并利用新 CASCADE 简化),收敛 4 处重复的 `user()`、5 处重复的 `login()`、3 个类的手写 DELETE 清理。
2. **适配新种子**:ReviewFlowTest 中无过滤的申诉总数断言补 orderId 过滤(否则新增的 2 条种子申诉会干扰)。
3. **去掉真实文件依赖**:UpdateInfoTest 不再读 `upload/avatar/testAvatar.png`,改为内存构造字节。
4. **动作字符串改枚举**:各测试中 `service.act(..., "pay", ...)` 等改为 OrderActionEnum。
5. **命名与结构**:修正 `RegisterTest`/`AddRoleToExistingUser` 等大写开头方法名;拆分 OrderFlowTest 里混合多个场景的 `httpAuthenticationValidationAndIdSerialization`。

## 五、验证

- `mvn test` 跑全部 16 个测试类(本地 MySQL campus_express,root/Root)。
- 重建数据库:DROP campus_express 后启动应用,确认 11 张表种子数据齐全、时间线自洽。
- 若编译/测试中发现计划与实际不符的细节(如测试断言受新种子影响),按"不破坏契约、保持幂等"原则就地调整。