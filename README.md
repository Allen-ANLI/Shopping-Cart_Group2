# Nexus Digital — 全栈电商 Web 应用

Nexus Digital 是一个以数码与办公产品为主题的 B2C 电商 Web 应用，覆盖
**浏览 → 购物车 → 结账 → 支付 → 收货 → 评价** 的完整购物闭环，并提供管理后台与销售数据看板。
商品目录由 Angular 单页应用驱动，账号、购物车、结账与订单页面采用 Spring Boot MVC + Thymeleaf 服务端渲染，
两者共享同一套 REST API 与会话认证。

> English summary at the bottom. 中文为主，文末附英文摘要。

## 主要功能

### 购物体验
- 商品目录：分类筛选、关键词搜索、按价格 / 评分 / 销量排序、分页（支持点击与输入页码跳转）
- 商品详情：图片放大、快速加购、评分分布与评价列表
- 首页轮播横幅：自动轮播、悬停暂停，动态展示最高折扣商品；每日促销页 `/deals`
- 折扣与基于规则的个性化推荐（依据浏览、购买、好评、评分与销量）
- 中 / 英双语一键切换

### 账号与安全
- 注册、登录、登出；密码 BCrypt 哈希存储
- 个人资料编辑、修改密码、头像上传（圆形裁剪，全站同步显示）
- 收货地址管理；拦截器统一校验登录态与管理员权限

### 购物车、结账与支付
- 会话级购物车：数量加减、失焦自动更新与合法性校验，非法数量无法结账
- 结账须选择本人收货地址；事务内完成重新核价、库存校验与订单快照（商品名 / 价格 / 收货地址）
- 结账令牌 + 订单锁防止重复提交；订单创建后进入待支付状态
- 模拟 Visa / Mastercard 支付（不产生真实扣款）：事务内校验并扣减库存，含 9% GST
- PDF 支付凭证下载（Apache PDFBox）；完整卡号与 CVV 不落库

### 订单与评价
- 订单历史、订单详情，待支付订单可继续支付
- 确认收货后逐商品进行五星评分与文字评价

### 管理后台
- 商品增删改查、库存与上下架管理，缺货自动隐藏
- 销售数据看板：基于真实已支付订单聚合统计

## 技术栈

| 层面 | 技术 |
|---|---|
| 后端框架 | Spring Boot 4.1.1（Java 17）、Spring MVC、Bean Validation |
| 服务端渲染 | Thymeleaf |
| 前端 | Angular 22.2.1、TypeScript 6.0、RxJS |
| 持久化 | Spring Data JPA / Hibernate、MySQL 8 |
| 安全 | 会话认证 + 拦截器、BCrypt（spring-security-crypto） |
| PDF 生成 | Apache PDFBox 3.0.8 |
| 测试 | Spring Boot Test（隔离 H2）、Vitest + jsdom、Node test runner |
| 构建 | Maven Wrapper、Angular CLI |
| CI/CD | GitHub Actions、Docker 多阶段构建、Docker Hub |
| 云部署 | AWS ECS Fargate、Amazon RDS for MySQL、CloudWatch Logs |

## 架构与工程亮点

- **混合渲染架构**：Angular SPA 负责强交互的商品目录，Thymeleaf 负责表单密集的账号 / 结账页面，共享 REST API 与会话身份
- **清晰分层**：controller → service → repository；DTO / Form 与实体分离；`@ControllerAdvice` 统一异常处理
- **事务一致性**：结账核价、库存校验 / 扣减、订单快照与购物车清空在同一事务内完成；支付同样在事务内校验库存
- **防重与幂等**：结账令牌与订单锁抵御重复提交
- **订单快照**：历史订单保留下单时的商品名、价格与收货地址，不受后续商品变更影响
- **版本化种子数据**：商品与示例评价初始化记录版本号，重启不重复插入
- **国际化**：中 / 英资源束（`messages*.properties`）
- **前后端一体交付**：Angular 构建产物嵌入 Spring Boot 静态资源，单 JAR / 单容器即可运行

## 项目结构

```text
Group2/                          Spring Boot Maven 工程
├── pom.xml / mvnw / mvnw.cmd    依赖与构建配置（含 Maven Wrapper）
├── Dockerfile                   多阶段构建：Node → Maven → JRE
├── src/main/java/sg/edu/nus/iss/shoppingcart/
│   ├── config/                  应用配置与数据初始化
│   ├── controller/              MVC 页面与 REST 端点
│   ├── service/                 业务规则与事务
│   ├── repository/              Spring Data JPA 数据访问
│   ├── entity/                  领域实体
│   ├── dto/  form/              请求响应数据与校验表单
│   ├── interceptor/             登录与管理员权限拦截
│   ├── exception/               业务异常与统一处理
│   └── model/                   会话购物车与目录模型
├── src/main/resources/          配置、Thymeleaf 模板、静态资源、国际化资源
├── src/test/                    后端测试（隔离 H2）
└── frontend/                    Angular 源码与前端测试
.github/workflows/ci.yml         CI：前后端测试 → Docker 镜像发布
```

## 页面入口

| 路径 | 说明 |
|---|---|
| `/products` | Angular 商品目录、分页与详情 |
| `/deals` | 每日促销 |
| `/register`、`/login` | 注册与登录 |
| `/account`、`/account/addresses` | 个人资料、密码、头像与收货地址 |
| `/cart`、`/checkout` | 购物车与结账 |
| `/orders` | 订单历史、继续支付与订单详情 |
| `/admin/products` | 商品、库存与上下架管理 |
| `/admin/dashboard` | 销售数据看板 |

## 快速开始

