# A 模块开发日志：商品浏览、REST 接口与 Angular 前端

- 整理日期：2026-10-05
- 最后更新：2026-10-06
- 负责人：王重一
- 当前状态：A 模块商品浏览功能、Angular 前端、本机部署验收及作者署名已完成；新环境启动和跨模块验收待收尾。
- 记录依据：现有代码、开发过程中实际操作与反馈、命令和浏览器检查结果，以及《CA购物车项目六人分工与规划》中的 A 模块要求。
- 时间说明：未逐项记录日期的开发工作按功能阶段整理，不补写工时或提交次数。

路径约定：正文中的 `shopping-cart/` 表示项目根目录，文件链接相对当前日志；验收截图随项目保存在 `docs/screenshots/`。不记录开发者电脑的盘符、用户名或项目存放前缀。本机备份只保留目录名称，备份不随项目交付。

## 1. 模块目标与范围

负责商品实体、数据库查询 Repository 与 Service、商品列表和详情 REST 接口、Angular 商品列表与详情页面、分页及状态提示，并提供商品示例和图片。

商品浏览只展示上架商品。页面使用英文文案，支持图片、SGD 价格、详情跳转、加载提示、空结果、请求失败重试和商品不存在提示。构建后的前端接入 Spring Boot，通过同一个应用访问页面、接口和图片。

购物车操作、结账、登录权限和管理员商品编辑属于其他模块。A 提供商品查询和上架状态规则；加购或结账时拒绝不存在、下架商品的业务处理，需要 C、D 模块调用这些查询并联合验证。

分工依据：《CA购物车项目六人分工与规划》第 3 节 A 模块及共同交付要求、第 5 节共享接口约定。

## 2. 后端开发与数据库接入

### 建立工程并连接数据库

创建并运行 Spring Boot 工程，使用 MySQL Workbench 建立 `shopping_cart` 数据库，配置数据源和 JPA，通过实体映射生成商品表。启动过程中遇到端口占用，排查并调整配置后，将后端和前端代理目标统一为 `18080`。

数据库账号与密码由环境变量提供，不写入日志。Workbench 修改只影响当前连接的数据库，组员使用独立数据库时需通过项目初始化规则或补充 SQL 准备数据。

当前工程声明 Spring Boot 4.1.1、Java 17；本机运行使用 JDK 25。前端使用 Angular 22.2.1、TypeScript 6.0.x、RxJS 7.8.x，Node 24.18.0；依赖版本由 `package-lock.json` 锁定。这些是本机已使用的环境，不表示所有组员环境均已验证。

### 商品实体与查询接口

建立 `Product` 实体，包含 ID、名称、描述、价格、图片地址和上架状态。Java 金额使用 `BigDecimal`，数据库使用 `DECIMAL(12,2)`；图片字段保存地址，实际图片由静态资源目录提供。

先通过浏览器和 Postman 确认商品列表返回 Keyboard、Mouse，再补充查询服务及详情接口。商品存在且上架时详情返回 HTTP 200，不存在或下架时返回 HTTP 404。

当前请求调用链为：

```text
Angular 商品查询服务 / Postman
    → ProductRestController
    → ProductService
    → ProductRepository
    → MySQL
    → 商品 JSON
```

Controller 接收请求并组织响应；Service 组织商品查询、排序与只读事务；Repository 访问数据库。其他模块可直接复用 Java 商品查询服务。

### 上架状态与初始化保护

`active=true` 表示上架，`false` 表示下架。Java 字段默认值为 `true`，实体列配置为 `BOOLEAN DEFAULT TRUE`。本机通过 JPA 自动更新处理字段增加；新数据库和其他电脑启动仍待实际验证，不能将这项配置视为通用数据库迁移方案。

Repository 提供只查询上架商品的方法，Service 的列表、分页和详情均使用上架条件。用户已通过下架商品和全部下架的操作确认过滤及空列表行为。

`DataInitializer` 只在商品表完全为空时插入示例，不按固定 ID 强制修改图片或价格。商品表已有数据时跳过整批商品初始化，保留管理员修改及下架状态。当前默认示例已扩充为 12 条，实际扩充记录见第 6 节。

### 分页与参数校验

保留数组列表接口，通过是否包含 `page` 参数区分分页接口。Service 使用 `PageRequest`，按商品 ID 升序排序；Repository 在数据库阶段分页并过滤下架商品。

`ProductPageResponse` 提供 `content`、`page`、`size`、`totalElements` 和 `totalPages`。服务端限制 `page >= 0`、`1 <= size <= 100`，省略 `size` 时默认值为 6。接口页码从 0 开始，界面显示页码从 1 开始。

## 3. Angular 页面与商城界面

### 组件与页面状态

