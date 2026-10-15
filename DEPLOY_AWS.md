# 零成本 AWS 部署手册（AWS Academy Learner Lab 专用）

> 目标读者：照着抄就行，不需要懂 AWS。
> 你的额度：$50（AWS Academy 学习实验室）。本方案每月约花 **$20~25**，额度够用约 2 个月；
> 学会"用完就关"（见第 7 节）后，几乎可以无限期免费。

## 最终架构

```
开发者 git push
   ↓
GitHub Actions（自动：跑测试 → 打 Docker 镜像 → 推到 Docker Hub）
   ↓
你在 AWS 控制台点一下「强制重新部署」
   ↓
AWS ECS Fargate 拉取最新镜像，运行 Spring Boot（公网 IP 直接访问）
   ↓
AWS RDS MySQL（数据库）
```

**为什么和标准架构不一样、没有完全自动部署到 ECS？**
AWS Academy 实验室账号**禁止创建 IAM 用户和 OIDC Provider**（写死在权限里），所以 GitHub Actions
拿不到长期有效的 AWS 密钥，无法直接操作 ECS。这是所有 Learner Lab 用户的共同限制。
因此最后一步改成手动点一次「强制重新部署」，只需 10 秒，纯鼠标点击。

**全程只用这些免费/已付费的东西**：GitHub（免费）、GitHub Actions 公共仓库免费额度（免费）、
Docker Hub（免费）、AWS 的 $50 额度。**你自己一分钱都不用出。**

---

## 全局规则（开始前必读）

1. **区域必须是 US East (N. Virginia) `us-east-1`**（或 us-west-2）。Learner Lab 只支持这两个区域，
   页面右上角先确认区域再动手，中途不要切换。
2. 每次开始操作前，先回到 Canvas 课程页面点 **Start Lab**，绿色圆点亮了才能进 AWS 控制台。
   每次会话最长 4 小时，超时后重新 Start Lab 即可，**已创建的资源不会丢**。
3. 控制台顶部如果出现橙色横幅，忽略即可（不影响使用）。
4. 遇到任何服务页面显示 "Access Denied / not authorized"，说明 Learner Lab 不开放该功能，换用本手册的方法。

---

## 第 1 步：配置 GitHub 和 Docker Hub（一次性，10 分钟）

### 1.1 注册 Docker Hub 并创建镜像仓库

1. 打开 https://hub.docker.com → Sign Up 注册（记下用户名，下面叫 `<你的DockerHub用户名>`）。
2. 登录后 → Repositories → **Create repository**：
   - Name：`shopping-cart`
   - Visibility：**Public**（必须选 Public，ECS 才能免密拉取）
   - 点 Create。

### 1.2 创建 Docker Hub 访问令牌

1. Docker Hub 右上角头像 → **Account Settings** → **Security** → **New Access Token**。
2. Description 填 `github-actions`，权限选 **Read & Write** → Generate。
3. **立刻复制**弹出的令牌（只显示一次），下面叫 `<你的DockerHub令牌>`。

### 1.3 把令牌存进 GitHub 仓库

1. 打开你的 GitHub 仓库 https://github.com/Allen-ANLI/Shopping-Cart_Group2
2. Settings → Secrets and variables → **Actions** → **New repository secret**，添加两条：
   - Name：`DOCKERHUB_USERNAME`，Secret：`<你的DockerHub用户名>`
   - Name：`DOCKERHUB_TOKEN`，Secret：`<你的DockerHub令牌>`

### 1.4 验证流水线

本仓库已包含 `.github/workflows/ci.yml` 和 `Group2/Dockerfile`。把代码 push 到 `main` 分支：

```powershell
git add .
git commit -m "add docker and ci"
git push
```

然后到 GitHub 仓库页 → **Actions** 标签，能看到 `CI-CD` 在跑，全绿后到 Docker Hub
仓库页面能看到 `latest` 标签的镜像。**这一步成功 = 架构的左半边（GitHub Actions → Docker Image）已经通了。**

> 注意：workflow 文件必须在仓库根目录（`.github/workflows/`），本项目仓库根是
> `CA_Web Application/Group2` 这一层，我已经放好了，直接 push 即可。

---

## 第 2 步：创建 RDS MySQL 数据库（15 分钟）

1. 进入 AWS 控制台（确认右上角区域是 **N. Virginia us-east-1**）。
2. 顶部搜索框输入 `RDS` → 进入 RDS → 左侧 **Databases** → **Create database**。
3. 按下面填（没提到的保持默认）：

