package org.example.springbootdemo.agent.steps;

import org.example.springbootdemo.agent.AgentStep;
import org.example.springbootdemo.agent.AnalysisContext;
import org.example.springbootdemo.agent.StepResult;
import org.example.springbootdemo.dto.BorrowerData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Step3: 财务状况分析
 * 权重: 收入变化30% + 负债率35% + 负债率变化20% + 现金流15%
 */
@Component
public class FinancialAnalysisStep implements AgentStep {

    @Override
    public String getStepName() { return "财务状况分析"; }

    @Override
    public int getStepOrder() { return 3; }

    @Override
    public StepResult execute(AnalysisContext context) {
        BorrowerData.FinancialInfo fi = context.getBorrowerData().getFinancialInfo();

        // 收入变化率 (30%) - 下降越多分越低
        BigDecimal incomeChange = fi.getMonthlyIncomeChange() == null ? BigDecimal.ZERO : fi.getMonthlyIncomeChange();
        BigDecimal incomeScore;
        if (incomeChange.compareTo(BigDecimal.ZERO) >= 0) {
            incomeScore = BigDecimal.valueOf(30);
        } else if (incomeChange.compareTo(BigDecimal.valueOf(-0.3)) <= 0) {
            incomeScore = BigDecimal.ZERO;
        } else {
            incomeScore = BigDecimal.valueOf(30)
                    .multiply(BigDecimal.ONE.add(incomeChange.divide(BigDecimal.valueOf(0.3), 4, RoundingMode.HALF_UP)));
        }

        // 负债率 (35%) - <0.3满分, >0.7零分
        BigDecimal debtRatio = fi.getDebtRatio() == null ? BigDecimal.ZERO : fi.getDebtRatio();
        BigDecimal debtScore;
        if (debtRatio.compareTo(BigDecimal.valueOf(0.3)) <= 0) {
            debtScore = BigDecimal.valueOf(35);
        } else if (debtRatio.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
            debtScore = BigDecimal.ZERO;
        } else {
            debtScore = BigDecimal.valueOf(35)
                    .multiply(BigDecimal.ONE.subtract(
                            debtRatio.subtract(BigDecimal.valueOf(0.3)).divide(BigDecimal.valueOf(0.4), 4, RoundingMode.HALF_UP)));
        }

        // 负债率变化 (20%) - 上升越多分越低
        BigDecimal debtChange = fi.getDebtRatioChange() == null ? BigDecimal.ZERO : fi.getDebtRatioChange();
        BigDecimal debtChangeScore;
        if (debtChange.compareTo(BigDecimal.ZERO) <= 0) {
            debtChangeScore = BigDecimal.valueOf(20);
        } else if (debtChange.compareTo(BigDecimal.valueOf(0.2)) >= 0) {
            debtChangeScore = BigDecimal.ZERO;
        } else {
            debtChangeScore = BigDecimal.valueOf(20)
                    .multiply(BigDecimal.ONE.subtract(debtChange.divide(BigDecimal.valueOf(0.2), 4, RoundingMode.HALF_UP)));
        }

        // 现金流 (15%)
        String cashFlow = fi.getCashFlowStatus() == null ? "" : fi.getCashFlowStatus();
        BigDecimal cashScore;
        if (cashFlow.contains("正常") || cashFlow.contains("良好")) {
            cashScore = BigDecimal.valueOf(15);
        } else if (cashFlow.contains("紧张") || cashFlow.contains("偏紧")) {
            cashScore = BigDecimal.valueOf(7.5);
        } else {
            cashScore = BigDecimal.ZERO;
        }

        BigDecimal total = incomeScore.add(debtScore).add(debtChangeScore).add(cashScore)
                .setScale(1, RoundingMode.HALF_UP);
        context.setFinancialScore(total);

        // 评语
        StringBuilder comment = new StringBuilder();
        if (incomeChange.compareTo(BigDecimal.ZERO) < 0) {
            comment.append("收入下降").append(incomeChange.multiply(BigDecimal.valueOf(100))).append("%，");
        } else {
            comment.append("收入稳定，");
        }
        comment.append("负债率").append(debtRatio.multiply(BigDecimal.valueOf(100))).append("%，");
        comment.append("现金流").append(cashFlow).append("。");
        context.setFinancialComment(comment.toString());

        return StepResult.success("财务评分=" + total + "，" + comment);
    }
}