前端采用独立组件、TypeScript、独立 HTML 模板与 CSS，启用严格类型检查，不启用服务端渲染。根组件读取查询参数 `id`，据此显示列表或详情；公共布局提供品牌区、导航、跳过导航和页脚。

商品查询服务通过 `HttpClient` 请求分页或详情接口，`Product` 和 `ProductPage` 接口描述返回字段。页面使用 signals 保存商品、页码、页大小、加载和错误状态。

列表默认每页 6 条，可选 1、6、12 条。切换页大小回到第一页，首页 Previous 和末页 Next 禁用。列表显示图片、名称、价格和详情入口，详细描述在详情页显示。

开始新请求前取消旧订阅，组件销毁时也取消未完成请求；详情 ID 改变时采用相同处理，防止旧响应覆盖当前页面。错误状态提供 Try again；详情 404 显示 Product not found。

页面使用普通链接和查询参数，没有 Angular Router。列表地址为 `/products`，详情地址为 `/products?id=商品ID`；链接显式生成路径。返回列表重新加载，恢复第一页和默认每页 6 条。

### 界面和图片布局

参考用户提供的商城截图，采用橙色主题、顶部导航、分类介绍、推荐横幅和商品卡片，保留英文文案。Explore 是商品浏览入口，分类文字用于介绍；当前未实现搜索或分类筛选。

商品图片使用受限宽度、固定展示高度和 `object-fit: contain`，修复原图超出卡片的问题。共享 CSS 提供桌面与手机布局、键盘焦点和减少动画偏好适配。

