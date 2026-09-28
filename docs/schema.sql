-- ============================================================
-- 贷后智能体 - 数据库建表脚本
-- 版本：V1.0
-- 日期：2026-09-15
-- 数据库：MySQL 8.0
-- 字符集：utf8mb4
-- ============================================================

-- 请确保已选中 springbootdemo 数据库
-- ============================================================
-- 1. borrower（借款人表）
-- ============================================================
CREATE TABLE IF NOT EXISTS borrower (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    borrower_id VARCHAR(32) NOT NULL COMMENT '借款人唯一标识',
    borrower_name VARCHAR(50) NOT NULL COMMENT '借款人姓名',
    id_card VARCHAR(128) COMMENT '身份证号（加密）',
    phone VARCHAR(128) COMMENT '联系电话（加密）',
    borrower_type VARCHAR(20) DEFAULT '个人' COMMENT '借款人类型',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_borrower_id (borrower_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借款人表';

-- ============================================================
-- 2. loan（贷款合同表）
-- ============================================================
CREATE TABLE IF NOT EXISTS loan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    loan_id VARCHAR(32) NOT NULL COMMENT '贷款合同编号',
    borrower_id VARCHAR(32) NOT NULL COMMENT '借款人ID',
    loan_amount DECIMAL(15,2) NOT NULL COMMENT '贷款金额',
    loan_balance DECIMAL(15,2) NOT NULL COMMENT '当前余额',
    loan_type VARCHAR(20) NOT NULL COMMENT '贷款类型',
    loan_start_date DATE NOT NULL COMMENT '开始日期',
    loan_end_date DATE NOT NULL COMMENT '到期日期',
    interest_rate DECIMAL(5,4) NOT NULL COMMENT '年利率',
    loan_status VARCHAR(20) DEFAULT 'NORMAL' COMMENT '贷款状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_loan_id (loan_id),
    INDEX idx_borrower_id (borrower_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贷款合同表';

-- ============================================================
-- 3. analysis_task（分析任务表）
-- ============================================================
CREATE TABLE IF NOT EXISTS analysis_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(32) NOT NULL COMMENT '任务ID',
    borrower_id VARCHAR(32) NOT NULL COMMENT '借款人ID',
    loan_id VARCHAR(32) COMMENT '贷款合同ID',
    report_period VARCHAR(10) NOT NULL COMMENT '报告期次',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
    current_step VARCHAR(100) COMMENT '当前步骤',
    risk_score DECIMAL(5,1) COMMENT '风险评分',
    risk_grade VARCHAR(5) COMMENT '风险评级',
    report_id VARCHAR(32) COMMENT '关联报告ID',
    error_msg TEXT COMMENT '错误信息',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_id (task_id),
    UNIQUE KEY uk_borrower_period (borrower_id, report_period),
    INDEX idx_borrower_id (borrower_id),
    INDEX idx_status (status),
    INDEX idx_report_period (report_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析任务表';

-- ============================================================
-- 4. task_step_log（任务步骤日志表）
-- ============================================================
CREATE TABLE IF NOT EXISTS task_step_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(32) NOT NULL COMMENT '任务ID',
    step_name VARCHAR(50) NOT NULL COMMENT '步骤名称',
    step_order INT NOT NULL COMMENT '步骤序号',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    input_data TEXT COMMENT '输入数据',
    output_data TEXT COMMENT '输出数据',
    duration_ms INT COMMENT '耗时(毫秒)',
    error_msg TEXT COMMENT '错误信息',
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务步骤日志表';

-- ============================================================
-- 5. report（分析报告表）
-- ============================================================
CREATE TABLE IF NOT EXISTS report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_id VARCHAR(32) NOT NULL COMMENT '报告ID',
    task_id VARCHAR(32) NOT NULL COMMENT '任务ID',
    borrower_id VARCHAR(32) NOT NULL COMMENT '借款人ID',
    borrower_name VARCHAR(50) NOT NULL COMMENT '借款人姓名',
    loan_id VARCHAR(32) COMMENT '贷款合同ID',
    report_period VARCHAR(10) NOT NULL COMMENT '报告期次',
    repayment_score DECIMAL(5,1) COMMENT '还款评分',
    financial_score DECIMAL(5,1) COMMENT '财务评分',
    risk_signal_deduction DECIMAL(5,1) COMMENT '风险信号扣分',
    risk_score DECIMAL(5,1) NOT NULL COMMENT '综合评分',
    risk_grade VARCHAR(5) NOT NULL COMMENT '风险评级',
    previous_grade VARCHAR(5) COMMENT '上期评级',
    conclusion TEXT NOT NULL COMMENT '总体结论',
    suggestions TEXT COMMENT '建议措施(JSON)',
    report_content LONGTEXT COMMENT '完整报告(JSON)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_report_id (report_id),
    INDEX idx_borrower_id (borrower_id),
    INDEX idx_report_period (report_period),
    INDEX idx_risk_grade (risk_grade)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析报告表';

-- ============================================================
-- 6. alert（预警工单表）
-- ============================================================
CREATE TABLE IF NOT EXISTS alert (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alert_id VARCHAR(32) NOT NULL COMMENT '预警ID',
    task_id VARCHAR(32) NOT NULL COMMENT '任务ID',
    borrower_id VARCHAR(32) NOT NULL COMMENT '借款人ID',
    borrower_name VARCHAR(50) NOT NULL COMMENT '借款人姓名',
    rule_id VARCHAR(32) NOT NULL COMMENT '规则ID',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    level VARCHAR(20) NOT NULL COMMENT '预警级别',
    status VARCHAR(20) NOT NULL DEFAULT 'TRIGGERED' COMMENT '工单状态',
    handler VARCHAR(50) COMMENT '处理人',
    handle_comment TEXT COMMENT '处理意见',
    handle_time DATETIME COMMENT '处理时间',
    approver VARCHAR(50) COMMENT '审批人',
    close_reason TEXT COMMENT '关闭原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_alert_id (alert_id),
    INDEX idx_borrower_id (borrower_id),
    INDEX idx_status (status),
    INDEX idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警工单表';

-- ============================================================
-- 7. alert_rule（预警规则配置表）
-- ============================================================
CREATE TABLE IF NOT EXISTS alert_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id VARCHAR(32) NOT NULL COMMENT '规则ID',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    condition_expr VARCHAR(500) NOT NULL COMMENT '条件表达式',
    level VARCHAR(20) NOT NULL COMMENT '预警级别(LOW/MEDIUM/HIGH/URGENT)',
    notify_methods VARCHAR(200) NOT NULL COMMENT '通知方式(逗号分隔)',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rule_id (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警规则配置表';

-- ============================================================
-- 初始化数据：预警规则
-- ============================================================
INSERT INTO alert_rule (rule_id, rule_name, condition_expr, level, notify_methods, enabled) VALUES
('RULE-001', '评级下降', 'gradeDrop >= 2', 'HIGH', 'FEISHU,EMAIL', 1),
('RULE-002', '评级为D', 'riskGrade == "D"', 'MEDIUM', 'FEISHU', 1),
('RULE-003', '评级为E', 'riskGrade == "E"', 'URGENT', 'FEISHU,EMAIL,SMS', 1),
('RULE-004', '逾期超限', 'currentOverdueDays > 30', 'HIGH', 'FEISHU,EMAIL', 1),
('RULE-005', '多头借贷', 'multiLending == true', 'MEDIUM', 'FEISHU', 1),
('RULE-006', '收入下降', 'monthlyIncomeChange < -0.2', 'MEDIUM', 'FEISHU', 1),
('RULE-007', '负债率过高', 'debtRatio > 0.6', 'MEDIUM', 'FEISHU', 1);
