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

### 2. 配置数据库密码

数据源密码通过环境变量注入，**仓库中不含任何明文密码**。

在 IDEA 中：`Run → Edit Configurations → 启动类 → Environment variables`，填入：

```
DB_PASSWORD=你的MySQL密码
```

或在系统环境变量中设置 `DB_PASSWORD`。

> 未配置该变量时，应用启动会报 `Could not resolve placeholder 'DB_PASSWORD'`。

### 3. 启动后端

```bash
./mvnw spring-boot:run
```

服务监听 `http://localhost:8080`。验证：

```bash
curl http://localhost:8080/api/hello
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。Vite 已配置 `/api` 代理到 `http://localhost:8080`，无需处理跨域。

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

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/agent/ingest` | 接收贷后数据，返回 `taskId`，异步触发分析 |
| GET | `/api/agent/task/{taskId}` | 查询单个任务状态与执行步骤 |
| GET | `/api/agent/tasks` | 查询任务列表（支持 `borrowerId`、`status` 过滤） |
| GET | `/api/report/list` | 查询报告列表（支持 `borrowerId`、`riskGrade` 过滤） |
| GET | `/api/report/{reportId}` | 查询报告详情 |
| GET | `/api/alert/list` | 查询预警列表（支持 `borrowerId`、`status` 过滤） |
| POST | `/api/alert/{alertId}/handle` | 处置预警工单 |
| GET | `/api/hello` | 连通性自检 |

完整字段定义见 [docs/03-接口设计文档.md](docs/03-接口设计文档.md)。

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
- Vue 3 前端五个视图

### 待实现

| 项 | 说明 |
|----|------|
| 报告导出 | PDF / HTML 导出（需引入 Thymeleaf 与 PDF 渲染库） |
| 定时批量分析 | `@Scheduled` 批量触发（设计文档中的 `AnalysisScheduler`） |
| 预警推送 | 飞书 / 邮件 / 短信通道对接，目前仅落库为工单 |
| 模板管理 | 报告模板与模板版本管理（FR-020） |
| 批量导入 | CSV / Excel 批量数据接入（FR-004） |
| 趋势分析 | 多期数据横向对比（FR-015） |
| 接口鉴权 | 设计文档要求 Bearer Token，当前未接入认证框架 |

---

## 说明

- 本项目为贷后智能体 Demo，侧重流程编排与规则分析的完整链路实现。算法参数（权重、扣分、阈值）目前硬编码在对应步骤类中，可按需调整。
- `application.properties` 中的数据源密码已改为环境变量注入，请勿把真实密码提交回仓库。
