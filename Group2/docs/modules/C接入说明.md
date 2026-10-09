# C 模块接入说明

C 负责人：Letian Xie。新版商品前端是 Angular，后端沿用 `sg.edu.nus.iss.shoppingcart` 工程。这里说明商品详情怎样使用已有 Session 购物车，以及与 B 身份、D 事务和 F 构建的衔接。A、B、D、E 的代码按原作者保留，不计入 C 的独立成果。

## 先确定合并基底

`联调工程` 来自 B 的整合工程，已经包含 A 的商品、B 的身份、C 的 Session 购物车、D 的订单以及 E 的历史和后台。C 在这个基底上新增 Angular 加购组件、只读表单接口，并调整购物车与指定商品入口的链接。

已有组内项目时，将 `C接入文件/src` 与 `C接入文件/frontend` 按同一目录结构逐项合并，具体文件见[C文件变更清单](C文件变更清单.md)。若项目已经含同名 CartService、CartController 或 DTO，应比较并更新原文件，不再放一套重复类。共用的 BusinessException、NotAuthenticatedException 也只保留一个实现。

**不要复制旧 CA 的 `com.nusiss.ca` 源码、启动类、pom、schema 或订单代码。** 新版使用 A 的实体、B 的 Long 身份和 D 的 Map 输入，与旧 CA 的用户对象、地址和库存方案不同。也不要为 C 覆盖 B 的 AuthController、AuthService、User、UserRepository、WebConfig、LoginInterceptor，或覆盖 D 的订单核心类。

以下 Java 路径从 `src/main/java/sg/edu/nus/iss/shoppingcart/` 开始，模板与资源从 `src/main/resources/` 开始。

## C 的文件与职责

| 文件 | 接入时查看的内容 |
| --- | --- |
| `controller/CartController.java` | 页面和增改删清表单；写操作与令牌检查放在同一 Session 锁内 |
| `controller/CartPageAdvice.java` | C 页面模型中的 `cartFormToken`，身份与业务错误处理 |
| `controller/CartProductPageController.java` | `/cart/products` 普通表单入口；可带 productId，仅显示所选上架商品 |
| `controller/CartFormController.java` | 新增 `/api/cart/form`，为 Angular 取得令牌和数量 |
| `dto/CartFormState.java` | JSON 响应的令牌、种类数、总件数 |
| `dto/CartLine.java` | 请求期间的商品名称、价格、小计和不可购买原因 |
| `dto/CartQuantityForm.java`、`CartProductForm.java` | 商品 ID、整数数量的服务器校验 |
| `interceptor/CartSessionIdentity.java` | 从 B 的会话读取 Long 身份，拒绝客户端身份参数 |
| `model/SessionCart.java` | 归属用户、商品 ID/数量、版本，不存商品实体或价格 |
| `service/CartService.java` | 会话存取、合并、金额、可售检查、令牌、供 D 读取和清空 |
| `templates/cart/` | 明细、空车、异常提示、指定商品加购和公共片段 |
| `static/cart-assets/` | 购物车与结账布局、提交状态脚本 |

`templates/orders/checkout.html` 与 `checkout-success.html` 按 D 的模型使用。成功页取 `receipt`，而不是旧 CA 的 `order`，见下方 D 契约。有关 C 的测试随接入文件提供；保留组内已有测试，不用旧 CA 测试替换新版实体和接口。

增量文件需要完整的 A/B/D 工程：商品实体和查询来自 A；登录拦截、AuthModelAdvice 以及片段引用的 `auth-assets/navigation.css` 来自 B；结账控制器、回执和事务来自 D。H2 依赖及测试配置沿用 B 的整合工程。因此不能只复制 C 目录到空项目运行，也不要为了缺少这些依赖再写一份身份或订单实现。

## A：Angular 商品详情与表单接口

### 前端改动位置

