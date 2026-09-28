package org.example.springbootdemo.agent.steps;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.springbootdemo.agent.AgentStep;
import org.example.springbootdemo.agent.AnalysisContext;
import org.example.springbootdemo.agent.StepResult;
import org.example.springbootdemo.dto.BorrowerData;
import org.example.springbootdemo.entity.Report;
import org.example.springbootdemo.mapper.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * Step5: 评分评级计算
 * 综合评分 = 还款评分×0.35 + 财务评分×0.30 - 风险信号扣分 + 外部信用评分×0.35
 * 评级: 85-100=A, 70-84=B, 55-69=C, 40-54=D, 0-39=E
 */
@Component
public class ScoringStep implements AgentStep {

    @Autowired
    private ReportMapper reportMapper;

    private static final List<String> GRADE_ORDER = Arrays.asList("A", "B", "C", "D", "E");

    @Override
    public String getStepName() { return "评分评级计算"; }

    @Override
    public int getStepOrder() { return 5; }

    @Override
    public StepResult execute(AnalysisContext context) {

        // 还款评分 × 0.35
        BigDecimal repaymentPart = context.getRepaymentScore()
                .multiply(BigDecimal.valueOf(0.35));

        // 财务评分 × 0.30
        BigDecimal financialPart = context.getFinancialScore()
                .multiply(BigDecimal.valueOf(0.30));

        // 风险信号扣分
        BigDecimal deduction = context.getRiskSignalDeduction() == null
                ? BigDecimal.ZERO : context.getRiskSignalDeduction();

        // 外部信用评分 × 0.35 (归一化到0-100)
        BorrowerData bd = context.getBorrowerData();
        BigDecimal externalPart = BigDecimal.ZERO;
        if (bd.getExternalData() != null && bd.getExternalData().getCreditScore() != null) {
            externalPart = BigDecimal.valueOf(bd.getExternalData().getCreditScore())
                    .multiply(BigDecimal.valueOf(0.35));
        }

        // 综合评分
        BigDecimal score = repaymentPart.add(financialPart).subtract(deduction).add(externalPart);
        score = score.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);

        context.setRiskScore(score);

        // 评级映射
        String grade = scoreToGrade(score);
        context.setRiskGrade(grade);

        // 查询上期评级
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getBorrowerId, context.getBorrowerData().getBorrowerId())
               .orderByDesc(Report::getCreateTime)
               .last("LIMIT 1");
        Report lastReport = reportMapper.selectOne(wrapper);
        if (lastReport != null && lastReport.getRiskGrade() != null) {
            context.setPreviousGrade(lastReport.getRiskGrade());
        }

        return StepResult.success("综合评分=" + score + "，评级=" + grade
                + (context.getPreviousGrade() != null ? "，上期=" + context.getPreviousGrade() : ""));
    }

    private String scoreToGrade(BigDecimal score) {
        int s = score.intValue();
        if (s >= 85) return "A";
        if (s >= 70) return "B";
        if (s >= 55) return "C";
        if (s >= 40) return "D";
        return "E";
    }

    /**
     * 计算评级落差（用于预警）
     */
    public int getGradeDrop(String previous, String current) {
        if (previous == null || current == null) return 0;
        int prevIdx = GRADE_ORDER.indexOf(previous);
        int currIdx = GRADE_ORDER.indexOf(current);
        if (prevIdx < 0 || currIdx < 0) return 0;
        return currIdx - prevIdx;
    }
}
