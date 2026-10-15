# Angular 商品目录

`frontend` 是本项目的 Angular 商品目录前端。它通过后端 REST 接口读取商品、分类、评价和购物车状态，生产资源部署到 `src/main/resources/static/products`。

## 环境和运行

项目使用 Angular 22、TypeScript 6 和 RxJS 7。进入 `frontend` 后执行：

```powershell
npm ci
npm run dev
```

开发地址为 `http://127.0.0.1:4200/`，代理会把 `/api/**` 和 `/images/**` 转发到本地 Spring Boot 服务。正式运行时不需要单独启动 Angular，后端会提供已经部署的 `/products` 页面。

如果电脑没有全局 Node.js，可以使用项目内的 `.local/tools/node-v24.19.0-win-x64/npm.cmd`。

## 常用命令

| 命令 | 作用 |
| --- | --- |
| `npm run dev` | 启动 Angular 开发服务器。 |
| `npm run check` | 检查 TypeScript、HTML 模板和测试代码。 |
| `npm run test` | 执行 Angular 测试。 |
| `npm run test:cart` | 验证购物车数量离开输入框后的校验、范围修正、自动保存及结账拦截。 |
| `npm run build` | 构建生产资源。 |
| `npm run deploy` | 检查、运行 Angular 与购物车脚本测试、构建并部署到 Spring Boot 静态目录。 |

修改 Angular 源码后，运行 `npm run deploy`，再重新打包后端，确保 JAR 中包含最新页面。

## 主要源码

- `src/main.ts`：启动 Angular 应用。
- `src/app/app.component.*`：根据 URL 显示商品列表或详情。
- `src/app/product-list/`：商品列表、筛选、排序和分页。
- `src/app/product-detail/`：商品详情和评价交互。
- `src/app/add-to-cart/`：登录用户快速加购。
- `src/app/services/`：HttpClient 服务和后端接口调用。
- `src/app/models/`：商品和登录状态的 TypeScript 类型。
- `src/styles.css`：共享页面样式。
- `scripts/deploy.mjs`：将构建结果复制到后端静态资源目录。

商品图片位于后端的 `src/main/resources/static/images/`，不需要网络图片服务。
