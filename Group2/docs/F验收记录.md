# Group 2 整合验收记录

> 原交付记录保留供追溯。后续本机复查修复了分页和表单问题，当前测试与运行步骤请看 [本机检查与运行指南](本机检查与运行指南.md)。下列 MySQL 验收不是后续修正版在此电脑上的验证结果。

验收负责人：李岸 Li An。执行日期：2026 年 10 月 9 日（北京时间）。本记录区分原有自动化测试与 F 的实际整合检查，不将其他成员的测试归为 F 编写。

## 环境与范围

Spring Boot 4.1.1，Maven 3.9.9，JDK 25.0.3（release 17）；Angular 22.2.1，Node 24.19.0。正式配置在工作区内全新独立 MySQL 8.0.46、端口 13306 验证，未操作原有数据库。应用使用重新构建并部署的 Angular 静态资源及新打包 JAR，HTTP 测试端口为 18085。测试专用空密码 root 仅用于本次隔离的临时库，不是交付运行配置。

## 后端自动化

146 项全部通过，失败 0、错误 0、跳过 0。测试使用隔离 H2，包括订单事务回滚、失败保留购物车、重新核价、重复提交、账户和权限、服务与页面检查。

| 测试类 | 数量 | 结果 |
| --- | --- | --- |
| AdminProductServiceTest | 12 | PASS |
| BExistingSchemaTest | 2 | PASS |
| BIntegratedFlowTest | 49 | PASS |
| BLoginRedirectTest | 20 | PASS |
| CAdaptationIntegrationTest | 22 | PASS |
| CartModuleTest | 8 | PASS |
| DataInitializerTests | 4 | PASS |
| ModuleEWebTest | 19 | PASS |
| PurchaseHistoryServiceTest | 9 | PASS |
| ShoppingCartApplicationTests | 1 | PASS |

## 前端与构建

- npm ci 按 package-lock.json 安装依赖。
- npm run check 通过。
- 前端 5 个测试文件、44 项测试全部通过。
- npm run build 生产构建成功，初始资源合计约 180.06 kB。
- deploy 从生产构建生成 static/products，备份位置已改为项目内目录。
- 后端 package 成功生成可执行 JAR，包含当前 Angular 页面。

受限执行环境首次遇到 Mockito 动态挂载限制和 Angular 临时文件读取错误。重跑时将 TEMP/TMP（Java 同时设置 java.io.tmpdir）指向工作区；后端测试使用 -javaagent 预加载实际依赖 Mockito 5.23.0。调整后全部通过，没有跳过或删掉失败测试，也没有为测试改动业务逻辑。上述参数是当前执行环境适配，未硬编码进交付项目。

## MySQL 实际流程

HTTP 访问实际运行的 JAR，同时查询真实 MySQL 确认结果，不只检查页面状态码。

| 检查 | 结果 |
| --- | --- |
| Angular生产页面及资源可访问 | PASS |
| REST分页读取MySQL商品 | PASS |
| 匿名购物车API拒绝访问 | PASS |
| 错误密码登录失败 | PASS |
| 数据库账号登录成功 | PASS |
| 商品加购数量正确 | PASS |
| 非法数量被拒且保留原购物车 | PASS |
| 结账成功并显示回执 | PASS |
| 按最新价格生成订单与成交快照 | PASS |
| 结账后清空购物车 | PASS |
| 重复提交仅一笔订单且保留后来新增购物项 | PASS |
| 本人购买历史和详情正常 | PASS |
| 不同用户购物车隔离 | PASS |
| 其他用户不能读取订单 | PASS |
| 普通用户无法进入后台 | PASS |
| 注册不能自行指定管理员角色 | PASS |
| 管理员列表和新增页正常 | PASS |
| 新增商品出现在REST结果 | PASS |
| 后台非法输入服务端拒绝 | PASS |
| 下架商品不再从REST出售 | PASS |
| 下架商品不能加入购物车 | PASS |
| 改价和下架不影响历史成交金额 | PASS |
| 被订单引用的商品不能物理删除 | PASS |
| 退出后API权限失效 | PASS |
| 再次登录购物车为空 | PASS |

应用第二次启动连接同一数据库后，商品/用户/订单数量仍为 13/3/1，与第一次流程验收结束时一致；没有重复初始化数据，也未覆盖管理员编辑或订单。这 13 件商品包含验收中管理员新建的 1 件，3 个账号包含验收中注册的专用管理员。

## 压缩包解压启动验收

对 Group2.zip 完成 CRC 与逐文件 SHA256 核对，确认无依赖缓存或本机数据库。解压后运行包内 runtime/shopping-cart.jar，以 DB_URL 环境变量连接同一独立 MySQL：Angular 页面、生产 JS 和 REST 商品读取均正常，数据库数量仍为 13/3/1。已停止本次测试应用与独立 MySQL。

## 尚需团队完成

录制并加入演示视频，组员确认作者信息，在实际演示电脑使用自己的 MySQL 凭据复核。自动化和 HTTP 检查不能替代全员现场代码讲解，也不代表已录制视频。本次没有评估真实支付、库存、商品评论等未实现的可选功能。
