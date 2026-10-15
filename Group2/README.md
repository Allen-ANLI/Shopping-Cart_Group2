# Group 2 Shopping Cart

这是一个 Spring Boot + Angular 的购物车网站。商品目录使用 Angular，登录、账户、购物车、结账、订单历史和管理员后台使用 Spring MVC、Thymeleaf、Spring Data JPA；正式数据库使用 MySQL，演示模式使用 H2。

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