新增目录是 `frontend/src/app/add-to-cart/`，包含组件、HTML、CSS 和测试。商品详情的 `product-detail.component.ts` 导入该组件，HTML 在描述下加入：

```html
<app-add-to-cart [productId]="item.id" />
```

A 的商品请求、加载状态、描述、价格、图片和返回列表逻辑继续保留。详情相关测试需处理新增的令牌请求；根组件详情测试也补了该请求响应，保留 A/B 原测试与作者信息。不要复制整个前端覆盖 A 的其他更新，先合并这小块接入和新组件。

### 只读接口

```text
GET /api/cart/form
```

已登录返回 200 JSON，例如：

```json
{"cartFormToken":"服务器生成的会话令牌","itemCount":1,"totalQuantity":3}
```

`itemCount` 是商品种类数，`totalQuantity` 是所有数量相加的件数。接口不接受 productId、用户 ID 或价格，不改变购物项，不写订单；首次需要时会准备会话中的空购物车和令牌。响应包含 `Cache-Control: no-store`，200 响应还按 Cookie 区分。它没有公开用户资料、密码或哈希。

未登录、会话失效或身份被 B 拦截时，返回 401 JSON：

```json
{"error":"LOGIN_REQUIRED","message":"Please log in to continue"}
```

401 也禁止缓存。不要把此接口改成重定向登录 HTML，Angular 依赖 HTTP 401 判断登录状态。

### 加购与错误交互

取得令牌后，组件显示原生表单：

```text
POST /cart/add
Content-Type: application/x-www-form-urlencoded
productId=商品ID
quantity=1到99的整数
cartFormToken=刚取得的令牌
```

写请求沿用 HTML 表单和 302 重定向，成功与业务错误均返回购物车，由页面消息表示结果。因此组件允许浏览器真实提交并导航，不通过 HttpClient 发送加购，不把 HTML 读取为 JSON。提交中只禁用按钮，隐藏字段与数量仍可提交；不要禁用数量输入，否则浏览器会漏传该字段。

接口 401 时显示 `Log in to add to cart`，链接 `/cart/products?productId=ID`。这是受保护 GET 入口，B 会保存安全的目标地址，登录后回到所选商品的数量表单。没有自动加购或重放原 POST，也没有自行增加 B 的 redirect 参数。其他请求失败显示提示和 Try again；销毁、切换商品、重试时取消旧请求。浏览器从缓存返回时重新获取令牌，避免继续显示旧账号的加购状态。

### 商品规则与 A 的协作

C 依赖 A 的 `id/name/price/active`，金额使用 BigDecimal。展示通过 `findById` 查到原购物项，购买校验通过 `findByIdAndActiveTrue`；`ProductService.findProductById` 也只返回上架商品。重复加购校验合并后的 1–99；更新为 0 会移除，下架或删除的项目仍可移除。

数据库改价会影响之后显示的购物车金额，提交时 D 再核价；成交快照不随以后改价变化。新版没有 stockQuantity，不存在预扣库存、库存不足或抢最后一件的流程。A 调整实体或商品查询时，请同步 C 的展示与可售查询用途。

## B：Long 身份、退出锁与开发代理

身份只使用 `session.getAttribute("loginUserId")` 的正数 Long。`CartSessionIdentity.currentUserId(HttpSession)` 在会话失效时返回未登录。不要写入 String/Integer 用户 ID，也不要恢复旧 CA 的 `sessionUser`、`isLoggedIn` 会话属性。页面仍使用 B 的 AuthModelAdvice 提供的 `isLoggedIn`、`isAdmin` 模型，不需要删除这些模板属性。B 已通过数据库和密码哈希校验后建立新 Session，角色是 `CUSTOMER` 或 `ADMIN`；前端与请求参数不能指定身份或授予管理员。

