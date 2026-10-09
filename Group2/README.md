# Group 2 Shopping Cart

整合负责人：李岸 Li An（F）。本项目以老师的 Shopping Cart C26 PDF 为验收依据。商品页面使用 Angular，账号、购物车、结账和历史页面使用 Spring Boot MVC 与 Thymeleaf；MySQL 为正式数据库，Spring Data JPA 负责持久化。

本交付已包含 A–E 的整合源码、构建后的 Angular 静态页面、可运行 JAR、验收记录及录像提纲。演示视频尚未提供，最终提交前必须将视频放入 video 目录，并由组员核对实际作者归属。

2026-10-09 本机复查：已修复 HTTP 错误状态、购买历史分页排序和新增商品上架勾选项。后端 153 项、前端 44 项测试通过，runtime JAR 已更新。新增 `Start-Demo.cmd`、`Start-MySQL.cmd`、`Start-Frontend.cmd`；详细操作和本次验证范围见 [本机检查与运行指南](docs/本机检查与运行指南.md)。本次尚未连接你现有的 MySQL，历史 MySQL 验收记录不能视为这次重跑结果。

## 环境

- JDK 17 或以上；本次实际使用 JDK 25.0.3，编译目标 Java 17。
- MySQL 8；本次在全新独立 MySQL 8.0.46 数据库验证。
- 修改前端时需要 Node.js；本次实际使用 24.19.0。Angular 22.2.1，精确依赖由锁文件确定。
- Maven Wrapper 已固定为本次验证使用的 3.9.9。首次构建需联网。
- 本次后端 146 项自动化测试、前端 44 项测试及 MySQL 25 项流程检查均通过。详见 docs/F验收记录.md。
- 上述 146 项与 MySQL 记录是原交付记录；本机复查后的结果以 docs/本机检查与运行指南.md 为准。

## 正式 MySQL 运行

在 MySQL 客户端创建空库（已有库无需重复建）：

```sql
CREATE DATABASE IF NOT EXISTS shopping_cart
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

在项目根目录打开 PowerShell，使用你自己已有的 MySQL 账号密码：

```powershell
$env:DB_USERNAME = Read-Host 'MySQL username'
$mysqlPassword = Read-Host 'MySQL password' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $mysqlPassword).Password
java -jar runtime/shopping-cart.jar
```

地址：http://localhost:18080/products 。正式运行只需一个后端进程，Angular 已打包到 JAR 中，无需另开 4200。

若数据库主机、端口或库名不同，可先设置：

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/shopping_cart'
```

JPA 使用 `ddl-auto=update` 建立/更新实体对应表。空商品表会初始化 12 件示例商品；缺失的 alice/bob 会被创建，已有商品、账号、密码与角色不会重置。默认普通账号 alice / demo123、bob / demo123，仅在账号首次初始化时适用。

### 正式库的管理员

正式 MySQL 配置不会自动创建 admin。先通过 `/register` 注册专用账号，例如 group2admin；然后由数据库管理者执行以下语句，并核对更新的是该账号：

```sql
USE shopping_cart;
UPDATE app_users SET role = 'ADMIN' WHERE username = 'group2admin';
SELECT id, username, role FROM app_users WHERE username = 'group2admin';
```

再以注册时设置的密码登录，访问 `/admin/products`。注册页面不能自行指定管理员身份。

## 无需 MySQL 的临时演示

```powershell
java -jar runtime/shopping-cart.jar --spring.profiles.active=b-demo
```

地址：http://127.0.0.1:18083/products 。普通账号 alice / demo123、bob / demo123；管理员 admin / admin123。此模式使用内存 H2，停止应用后数据丢失，只用于临时演示，不代替老师要求的 MySQL 验收。

## 从源码重建

在项目根目录执行：

```powershell
cd frontend
npm ci
npm run deploy
cd ..
.\mvnw.cmd clean package
java -jar target/shopping-cart-0.0.1-SNAPSHOT.jar
```

`deploy` 依次检查类型、运行前端测试、构建并部署静态资源；旧静态目录备份到项目内 `.deploy-backups`。如需自定义备份位置，设置 `SHOPPING_CART_BACKUP_DIR`。必须先部署前端，再打包后端，确保 JAR 与源码一致。`runtime/shopping-cart.jar` 是交付时构建的版本；修改源码后使用新生成的 target JAR，或自行更新 runtime JAR。

后端自动化测试使用隔离 H2，不会操作你的 MySQL。若仅改后端，可运行 `.\mvnw.cmd test`。本次受限执行环境的测试参数调整记录在 docs/F验收记录.md，普通电脑通常无需这些参数。

前端开发使用 `cd frontend; npm run dev`，开发入口 http://127.0.0.1:4200/ ，代理默认连接 18080。需要同一浏览器主机名，避免混用 localhost 与 127.0.0.1 导致会话不一致；更换后端端口时同步修改 frontend/proxy.conf.json。

## 页面与接口

| 功能 | 地址 |
| --- | --- |
| Angular 商品列表与分页 | /products |
| Angular 商品详情 | /products?id=1 |
| 商品 REST | /api/products?page=0&size=6、/api/products/1 |
| 登录、注册、资料 | /login、/register、/account |
| 购物车、结账 | /cart、/checkout |
| 购买历史 | /orders |
| 管理后台 | /admin/products |

商品 ID 以上仅为新库示例。详情加购通过 C 的表单接口取得当前 Session 的令牌，再向 `/cart/add` 提交。身份只取服务器 Session 的 `loginUserId`（Long）。订单在事务中重新核价、保存明细快照；事务提交成功后清空购物车。

## 成员与交付

| 成员 | 现有标注姓名 | 模块 |
| --- | --- | --- |
| A | 王重一 | 商品 REST、Angular 与分页 |
| B | luopeiwen | 账号、登录、资料与权限 |
| C | Letian Xie | Session 购物车、结账页面、Angular 加购 |
| D | 邱弈杰 | 订单、结账事务、重复请求处理 |
| E | 蔡千一 | 历史查询、商品后台 |
| F | 李岸 Li An | 差异核对、构建配置、整合验证、运行与交付材料 |

A–E 姓名来自各自交付文件，请组员最终确认。原始缺少作者注释的三个基础类保留共享实现，并标明 Group 2 原始工程及 Li An 的整合验收职责；具体原始作者仍请 A/B 核对，不能将注释补充视为 F 独立开发这些类。

源码目录为 src，Angular 源码为 frontend，交付说明为 docs，可运行程序为 runtime，录像放入 video。压缩包排除 node_modules、target、缓存、IDE 设置和本机数据库。

PDF 第 4 页要求一个以队名命名的 ZIP，包含整合源码和演示视频，视频尽量 15 分钟内。PDF 写明 10 月 16 日 23:00 截止，同时要求参考 Canvas，最终以 Canvas 为准。现场演示无需 PPT。提交前按 docs/F提交检查.md 完成最后检查。