| 项目 | 选择/填写 |
|---|---|
| Choose a database creation method | Standard create |
| Engine type | MySQL |
| Edition | MySQL Community |
| Version | 默认（8.0.x 即可） |
| Templates | **Free tier**（如果出现这个选项就选它） |
| DB instance identifier | `shopping-cart-db` |
| Master username | `admin` |
| Credentials management | Self managed（选这个，别选 Secrets Manager） |
| Master password | 自己设一个强密码，**记下来**（例：`DbP@ss12345678`） |
| Instance configuration | Burstable classes → **db.t3.micro**（没有就 db.t4g.micro） |
| Storage type | General Purpose SSD (gp3) |
| Allocated storage | `20` |
| **Enable storage autoscaling** | **取消勾选**（重要，否则可能自动扩容花钱） |
| Compute / Availability & durability | Don't create a standby instance（单实例，默认） |
| Connectivity → VPC | 默认（default VPC） |
| Public access | **Yes**（方便你本地连数据库做管理） |
| VPC security group | 选 Create new，名字默认 |
| Database authentication | Password authentication |
| Initial database name | `shopping_cart`（这样就不需要执行 create-database.sql 了） |
| Backup retention period | `1 day` |
| 其余（加密、日志导出、维护等） | 默认 |

4. 点 **Create database**，等状态从 Creating 变成 **Available**（约 5~10 分钟）。
5. 点进这个数据库，找到 **Endpoint**（形如 `shopping-cart-db.xxxxxx.us-east-1.rds.amazonaws.com`），复制保存。
6. 数据库详情页 → **Configuration** 标签 → 记下 Security group 名字（形如 `rds-launch-wizard-x`）。

### 2.1 放开数据库的访问端口（一次性）

1. 顶部搜索 `EC2` → 左侧 **Security Groups** → 点上面记下的安全组。
2. **Edit inbound rules** → **Add rule**：
   - Type：`MySQL/Aurora`，Port 3306，Source：`Anywhere-IPv4 (0.0.0.0/0)`
3. Save rules。
   （学校课程演示这样即可；正式生产应只允许你的 IP 和 ECS 的安全组。）

### 2.2 建管理员账号

1. 用 IDEA 右侧 Database 面板（或 MySQL Workbench）新建 MySQL 连接：
   Host = 第 5 步复制的 Endpoint，Port = 3306，用户 `admin`，密码是你设的。
2. 连上后执行：

```sql
UPDATE shopping_cart.app_users SET role = 'ADMIN' WHERE username = '你的用户名';
```

（先在网站上注册这个用户，再执行这条 SQL 把它升级为管理员。JPA 会自动建表，不用手动跑其他 SQL。）

---

## 第 3 步：创建 ECS 并上线网站（20 分钟）

### 3.1 创建集群

1. 顶部搜索 `ECS` → Amazon ECS → 左侧 **Clusters** → **Create cluster**。
2. Infrastructure 选 **AWS Fargate (serverless)**，Cluster name：`shopping-cart-cluster`，其余默认 → Create。

### 3.2 创建任务定义（告诉 ECS 怎么运行你的程序）

1. 左侧 **Task definitions** → **Create new task definition** → **Create new task definition with JSON**（直接粘贴 JSON 最不容易错）。
2. 把下面 JSON 整体粘贴进去，**先改 3 个地方**：
   - `你的DockerHub用户名`（出现 1 处）
   - `你的RDS端点`（出现 1 处）
   - `你的数据库密码`（出现 1 处）
3. 粘贴后点 Create：

```json
{
  "family": "shopping-cart",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::000000000000:role/LabRole",
  "taskRoleArn": "arn:aws:iam::000000000000:role/LabRole",
  "containerDefinitions": [
    {
      "name": "web",
      "image": "docker.io/你的DockerHub用户名/shopping-cart:latest",
      "portMappings": [{ "containerPort": 18080, "protocol": "tcp" }],
      "environment": [
        { "name": "DB_URL", "value": "jdbc:mysql://你的RDS端点:3306/shopping_cart" },
        { "name": "DB_USERNAME", "value": "admin" },
        { "name": "DB_PASSWORD", "value": "你的数据库密码" }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/shopping-cart",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "web"
        }
      }
    }
  ]
}
```

> `LabRole` 里的 `000000000000` 不用改成真实账号 ID —— ECS 会接受 `LabRole` 这个名字的 ARN（账号 ID 无关紧要）。
> 如果创建时报错说 role 不存在，去 IAM 控制台搜 `LabRole`，复制它完整的 ARN 填进来。

### 3.3 创建服务（让它一直跑着）

1. 回到 Cluster `shopping-cart-cluster` → **Services** 标签 → **Create**。
2. 按下面填（没提到的保持默认）：

| 项目 | 选择/填写 |
|---|---|
| Task definition family | `shopping-cart`（刚才建的） |
| Launch type | Fargate |
| Desired tasks | `1` |
| VPC / Subnets | 默认（default VPC） |
| Security group | Create a new security group，名字 `shopping-cart-sg`，**把自动创建的 inbound 规则删掉，然后手动加一条：Custom TCP，Port 18080，Source `Anywhere-IPv4 0.0.0.0/0`** |
| Public IP | **Turned on**（默认就是开着，确认一下） |
| Load balancing | **None**（省钱的关键：负载均衡器一个月要 $16+，不要开） |
| Service auto scaling | 关闭 |
| Deployment options | Rolling update；**Deployment circuit breaker 勾选 Enable + 勾选 Rollback**（部署失败自动回滚，不背锅） |

