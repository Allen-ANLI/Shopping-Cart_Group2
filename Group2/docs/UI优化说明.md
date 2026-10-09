# 商城 UI 优化说明

日期：2026-10-09。基于 Allen-ANLI/Shopping-Cart_Group2 的提交 `f53a85989e14ab116b1686827caa058586097cbc`，本地分支 `ui-shopping-flow`。下载时远端连接重置，使用已有完整源码副本重建工作目录；上传前再次核对远端 main，确认仍为上述基础提交。本次改动通过独立分支供团队审阅合并，原工作项目与正式 MySQL 未修改。

## 已完成的操作调整

| 位置 | 修改后行为 |
| --- | --- |
| 商品页头 | Search 按名称搜索全部上架商品，最长 100 字符。 |
| 商品工具栏 | Featured、价格升序、价格降序；搜索后再分页，同价按 ID 稳定排序。 |
| 浏览状态 | 排序、页码、页大小和搜索保存在 URL；详情返回与刷新保留状态。 |
| 公共导航 | Products、Cart（总数量）、登录后的 Orders、Account，以及有权限时的 Admin。 |
| 冗余入口 | 移除无实现的分类、重复促销入口和含义不清的 Add products 全局链接。 |
| 商品详情 | Add to cart 为主操作，Back to products 为次操作；图片、商品名仍可进入详情。 |
| 购物车 | 减号、数量、加号及 Update quantity；修改后明确点击保存，0 删除、99 为上限。 |
| 清空购物车 | 先进入确认页；Cancel 保留商品，确认后沿用受令牌保护的 POST 清空。 |
| 订单成功页 | View order 进入本次订单详情，Continue shopping 返回商品集合。 |
| 错误页 | 保留当前已核验的登录导航；REST 参数错误返回 JSON。 |

保留原商品实体、数据库结构、Session 购物车、订单事务及权限检查。没有添加收藏、优惠券、支付或虚构分类。搜索参数仅扩展现有分页接口，旧无分页数组接口保持兼容。

## 文件职责与模块边界

- `frontend/src/app/store-layout/`：商城页头、搜索与布局。
- `frontend/src/app/product-list/`：搜索结果、排序、分页与错误重试。
- `frontend/src/app/product-detail/`：详情与保留浏览状态的返回链接。
- `frontend/src/app/services/catalog-navigation.service.ts`：共享 URL 状态，让页内链接随商品页变化更新。
- `ProductRestController`、`ProductService`、`ProductRepository`：参数校验、排序选择、全库名称筛选及分页。
- `AuthSessionController`、`AuthModelAdvice`：Angular 与 MVC 的购物车数量及身份导航。
- `CartController`、`templates/cart/clear.html`：只读确认页，真正清空仍使用原 POST 接口。
- `static/cart-assets/cart.js`：数量步进、边界禁用和 Update quantity 启用；不自动保存。禁用 JavaScript 时仍可输入数量并使用原表单。
- `templates/orders/checkout-success.html`：订单回执中的查看订单入口。
- `GlobalExceptionHandler`：异常响应及已验证导航信息的恢复。

这些公共页面改动涉及 A 与 C、D、E 的展示衔接，保留原作者信息，不把整个模块重新署名为 A。

## 运行与交接

在含 `pom.xml` 的 `Group2/` 项目目录双击 `Start-Demo.cmd`，使用内存演示数据库，入口为 `http://localhost:18083/products`。普通演示账号 `alice / demo123`，管理员 `admin / admin123`。重启后演示数据重置，不是正式持久化模式。

`Start-MySQL.cmd` 用成员自己的数据库配置运行正式模式，默认入口 `http://localhost:18080/products`。不要把演示模式代替 MySQL 验收。项目路径可以自行选择；全部说明采用项目相对路径。

修改前端后，在 `frontend/` 执行：

```powershell
npm ci
npm run deploy
```

Java 或运行包需更新时，在项目根目录执行：

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
Copy-Item target/shopping-cart-0.0.1-SNAPSHOT.jar runtime/shopping-cart.jar
```

运行包已包含生产 Angular 页面，不需要启动 4200。原运行包保留了本机可恢复备份；正式交付不包含依赖缓存、临时日志与构建缓存。

## 本次验证

- 后端：162 项测试通过，失败 0、错误 0；H2 隔离测试，不写入正式数据。新增测试覆盖名称搜索、价格稳定顺序、LIKE 特殊字符、空结果、参数错误、购物车数量、清空确认及异常导航。
- 前端：严格类型和模板检查通过；5 个测试文件、49 项测试通过。覆盖原分页、取消旧请求、错误重试、详情 404，以及新增搜索、排序、数量导航和页内链接状态。
- 构建：Angular 生产构建、静态目录部署和可执行 JAR 打包通过，`runtime/shopping-cart.jar` 已更新。
- 浏览器：使用新 JAR 的独立 H2 演示验证商品搜索、空结果、排序、分页、详情与返回、刷新、图片、购物车数量、清空取消/确认、结账及查看本次订单。未启动 Angular 开发服务器。
- 响应式：默认桌面及 375×812 手机商品、详情、购物车与确认页无整页横向溢出；手机表格允许局部滚动。
- 限制：没有重跑正式 MySQL 持久化、另一台电脑启动或最终作业视频检查。先由各模块作者审阅改动，再进行正式整合验收。

截图：[商城桌面](screenshots/ui-products-desktop-2026-10-09.png)、[购物车确认](screenshots/ui-clear-cart-2026-10-09.png)。

交接校验清单按本项目 `.gitattributes` 规定的换行保存：文本采用 LF，Windows 命令文件采用 CRLF；Git 检出后的文件可按此规则核验。