C 使用的状态键为 `cart`（SessionCart）、`cartFormToken`（String）、`checkoutState`（D 协调器内部状态）。登录切换会销毁旧会话，退出也 invalidate，连同购物车、令牌和回执一起清除。B 的登录/退出与 C 增改操作、D 结账共用同一个 Session 同步锁，合并时保留锁边界。退出不会撤销已经提交的订单；若先退出，旧请求应回登录或在 API 中得到 401。

B 的 `/api/auth/session` 负责 Angular 账户导航，返回安全的用户摘要；C 的 `/api/cart/form` 只负责加购令牌，二者各自保留。无需给身份接口追加购物车字段，也无需改 B 的导航组件。`WebConfig` 已保护 `/api/cart/**` 与 C 页面，匿名 API 返回 JSON，页面请求重定向登录；只保存受保护 GET 目标，不重放加购或结账 POST。

开发时继续使用 B 的 `frontend/proxy.conf.json`，4200 同源代理已经覆盖 `/api/**`、`/cart/**`、登录、结账、订单及相关资源，默认目标 18080。同源 HttpClient 与原生表单会带浏览器 Session Cookie，无需前端存储凭据。统一用一个主机名访问，不混用 localhost 和 127.0.0.1。跨源独立访问的 CORS、cookie 方案不在这套接入中；优先保留现有开发代理。

B/C 联调检查：两账号购物车隔离；换账号旧令牌失效；退出后 API 401；登录后回选中商品；普通用户导航不显示后台且服务端仍拒绝后台访问。角色展示采用 B 的模型，模板不能再引用旧 CA 的完整 User 字段。

## D：Map 输入、事务和 receipt

当前接口为：

```java
Map<Long, Integer> snapshot = cartService.readForCheckout(session);
Order checkout(Long userId, Map<Long, Integer> cart, String checkoutToken);
```

快照不提供客户端价格或用户身份，且外部修改返回 Map 不会修改 Session 内部购物车。D 的 CheckoutCoordinator 绑定购物车版本，调用 `CheckoutService.checkout()`，成功后 `clearCart(session)`；失败不会清空。不要改回旧 CA 的 CheckoutItem 列表、带地址的四参数方法或 ShopOrder 实体。

D 的外层 CheckoutService 使用 `NOT_SUPPORTED`，通过独立 CheckoutTransactionService 的事务重新查询上架商品、核价并保存订单与明细。事务失败回滚，数据库请求唯一约束阻止重复订单；约束冲突后外层在失败事务结束后查询本人已完成订单。同 Session 的协调器另有同步锁。这里没有旧 CA 的用户行锁或商品库存锁，不要在文档或代码里沿用旧描述。

事务成功返回后才清空 Session；Session 改动不随数据库事务回滚。请勿把清空提前到保存过程。若修改 D 的事务传播方式或将订单写入合并到别的事务，应重新确认真正提交后的清空时机；当前保留外层 NOT_SUPPORTED 与独立写入事务。按钮禁用负责体验，后端请求唯一性由 D 保证。

路由由 D 的 CheckoutController 唯一维护：

| 路由 | 参数 / 模型 |
| --- | --- |
| `GET /checkout` | `cartItems`、`cartTotal`、`totalQuantity`、`canCheckout`、`checkoutToken` |
| `POST /checkout` | 仅 `checkoutToken`，成功 302 到成功页，失败回显确认页 |
| `GET /checkout/success?key=...` | 确认当前 Session 回执，再以 token 和当前用户查订单，模型名 `receipt` |

`receipt` 包含 `id/createdAt/totalAmount/items`；明细字段为 `productName/unitPrice/quantity/subtotal`。模板按成交快照展示，不直接遍历旧 JPA order 对象。Session 保留最近 5 个完成标识，重复成功请求回同一订单并保留后来新加的购物项。退出、换会话或回执淘汰后，通过 E 的订单历史查看长期数据。