3. 点 Create，等服务状态变成 **Running**（约 3~5 分钟；第一次要拉镜像，慢一点正常）。

### 3.4 打开你的网站 🎉

1. Cluster → Services → 点 `shopping-cart-service`（或你起的名字）→ **Tasks** 标签 → 点那个 Task。
2. Task 详情页往下找 **Public IP**（形如 `3.87.xxx.xxx`），复制。
3. 浏览器打开：

```
http://那个IP:18080/products
```

看到你的购物车网站就成功了！把这个 IP 发给同学就能访问。

> **注意：每次重新部署，公网 IP 都会变**（这是不开负载均衡器的代价，省了 $16/月）。重新按 1~3 找新 IP 即可。
> 想要固定网址可以后加负载均衡器，但会烧钱，课程演示不建议。

---

## 第 4 步：日常更新（每次改完代码后）

1. 正常提交并 push 到 `main`：

```powershell
git add .
git commit -m "更新说明"
git push
```

2. 去 GitHub → Actions，等 `CI-CD` 全绿（约 5~10 分钟）。这一步自动完成了：测试 + 打镜像 + 推到 Docker Hub。
3. AWS 控制台 → ECS → Cluster → Service → **Update service** → 勾选 **Force new deployment** → 点 Update。
4. 等 2~3 分钟，到 Task 里找新的 Public IP，访问 `http://新IP:18080/products`。

整个过程你只做了两个动作：`git push` 和点一次「强制重新部署」。这就是你的 CI/CD。

---

## 第 5 步（强烈推荐）：省钱开关 —— 用完就关

网站是按小时计费的。不上课、不演示、不开发时把它关掉，账单几乎归零：

**关闭网站（ECS 算力停止计费，免费）：**
1. ECS → Cluster → Service → **Update service** → Desired tasks 改成 `0` → Update。
2. 要用的时候同样路径改回 `1`。

**关闭数据库（RDS）：**
- RDS → Databases → 选数据库 → **Actions → Stop temporarily**（可免费停最多 7 天，之后会自动重启）。
- 要用就 Actions → Start。

| 状态 | 每天花费 |
|---|---|
| 全天开着 | ≈ $0.80/天 |
| 只在演示时开 2 小时 | ≈ $0.07/天 |

> 停止网站/数据库期间，你本地的 `localhost` 开发完全不受影响。

---

## 第 6 步：课程结束时的彻底清理（避免额度被扣光）

按顺序删除（漏删 RDS 会一直扣钱）：

1. ECS → Cluster → Service → 删除服务（Update service 无法删除时，先把 Desired tasks 改为 0，等 Tasks 清空再删）。
2. ECS → Task definitions → 选 `shopping-cart` → Actions → Deregister 所有版本。
3. ECS → Clusters → 删除 `shopping-cart-cluster`。
4. RDS → Databases → 选数据库 → Actions → **Delete**：勾选 **Do NOT create a final snapshot**（省钱），勾选确认框，输入 `delete me` 确认。
5. EC2 → Security Groups → 删除 `shopping-cart-sg`（RDS 的安全组会随库一起删）。
6. CloudWatch → Log groups → 删除 `/ecs/shopping-cart`。

删完第二天去 Billing 控制台看一眼 Bills，确认没有新增费用。

---

## 常见问题对照表

| 现象 | 原因 | 解决 |
|---|---|---|
| ECS Task 状态 `PROVISIONING` 一直不动 | 第一次拉镜像慢 | 等 5~10 分钟 |
| Task 状态 `PENDING` 后变 `STOPPED` | 点进 Task → **Logs** 标签看报错 | 见下面两行 |
| 日志里 `Communications link failure` / `Access denied for user` | 数据库连不上 | 检查 DB_URL 端点拼写、RDS 是否 Available、安全组 3306 是否开放 |
| 日志里 `Container exited... 137` | 内存不够 | Task definition 里 memory 改成 `2048`（会多花约 $3/月） |
| 网站打不开（超时） | 安全组没开 18080 | EC2 → Security Groups → `shopping-cart-sg` → inbound 加 TCP 18080 |
| IP 变了访问不了 | 重新部署 IP 会变 | 去 Task 里找新的 Public IP |
| GitHub Actions 里 docker push 报 `denied` | Docker Hub 令牌错了/过期 | 重新生成令牌并更新 GitHub Secret |
| Learner Lab 会话过期（4 小时） | 正常现象 | 回 Canvas 重新 Start Lab，资源不丢 |
| 某些 AWS 服务页面 Access Denied | Learner Lab 不开放该服务 | 用本手册的替代方案，不要硬刚 |

---

## 本手册用到的文件

| 文件 | 作用 |
|---|---|
| `Group2/Dockerfile` | 三阶段构建：Angular → Spring Boot jar → 最终运行镜像 |
| `Group2/.dockerignore` | 让镜像更小、构建更快 |
| `.github/workflows/ci.yml` | GitHub Actions：测试 + 打镜像 + 推 Docker Hub |
