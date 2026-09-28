package org.example.springbootdemo.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springbootdemo.agent.steps.*;
import org.example.springbootdemo.dto.BorrowerData;
import org.example.springbootdemo.entity.AnalysisTask;
import org.example.springbootdemo.entity.TaskStepLog;
import org.example.springbootdemo.mapper.AnalysisTaskMapper;
import org.example.springbootdemo.mapper.TaskStepLogMapper;
import org.example.springbootdemo.service.AlertService;
import org.example.springbootdemo.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 智能体引擎 - 编排 8 个分析步骤
 */
@Component
public class AgentEngine {

    @Autowired
    private AnalysisTaskMapper analysisTaskMapper;

    @Autowired
    private TaskStepLogMapper taskStepLogMapper;

    @Autowired
    private DataValidationStep dataValidationStep;

    @Autowired
    private RepaymentAnalysisStep repaymentAnalysisStep;

    @Autowired
    private FinancialAnalysisStep financialAnalysisStep;

    @Autowired
    private RiskSignalStep riskSignalStep;

    @Autowired
    private ScoringStep scoringStep;

    @Autowired
    private ReportService reportService;

    @Autowired
    private AlertService alertService;

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行分析任务
     */
    public void execute(String taskId, BorrowerData borrowerData) {
        // 更新任务状态为执行中
        AnalysisTask task = analysisTaskMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AnalysisTask>()
                        .eq(AnalysisTask::getTaskId, taskId));
        if (task == null) {
            return;
        }

        task.setStatus("RUNNING");
        task.setStartTime(LocalDateTime.now());
        analysisTaskMapper.updateById(task);

        // 构建分析上下文
        AnalysisContext context = new AnalysisContext();
        context.setTaskId(taskId);
        context.setBorrowerData(borrowerData);

        // 8 步顺序执行
        List<AgentStep> steps = Arrays.asList(
                dataValidationStep,      // Step1: 数据校验
                repaymentAnalysisStep,   // Step2: 还款分析
                financialAnalysisStep,   // Step3: 财务分析
                riskSignalStep,          // Step4: 风险信号
                scoringStep,             // Step5: 评分评级
                null,                    // Step6: 报告生成（特殊处理）
                null,                    // Step7: 预警匹配（特殊处理）
                null                     // Step8: 完成归档（特殊处理）
        );

        boolean failed = false;

        for (int i = 0; i < 5; i++) {
            AgentStep step = steps.get(i);
            StepResult result = runStep(task, step, context);
            if (!result.isSuccess()) {
                task.setStatus("FAILED");
                task.setErrorMsg("Step" + (i + 1) + "[" + step.getStepName() + "]失败: " + result.getMessage());
                task.setEndTime(LocalDateTime.now());
                analysisTaskMapper.updateById(task);
                failed = true;
                break;
            }
            task.setCurrentStep("Step" + (i + 1) + ": " + step.getStepName() + " ✓");
            analysisTaskMapper.updateById(task);
        }

        if (!failed) {
            // Step6: 生成报告
            runStep(task, new ReportStep(), context);

            // Step7: 预警匹配
            runStep(task, new AlertStep(), context);

            // 更新任务评分和评级
            task.setRiskScore(context.getRiskScore());
            task.setRiskGrade(context.getRiskGrade());
            task.setReportId(context.getReportId());

            // Step8: 完成归档
            logStep(task, "任务完成归档", 8, "SUCCESS", "分析完成", null);

            task.setStatus("COMPLETED");
            task.setEndTime(LocalDateTime.now());
            task.setCurrentStep("全部完成");
            analysisTaskMapper.updateById(task);

            System.out.println("====================================");
            System.out.println("  智能体分析完成！");
            System.out.println("  借款人: " + borrowerData.getBorrowerName());
            System.out.println("  风险评分: " + context.getRiskScore());
            System.out.println("  风险评级: " + context.getRiskGrade());
            System.out.println("  报告ID: " + context.getReportId());
            System.out.println("  预警数: " + context.getAlerts().size());
            System.out.println("====================================");
        }
    }

    /**
     * 执行单步并记录日志
     */
    private StepResult runStep(AnalysisTask task, AgentStep step, AnalysisContext context) {
        LocalDateTime startTime = LocalDateTime.now();
        long startMs = System.currentTimeMillis();

        StepResult result = step.execute(context);

        int duration = (int) (System.currentTimeMillis() - startMs);
        String status = result.isSuccess() ? "SUCCESS" : "FAILED";
        String output = result.getMessage();

        logStep(task, step.getStepName(), step.getStepOrder(), status, output, duration);

        return result;
    }

    /**
     * 记录步骤日志
     */
    private void logStep(AnalysisTask task, String stepName, int order,
                         String status, String output, Integer durationMs) {
        TaskStepLog log = new TaskStepLog();
        log.setTaskId(task.getTaskId());
        log.setStepName(stepName);
        log.setStepOrder(order);
        log.setStatus(status);
        log.setOutputData(output);
        log.setDurationMs(durationMs);
        log.setStartTime(LocalDateTime.now());
        log.setEndTime(LocalDateTime.now());
        taskStepLogMapper.insert(log);
    }

    /**
     * Step6: 报告生成（实现 AgentStep 接口以复用日志逻辑）
     */
    private class ReportStep implements AgentStep {
        @Override
        public String getStepName() { return "生成分析报告"; }

        @Override
        public int getStepOrder() { return 6; }

        @Override
        public StepResult execute(AnalysisContext context) {
            reportService.generateReport(context);
            return StepResult.success("报告已生成，reportId=" + context.getReportId());
        }
    }

    /**
     * Step7: 预警匹配
     */
    private class AlertStep implements AgentStep {
        @Override
        public String getStepName() { return "预警规则匹配"; }

        @Override
        public int getStepOrder() { return 7; }

        @Override
        public StepResult execute(AnalysisContext context) {
            alertService.checkAndTriggerAlerts(context);
            int count = context.getAlerts().size();
            return StepResult.success("预警匹配完成，触发" + count + "条预警");
        }
    }
}
