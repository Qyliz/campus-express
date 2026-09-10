# User 管理接口路径调整

管理员操作的目标标识放在 URL 中，操作内容放请求体，身份仍由服务端登录会话和管理员权限校验确定。旧写入路径已替换，前后端必须同步更新。

| 方法 | 路径 | 请求体 |
| --- | --- | --- |
| PUT | /api/user/audit/{recordId} | `{status, reason}` |
| POST | /api/user/{userId}/roles/{role}/ban | `{reason}` |
| POST | /api/user/{userId}/roles/{role}/unban | 无 |
| POST | /api/user/{userId}/kickout | 无 |
| POST | /api/user/{userId}/reset-password | `{newPassword}` |

`recordId` 为审核记录 ID，`userId` 为用户主表 ID，`role` 使用 `CUSTOMER`、`COURIER`、`ADMIN`。封禁、解封针对角色账户；强制下线、重置密码针对共用的用户身份。

本人修改资料、手机号、邮箱、密码及注销等接口仍使用登录会话定位本人，不新增用户 ID 参数。查询列表仍采用 GET 查询参数，注册和登录等接口保持现有契约。

前端管理操作对象可携带目标 ID 供页面调用，但 API 层只将目标标识写入路径，请求体不再重复发送 ID。
