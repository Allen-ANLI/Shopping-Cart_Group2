# CA_E 模块交付说明

负责人：蔡千一。CA_E 提供用户订单历史页和管理员商品管理后台，代码基于 `CA_C/联调工程` 中已经接入的 E 模块实现整理。订单历史只按服务器 Session 中的当前用户 ID 查询；管理员后台由 B 的身份与角色拦截器保护。商品编辑保留订单明细里的成交快照，已被订单引用的商品只能下架，不能物理删除。

## 功能与入口

| 功能 | 地址 | 行为 |
| --- | --- | --- |
| 订单历史 | `GET /orders` | 按下单时间倒序分页，每页 5 笔，显示订单数和累计金额 |
| 订单详情 | `GET /orders/{id}` | 查询当前用户自己的订单及成交明细 |
| 后台商品列表 | `GET /admin/products` | 展示全部商品（包括下架），每页 10 项 |
| 新增商品 | `GET/POST /admin/products/new`、`POST /admin/products` | 使用图片 URL；名称、价格和字段长度在服务端校验 |
| 编辑商品 | `GET/POST /admin/products/{id}/edit`、`POST /admin/products/{id}` | 可修改商品资料、价格和上下架状态 |
| 上下架 | `POST /admin/products/{id}/toggle` | 切换商品可售状态 |
| 删除未引用商品 | `POST /admin/products/{id}/delete` | 只有没有订单明细引用的商品可删除 |

## 接入方式

将本目录 `src` 合并到包含 A/B/C/D 的 Spring Boot 工程根目录（`pom.xml` 同级），不要把 CA_E 的 `src` 复制到已有 `src` 里面。模板依赖整合工程现有的 `templates/fragments.html` 和公共 CSS。

E 与 D 共用 `Order`、`OrderItem` 实体，不修改 D 的实体。E 使用独立的 `OrderQueryRepository`、`OrderItemQueryRepository` 读取历史数据，D 保留自己的 `OrderRepository` 写入结账数据。E 的 `AdminProductQueryRepository` 读取 A 的 `Product` 实体；不新增商品表或第二套实体。

B/F 需在统一 Web 配置中确保登录拦截器保护 `/orders/**`、`/admin/**`，管理员拦截器继续保护 `/admin/**`，并在导航中提供订单历史和管理员后台入口。CA_C 联调工程已包含相应的 `WebConfig`、`AdminAccessInterceptor` 及导航接入；这些是与 B 合并后的共享文件，不在本包重复覆盖。身份字段遵循 B 的 `loginUserId`（`Long`）和 `role` 约定。

正式数据库沿用项目 `shopping_cart` 和 JPA `ddl-auto=update` 配置，不包含凭据或独立数据库脚本。请与 F 一起核对目标工程的实体映射和初始化配置。

## 文件归属

- `controller/`：订单历史及管理员商品页面路由。
- `service/`：历史查询、归属校验、商品增改/上下架/受限删除。
- `repository/`：E 专用的只读查询接口，与 D 的写入仓库并存。
- `dto/`：历史列表、详情数据和后台表单。
- `templates/`：订单历史/详情页及管理员商品列表/表单。
- `src/test/`：E 的服务与 Web/权限覆盖，使用整合工程的 H2 测试配置。

作者注释保留蔡千一（Module E）。
