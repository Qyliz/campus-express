# 订单模块

## 业务流程

待支付 → 待接单 → 待揽收 → 配送中 → 待取件 → 已完成。

收寄件人发布订单并填写配送费，模拟支付后配送员才能接单。
配送员依次确认揽收、送达，最后由下单账户或匹配的收件人确认取件。
下单账户可在接单前取消订单；接单后由管理员取消。取消必须填写原因，
已支付的订单取消时标记模拟退款，不发生真实资金交易。

配送费范围为 0.01～9999.99 元，最多两位小数。联系电话按中国大陆手机号校验。
不提供订单编辑、删除、自动超时、改派或真实支付。

## 数据与并发

建表脚本在 backend/src/main/resources/db/schema.sql，应用启动时幂等创建：

- express_order：下单时的取送信息、费用、当前状态、支付状态、版本及创建/更新时间。
- order_status_record：状态变更、操作用户和角色、操作时间、取消原因等说明。

订单 id 同时作为订单编号；接口以字符串返回 ID。
customer_id、courier_id 引用角色账户 ID，操作记录的 operator_id 引用用户 ID。
节点时间统一从状态记录读取。订单及记录不随账户逻辑删除而删除。

订单状态 order_status：0 待支付、1 待接单、2 待揽收、3 配送中、
4 待取件、5 已完成、6 已取消。
支付状态 payment_status：0 未支付、1 已支付、2 已退款。
Java/JSON 属性使用 orderStatus、paymentStatus。

所有订单变更使用 MyBatis-Plus @Version 和 updateById，
版本不匹配时拒绝操作并提示刷新，不自动重试接单。
订单变更与记录写入处于同一事务，记录失败也会回滚订单状态、支付状态及版本。

## 接口

所有接口要求登录，统一返回现有 Result 信封。

| 方法 | 路径 | 功能 |
| --- | --- | --- |
| POST | /api/order | 创建，返回订单 ID |
| GET | /api/order/mine | 收寄件人自己的订单 |
| GET | /api/order/available | 配送员接单大厅 |
| GET | /api/order/assigned | 配送员自己的任务 |
| GET | /api/order/admin | 管理员订单列表 |
| GET | /api/order/{id} | 详情，返回 order 和 records |
| POST | /api/order/{id}/pay | 模拟支付 |
| POST | /api/order/{id}/accept | 接单 |
| POST | /api/order/{id}/pickup | 确认揽收 |
| POST | /api/order/{id}/deliver | 确认送达 |
| POST | /api/order/{id}/complete | 确认取件 |
| POST | /api/order/{id}/cancel | 用户取消 |
| POST | /api/order/{id}/admin-cancel | 管理员取消 |

创建请求包含 pickupAddress、pickupName、pickupPhone、
deliveryAddress、deliveryName、deliveryPhone（必填，11 位手机号）、itemDescription、
fee 以及可选的 remark。取消请求为 {"reason":"取消原因"}。

列表参数：currentPage（从 1 开始）、可选 orderStatus；
管理列表另支持 orderId 精确查询。每页 10 条，按创建时间及 ID 倒序。

接单大厅隐藏联系人、电话及备注，完整详情向下单人、匹配的收件人、实际配送员和管理员开放。
同一用户不能接自己的订单，管理员身份不能代替配送员接单或执行配送。
订单模块检查当前角色账户正常，不改变原有用户注销、审核、封禁逻辑。

## 验证

后端测试：在 backend 下运行 mvnw.cmd test。
前端检查和构建：在 frontend 下运行 npm run build。

新增测试覆盖完整流程、取消退款、权限、隐私、非法金额、
并发抢单、旧版本覆盖失败、记录保存失败时事务回滚，以及登录驳回原因。
测试使用可回滚数据；跨线程及事务测试仅清理自己创建的记录。

## 收件人访问

`GET /api/order/mine` 增加 `relation=ALL|CREATED|RECEIVED`，默认 `ALL`。通过一次带 OR 条件的数据库分页查询匹配下单角色账户 ID 或当前用户手机号，不拼接两组分页结果。仅正常的 CUSTOMER 角色账户可使用此入口。