环境要求：JDK 17、MySQL 8（Maven Wrapper 已内置）。仓库已包含编译好的 Angular 静态资源，
仅修改前端时需要 Node.js 22+。

1. 创建数据库：

   ```sql
   CREATE DATABASE IF NOT EXISTS shopping_cart
     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. 配置数据库连接并启动（PowerShell 示例；macOS / Linux 用 `export` 设置同名变量）：

   ```powershell
   $env:DB_URL = 'jdbc:mysql://localhost:3306/shopping_cart'
   $env:DB_USERNAME = 'your_mysql_username'
   $env:DB_PASSWORD = 'your_mysql_password'
   cd Group2
   .\mvnw.cmd spring-boot:run
   ```

   JPA 自动建表 / 更新表结构，首次启动初始化商品目录与示例评价数据。

3. 打开 <http://localhost:18080/products>。

演示账号：`alice / demo123`、`bob / demo123`（普通顾客）。
管理员：先通过 `/register` 注册账号，再由数据库管理员执行
`UPDATE shopping_cart.app_users SET role = 'ADMIN' WHERE username = '你的用户名';`，
重新登录后访问 `/admin/products`。

演示支付（模拟，不产生真实扣款）：持卡人 `Demo Customer`，Visa 卡号
`4242 4242 4242 4242`，有效期 `12/99`，安全码 `123`。

打包为 JAR 运行：

```powershell
.\mvnw.cmd clean package
java -jar target/shopping-cart-0.0.1-SNAPSHOT.jar
```

## 测试

后端测试使用隔离的内存 H2（MySQL 兼容模式），不触碰正式数据库：

```powershell
cd Group2
.\mvnw.cmd test
```

前端类型检查与测试：

```powershell
cd Group2/frontend
npm ci
npm run check      # TypeScript 类型检查
npm test           # Vitest 组件 / 服务测试
npm run test:cart  # 购物车页面流程测试
```

重建前端并部署到 Spring Boot 静态资源：`npm run deploy`
（依次执行类型检查、测试、构建与复制，并自动备份旧产物）。

## CI/CD 与部署

GitHub Actions 在推送 `main` 时自动执行：

1. 前端类型检查、测试、构建并嵌入后端静态资源
2. 后端全量测试
3. Docker 多阶段构建，推送 Docker Hub 镜像（`latest` + commit SHA 双标签）

Dockerfile 三阶段构建：`node:22-alpine` 编译 Angular → `maven:3.9-temurin-17` 打包 Spring Boot →
`eclipse-temurin:17-jre` 最终运行镜像，前后端交付为单个容器。

云端运行于 AWS：ECS Fargate 承载应用容器，Amazon RDS for MySQL 提供数据库，
CloudWatch Logs 收集日志；数据库凭据通过环境变量注入，安全组限制应用端口 18080 与数据库访问。

本地 Docker 运行：

```powershell
docker build -t nexus-shopping-cart ./Group2
docker run --rm -p 18080:18080 `
  -e DB_URL='jdbc:mysql://your_mysql_endpoint:3306/shopping_cart' `
  -e DB_USERNAME='your_mysql_username' `
  -e DB_PASSWORD='your_mysql_password' `
  nexus-shopping-cart
```

---

## English Summary

**Nexus Digital** is a full-stack B2C e-commerce web application for digital and
office products, covering the complete journey from browsing to cart, checkout,
simulated payment, receipt confirmation and product reviews, plus an admin
console with a sales dashboard.

### Key features
- Angular catalogue with category filters, search, sorting (price / rating /
  sales), pagination, image zoom and quick add-to-cart; home carousel and daily deals
- Registration / login with BCrypt-hashed passwords, profile editing, avatar
  upload and shipping-address management
- Session cart with quantity validation; transactional checkout with server-side
  repricing, stock checks and order snapshots (product name / price / address);
  checkout tokens and order locking prevent duplicate submissions
- Simulated Visa / Mastercard payment with in-transaction stock deduction, 9% GST
  and downloadable PDF receipts (Apache PDFBox); full card numbers and CVV are never persisted
- Purchase history, five-star reviews after receipt confirmation, rule-based
  recommendations, Chinese / English i18n
- Admin product / stock / visibility management (out-of-stock auto-hide) and a
  sales dashboard aggregated from real paid orders

### Tech stack
Spring Boot 4.1.1 (Java 17), Spring MVC + Thymeleaf, Angular 22.2.1 /
TypeScript 6.0, Spring Data JPA / Hibernate, MySQL 8, Bean Validation,
BCrypt, Apache PDFBox 3.0.8; tests with Spring Boot Test (isolated H2) and
Vitest; GitHub Actions CI publishing multi-stage Docker images to Docker Hub;
deployed on AWS ECS Fargate with Amazon RDS for MySQL and CloudWatch Logs.

### Run locally
Requires JDK 17 and MySQL 8 (Maven wrapper included; compiled Angular assets are
already bundled, Node.js 22+ only for frontend development):

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/shopping_cart'
$env:DB_USERNAME = 'your_mysql_username'
$env:DB_PASSWORD = 'your_mysql_password'
cd Group2
.\mvnw.cmd spring-boot:run
```

Then open <http://localhost:18080/products>. Demo customers: `alice / demo123`,
`bob / demo123`. Demo payment: cardholder `Demo Customer`, Visa
`4242 4242 4242 4242`, expiry `12/99`, CVV `123` (simulation only).

### Tests and delivery
`.\mvnw.cmd test` runs the backend suite against an isolated in-memory H2;
`npm run check`, `npm test` and `npm run test:cart` cover the frontend.
On every push to `main`, GitHub Actions runs all checks and tests, then builds
and publishes a Docker image (`latest` + commit SHA) for deployment to ECS
Fargate.