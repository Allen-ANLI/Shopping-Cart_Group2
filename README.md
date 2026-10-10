[README.md](https://github.com/user-attachments/files/33283503/README.md)
# Group 2 Shopping Cart

整合负责人：李岸 Li An（F）。本项目以老师的 Shopping Cart C26 PDF 为验收依据。商品页面使用 Angular，账号、购物车、结账和历史页面使用 Spring Boot MVC 与 Thymeleaf；MySQL 为正式数据库，Spring Data JPA 负责持久化。

本交付已包含 A–E 的整合源码、构建后的 Angular 静态页面、可运行 JAR、验收记录及录像提纲。演示视频尚未提供，最终提交前必须将视频放入 video 目录，并由组员核对实际作者归属。

2026-10-10 最新首页与导航更新：桌面分类保持单行，窄屏使用分类选择框；横幅移除播放/暂停按钮，修正台灯矩形背景，新增动态展示最高折扣商品的第三张横幅；用户头像在各页面用户名旁显示，上传后同步更新。后端 212 项、前端 66 项、浏览器 53 项检查通过。最新运行包为 `runtime/nexus-shopping-cart.jar`，预览地址为 http://127.0.0.1:18090/products ，详见 [单行分类、三张横幅与公共头像](docs/NEXUS单行分类与公共头像更新.md)。以下记录保留为历史版本。

2026-10-10 上一轮首页与账户更新：修复中文搜索按钮换行，分类使用无需横向滑动的紧凑网格；支持上传图片并圆形裁剪头像；首页横幅支持悬停显示箭头、自动轮播与悬停暂停；新增每日促销页面。后端 212 项、前端 63 项、浏览器 43 项检查通过，记录见 [首页轮播与头像更新](docs/NEXUS首页轮播与头像更新.md)。

2026-10-10 商品浏览与订单交互修复：促销与非促销商品的 View details 按钮对齐；商品分页支持点击和输入页码；订单使用小尺寸铅笔评价入口，商品栏改为 Order products／订单商品，商品名称可打开详情，评价与评分及时刷新。手机订单商品栏也已适配。前端 59 项、相关后端 33 项、浏览器 27 项检查通过，说明见 [商品浏览与订单交互更新](docs/NEXUS商品浏览与订单交互更新.md)。

2026-10-10 上一轮交互更新：提交订单后进入独立支付页，支持 Visa / Mastercard 自动格式化输入；加入 9% GST、PDF 支付凭证下载，以及确认收货后逐商品进入五星评价页。注册眼睛按钮、登录回首页、分类导航、购物车数量同步、评论语言与评分分布、99% 折扣也已更新。后端 205 项、前端 55 项、浏览器 38 项检查通过。使用说明见 [结账与评价流程更新](docs/NEXUS结账与评价流程更新.md)。

2026-10-10 NEXUS Digital 升级：本轮调整账号与双语登录交互、统一导航、扩充数码办公产品矩阵，并加入折扣、银行卡与加密货币支付模拟、物流及确认收货后原地评价。13 项需求、数据规则和演示步骤见 [NEXUS 商城升级说明](docs/NEXUS商城升级说明.md)。本轮最终测试与打包结果以该文档为准，下方记录属于此前版本。

2026-10-10 功能扩展：已完成总览数量加减、强化详情入口、示例评价与评分/销量排序、购买后评价权限、导航高亮、首页广告与每日推荐、后台库存及缺货自动隐藏、服务层扩展和模拟支付。后端 179 项、前端 55 项测试通过，源码、静态资源与 runtime JAR 已同步。功能规则、示例数据及演示步骤见 [商城功能扩展说明](docs/商城功能扩展说明.md)。下方商城升级记录属于本轮扩展前的版本。

2026-10-10 商城升级：已完成首页与分类重设计、40 件双语商品、图片放大与快速加购、真实评价、资料编辑、收货地址管理、订单地址快照及中英文切换。后端 170 项、前端 51 项、HTTP 流程 35 项、浏览器流程 21 项检查通过，源码、Angular 静态资源及 runtime JAR 已同步更新。功能、运行与演示步骤见 [商城升级说明](docs/商城升级说明.md)。本轮使用隔离 H2 验证，尚未连接你现有的 MySQL；下方早期 MySQL 验收记录属于历史记录。

2026-10-09 的启动器与故障排查仍可参考 [本机检查与运行指南](docs/本机检查与运行指南.md)，其中商品数量与测试数量以本轮升级说明为准。

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

JPA 使用 `ddl-auto=update` 建立/更新实体对应表。首次升级会补充四类示例商品（新库每类 10 件，共 40 件），并记录初始化版本；后续启动不重建已删除商品。缺失的 alice/bob 会被创建，已有商品内容、账号、密码与角色不会重置。默认普通账号 alice / demo123、bob / demo123，仅在账号首次初始化时适用。

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
| Angular 首页、分类与分页 | /products、/products?category=audio |
| Angular 商品详情 | /products?id=1 |
| 商品 REST | /api/products?page=0&size=6、/api/products/1 |
| 登录、注册、资料 | /login、/register、/account |
| 收货地址管理 | /account/addresses |
| 分类、评价、快速加购 REST | /api/categories、/api/products/{id}/reviews、/api/cart/items |
| 购物车、结账 | /cart、/checkout |
| 购买历史 | /orders |
| 管理后台 | /admin/products |

商品 ID 以上仅为新库示例。Angular 加购先取得当前 Session 的购物车令牌，再向 `/api/cart/items` 提交 JSON，原 `/cart/add` 表单接口保留。身份只取服务器 Session 的 `loginUserId`（Long）。结账必须选择自己的收货地址；订单在事务中重新核价、保存商品明细与收货地址快照，事务提交成功后清空购物车。

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