收件人只凭手机号认领订单，不再支持邮箱。手机号由后端读取当前用户资料，不接受客户端指定访问身份；手机号为空时永不参与匹配。收件手机号不要求收件人已注册，也不设置外键。后注册或换绑后，查看权限随当前绑定的手机号变化。

列表返回 `createdByMe`、`receivedByMe`、`pendingException`，以及接单骑手的展示信息 `courierName`、`courierPhone`（`courierId` 是角色账户主键，姓名与手机号在 user 表，由 courier → user 两跳按整页批量回填，不是数据库列）。详情返回 `order`、`records`、`exceptions`、`allowedActions`、`canReportException`。前端使用后端返回的可执行操作显示按钮，后端执行时仍再次鉴权。支付、取消只允许下单账户。

## 配送异常

配送员在待揽收、配送中上报；每单最多一条待处理异常，暂停揽收、送达。异常处理结果为恢复配送或取消订单；恢复不改变配送阶段，取消同步模拟退款。管理员原有取消入口也关闭待处理异常。处理完可以重新上报，历史不可修改删除。

| 方法 | 路径 | 请求或返回 |
| --- | --- | --- |
| POST | /api/order/{id}/exceptions | `{type, description}`，返回异常 ID |
| GET | /api/exception/admin | `currentPage`、可选 `status`、`orderId`，每页 10 条 |
| GET | /api/exception/{id} | 异常详情，仅订单参与者或管理员可查看 |
| POST | /api/exception/{id}/resolve | `{resolution, description}`，仅管理员 |

类型 `ExceptionTypeEnum`：`CONTACT` 联系不上、`ADDRESS` 地址问题、`ITEM` 物品问题、`COURIER` 配送员突发情况、`OTHER` 其他；说明必填，最多 255 字。状态 `ExceptionStatusEnum`：`PENDING` 待处理、`RESOLVED` 已处理。结果 `ExceptionResolutionEnum`：`RESUME` 恢复配送、`CANCEL` 取消订单。三者都是 `@EnumValue` 枚举，数据库存 code、接口传枚举名，取值范围由枚举本身限定，不再依赖 `@Pattern` 白名单；数据库列也因此从 VARCHAR 改为 TINYINT。所有异常变更同步竞争订单版本，与订单和进度记录在同一事务中提交。

管理后台“异常管理”默认查看全部异常，可按状态筛选，通过“查看并处理”进入订单详情填写结果。配送员从订单详情上报，订单参与者在同页查看处理记录，使用刷新按钮获取最新状态。

## 数据库重建

本模块**不使用增量迁移脚本**。`schema.sql` 全程 `CREATE TABLE IF NOT EXISTS`，只负责给空库建出目标结构，不会修改已存在的表；`application.yml` 的 `spring.sql.init.schema-locations` 只有 `classpath:db/schema.sql` 一项。

早先用于补充收件邮箱的 `db/upgrade-recipient.sql` 已随邮箱一起删除。由于本次同时改动了列类型（`delivery_exception.type`/`resolution` 从 VARCHAR 变为 TINYINT）、删除了列（`express_order.delivery_email`）、并把状态字段的接口契约从数字换成枚举名，旧库数据无法原地兼容，**需要删库重建**：

```sql
DROP DATABASE IF EXISTS campus_express;
CREATE DATABASE campus_express DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

注意：数据源不会自动建库，DROP 之后必须 CREATE，否则启动时连不上。启动会自动执行 `schema.sql` 建表、`data.sql` 灌种子。删除 resource 下的脚本后 **必须 `mvn clean`**，否则旧脚本仍留在 `target/classes` 里被继续执行。

新增 `RecipientExceptionTest` 覆盖收件人权限、手机号动态匹配与换号、分页去重、异常暂停恢复、两个取消入口、并发操作和异常保存失败回滚；`ReviewFlowTest` 另覆盖「接口传枚举名、数据库存 code」以及非法枚举名与旧数字写法被拒绝。
