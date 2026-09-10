# 校园快递管理系统 · 前端

Vue 3 + TypeScript + Vite + Element Plus，对接 `backend/` 的 user 模块（注册 / 登录 / 资料维护 / 验证码 / 管理端审核与封禁）。

## 启动

需要两个终端，**后端必须先起来**。

```sh
# 终端 1 —— 后端（需要 MySQL 已在 3306 上运行，库名 campus_express）
cd ../backend
./mvnw spring-boot:run          # → http://localhost:8080
```

```sh
# 终端 2 —— 前端
npm install                     # 首次
npm run dev                     # → http://localhost:5173
```

> **后端必须从 `backend/` 目录启动**：`WebConfig` 把 `/upload/**` 映射到相对路径 `file:upload/`，
> 换了工作目录，之前上传的头像和审核材料会全部 404。

> **每次重启 MySQL 后后端可能起不来**，报 `Public Key Retrieval is not allowed`：
> MySQL 8 默认用 `caching_sha2_password`，服务器重启后凭据缓存是空的，
> 在 `useSSL=false` 的非加密连接上第一次握手要取服务端 RSA 公钥。
> `application.yml` 的 JDBC URL 里已经加了 `allowPublicKeyRetrieval=true`，别再删掉。

种子管理员（`data.sql` 每次启动幂等插入）：`admin@email.com` / `IamADMIN`，登录时身份选「管理员」。

## 脚本

| 命令 | 作用 |
| --- | --- |
| `npm run dev` | 开发服务器（含 `/api` 与 `/upload` 反向代理） |
| `npm run type-check` | `vue-tsc --build`，增量；怀疑结果过期时用 `npx vue-tsc --build --force` |
| `npm run build` | type-check + 打包 |
| `npm run preview` | 预览打包产物 |
| `npm run format` | Prettier（无分号、单引号、行宽 100） |

## 几个不看代码就想不到的设计

**登录态只存在于 cookie 里，前端没有 token。**
`POST /api/user/login` 返回 `Result<Void>`，响应体里没有 token；Sa-Token 通过 `Set-Cookie: satoken=...` 下发。实测这个 cookie **没有** HttpOnly（`document.cookie` 读得到），但我们刻意不读它 —— 「浏览器里有个 token」既不能说明它还有效，也不告诉你是哪个角色在登录。所以 `stores/auth.ts` 不存任何 token，首次加载时调用 `GET /api/user/session` 查询登录状态；匿名或失效会话返回成功响应及空数据，已登录时返回用户资料。路由守卫是 `async` 的并 `await auth.init()`，因此首屏渲染时状态已就绪，不会闪一下「未登录」。刷新页面能保持登录，因为 `satoken` 是会话 cookie；`POST /api/user/logout` 会把它清掉。

**所有请求走相对路径 + Vite 代理。**
`vite.config.ts` 把 `/api` 和 `/upload` 都转发到 `:8080`。浏览器眼中一切都是同源(`:5173`)，cookie 自动携带，因此既不需要 `withCredentials`，后端也不需要任何 CORS 配置。`/upload` 这条不能漏：它不在 Sa-Token 拦截器的排除名单里，图片本身也要登录态，漏配会得到 404（比 401 更难查）。

**业务错误是 HTTP 200。**
`api/request.ts` 的响应拦截器在**成功分支**里判 `code`：`code === 0` 才解包出 `data`，否则弹提示并 reject 一个 `ApiError`。只有 401（未登录 / 被顶号 / 被踢）、403（角色不对）、500 才用真实的 HTTP 状态码。`silent: true` 可以让某个请求跳过全局提示和跳转 —— 启动时的 `/session` 查询允许匿名访问，不会因未登录产生 401；`/profile` 仍要求登录。

**后端主键是雪花 ID，以字符串下发。**
19 位 Long 超出 JS 的 `Number.MAX_SAFE_INTEGER`，直接当数字接收会被 `JSON.parse` 舍位，管理端就会拿着错的 id 去封禁 / 踢人。后端 `common/config/JacksonConfig.java` 把 `Long` 序列化成字符串，前端一律用 `EntityId = string` 承载，只原样回传、从不做算术。注意那里**只注册了 `Long.class`**：`PageResult` 的 `total/current/size/pages` 是原始 `long`，一起注册会让分页组件收到字符串。

**时间字段是 ISO-8601 字符串，不是 epoch 毫秒。**
实测 `createTime` 下发成 `"2026-09-09T16:35:30.000Z"`（UTC）。因为 Spring Boot 4 用的是 **Jackson 3**，它默认关掉了 `WRITE_DATES_AS_TIMESTAMPS` —— 这和 Jackson 2「默认输出 epoch 毫秒数字」的行为正好相反，按旧经验想当然就会把类型标错。`types/index.ts` 里因此有个 `ServerDate = string` 别名；`utils/date.ts` 的 `formatDateTime()` 数字和字符串两种形状都能吃，并且用本地时区的 getter 输出，所以 UTC 的 `16:35:30Z` 会正确显示成 `2026-09-10 00:35:30`。

**Element Plus 按需导入有两条硬规则。**
`ElMessage` / `ElMessageBox` 是命令式 API，模板里没有对应标签，`unplugin-vue-components` 看不见它们，样式也就不会被注入。所以：

1. **永远不要** `import { ElMessage } from 'element-plus'` —— 直接用 `ElMessage.error(...)`，样式由 `unplugin-auto-import` 一并带上。手写 import 能编译，但提示会变成页面角落里无样式的裸文本。
2. 类型要 `import type { FormInstance, FormRules, UploadFile } from 'element-plus'`，图标要 `import { Search } from '@element-plus/icons-vue'`，这两类不走自动导入。

两个插件生成的 `src/types/auto-imports.d.ts` 与 `components.d.ts` **要提交进 git**：`tsconfig.app.json` 的 `include` 只覆盖 `src/**/*`，而且新克隆的仓库在跑过 `dev` 之前就得能 `type-check`。

**`el-upload` 一律 `:auto-upload="false"`，从不配 `action=`。**
配了 `action` 它会发自己的 XHR，绕过 axios 实例（丢掉 `Result` 解包、错误提示、401 处理），而且它的 `on-success` 在 HTTP 200 时就触发，即使 `code !== 0` —— 被拒绝的上传看起来像成功。

## 目录

```
src/
  api/         request.ts（axios 实例 + 拦截器）、user.ts（登录者可调）、admin.ts（管理员专属）
  types/       与后端 DTO / VO 一一对应的类型；两个 *.d.ts 由插件生成
  constants/   枚举中文 label、el-tag 配色、下拉 options、PAGE_SIZE
  utils/       date / image / patterns（正则与后端校验注解逐字符一致）
  stores/      auth.ts —— /session 初始化登录状态，没有 token
  composables/ useVerifyCode.ts —— 发码 + 60s 倒计时 + 展示后端返回的模拟验证码
  router/      路由表 + RouteMeta 声明 + 守卫
  layouts/     DefaultLayout（顶部导航）、AdminLayout（侧边栏）
  views/       页面；admin/ 下是管理端三个列表页
```

`api/` 是按**授权边界**切的，不是按实体切的：`user.ts` 里的接口登录者都能调，`admin.ts` 里的全部标了 `@SaCheckRole("ADMIN")`。请求 403 时要推理的正是这条边界。
