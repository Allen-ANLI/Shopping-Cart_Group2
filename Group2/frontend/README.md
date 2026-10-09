# A 模块前端：Angular 商品浏览

当前前端使用 Angular **22.2.1**、TypeScript **6.0.x** 和 RxJS **7.8.x**。现有 Node **24.18.0** 已完成本机验证。版本依据：[Angular 官方兼容表](https://angular.dev/reference/versions)；实际精确依赖由 `package-lock.json` 锁定。


## 项目路径与文档约定

`shopping-cart/` 表示你保存的项目根目录，项目可放在任意盘符和文件夹；README 和开发日志不使用开发者电脑的绝对路径前缀。正文的项目路径用于定位文件，Markdown 链接相对于所在文档解析。

本 README 的前端文件表以 `shopping-cart/frontend/` 为起点；命令示例会注明执行目录。验收截图随项目保存在 `shopping-cart/docs/screenshots/`，本机备份只记录名称，不表示备份已包含在项目内。团队后续新增说明与日志也使用此约定。

## 运行与构建

先在 IDEA 启动 `ShoppingCartApplication`，数据库环境变量继续沿用原配置。后端端口为 **18080**。

在 PowerShell 中先进入你保存的 `shopping-cart` 项目根目录，再进入前端目录并安装锁定的依赖：

```powershell
cd frontend
npm ci
npm run dev
```

`npm ci` 根据锁定文件安装依赖；第一次准备环境或依赖文件改变时执行。`npm run dev` 启动 Angular 开发服务器并持续监听代码修改，终端会保持运行；停止时按 `Ctrl+C`，若询问是否终止批处理则输入 `Y`。

- 开发入口：`http://127.0.0.1:4200/`。
- 正式入口：`http://localhost:18080/products`。
- 正式详情：`http://localhost:18080/products?id=1`。

| 命令 | 作用 |
| --- | --- |
| `npm run dev` | 在 4200 启动开发页面，将 API 和图片请求代理至 18080。 |
| `npm run check` | 使用 Angular 编译器检查应用、测试代码的严格 TypeScript 类型、HTML 模板及未使用的变量、导入和参数，不生成生产资源。 |
| `npm test` / `npm run test` | 执行一次 Angular/Vitest 测试后退出，使用模拟 HTTP 响应。 |
| `npm run build` | 生成生产代码，基础路径为 `/products/`，浏览器资源位于 `dist/browser`。 |
| `npm run deploy` | 依次执行 check、test、build；全部成功后备份旧页面并部署新页面。 |

开发服务器的页面实时更新。18080 提供的是已经构建的静态文件；修改源码后需重新运行 `npm run deploy`。正式运行不需要同时启动 4200。

部署位置：

1. 源码静态目录：`shopping-cart/src/main/resources/static/products`。
2. 若已存在编译输出目录，额外同步：`shopping-cart/target/classes/static/products`，使使用该目录的 IDEA 应用加载新页面。

`target` 是生成目录。交接、重新编译和打包时应以源码目录为准。部署不会改动 `static/images` 或 Java 文件；后端端口改变时，在 `proxy.conf.json` 中调整开发代理目标。

## Angular 文件职责

下表中的路径均位于 `shopping-cart/frontend`。组件的 `.ts`、`.html` 与 `.css` 是三个协作文件，而不是三个独立页面。

| 文件 | 职责 |
| --- | --- |
| `src/index.html` | HTML 容器、英文语言、页面标题、基础路径和图标，包含 `<app-root>`。 |
| `src/main.ts` | 调用 `bootstrapApplication`，创建根组件并传入应用配置。 |
| `src/app/app.config.ts` | 注册 `HttpClient` 和全局浏览器错误监听，供依赖注入使用。 |
| `src/app/app.component.ts`、`.html`、`.css` | 读取 URL 的 `id`；有 `id` 显示详情，否则显示列表；外层使用公共布局。 |
| `src/app/store-layout/store-layout.component.ts`、`.html`、`.css` | 顶部品牌、导航、跳过导航链接、页脚；通过 `<ng-content>` 放入商品页面。 |
| `src/app/product-list/product-list.component.ts`、`.html`、`.css` | 推荐横幅、商品卡片、分页、页大小选择以及加载、空结果、失败重试。 |
| `src/app/product-detail/product-detail.component.ts`、`.html`、`.css` | 根据输入 ID 查询单个商品；展示图片、价格、描述、404、错误重试和返回入口。 |
| `src/app/icon/icon.component.ts`、`.html`、`.css` | 根据图标名称绘制内联 SVG；图标属于装饰，不进入屏幕阅读器描述。 |
| `src/app/models/product.ts` | 定义 `Product` 和 `ProductPage` 接口，描述现有 JSON 字段及类型。 |
| `src/app/services/product.service.ts` | 发送商品名称搜索、价格排序、分页和详情 GET 请求。 |
| `src/app/services/catalog-navigation.service.ts` | 用共享 signals 同步当前商品查询参数，让页头和横幅的页内链接保留浏览状态。 |
| `src/styles.css` | 共享的橙色主题、布局、断点、焦点和动画偏好样式。 |
| `src/app/**/*.spec.ts` | 根页面地址、列表和详情的自动化行为测试。 |
| `src/app/testing/product-fixtures.ts` | 只供测试使用的完整商品与分页示例，替代真实数据库响应。 |
| `public/favicon.svg` | 商城购物袋图标，构建时原样复制。 |
| `angular.json` | CLI 的构建、开发服务器和测试目标；资源、CSS、输出目录和生产体积限制。 |
| `proxy.conf.json` | 将 `/api/**` 和 `/images/**` 转发给本地后端。 |
| `tsconfig.json`、`tsconfig.app.json`、`tsconfig.spec.json` | 共享严格检查设置，分别限定应用与测试的编译范围。 |
| `package.json`、`package-lock.json` | 脚本、依赖声明、精确安装版本。 |
| `scripts/deploy.mjs` | 验证 Angular 构建，备份旧部署，替换商品静态文件，并同步已有编译输出。 |
| `.gitignore` | 排除依赖、构建产物和 Angular 缓存。 |
| `.editorconfig`、`.prettierrc` | 编辑器与格式化设置，不参与页面业务逻辑。 |

各组件 CSS 只声明必要的宿主显示方式；图标 CSS 还设置内部 SVG 大小。大部分视觉规则集中在 `src/styles.css`，方便列表与详情共享。图片实际位于后端 `src/main/resources/static/images`，仍通过 `/images/keyboard.png` 与 `/images/mouse.png` 读取。

## 一次商品查询是怎样执行的

以打开 `/products` 为例：

```text
src/index.html 的 app-root
    → main.ts 启动 AppComponent
    → AppComponent 未读到 id，显示 ProductListComponent
    → ngOnInit() 调用 load()
    → ProductService.getPage(0, 6)
    → HttpClient GET /api/products?page=0&size=6
    → ProductRestController → ProductService → Repository → MySQL
    → ProductPage JSON
    → signals 更新
    → HTML 显示卡片与分页按钮
```

`@Component` 告诉 Angular 组件的标签名、模板、样式及依赖。当前组件采用独立组件：每个组件在 `imports` 中声明自己模板需要的其他组件，无需建立 `AppModule`。

`inject(ProductService)` 由 Angular 提供商品查询服务实例；服务再通过 `inject(HttpClient)` 获得 HTTP 客户端。`app.config.ts` 中的 `provideHttpClient()` 为这一步注册依赖。

`getPage()` 返回 RxJS 的 `Observable<ProductPage>`，即可以订阅的响应流；`subscribe(...)` 开始请求。成功时执行 `next`，失败时执行 `error`。方法调用时的 `0` 和 `6` 分别代表后端第 0 页及每页 6 条；用户看到的页码使用 `page() + 1`。

`Product` 和 `ProductPage` 是 TypeScript 接口，在编译时检查字段使用是否正确。它们描述现有后端数据，不负责生成数据库表，也不在运行时自动校验任意 JSON。

## signals 与模板如何协作

列表中的 `signal(0)` 保存当前页码：

```typescript
readonly page = signal(0);
// 读取当前页：this.page()
// 设置下一页：this.page.set(1)
```

signals 是 Angular 提供的响应式状态容器；模板读取状态后，状态变化会触发相应显示更新。列表的 `ngOnInit` 在初始化时加载商品，详情的 `ngOnChanges` 在输入 ID 改变时重新查询，`ngOnDestroy` 在组件销毁时取消未完成的请求。

常见模板写法：

- `{{ product.name }}`：把商品名称显示为文本。
- `[href]="productUrl(product.id)"`：把计算出的完整页面路径绑定给链接。
- `(click)="retry()"`：点击按钮时调用组件方法。
- `[disabled]="loading()"`：请求尚未结束时禁止重复操作。
- `@if (...)`：选择加载、错误、空结果或正常页面。
- `@for (product of products(); track product.id)`：遍历商品，用唯一 ID 识别对应的卡片。
- `@Input({ required: true }) productId`：详情组件从根组件接收商品 ID。

例如点击 Next 时，`changePage` 先检查是否正在加载、是否越界，然后修改页码并重新请求。选择每页 12 条时，`changeSize` 先把页码改为 0，再按 12 条请求；因此不会保留原来的第二页位置。

开始新请求前，组件调用旧订阅的 `unsubscribe()`。对于 `HttpClient`，取消订阅会取消尚未完成的 HTTP 请求；组件销毁时也执行同样清理，避免旧响应影响新页面。这里取消的是前端请求，不代表撤销后端已经完成的数据库操作；本模块发出的都是只读 GET 请求。依据：[Angular HttpClient 请求说明](https://angular.dev/guide/http/making-requests)。

## URL、静态资源与返回行为

本模块继续使用普通链接和查询参数，没有 Angular Router。

生产 `base href` 为 `/products/`，用于浏览器解析 `main-*.js`、`styles-*.css` 和图标。页面链接明确由当前 `window.location.pathname` 生成：正式列表 `/products`、详情 `/products?id=1`；开发列表 `/`、详情 `/?id=1`。因此页面链接不会被基础路径隐式改为 `/products/?id=1`。

返回链接只去掉 `id`，保留搜索词、排序、页码和每页数量；直接刷新也读取这些参数。无参数进入列表时使用第一页和每页 6 条。跳过导航链接会保留当前查询参数，确保详情页的 Skip to content 仍指向当前详情正文。

商品图片与接口地址以 `/` 开头，使用 `/images/...` 和 `/api/...`；它们从站点根路径读取，与生产基础路径独立。

## 测试、备份与本次验证

自动测试使用真实 Angular 组件、商品查询服务和 `HttpClient`，仅在 HTTP 边界使用 `HttpTestingController` 返回模拟数据。测试不会连接 MySQL，也不会修改真实商品的上架状态。测试工具依据：[Angular HTTP 测试文档](https://angular.dev/guide/http/testing)。

2026-10-06 本机验证：严格检查通过；3 个测试文件、15 项测试通过；生产构建通过；4200 商品数据和图片代理通过；18080 的列表、分页、详情、404 和直接刷新通过；1440×1000 与 375×812 的页面无整体横向溢出。停止 4200 后，18080 继续加载 Angular 构建资源并可正常浏览。

后续每次部署会在本机备份目录中创建 `shopping-cart-deploy-时间`，保存之前的商品静态目录。组员运行部署前应通过环境变量 `SHOPPING_CART_BACKUP_DIR` 指定自己电脑上可写的备份目录。恢复旧版本时，应按备份还原对应源码及部署目录，并使用其锁定文件重新安装依赖。

开发模式记录到 `NG0913` 图片尺寸警告：沿用的商品原图大于卡片显示尺寸。这是图片加载性能提示，实际图片和布局正常；保留现有商品原图，未关闭该提示。生产页面功能验证已通过。

本次验收范围为 A 模块，作者署名已完成。新电脑/新数据库启动及购物车、结账、订单历史的跨模块联调仍按[开发日志](../docs/A模块开发日志.md)交接。

## 示例商品扩充（2026-10-06）

商品集合从 2 条扩充到 12 条。新增商品用于课堂演示，价格为自拟示例金额；新增图片是项目内的 SVG 商品图示，没有外部图片地址或网络加载依赖。

| 新增商品 | 示例价格（SGD） | 图片文件 |
| --- | ---: | --- |
| Headphones | 79.00 | headphones.svg |
| USB-C Hub | 39.90 | usb-c-hub.svg |
| Laptop Stand | 35.00 | laptop-stand.svg |
| Desk Lamp | 29.00 | desk-lamp.svg |
| Webcam | 59.90 | webcam.svg |
| Portable SSD | 99.90 | portable-ssd.svg |
| Monitor | 199.90 | monitor.svg |
| Bluetooth Speaker | 49.90 | bluetooth-speaker.svg |
| Desk Mat | 12.90 | desk-mat.svg |
| USB Microphone | 89.90 | usb-microphone.svg |

默认每页 6 条，因此目前有两页；选择每页 12 条可查看整个集合。商品列表仍隐藏详细描述，点击商品进入详情页后显示描述。

### 数据如何进入页面

`src/main/java/sg/edu/nus/iss/shoppingcart/config/DataInitializer.java` 是启动数据初始化类。Spring Boot 启动时调用其 `run` 方法；商品表完全为空时，`createProduct` 创建 12 个 Product 对象，`saveAll` 将它们保存到数据库。已有任何商品时跳过整批初始化，保留用户已经修改的价格、图片和上下架状态。

已有数据库的组员应在 MySQL Workbench 打开项目根目录下的 `docs/sql/add-demo-products.sql` 并执行。该脚本只补入缺少的 10 个新商品，按商品名称判断是否已存在；顺序重复执行不会重复添加，也不会更新同名商品。脚本使用 `shopping_cart` 数据库，数据库名不同的成员先调整 `USE` 语句。空数据库的成员直接启动后端即可，不必先运行补充脚本。

`src/main/resources/static/images/` 保存图片，Product 的 `imageUrl` 保存对应的 `/images/...` 路径。Angular 已有的商品服务读取 REST JSON，列表模板循环显示返回的商品；因此本次新增数据无需修改 Angular 组件或重新构建前端。IDEA 编译目录中的图片已同步，当前运行的 18080 页面可直接读取它们。

`src/test/java/sg/edu/nus/iss/shoppingcart/config/DataInitializerTests.java` 使用模拟仓库验证空表生成 12 条、已有商品保留、重复启动不重复添加及已有用户保留。测试不会连接真实 MySQL；测试资源中的 Mockito 配置选择接口代理实现。

2026-10-06 实际验证：4 项初始化单元测试通过；当前数据库补入后为 12 条；补充 SQL 顺序重复执行后商品 JSON 不变，原有 Keyboard 和 Mouse 字段保持不变；默认两页各 6 条、每页 12 条、Headphones 详情与直接刷新、返回默认分页均正常。默认桌面宽度及 375×812 手机视口下未发现整体横向溢出，10 个新增商品图示全部加载成功。


## 2026-10-09：商城操作优化

共享页头提供真实商品名称搜索，搜索覆盖全部上架商品，再进行分页。商品工具栏支持 Featured（ID 升序）、Price: low to high、Price: high to low；价格相同时按 ID 升序。更改排序或每页数量回到第一页。查询参数 `q`、`sort`、`page`、`size` 保留在地址栏及详情返回链接中。搜索无结果可 Reset search；请求失败仍可 Try again。

分页接口示例：`/api/products?page=0&size=6&q=Keyboard&sort=price-asc`。`q` 最长 100 字符，按名称忽略大小写匹配；`sort` 只允许 `featured`、`price-asc`、`price-desc`。原 `/api/products` 数组接口以及 `/api/products/{id}` 保留，旧调用不传新增参数仍按原方式工作。

移除未实现的分类筛选、模糊的 Add products 全局入口及重复促销按钮。账户导航显示 Products、Cart（商品总数量），登录后显示 Orders 和 Account；Admin 仍仅对管理员显示。详情以 Add to cart 为主要操作，Back to products 使用次要样式。

严格类型和模板检查通过，5 个文件共 49 项前端测试通过，生产构建和部署通过。浏览器验证范围及后端联动见 [UI 优化说明](../docs/UI优化说明.md)。以上新增搜索、排序、导航状态测试使用模拟响应；真实界面使用独立 H2 演示库，不连接成员的正式 MySQL。
