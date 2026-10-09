# Group 2 整合说明

负责人：李岸 Li An。老师 PDF 是功能、技术及提交要求的依据，六人分工 Word 仅用于参考模块归属。Word 所述书店、React、工时与日期规划不作为额外硬性要求；PDF 允许 Angular，现有通用商品商城符合题目范围。

## 合并基底与来源核对

使用 CA_C_适配新版A 中的联调工程作为基底。逐文件比较结果：D 独立交付 11 个 Java 核心文件与基底完全相同；E 独立包中所有 src 文件与基底完全相同。B 的身份与权限实现已在基底保留，C 在其上增加 Angular 加购与购物车衔接。A 与基底的差异主要是 B 的账号适配及 B/C 的前端接入，不能将原始 A 再整包覆盖。

新提供的 CA_D_适配新版A (2).zip 包含 A/B/C/D/E 及作业文档，并非仅 D 模块。排除依赖、编译输出等生成目录后，其所有文件与原目录一致。整合没有重复放置启动类、控制器或订单实体。

## F 本次变更

1. 将联调工程复制到独立交付目录，保留原始五个文件夹。
2. 前端 deploy.mjs 的默认备份位置从开发者 D 盘改为项目内 .deploy-backups，保留环境变量覆盖。
3. 数据库 URL 支持 DB_URL，默认仍为 MySQL shopping_cart；用户名和密码仍由环境变量提供。
4. Maven Wrapper 固定为已实际验证的 3.9.9。
5. 增补 .gitignore 的依赖、缓存、备份与日志规则。
6. 为三个未标注作者的共享基础类补充 Group 2 原始工程与 F 整合职责说明，未改变运行逻辑。原始个人归属交组员确认。
7. 从 Angular 源码重新 check、test、build、deploy，再构建后端可运行 JAR。
8. 补充 MySQL 管理员设置方法、运行说明、验收结果、录像提纲及提交检查。

没有另写或覆盖 A–E 的业务实现，没有将 A–E 原测试记为 F 独立编写。F 新增的 MySQL HTTP 检查属于整合验收。

## 需求对应

| PDF 要求 | 工程实现与证据 |
| --- | --- |
| 浏览商品 | Angular 列表、详情、分页，ProductRestController / ProductService |
| 登录退出 | AuthController / AuthService，数据库验证与 Session 销毁 |
| 加入购物车 | CartService、CartController、Angular 加购组件 |
| 结账 | CheckoutController、Coordinator、独立事务写订单与明细 |
| 购买历史 | PurchaseHistoryController / Service，当前用户归属过滤 |
| Spring Boot MVC 与 URL 映射 | 单一 ShoppingCartApplication，控制器明确路由 |
| MySQL 与 Spring Data JPA | 正式 MySQL 默认配置、实体与 Repository；全新 MySQL 实测 |
| 至少一项 REST + Angular/React | Angular 商品分页、详情调用商品 REST |
| 服务端输入校验 | 表单 Bean Validation、Service 业务规则、REST 分页参数限制 |
| 异常处理与清晰结构 | 公共异常类、Advice、controller/service/repository/entity/dto 分层 |
| 可选加分 | Service Layer、分页、注册和资料修改、商品管理后台 |
| 作者标注 | 原始注释保留，共享基础类补说明，需团队确认真实个人归属 |
| 一个整合 ZIP 与视频 | 整合代码与 JAR 已准备；录像待团队录制并加入 |

模拟支付、评价评分、库存管理不是老师五项核心功能的必需项。此次不扩展这些功能。