联调工程中以下 11 个 D 核心文件与组内 D 版本对齐，不要再放第二个 Bean 或另写同一路由：CheckoutController、CheckoutPageAdvice、CheckoutReceipt、Order、OrderItem、OrderRepository、OrderItemRepository、CheckoutCoordinator、CheckoutReceiptService、CheckoutService、CheckoutTransactionService。它们分别位于 controller、dto、entity、repository、service。C 的模板和脚本与这些契约配合，D 原作者邱弈杰保留。

新版没有收货地址或支付状态。确认页不提交 shippingAddress，成功页不显示地址或支付按钮。C/D 联调应检查空车、下架、改价、失败保留、重复请求、事务回滚和回执归属，不能用旧 CA 的库存、地址、模拟支付测试代替。

## F：合并、构建和交付

顺序建议为：确认新版 A 的实体与依赖 → 保留 B 的唯一身份/权限实现 → 对齐 D 的 11 个核心类 → 合并 C 的新接口和前端小块 → 核对 E 历史与后台 → 完成全流程检查。所有模块都只保留一个 Spring Boot 启动类。公共模板、异常类、仓库和样式按实际差异处理，不以整份覆盖消除冲突。

默认后端是 Spring Boot 4.1.1、Java 17、MySQL/18080，以 DB_USERNAME/DB_PASSWORD 读取本机凭据。H2 用于隔离演示和测试，使用 B 的 b-demo 并在启动命令覆盖为 18084；不改正式配置、不执行旧 CA schema。只有演示管理员受 b-demo 限定，A 的 DataInitializer 在所有配置下都会补充缺失的 alice/bob，已有账号不重置，应按团队实际库核对。

C 的令牌接口和 Angular 适配没有新增数据库表或列。订单的 checkout_token 唯一约束来自 D，由 D/F 维护；C 不带旧 CA 的 checkout_request_key 迁移。

Angular 22.2.1 的 Node 范围为 22.x 从 22.22.3 起、24.x 从 24.15.0 起，或 26 以上。按锁文件 `npm ci`；修改完成后 check、test、build，再以本机可写的 SHOPPING_CART_BACKUP_DIR 运行 deploy。脚本默认 Windows 路径不适合直接用于其他电脑。重新部署 `static/products` 后再构建 JAR，避免源码已变而页面仍加载旧 bundle。

实际命令和结果见[测试结果](../验证/测试结果.md)。附带的联调项目含其他成员测试，其总数不能算作 C 独立编写。正式 MySQL、新电脑和最终团队分支另行验收，作者信息按真实贡献核对：A 王重一、B luopeiwen、C Letian Xie、D 邱弈杰、E 蔡千一。

## 出现问题时先看哪里

| 现象 | 先检查 |
| --- | --- |
| 详情没有新加购控件 | 是否部署了新版 Angular 静态目录，JAR 是否重新构建 |
| 令牌接口拿到 HTML | 是否误改为登录重定向、请求地址/代理是否正确；API 应为 200 或 401 JSON |
| 登录后仍要求登录 | 同源 cookie、主机名、代理目标端口和 Long 身份类型 |
| 提示表单过期 | 是否退出、换账号或恢复了旧页；刷新取得当前令牌 |
| 数量绑定失败 | 表单是否有 quantity，是否被设为 disabled，值是否为 1–99 整数 |
| 同名 Bean / 路由冲突 | 是否复制了旧命名空间、两套身份类或第二套结账控制器 |
| 成功页找不到 order 属性 | 新模板使用 receipt，与 D 的 DTO 字段对齐 |
| 4200 页面接口失败 | 代理默认连 18080，演示实际在 18084 时需本机调整目标 |
| 订单已经保存但旧 key 不可看 | 回执是否淘汰或会话变化；到 E 的订单历史查记录 |

购物车 POST 的 302 表示回到页面，不能单凭重定向判断操作成功，需看购物车提示及状态。结账失败时查看服务端日志与实际订单数据；不要把页面 200 当作事务成功。
