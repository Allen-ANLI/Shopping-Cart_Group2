# NEXUS Digital：账户密码更新

日期：2026-10-10。

## 使用

登录后进入 **Account / 账户 → Change password / 修改密码**，填写原密码、新密码和确认新密码，再点击“更新密码”。入口会定位到账户页下方的密码设置区。

新密码沿用注册规则：8～50 个字符，包含大写字母、小写字母及特殊字符，UTF-8 长度不超过 72 字节；不能与原密码相同。原密码错误或两次新密码不一致时，密码不会更新。提示随中英文切换。

成功后保留当前登录和购物车，下次登录使用新密码；旧密码不再有效。成功或服务端校验失败后均不回填密码字段。

## 实现

- 登录、注册和修改密码共用一个密码显示按钮片段，支持键盘操作及显示状态提示。
- 针对带网站按钮的输入框隐藏 Edge 原生 `::-ms-reveal`，避免输入后出现两个眼睛。处理方式参考 [Microsoft Edge 官方文档](https://learn.microsoft.com/en-us/microsoft-edge/web-platform/password-reveal)。
- 服务层验证原密码并保存新密码的 BCrypt 哈希；账号 ID 只从服务器会话取得，修改接口校验账户表单令牌。
- 修改密码后更新当前会话标识，保留会话中的购物车等状态；资料修改和密码修改共用账户行锁。
- 新增 `PasswordChangeForm`，前端校验与后端校验保持一致。

## 验证与运行

- 后端完整 233 项测试通过，新增 16 项密码设置、规则、错误反馈、账号隔离和登录验证检查。
- Edge 浏览器完整复测 47 项通过：原生 Shadow DOM 的眼睛节点为 `display: none`；每个密码框仅一个网站按钮，中英文、手机布局、错误反馈、新旧密码登录和购物车保留正常。
- 已检查登录输入框和账户页桌面/手机截图；完整复测无 JavaScript 异常。
- 本轮未改动 Angular，沿用上一版已构建的前端资源。
- 最新预览：<http://127.0.0.1:18092/products>。管理员 `admin / admin123`，普通演示账号 `alice / demo123`；浏览器测试使用独立测试账号。
- 最新运行包：`runtime/nexus-shopping-cart.jar`。与 target 构建包、本轮预览包 SHA-256 一致：`6484BA0C483A42247F80878CC5EC3342D0487E0566CE2448761993251ACA475D`。

在含 `pom.xml` 的项目目录启动：

```powershell
java -jar runtime/nexus-shopping-cart.jar --spring.profiles.active=b-demo --server.port=18092
```

演示数据库为内存 H2，重启会重置。本轮没有操作现有 MySQL 数据库。
