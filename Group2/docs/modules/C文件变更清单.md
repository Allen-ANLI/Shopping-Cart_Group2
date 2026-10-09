# C 文件变更清单

负责人：Letian Xie。日期：2026-10-08。

本次以组员发来的 `CA_B.zip` 中“整合工程”为联调起点，对照 `shopping-cart-A-angular.zip` 和 `CA_D_适配新版A.zip`。B 的工程已经有迁移到新版包名的 C 购物车骨架，本次补齐 Angular 详情加购、指定商品的登录返回和页面衔接。旧 C 包保留作历史版本，不混入新版。

## 本次新增

以下路径相对于 `联调工程/`。

| 文件 | 用途 |
| --- | --- |
| `src/main/java/sg/edu/nus/iss/shoppingcart/controller/CartFormController.java` | GET `/api/cart/form`，登录后取得当前会话的表单令牌与数量信息 |
| `src/main/java/sg/edu/nus/iss/shoppingcart/dto/CartFormState.java` | 令牌接口的响应结构 |
| `frontend/src/app/add-to-cart/add-to-cart.component.ts` | 令牌请求、登录入口、提交状态及浏览器返回时的状态刷新 |
| `frontend/src/app/add-to-cart/add-to-cart.component.html` | 隐藏令牌、商品 ID、数量和原生 POST 表单 |
| `frontend/src/app/add-to-cart/add-to-cart.component.css` | 与 A 商品详情匹配的数量输入及按钮布局 |
| `frontend/src/app/add-to-cart/add-to-cart.component.spec.ts` | 加载、错误、401、提交、切换商品、销毁及返回页面等测试 |
| `src/test/java/sg/edu/nus/iss/shoppingcart/CAdaptationIntegrationTest.java` | C 与真实 B 登录、D 事务的 22 个回归用例，含参数化测试 |

## 本次修改

| 文件或目录 | 改动 |
| --- | --- |
| `frontend/src/app/product-detail/product-detail.component.ts`、`.html` | 在 A 详情中导入并显示加购组件，保留 A 主体与署名 |
| `frontend/src/app/product-detail/product-detail.component.spec.ts` | 处理新增令牌请求，检查加购及登录入口 |
| `frontend/src/app/app.component.spec.ts` | 详情场景补充匿名令牌接口的响应，保留 A/B 原测试 |
| `controller/CartProductPageController.java` | `/cart/products?productId=ID` 只显示指定上架商品；无参数仍显示全部可售商品 |
| `controller/CartPageAdvice.java` | 非法商品参数转为页面提示；仍只处理 C 页面，不接管 D 结账 |
| `templates/cart/products.html` | 指定商品的图片、价格和数量表单，以及返回 Angular 商品列表的入口 |
| `templates/cart/view.html` | 商品链接改为 `/products?id=ID`；继续购物及空车入口回到 A |
| `templates/orders/checkout.html` | 空车的继续购物入口回到 A |
| `templates/orders/checkout-success.html` | 按 D 的 `receipt` 显示订单 ID、时间、单价、数量、小计与总额，整理页面布局 |
| `templates/cart/fragments.html` | C 页面品牌文字统一为 Shopping Cart，保留 B 提供的登录状态与导航 |
| `static/cart-assets/style.css` | C 的按钮色与 A 衔接，补充所选商品的布局 |
| `interceptor/CartSessionIdentity.java`、`CartModuleTest.java` | 补充 C 署名、整理说明；原有身份判断及测试逻辑保留 |

表内简写的 Java 路径以 `src/main/java/sg/edu/nus/iss/shoppingcart/` 为根；模板和资源以 `src/main/resources/` 为根；测试位于 `src/test/java/sg/edu/nus/iss/shoppingcart/`。

## C 接入文件的范围

`C接入文件/` 提供完整的 C 购物车相关文件，方便 F 核对依赖，不仅是这次改过的几行。共有 34 个文件，其中：

- Java 主代码 13 个：4 个控制器/页面处理器、4 个 DTO、身份读取、SessionCart、CartService，以及共享的 BusinessException 和 NotAuthenticatedException。
- C 模板和资源 7 个：3 个 cart 模板、2 个 checkout 模板、CSS 与 checkout.js。
- Java 测试 2 个：原有 CartModuleTest 和本次 CAdaptationIntegrationTest。
- Angular 源码及测试 8 个。
- 本次构建的 `static/products` 资源 4 个，方便没有前端构建环境的组员核对部署结果。

这 34 个文件包含沿用的 C 代码和共享异常类型，不能全部算作本次新增。共享类型若已经存在且接口一致，保留现有版本；不要再复制一份同包同名的类。

前端产物为 `main-FGZVKWKL.js`，`index.html` 已引用这个文件。合并前端源码后推荐重新构建并使用 A 的部署脚本替换商品静态目录。选择直接接入预构建资源时，先备份，再整体替换 `static/products/`；不要只换 JS 而保留旧 index，也不要把旧 `main-WNMTM7YH.js` 当作本次构建结果。

## 共同工程中保留的内容

A 的商品实体、查询与分页接口、页面控制器、应用配置、Angular 主体及锁文件；B 的登录、会话、角色、账户、代理及演示配置；D 的 11 个核心 Java 文件；E 的历史订单与管理员商品维护均保留。D 核心文件已与此次发来的 D 包逐项比对，内容一致。

`C接入文件` 不含 D 的 CheckoutController、订单实体或订单事务服务。完整的 `联调工程` 含这些队友文件以便运行，共同作者信息保留。没有新增库存、地址、支付、数据库迁移或第二套订单实现。

合并步骤和接口示例见 [C 模块接入说明](C模块接入说明.md)；验证记录见 [测试结果](../验证/测试结果.md)。