Keyboard、Mouse 图片来自 Logitech 官方产品页面：[K120](https://www.logitech.com/en-us/shop/p/k120-usb-standard-computer)、[M185](https://www.logitech.com/en-us/shop/p/m185-wireless-mouse)。它们用于课程演示，不标记为自行创作或开放许可素材。其余 10 个示例使用项目内的 SVG 图示。

### 构建与接入

开发服务器使用 `4200`，通过 `proxy.conf.json` 将 `/api/**`、`/images/**` 代理到后端 `18080`。

生产构建使用 `ng build --configuration production --base-href=/products/`，浏览器产物位于 `frontend/dist/browser`。`ProductPageController` 将 `/products` 转发到静态入口，图片和接口使用站点根路径。

`npm run deploy` 依次检查、测试、构建，再由部署脚本备份并替换商品静态目录。产物同步到 `shopping-cart/src/main/resources/static/products`，已有编译输出时同步到 `shopping-cart/target/classes/static/products`。编译输出不是源码，交付应能从项目源码重新构建。

## 4. 主要文件与接口

下表为 A 模块文件及涉及的共享资源；共享文件不表示全部内容均由 A 独立负责。

| 文件 | 作用 |
| --- | --- |
| [Product.java](../src/main/java/sg/edu/nus/iss/shoppingcart/entity/Product.java) | 商品数据及数据库映射。 |
| [ProductRepository.java](../src/main/java/sg/edu/nus/iss/shoppingcart/repository/ProductRepository.java) | 上架商品列表、分页及按 ID 查询。 |
| [ProductService.java](../src/main/java/sg/edu/nus/iss/shoppingcart/service/ProductService.java) | 商品查询、排序与只读事务。 |
| [ProductRestController.java](../src/main/java/sg/edu/nus/iss/shoppingcart/controller/ProductRestController.java) | 商品 API、参数校验和响应。 |
| [ProductPageResponse.java](../src/main/java/sg/edu/nus/iss/shoppingcart/dto/ProductPageResponse.java) | 分页响应字段，不是数据库实体。 |
| [ProductPageController.java](../src/main/java/sg/edu/nus/iss/shoppingcart/controller/ProductPageController.java) | 商品页面入口。 |
| [DataInitializer.java](../src/main/java/sg/edu/nus/iss/shoppingcart/config/DataInitializer.java) | 共享初始化入口，A 负责商品示例部分。 |
| [app.component.ts](../frontend/src/app/app.component.ts) | 根据商品 ID 选择列表或详情。 |
| [store-layout.component.ts](../frontend/src/app/store-layout/store-layout.component.ts) | 公共布局和导航。 |
| [product-list.component.ts](../frontend/src/app/product-list/product-list.component.ts) | 列表、分页及请求状态。 |
| [product-detail.component.ts](../frontend/src/app/product-detail/product-detail.component.ts) | 单个商品、详情状态及返回入口。 |
| [icon.component.ts](../frontend/src/app/icon/icon.component.ts) | 页面装饰图标。 |
| [product.ts](../frontend/src/app/models/product.ts) | 商品及分页 TypeScript 类型。 |
| [product.service.ts](../frontend/src/app/services/product.service.ts) | Angular HTTP 查询服务。 |
| [styles.css](../frontend/src/styles.css) | 公共主题、响应式布局及状态样式。 |
| [deploy.mjs](../frontend/scripts/deploy.mjs) | 备份和部署静态产物。 |
| [前端 README](../frontend/README.md) | 运行命令、文件职责、语法和路径约定。 |

组件的 `.html` 和 `.css` 与同名 `.ts` 协作，详细职责见 README。静态图片位于 `shopping-cart/src/main/resources/static/images`。

| 入口或请求 | 行为 |
| --- | --- |
| `/products` | 商品列表页面。 |
| `/products?id=商品ID` | 商品详情页面。 |
| `GET /api/products` | 所有上架商品的数组。 |
| `GET /api/products?page=0&size=6` | 第一页商品及分页元数据。 |
| `GET /api/products?page=0` | 默认每页 6 条。 |
| `GET /api/products/{id}` | 上架商品详情；不存在或下架时返回 404。 |
| `GET /api/products?page=-1&size=1` | 非法页码返回 400。 |
| `GET /api/products?page=0&size=0` | 非法页大小返回 400；size 大于 100 同样非法。 |

示例商品使用数据库自增 ID。当前开发数据库的 Keyboard、Mouse ID 为 1、2，不应假定其他已有数据库也使用相同 ID。

## 5. 2026-10-06：测试、部署和浏览器验收

### 自动检查

先建立 15 项 Angular 测试和最小组件占位，确认测试因缺少预期商品请求失败；完成实现后全部通过。实际执行完整 `npm run deploy`：

| 检查 | 实际结果 |
| --- | --- |
| `npm run check` | 应用、测试代码的严格类型、模板及未使用变量检查通过。 |
| `npm run test` | 3 个测试文件、15 项测试通过。 |
| `npm run build` | 生产构建成功，输出 `dist/browser`，基础路径 `/products/`。 |
| `npm run deploy` | 检查、测试、构建与备份部署均成功。 |

测试使用真实 Angular 组件、查询服务和 `HttpClient`，通过 `HttpTestingController` 模拟响应，不连接 MySQL。覆盖默认分页参数、翻页、页大小改变回到第一页、禁用状态、图片和价格、空集合、失败重试、详情 404、详情链接、ID 编码、旧请求取消和组件销毁取消。

构建资源为 `main-3U7CDTEZ.js`、`styles-KRN2BM5E.css`；初始资源约 171.44 kB，构建器估计传输大小约 49.09 kB，并非实际网速测量。部署前的本机静态备份名称为 `shopping-cart-deploy-2026-10-06T12-19-11-123Z`。

### 浏览器与独立运行

在商品扩充前的两条数据基础上，实际验证：

1. 商品名称、SGD 价格、卡片与横幅图片正常；页面 `ng-version` 为 22.2.1。
2. 每页 1 条时，首页 Previous 禁用；下一页显示 Mouse，Next 禁用。
3. 第二页切换为每页 12 条回到第一页，显示两条商品。
4. Mouse 详情的图片、名称、价格、描述和返回入口正确，直接刷新仍正常。
5. 返回列表恢复第一页及每页 6 条；不存在 ID 99999 显示 Product not found，直接刷新仍正常。
6. 1440×1000 桌面和 375×812 手机的列表、详情无页面整体横向溢出，检查后恢复视口设置。
7. 开发服务器的接口和图片代理正常；停止 4200 后，18080 仍能独立刷新、浏览和翻页。
8. 18080 返回的 HTML 与构建入口一致，JS、CSS、favicon 返回 200，文件 SHA-256 与构建产物一致。

下架和全部下架的接口、页面行为曾由用户实际操作确认；分页参数的负页码、size 为 0 或 101 返回 400，已通过 HTTP 请求验证。上述结果按实际检查阶段记录，不将当前 12 条商品描述为当时的数据。

开发模式出现 `NG0913` 图片尺寸性能提示：原商品照片大于显示尺寸；图片和布局正常，未关闭提示。

截图：[桌面列表](screenshots/angular-desktop-list-2026-10-06.jpg)、[桌面详情](screenshots/angular-desktop-detail-2026-10-06.jpg)、[手机列表](screenshots/angular-mobile-list-2026-10-06.jpg)、[手机详情](screenshots/angular-mobile-detail-2026-10-06.jpg)。

### 代码整理

检查源码、配置、依赖和构建目录，移除空资源目录、不适用的忽略项及部署脚本临时分支；部署统一先备份再替换。TypeScript 启用 `noUnusedLocals`、`noUnusedParameters`。

Vite 是 Angular 构建工具和 Vitest 的实际间接依赖，经 `npm explain vite` 核对保留。`.angular`、格式化及编辑器配置属于当前工具链。B 模块的 AuthService、LoginForm 等预备代码保留，未作为 A 模块冗余删除。

清理后检查、15 项测试、构建、部署和资源一致性核对通过。该轮没有修改数据库，也没有重复整套浏览器交互验收。

## 6. 2026-10-06：扩充商品示例

商品集合从 2 条扩充到 12 条，新增 Headphones、USB-C Hub、Laptop Stand、Desk Lamp、Webcam、Portable SSD、Monitor、Bluetooth Speaker、Desk Mat、USB Microphone。每条有英文描述、示例价格、图片地址和上架状态。金额为课堂自拟数据，不代表市场报价。

新增 10 个 640×400 SVG 图示，无脚本、外部图片或网络资源引用，保留原 Keyboard、Mouse 图片。图片同步到源码和编译输出，已有 Angular 页面通过 API 读取新增商品，不需要改动组件或重新构建前端。

[补充 SQL](sql/add-demo-products.sql) 为已有数据库添加缺少的新商品，按名称判断，使用事务，不更新同名记录。空商品表由 DataInitializer 自动生成 12 条；已有数据的成员使用补充 SQL。当前数据库是在备份后执行 SQL 扩充，并非重启触发空表初始化。

[初始化测试](../src/test/java/sg/edu/nus/iss/shoppingcart/config/DataInitializerTests.java) 通过模拟仓库验证空表生成 12 条、已有修改和下架状态保留、重复启动不重复添加及已有用户保留。先确认旧的两条种子实现导致两项数量测试失败，扩充后执行 `mvnw.cmd -Dtest=DataInitializerTests test -q`，4 项全部通过，失败和错误为 0；测试不访问真实数据库。

本机 JDK 25 的 Mockito agent 遇到中文路径问题，测试资源选择接口代理后测试通过。启动控制台仍有 agent 路径诊断，不将其描述为完全无告警。

实际导入后 API 返回 12 条；顺序重复执行 SQL 后数量和商品 JSON 均不变。与导入前记录对照，Keyboard、Mouse 的名称、描述、价格、图片和上架字段未变。

浏览器确认默认两页各 6 条，切换每页 12 条显示全部商品；新增图示均加载成功；Headphones 详情显示 SGD 79.00、描述与图片，刷新及返回默认分页正常。默认桌面宽度与滚动宽度均为 1265，375×812 手机两页的可用宽度与滚动宽度均为 360，无整体横向溢出。

本机初始化源码、原商品 JSON 和商品表 SQL 备份名称为 `shopping-cart-demo-products-20261006-204010`。本次未进行购物车、结账或订单测试，也未连接一个全新的真实数据库验证启动；空表行为由上述单元测试验证。

截图：[全部商品桌面视图](screenshots/demo-products-desktop-2026-10-06.jpg)、[手机商品视图](screenshots/demo-products-mobile-2026-10-06.jpg)。

## 7. 2026-10-06：作者署名与文档整理

为 35 个 A 相关源码文件添加职责说明和 `@author 王重一`，包括商品后端、共享初始化的商品部分、初始化测试、Angular 源码和测试、HTML/CSS、部署脚本及补充 SQL。HTML 使用 HTML 注释，不将署名显示为页面内容。

DataInitializer 的说明明确商品部分责任，未替其他模块的用户、登录、密码配置或工程启动入口添加 A 署名。原照片的来源说明保留。逐文件核对，去掉新增署名注释后内容与备份相同；Java 编译、Angular 严格检查和部署脚本语法检查通过。署名备份名称为 `shopping-cart-author-comments-20261006-210027`。

README 与日志采用项目相对路径，不包含开发电脑的盘符或存放前缀。已将 10 张既有验收截图复制到 `docs/screenshots/`，图片可随项目交付；本机备份仅记录名称。当前日志围绕商品后端、Angular 前端、示例数据及实际验收整理。

本轮只调整文档、检查文件链接，没有修改代码、数据库或重跑功能测试。

## 8. 实际开发与辅助记录

王重一在 IDEA 中按逐步讲解创建、输入和保存 Java 商品代码，使用 Workbench、Postman、浏览器反馈结果。Codex 提供示例、文件职责讲解和排查帮助，并按授权直接完成 Angular 实现、样式、图示、检查、测试、构建、部署和文档整理。

记录保留实际辅助使用情况，不虚构独立开发工时、提交次数或未发生的团队协作。

## 9. 待办与交接

1. 向 F 交接入口、接口、运行说明和验收记录，配合最终打包。
2. 与 C、D 对接 `ProductService.findProductById(Long id)`；空 Optional 表示不存在或下架，调用方须拒绝加购或结账，结账时重新查询当前价格。
3. 与 E 联合验证商品改价、图片修改和上下架能反映到列表、分页与详情。
4. 补做管理员修改后重启、全新真实数据库及另一台电脑启动验证。
5. 准备列表、分页、详情、下架及异常状态演示，说明 Controller → Service → Repository → MySQL 的流程。

返回列表会回到第一页并恢复默认每页数量。A 模块本机验收不代表购物车、结账、订单或整个项目的联合验收已经通过。
