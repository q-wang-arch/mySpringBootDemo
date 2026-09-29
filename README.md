# 贷后智能体（Post-Loan Agent）

> 自动接收源头系统推送的贷后数据，按步骤编排多维度风险分析，输出结构化贷后分析报告与预警工单。

贷后管理是信贷业务的核心环节。传统方式依赖人工分析借款人还款行为、财务状况与风险信号，效率低、覆盖有限、识别滞后。本项目将这一流程自动化：一次数据接入即触发完整分析链路，产出可追溯的评级结论与处置建议。

---

## 目录

- [核心能力](#核心能力)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [智能体执行流程](#智能体执行流程)
- [风险评分与评级](#风险评分与评级)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [API 接口](#api-接口)
- [接口鉴权](#接口鉴权)
- [数据模型](#数据模型)
- [设计文档](#设计文档)
- [实现状态](#实现状态)

---

## 核心能力

| 能力 | 说明 |
|------|------|
| 数据接入 | REST API 接收源头加工数据，字段级校验 + 按「借款人 + 报告期次」去重 |
| 异步分析 | 接入请求立即返回 `taskId`，分析在独立线程池中执行，不阻塞调用方 |
| 多维度分析 | 还款行为、财务状况、风险信号三个维度独立评分 |
| 智能评级 | 加权计算 0–100 综合评分，映射 A/B/C/D/E 五级评级 |
| 报告生成 | 自动组装六段式结构化分析报告（概况 / 还款 / 财务 / 风险 / 评级 / 结论建议） |
| 预警工单 | 规则匹配自动触发预警，支持接单处置 |
| 全链路留痕 | 8 个执行步骤逐步记录状态、耗时与输出，支持任务回溯 |
| 可视化前端 | Vue 3 单页应用，覆盖数据接入、任务列表/详情、报告、预警五个视图 |

---

## 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 2.7.18 |
| JDK | Java | 1.8 |
| ORM | MyBatis-Plus | 3.5.3.1 |
| 数据库 | MySQL | 8.0（驱动 8.0.33） |
| 构建 | Maven | — |
| 前端框架 | Vue | ^3.4 |
| 构建工具 | Vite | ^5.0 |
| UI 组件库 | Element Plus | ^2.5 |
| HTTP 客户端 | axios | ^1.6 |

---

## 系统架构

```
┌──────────────────────────────────────────────────────────────┐
│                前端 Vue3 + Element Plus (:5173)              │
│   数据接入 │ 任务列表 │ 任务详情 │ 分析报告 │ 预警管理        │
└───────────────────────────┬──────────────────────────────────┘
                            │ HTTP / REST  (Vite 代理 /api)
┌───────────────────────────┴──────────────────────────────────┐
│                 Spring Boot 后端 (:8080)                     │
│                                                              │
│  Controller 层   AgentController │ ReportController          │
│                  AlertController │ HelloController           │
│                                                              │
│  Service 层      AgentIngestService ──┐                      │
│                  ReportService        │                      │
│                  AlertService         │                      │
│                                       ▼                      │
│  Agent 引擎      AgentEngine  ──>  AgentStep ×8              │
│                  AnalysisContext（步骤间数据载体）            │
│                                                              │
│  线程池          agentExecutor（分析异步执行）                │
│                                                              │
│  Mapper 层       MyBatis-Plus BaseMapper ×7                  │
└───────────────────────────┬──────────────────────────────────┘
                            │ JDBC
                    ┌───────┴────────┐
                    │  MySQL 8.0     │
                    │  7 张业务表     │
                    └────────────────┘
```

**设计要点**：`AgentIngestService.ingest()` 的事务只覆盖数据接入部分（校验 / 去重 / 入库 / 建任务），分析流程提交到 `agentExecutor` 线程池异步执行，避免长耗时分析拖住 HTTP 请求。

---

## 智能体执行流程

`AgentEngine` 顺序编排 8 个步骤，每步均写入 `task_step_log`：

| 步骤 | 名称 | 实现 | 职责 |
|------|------|------|------|
| Step1 | 数据校验 | `DataValidationStep` | 校验数据完整性，不通过则终止并标记异常 |
| Step2 | 还款行为分析 | `RepaymentAnalysisStep` | 准时率、逾期频率、逾期天数 |
| Step3 | 财务状况分析 | `FinancialAnalysisStep` | 收入变化、负债率、现金流 |
| Step4 | 风险信号识别 | `RiskSignalStep` | 多头借贷、担保链、诉讼、经营异常、资产转移 |
| Step5 | 评分评级计算 | `ScoringStep` | 加权算分、映射评级、比对上期评级 |
| Step6 | 生成分析报告 | `AgentEngine.ReportStep` | 组装并落库报告 |
| Step7 | 预警规则匹配 | `AgentEngine.AlertStep` | 命中规则则生成预警工单 |
| Step8 | 完成归档 | `AgentEngine` | 回写任务状态、评分、评级、报告 ID |

**任务状态机**：

```
PENDING ──> RUNNING ──> COMPLETED
               │
               └──> FAILED（Step1–5 任一步失败即中断，记录 errorMsg）
```

---

## 风险评分与评级

综合评分公式（见 `ScoringStep`）：

```
综合评分 = 还款评分 × 0.35
         + 财务评分 × 0.30
         − 风险信号扣分
         + 外部信用评分 × 0.35
```

结果钳制在 `0–100`，保留 1 位小数。

**风险信号扣分表**：

| 信号 | 扣分 |
|------|------|
| 多头借贷 `multiLending` | −15 |
| 资产转移 `assetTransfer` | −15 |
| 担保链异常 `guaranteeChainAbnormal` | −10 |
| 诉讼记录 `litigationRecord` | −10 |
| 经营异常 `businessAbnormal` | −10 |

**评级映射**：

| 评分区间 | 评级 | 含义 | 建议措施 |
|---------|------|------|---------|
| 85–100 | A | 优质，风险极低 | 常规监控 |
| 70–84 | B | 良好，风险较低 | 常规监控 |
| 55–69 | C | 关注，风险一般 | 提高监控频率 |
| 40–54 | D | 次级，风险较高 | 约谈 / 要求增信 |
| 0–39 | E | 损失，风险极高 | 保全措施 / 法律追偿 |

---

## 项目结构

```
springbootdemo/
├── src/main/java/org/example/springbootdemo/
│   ├── SpringbootdemoApplication.java
│   ├── agent/                  # 智能体引擎
│   │   ├── AgentEngine.java        # 主流程编排（8 步）
│   │   ├── AgentStep.java          # 步骤接口
│   │   ├── AnalysisContext.java    # 步骤间共享上下文
│   │   ├── StepResult.java         # 步骤执行结果
│   │   └── steps/                  # 5 个具体分析步骤
│   ├── config/                 # 线程池配置
│   ├── controller/             # 4 个 REST 控制器
│   ├── dto/                    # ApiResponse / BorrowerData
│   ├── entity/                 # 7 个实体（对应 7 张表）
│   ├── mapper/                 # MyBatis-Plus Mapper
│   └── service/                # 业务服务
├── src/main/resources/application.properties
├── frontend/                   # Vue3 前端
│   └── src/
│       ├── App.vue             # 五页签主框架
│       ├── api/index.js        # axios 封装 + 响应解包
│       └── views/              # DataIngest / TaskList / TaskDetail / ReportView / AlertView
├── docs/                       # 设计与需求文档
│   ├── 01-需求规格说明书.md
│   ├── 02-系统设计文档.md
│   ├── 03-接口设计文档.md
│   ├── 04-数据模型设计文档.md
│   └── schema.sql              # 建表脚本
└── pom.xml
```

---

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0

### 1. 初始化数据库

创建 `springbootdemo` 库，然后执行建表脚本：

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS springbootdemo DEFAULT CHARSET utf8mb4;"
mysql -u root -p springbootdemo < docs/schema.sql
```

### 2. 配置环境变量（数据库密码 + 接口令牌）

数据库密码与接口令牌都通过环境变量注入，**仓库中不含任何明文密码或令牌**。

在 IDEA 中：`Run → Edit Configurations → 选中启动类 → Environment variables`，填入：

```
DB_PASSWORD=你的MySQL密码
APP_TOKEN_RISK_ADMIN=自己生成的一串随机值
APP_TOKEN_RISK_APPROVER=自己生成的一串随机值
APP_TOKEN_API_CLIENT=自己生成的一串随机值
APP_TOKEN_SYS_ADMIN=自己生成的一串随机值
```

操作要点（都是实际踩过的坑）：

- 该输入框支持 **`KEY=value;KEY2=value2` 分号分隔**，上面五条可以整块粘贴进去；
  也可点输入框右侧图标用表格逐行录入，更不容易出错。
- **不要给值加引号**。写成 `DB_PASSWORD="123456"` 时，真实密码会变成含引号的 8 位字符串，
  报错表现为 `Access denied ... (using password: YES)`（密码传了但不对）。
- IDEA 运行配置里的值**优先级高于 Windows 系统环境变量**。之前若在系统环境变量里配过，
  在这里填对即可覆盖，无需去删系统变量。
- 若选择配在 Windows 系统环境变量，**必须完全退出 IDEA 再打开**才生效——
  IDEA 只在进程启动时读取一次系统环境变量，"Stop → Re-run" 是不够的。
- 只跑前端看效果时，**最少只需要 `APP_TOKEN_RISK_ADMIN` 一个**；
  要演示 403（角色不足）再加 `APP_TOKEN_RISK_APPROVER`，要模拟源头系统推送再加 `APP_TOKEN_API_CLIENT`。

生成随机令牌（任选一种）：

```bash
node -e "console.log(require('crypto').randomBytes(24).toString('base64url'))"
openssl rand -base64 24
```

> **三条必读提示**
> 1. 未配置 `DB_PASSWORD` 时启动会报 `Could not resolve placeholder 'DB_PASSWORD'`。
> 2. **接口鉴权默认开启**（`app.auth.enabled=true`）。若一个令牌都没配，启动会打印 ERROR 日志，
>    且所有 `/api/**` 请求返回 401——这是有意设计的"配置缺失即拒绝"，而不是默默放行。
>    本地想临时关闭，加 `APP_AUTH_ENABLED=false` 即可（UAT / 生产环境不可关闭）。
> 3. 前端使用的令牌不在环境变量里，而是写在 `frontend/.env.local`（见第 4 步）。
>    未配置令牌的用户会被自动跳过（视为账号不可用），不会退化成一个"空令牌即可通过"的后门。

### 3. 启动后端

```bash
./mvnw spring-boot:run
```

服务监听 `http://localhost:8080`。验证（`/api/hello` 属于免鉴权路径）：

```bash
curl http://localhost:8080/api/hello
```

**常见启动报错**

| 报错信息 | 原因 | 处理 |
|---------|------|------|
| `Access denied for user 'root'@'localhost' (using password: YES)` | `DB_PASSWORD` 的值与 MySQL 实际密码不符 | 注意别把**引号**、**尾随空格**一起填进环境变量；用 MySQL 客户端手工连一次确认密码 |
| `Access denied ... (using password: NO)` | 该账号设有密码但没传 | 补上 `DB_PASSWORD` |
| `Could not resolve placeholder 'DB_PASSWORD'` | 环境变量完全没配 | 检查是否填在 `Environment variables` 而不是 `VM options` |
| 日志出现 `[预警规则] 规则缓存加载失败` | 数据库连不上 | 应用**仍会启动**，但预警功能失效，需修复后重启 |

> **数据库不可用不再导致启动失败**：`AlertService` 加载预警规则失败时只打 ERROR 日志并保持规则缓存为空。
> 这样本地只想调接口、验鉴权时不必先连库；但线上必须盯住这条 ERROR——
> 规则缓存为空意味着所有分析都不会产生预警工单。可通过 `AlertService.getRuleCount()` / `isRuleCacheReady()` 做健康检查。

### 4. 启动前端

```bash
cd frontend
cp .env.example .env.local   # 然后填入与后端 APP_TOKEN_RISK_ADMIN 一致的令牌
npm install
npm run dev
```

访问 `http://localhost:5173`。Vite 已配置 `/api` 代理到 `http://localhost:8080`，无需处理跨域。
`frontend/.env.local` 已被 `.gitignore` 忽略，令牌不会进入仓库。

### 5. 跑通一次分析

在「数据接入」页签提交下方样例数据，随后在「任务列表」查看任务状态，在「分析报告」「预警管理」查看产出：

```json
{
  "borrowerId": "BR20260001",
  "borrowerName": "张三",
  "loanId": "LN20260001",
  "reportPeriod": "2026-09",
  "loanInfo": {
    "loanAmount": 500000,
    "loanBalance": 320000,
    "loanType": "经营贷",
    "loanStartDate": "2026-01-15",
    "loanEndDate": "2027-01-14",
    "interestRate": 5.85
  },
  "repaymentInfo": {
    "totalTerms": 12,
    "completedTerms": 8,
    "ontimeCount": 7,
    "overdueCount": 1,
    "maxOverdueDays": 5,
    "currentOverdueDays": 0
  },
  "financialInfo": {
    "monthlyIncome": 35000,
    "monthlyIncomeChange": -0.08,
    "debtRatio": 0.42,
    "debtRatioChange": 0.05,
    "cashFlowStatus": "正常"
  },
  "riskSignals": {
    "multiLending": false,
    "guaranteeChainAbnormal": false,
    "litigationRecord": false,
    "businessAbnormal": false,
    "assetTransfer": false
  },
  "externalData": {
    "creditScore": 680,
    "creditScoreChange": -15,
    "courtFilingCount": 0,
    "taxArrears": false
  }
}
```

---

## API 接口

统一响应体：`{ "code": 200, "message": "...", "data": {...}, "timestamp": ... }`

| 方法 | 路径 | 说明 | 所需角色 |
|------|------|------|----------|
| POST | `/api/agent/ingest` | 接收贷后数据，返回 `taskId`，异步触发分析 | `API_CLIENT` / `RISK_ADMIN` / `SYS_ADMIN` |
| GET | `/api/agent/task/{taskId}` | 查询单个任务状态与执行步骤 | `RISK_ADMIN` / `RISK_APPROVER` / `SYS_ADMIN` |
| GET | `/api/agent/tasks` | 查询任务列表（支持 `borrowerId`、`status` 过滤） | 同上 |
| GET | `/api/report/list` | 查询报告列表（支持 `borrowerId`、`riskGrade` 过滤） | 同上 |
| GET | `/api/report/{reportId}` | 查询报告详情 | 同上 |
| GET | `/api/alert/list` | 查询预警列表（支持 `borrowerId`、`status` 过滤） | 同上 |
| POST | `/api/alert/{alertId}/handle` | 处置预警工单 | `RISK_ADMIN` / `SYS_ADMIN` |
| GET | `/api/hello` | 连通性自检 | 免鉴权 |

所有接口需在请求头携带令牌，具体方式见[接口鉴权](#接口鉴权)。
完整字段定义见 [docs/03-接口设计文档.md](docs/03-接口设计文档.md)。

---

## 接口鉴权

鉴权拆成两层——**认证**回答"你是谁"（失败返回 401），**授权**回答"你能不能碰这个接口"（失败返回 403）。
两者混在一起是最常见的坑：排查时把 403 当成 401 查令牌，方向就完全跑偏了。

### 令牌怎么传

| 调用方 | 传递方式 | 说明 |
|--------|----------|------|
| 前端页面、人工调用 | `Authorization: Bearer <token>` | REST 常规写法，大小写不敏感 |
| 源头系统对接 | `X-Auth-Token: <token>` | 少拼前缀，脚本与定时任务更省事 |

### 令牌与角色

配置位于 `application.properties` 的 `app.auth.*`，**令牌值一律由环境变量注入**。

| 用户 | 角色 | 环境变量 | 可访问范围 |
|------|------|----------|-----------|
| `risk-admin` | `RISK_ADMIN` | `APP_TOKEN_RISK_ADMIN` | 报告 / 任务 / 预警查询 + 处置预警 |
| `risk-approver` | `RISK_APPROVER` | `APP_TOKEN_RISK_APPROVER` | 报告 / 任务 / 预警查询（处置接口返回 403） |
| `api-client` | `API_CLIENT` | `APP_TOKEN_API_CLIENT` | 仅 `POST /api/agent/ingest` |
| `sys-admin` | `SYS_ADMIN` + 以上内部角色 | `APP_TOKEN_SYS_ADMIN` | 全部接口 |

> 未设置环境变量的用户会被自动跳过（视为账号不可用），不会退化成一个"空令牌即可通过"的后门。

### 实现结构

| 组件 | 文件 | 职责 |
|------|------|------|
| 认证拦截器 | `auth/AuthInterceptor.java` | 校验令牌、写入身份上下文；失败返回 401 |
| 授权拦截器 | `auth/RoleInterceptor.java` | 读取 `@RequireRoles`、校验角色；失败返回 403 |
| 角色声明 | `auth/RequireRoles.java` | 可标在类或方法上，**方法级优先**，多个角色满足其一即放行 |
| 身份上下文 | `auth/AuthContext.java` | ThreadLocal 保存当前调用者，请求结束清理 |
| 鉴权配置 | `config/AuthProperties.java` | 绑定 `app.auth.*`，启动自检并打印有效用户 |
| 拦截器注册 | `config/WebMvcConfig.java` | 拦截 `/api/**`，认证 order=1、授权 order=2 |

### 三个刻意的设计取舍

1. **配置缺失即拒绝**：开关打开却没有任何有效令牌时，启动打印 ERROR，所有 `/api/**` 返回 401——
   采用"失败即拒绝"而不是"失败即放行"。
2. **空白令牌不算令牌**：未配置令牌的用户直接跳过，因此即使有人发一个空的 `Authorization: Bearer`，
   也拿不到任何身份。
3. **令牌不落仓库**：与数据库密码同一原则。前端令牌写在 `frontend/.env.local`（已 gitignore）。

### 怎么验证

```bash
TOKEN=$APP_TOKEN_RISK_ADMIN

curl -i http://localhost:8080/api/hello                       # 200 免鉴权
curl -i http://localhost:8080/api/agent/tasks                 # 401 未携带令牌
curl -i -H "Authorization: Bearer wrong" http://localhost:8080/api/agent/tasks   # 401 令牌无效
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/agent/tasks  # 200 通过
curl -i -X POST -H "X-Auth-Token: $APP_TOKEN_RISK_APPROVER" \
     -H "Content-Type: application/json" -d '{"handler":"张三"}' \
     http://localhost:8080/api/alert/<alertId>/handle           # 403 角色不足
```

自动化验证（**不依赖数据库**，直接构造拦截器与 Mock 请求）：

```bash
./mvnw test -Dtest=AuthInterceptorTest
```

> 上表的手工命令与单元测试均已在真实环境跑通（MySQL 8.0.19 + `springbootdemo` 库，JDK 1.8）：
> 无令牌 / 错令牌返回 401，审批人处置预警返回 403，`/api/hello` 返回 200，
> 管理员携带有效令牌可正常拿到任务与报告数据。

### 边界说明

当前是"UAT 可演示的最小版本"：**静态令牌 + 内存用户表**，未实现令牌过期、刷新、吊销、
审计日志与用户管理界面。接入真实用户体系时只需替换 `AuthProperties` 的数据来源，
两个拦截器与 `@RequireRoles` 的业务声明都无需改动。


---

## 数据模型

7 张业务表，建表脚本见 [docs/schema.sql](docs/schema.sql)。

| 表名 | 说明 |
|------|------|
| `borrower` | 借款人（身份证 / 手机号字段预留加密存储） |
| `loan` | 贷款合同 |
| `analysis_task` | 分析任务（状态机、当前步骤、评分、评级、报告 ID） |
| `task_step_log` | 步骤执行日志（步骤名、顺序、状态、输出、耗时） |
| `report` | 分析报告 |
| `alert` | 预警工单 |
| `alert_rule` | 预警规则配置 |

关系：`borrower 1:N loan`，`borrower 1:N analysis_task`，`analysis_task 1:1 report`，`report 1:N alert`，`alert_rule` 为全局配置。

完整字段说明见 [docs/04-数据模型设计文档.md](docs/04-数据模型设计文档.md)。

---

## 设计文档

| 文档 | 内容 |
|------|------|
| [01-需求规格说明书](docs/01-需求规格说明书.md) | 功能需求（FR-001 ~ FR-042）、非功能需求、输入数据规范、用户角色 |
| [02-系统设计文档](docs/02-系统设计文档.md) | 系统架构、智能体引擎设计、分析引擎算法、报告结构、预警规则、技术选型 |
| [03-接口设计文档](docs/03-接口设计文档.md) | 接口定义、请求 / 响应示例、错误码 |
| [04-数据模型设计文档](docs/04-数据模型设计文档.md) | ER 图、表结构、索引设计 |
| [schema.sql](docs/schema.sql) | 可直接执行的建表脚本 |

---

## 实现状态

### 已实现

- 数据接入：字段级校验、借款人 + 报告期次去重、借款人 / 合同 upsert
- 异步分析：线程池编排，HTTP 请求不阻塞
- Agent 引擎 8 步流程，逐步落库执行日志
- 还款 / 财务 / 风险信号三维度分析与加权评分、A–E 评级
- 报告自动生成与查询
- 预警规则匹配与工单处置
- 接口鉴权：静态令牌认证（401）+ 角色授权（403），拦截器 + `@RequireRoles` 注解实现
- Vue 3 前端五个视图，axios 统一注入令牌并处理 401 / 403

### 待实现

| 项 | 说明 |
|----|------|
| 报告导出 | PDF / HTML 导出（需引入 Thymeleaf 与 PDF 渲染库） |
| 定时批量分析 | `@Scheduled` 批量触发（设计文档中的 `AnalysisScheduler`） |
| 预警推送 | 飞书 / 邮件 / 短信通道对接，目前仅落库为工单 |
| 模板管理 | 报告模板与模板版本管理（FR-020） |
| 批量导入 | CSV / Excel 批量数据接入（FR-004） |
| 趋势分析 | 多期数据横向对比（FR-015） |
| 用户体系 | 令牌过期 / 刷新 / 吊销、审计日志、用户与角色管理界面 |
| 数据加密 | `borrower.id_card`、`phone` 的加密存储（设计中要求，当前未落库） |
| 预警审批 | 风险审批人的"审批处置结果"接口（当前仅有处置） |

---

## 说明

- 本项目为贷后智能体 Demo，侧重流程编排与规则分析的完整链路实现。算法参数（权重、扣分、阈值）目前硬编码在对应步骤类中，可按需调整。
- `application.properties` 中的数据源密码已改为环境变量注入，请勿把真实密码提交回仓库。
