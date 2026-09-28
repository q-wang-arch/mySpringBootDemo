package org.example.springbootdemo.agent.steps;

import org.example.springbootdemo.agent.AgentStep;
import org.example.springbootdemo.agent.AnalysisContext;
import org.example.springbootdemo.agent.StepResult;
import org.example.springbootdemo.dto.BorrowerData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Step4: 风险信号识别
 * 多头借贷-15 / 担保链异常-10 / 诉讼记录-10 / 经营异常-10 / 资产转移-15
 */
@Component
public class RiskSignalStep implements AgentStep {

    @Override
    public String getStepName() { return "风险信号识别"; }

    @Override
    public int getStepOrder() { return 4; }

    @Override
    public StepResult execute(AnalysisContext context) {
        BorrowerData.RiskSignals rs = context.getBorrowerData().getRiskSignals();

        BigDecimal deduction = BigDecimal.ZERO;
        StringBuilder signals = new StringBuilder();

        if (Boolean.TRUE.equals(rs.getMultiLending())) {
            deduction = deduction.add(BigDecimal.valueOf(15));
            signals.append("多头借贷、");
        }
        if (Boolean.TRUE.equals(rs.getGuaranteeChainAbnormal())) {
            deduction = deduction.add(BigDecimal.valueOf(10));
            signals.append("担保链异常、");
        }
        if (Boolean.TRUE.equals(rs.getLitigationRecord())) {
            deduction = deduction.add(BigDecimal.valueOf(10));
            signals.append("诉讼记录、");
        }
        if (Boolean.TRUE.equals(rs.getBusinessAbnormal())) {
            deduction = deduction.add(BigDecimal.valueOf(10));
            signals.append("经营异常、");
        }
        if (Boolean.TRUE.equals(rs.getAssetTransfer())) {
            deduction = deduction.add(BigDecimal.valueOf(15));
            signals.append("资产转移、");
        }

        context.setRiskSignalDeduction(deduction);

        String comment;
        if (signals.length() == 0) {
            comment = "未发现异常风险信号";
        } else {
            comment = "发现风险信号: " + signals.substring(0, signals.length() - 1) + "，扣分" + deduction;
        }
        context.setRiskSignalComment(comment);

        return StepResult.success("风险信号扣分=" + deduction + "，" + comment);
    }
}
