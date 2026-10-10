# NEXUS Digital：导航清理与问题修复

日期：2026-10-10。本文件描述本轮相对于上一版的改动。

## GitHub Summary

```text
Polish account navigation, remove unused code and fix UI bugs
```

## GitHub Description

```text
- Unify Log in and Register styling and hover animations; remove the Register background.
- Remove obsolete layouts, navigation styles, unused category requests and legacy review code.
- Refresh catalogue ratings, sales and stock when returning to the page; fix invalid-page back navigation.
- Preserve the cursor when editing card expiry and synchronize retained review ratings with their labels.
- Handle cancelled downloads without duplicate error responses, while preserving genuine server errors.
- Pass 217 backend tests, 69 frontend tests and 75 browser checks; update the runnable package.
```

## 已修复的问题

| 问题 | 修复结果 |
| --- | --- |
| Log in 与 Register 的背景、间距和交互不一致 | 两个入口使用同一个透明链接样式和下划线动画，支持键盘焦点及减少动画偏好。 |
| 从其他页面返回商品总览后，评分、销量、库存未更新 | 返回前台或恢复缓存页面时后台刷新数据，保留筛选、页码和滚动位置；失败时保留可用内容。 |
| 旧分页地址失效时，自动回退会增加历史记录 | 替换失效地址，避免返回键再次进入失效页码的循环。 |
| 修改银行卡有效期的月份时，光标跳到输入框末尾 | 卡号和有效期统一格式化，格式变化后保留数字对应的光标位置。 |
| 评价表单校验失败后，保留评分却仍显示“请选择评分” | 提示文字初始化为已选星级，并按中英文显示。 |
| 浏览器取消图片下载后，服务器误报错误并再次渲染页面 | 识别客户端断连，避免重复写响应；真实异常仍保留日志和适当的错误响应。 |

## 代码清理

删除无引用旧首页、分类卡片、导航与页脚、旧详情评价表单等样式；移除未使用的分类计数请求、分类展示字段、旧评论提交方法及演示账号页面属性。每日促销复用已有商品服务。删除空的 Angular 导航样式文件和无引用的 MVC 旧导航样式文件。

生产 Angular CSS 从约 23.14 KB 降至 16.87 KB。清理通过源码引用核对和页面回归验证；原有业务数据、历史文档和旧运行包未删除。

## 验证与运行

- 后端完整测试：217 项通过，含新增 5 项异常处理测试。
- 前端：类型检查、69 项测试、生产构建和静态资源部署通过，含新增 3 项商品刷新/分页回归测试。
- MVC 脚本及样式：15 项针对性检查通过。
- 实际浏览器：75 项检查通过，覆盖中英文、桌面/手机、登录注册动画、商品浏览、加购、支付编辑、PDF 凭证、收货确认及评价。未发现 JavaScript 异常，本轮预览日志无 ERROR。
- 已检查导航、商品卡片、详情、促销和支付页面截图。
- 最新运行包：`runtime/nexus-shopping-cart.jar`。本轮隔离预览：<http://127.0.0.1:18091/products>。
- 构建包、最新运行包与预览包 SHA-256 一致：`3E1DEBEA0A4219E30F383B129A07631FB659877CE49C442666E937A49A31B674`。

在含 `pom.xml` 的项目目录启动：

```powershell
java -jar runtime/nexus-shopping-cart.jar --spring.profiles.active=b-demo --server.port=18091
```

已有预览运行时无需重复启动。演示账号 `alice / demo123`，管理员 `admin / admin123`。演示使用内存数据库，停止后数据重置；本轮没有连接既有 MySQL 数据库。
