# Group 2 Shopping Cart

这是一个 Spring Boot + Angular 的购物车网站。商品目录使用 Angular，登录、账户、购物车、结账、订单历史和管理员后台使用 Spring MVC、Thymeleaf、Spring Data JPA；正式数据库使用 MySQL，演示模式使用 H2。

2026-10-16 搜索更新：仅匹配商品的中英文名称及品牌，介绍中偶然提及关键词的商品不算命中；所有排序及推荐排序均先筛选后返回，无匹配时显示“没有找到相关商品”，不会补入其他推荐。空输入及全空白输入提交时提示“请输入搜索内容”，保留已有搜索条件；后端对显式空白搜索同样返回错误提示。242 项后端测试、85 项 Angular 测试、25 项购物车脚本测试、生产构建及独立 H2 环境的实际运行包搜索接口检查通过。最新运行包为 `runtime/nexus-shopping-cart-search.jar`，包含此前横幅与购物车修改；停止旧服务后使用此包启动，普通 MySQL 模式沿用原有数据库环境变量，演示模式加上 `--spring.profiles.active=b-demo`。

2026-10-15 购物车最新交互：输入时不校验、不提示、不自动改值；点击其他位置离开数量框后，才自动校验并保存。超过库存调整为库存上限，0 或负数调整为 1，显示调整提示；空值、非数字及不符合整数格式的输入提示无法更改并恢复原数量。修正后的有效数量保存成功即可结账，未保存或仍有错误时禁止结账。25 项购物车交互测试及独立 H2 环境的实际运行包 HTML/JS/HTTP 联动检查通过。旧标准运行包与 target JAR 被其他进程占用，已用独立目录构建并另存 `runtime/nexus-shopping-cart-quantity-blur.jar`；停止旧服务后，使用这个新版 JAR 启动。普通 MySQL 模式可在原有数据库环境变量下执行 `java -jar runtime/nexus-shopping-cart-quantity-blur.jar`，演示模式加上 `--spring.profiles.active=b-demo`。

2026-10-15 购物车上一轮更新：移除 Update 按钮，数量变化自动保存，并同步小计、折扣、GST、总额及导航数量。数量上限统一为实际库存，支持超过 99 件；删除使用 Remove，服务端更新和下单均重新校验库存。该轮 236 项后端测试、78 项 Angular 测试及 15 项购物车交互测试通过。数量输入的具体交互以本页最新说明为准。

2026-10-15 首页横幅更新：统一按 01→02→03 循环，左右箭头、编号选择与自动轮播保持一致；第一屏保留今日特惠，第二屏展示真实推荐商品及介绍并直达商品详情，第三屏为无跳转链接的“广告位招租”。中英文同步更新。前端类型检查、72 项测试、生产构建及运行包 HTTP 检查通过；静态资源与 `runtime/nexus-shopping-cart.jar` 已同步，重启应用后生效。本轮横幅更新未运行后端测试，浏览器连接不可用，未完成截图目检。

## 快速演示

双击 `Start-H2.cmd`，应用会使用内存 H2 数据库并打开：

```text
http://127.0.0.1:18092/products
```

演示账号：

- 管理员：`admin / admin123`
- 普通用户：`alice / demo123`

演示数据库在应用停止后重置。停止时关闭启动窗口或按 `Ctrl+C`。

## MySQL 运行

先在 MySQL 中执行 [database/create-database.sql](database/create-database.sql)，然后双击 `Start-MySQL.cmd`，按提示输入数据库账号和密码。

也可以直接运行当前交付包：

```powershell
java -jar runtime/nexus-shopping-cart.jar
```

正式页面地址：

```text
http://127.0.0.1:18080/products
```

正式数据库不会自动创建管理员。请先注册账号，再由数据库管理员执行：

```sql
UPDATE app_users SET role = 'ADMIN' WHERE username = 'your-admin-name';
```

## 从源码构建

前端：

```powershell
cd frontend
npm ci
npm run check
npm run test
npm run build
npm run deploy
```

如果电脑没有全局 Node.js，可以使用项目内的 `.local/tools/node-v24.19.0-win-x64/npm.cmd`。

后端：

```powershell
cd ..
.\mvnw.cmd clean package
java -jar target/shopping-cart-0.0.1-SNAPSHOT.jar
```

修改源码后，先部署 Angular 静态资源，再重新打包后端，确保 JAR 中包含最新页面。

## 主要页面

| 功能 | 地址 |
| --- | --- |
| 商品列表和详情 | `/products`、`/products?id=1` |
| 登录和注册 | `/login`、`/register` |
| 账户和地址 | `/account`、`/account/addresses` |
| 购物车和结账 | `/cart`、`/checkout` |
| 购买历史 | `/orders` |
| 管理员商品管理 | `/admin/products` |
| 管理员数据看板 | `/admin/dashboard` |

商品、订单、支付、库存和评价数据均由后端服务和数据库统一处理。Angular 开发服务器可通过 `npm run dev` 启动，默认地址为 `http://127.0.0.1:4200/`。

## 项目目录

- `src/`：Spring Boot 后端源码、Thymeleaf 页面和静态资源。
- `frontend/src/`：Angular 源码。
- `database/`：MySQL 建库脚本。
- `runtime/`：当前可运行的交付 JAR。
- `scripts/`：本地运行脚本。
