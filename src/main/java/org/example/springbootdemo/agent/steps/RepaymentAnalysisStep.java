package org.example.springbootdemo.agent.steps;

import org.example.springbootdemo.agent.AgentStep;
import org.example.springbootdemo.agent.AnalysisContext;
import org.example.springbootdemo.agent.StepResult;
import org.example.springbootdemo.dto.BorrowerData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Step2: 还款行为分析
 * 权重: 准时率30% + 逾期频率25% + 最大逾期天数20% + 当前逾期天数25%
 */
@Component
public class RepaymentAnalysisStep implements AgentStep {

    @Override
    public String getStepName() { return "还款行为分析"; }

    @Override
    public int getStepOrder() { return 2; }

    @Override
    public StepResult execute(AnalysisContext context) {
        BorrowerData.RepaymentInfo ri = context.getBorrowerData().getRepaymentInfo();

        int completed = ri.getCompletedTerms() == 0 ? 1 : ri.getCompletedTerms();

        // 准时率 (30%)
        BigDecimal ontimeRate = BigDecimal.valueOf(ri.getOntimeCount())
                .divide(BigDecimal.valueOf(completed), 4, RoundingMode.HALF_UP);
        BigDecimal ontimeScore = ontimeRate.multiply(BigDecimal.valueOf(30));

        // 逾期频率 (25%) - 逾期越少分越高
        BigDecimal overdueRate = BigDecimal.valueOf(ri.getOverdueCount())
                .divide(BigDecimal.valueOf(completed), 4, RoundingMode.HALF_UP);
        BigDecimal overdueScore = BigDecimal.ONE.subtract(overdueRate)
                .multiply(BigDecimal.valueOf(25));

        // 最大逾期天数 (20%) - 0天=满分, 30天以上=0分
        int maxDays = ri.getMaxOverdueDays() == null ? 0 : ri.getMaxOverdueDays();
        BigDecimal maxDaysScore;
        if (maxDays == 0) {
            maxDaysScore = BigDecimal.valueOf(20);
        } else if (maxDays >= 30) {
            maxDaysScore = BigDecimal.ZERO;
        } else {
            maxDaysScore = BigDecimal.valueOf(20)
                    .multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(maxDays).divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP)));
        }

        // 当前逾期天数 (25%)
        int currentDays = ri.getCurrentOverdueDays() == null ? 0 : ri.getCurrentOverdueDays();
        BigDecimal currentScore;
        if (currentDays == 0) {
            currentScore = BigDecimal.valueOf(25);
        } else if (currentDays >= 60) {
            currentScore = BigDecimal.ZERO;
        } else {
            currentScore = BigDecimal.valueOf(25)
                    .multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(currentDays).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP)));
        }

        BigDecimal total = ontimeScore.add(overdueScore).add(maxDaysScore).add(currentScore)
                .setScale(1, RoundingMode.HALF_UP);

        context.setRepaymentScore(total);

        // 生成评语
        StringBuilder comment = new StringBuilder();
        comment.append("已完成").append(ri.getCompletedTerms()).append("期，");
        comment.append("准时").append(ri.getOntimeCount()).append("次，");
        comment.append("逾期").append(ri.getOverdueCount()).append("次，");
        if (maxDays > 0) {
            comment.append("最大逾期").append(maxDays).append("天，");
        }
        if (currentDays > 0) {
            comment.append("当前逾期").append(currentDays).append("天。");
        } else {
            comment.append("当前无逾期。");
        }
        context.setRepaymentComment(comment.toString());

        return StepResult.success("还款评分=" + total + "，" + comment);
    }
}
