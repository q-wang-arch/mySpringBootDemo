package org.example.springbootdemo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springbootdemo.agent.AnalysisContext;
import org.example.springbootdemo.dto.BorrowerData;
import org.example.springbootdemo.entity.Borrower;
import org.example.springbootdemo.entity.Loan;
import org.example.springbootdemo.entity.Report;
import org.example.springbootdemo.mapper.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 报告生成服务
 */
@Service
public class ReportService {

    @Autowired
    private ReportMapper reportMapper;

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据分析上下文生成报告
     */
    public Report generateReport(AnalysisContext context) {
        BorrowerData data = context.getBorrowerData();
        Borrower borrower = context.getBorrower();
        Loan loan = context.getLoan();

        Report report = new Report();
        report.setReportId("RPT" + System.currentTimeMillis());
        report.setTaskId(context.getTaskId());
        report.setBorrowerId(data.getBorrowerId());
        report.setBorrowerName(data.getBorrowerName());
        report.setLoanId(data.getLoanId());
        report.setReportPeriod(data.getReportPeriod());

        report.setRepaymentScore(context.getRepaymentScore());
        report.setFinancialScore(context.getFinancialScore());
        report.setRiskSignalDeduction(context.getRiskSignalDeduction());
        report.setRiskScore(context.getRiskScore());
        report.setRiskGrade(context.getRiskGrade());
        report.setPreviousGrade(context.getPreviousGrade());

        // 生成结论
        String conclusion = generateConclusion(context);
        report.setConclusion(conclusion);
        context.setConclusion(conclusion);

        // 生成建议
        List<String> suggestions = generateSuggestions(context.getRiskGrade());
        report.setSuggestions(String.join(";", suggestions));
        context.setSuggestions(suggestions);

        // 完整报告内容（JSON）
        Map<String, Object> content = buildReportContent(context);
        try {
            report.setReportContent(objectMapper.writeValueAsString(content));
        } catch (Exception e) {
            report.setReportContent("{}");
        }

        report.setCreateTime(LocalDateTime.now());
        reportMapper.insert(report);

        context.setReportId(report.getReportId());
        return report;
    }

    /**
     * 生成总体结论
     */
    private String generateConclusion(AnalysisContext context) {
        String grade = context.getRiskGrade();
        BorrowerData data = context.getBorrowerData();

        StringBuilder sb = new StringBuilder();
        sb.append("借款人[").append(data.getBorrowerName()).append("]");
        sb.append("综合风险评分").append(context.getRiskScore()).append("分，");
        sb.append("风险评级").append(grade).append("级。");

        switch (grade) {
            case "A":
                sb.append("整体风险极低，还款行为优秀，财务状况稳健。");
                break;
            case "B":
                sb.append("整体风险较低，还款行为良好，财务状况基本稳定。");
                break;
            case "C":
                sb.append("整体风险一般，需关注部分指标变化。");
                break;
            case "D":
                sb.append("整体风险较高，存在明显风险信号，需采取措施。");
                break;
            case "E":
                sb.append("整体风险极高，需立即采取保全措施。");
                break;
        }

        return sb.toString();
    }

    /**
     * 按评级生成建议措施
     */
    private List<String> generateSuggestions(String grade) {
        List<String> suggestions = new ArrayList<>();
        switch (grade) {
            case "A":
                suggestions.add("维持常规贷后监控频率");
                suggestions.add("继续保持良好还款关系");
                break;
            case "B":
                suggestions.add("维持常规贷后监控频率");
                suggestions.add("关注收入下降趋势");
                break;
            case "C":
                suggestions.add("加强贷后监控频率");
                suggestions.add("关注财务状况变化");
                suggestions.add("考虑要求补充财务资料");
                break;
            case "D":
                suggestions.add("约谈借款人了解经营状况");
                suggestions.add("要求追加担保或增信措施");
                suggestions.add("考虑调整贷款分类为次级");
                break;
            case "E":
                suggestions.add("立即启动保全措施");
                suggestions.add("启动法律追偿程序");
                suggestions.add("调整贷款分类为损失");
                break;
        }
        return suggestions;
    }

    /**
     * 构建完整报告内容
     */
    private Map<String, Object> buildReportContent(AnalysisContext context) {
        BorrowerData data = context.getBorrowerData();
        Loan loan = context.getLoan();

        Map<String, Object> content = new LinkedHashMap<>();

        // 一、借款人概况
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("borrowerName", data.getBorrowerName());
        overview.put("borrowerId", data.getBorrowerId());
        overview.put("loanId", data.getLoanId());
        overview.put("loanAmount", loan.getLoanAmount());
        overview.put("loanBalance", loan.getLoanBalance());
        overview.put("loanType", loan.getLoanType());
        overview.put("loanStartDate", loan.getLoanStartDate());
        overview.put("loanEndDate", loan.getLoanEndDate());
        content.put("borrowerOverview", overview);

        // 二、还款行为分析
        Map<String, Object> repayment = new LinkedHashMap<>();
        BorrowerData.RepaymentInfo ri = data.getRepaymentInfo();
        repayment.put("totalTerms", ri.getTotalTerms());
        repayment.put("completedTerms", ri.getCompletedTerms());
        repayment.put("ontimeCount", ri.getOntimeCount());
        repayment.put("overdueCount", ri.getOverdueCount());
        repayment.put("maxOverdueDays", ri.getMaxOverdueDays());
        repayment.put("currentOverdueDays", ri.getCurrentOverdueDays());
        repayment.put("score", context.getRepaymentScore());
        repayment.put("comment", context.getRepaymentComment());
        content.put("repaymentAnalysis", repayment);

        // 三、财务状况分析
        Map<String, Object> financial = new LinkedHashMap<>();
        BorrowerData.FinancialInfo fi = data.getFinancialInfo();
        financial.put("monthlyIncome", fi.getMonthlyIncome());
        financial.put("incomeChange", fi.getMonthlyIncomeChange());
        financial.put("debtRatio", fi.getDebtRatio());
        financial.put("debtRatioChange", fi.getDebtRatioChange());
        financial.put("cashFlowStatus", fi.getCashFlowStatus());
        financial.put("score", context.getFinancialScore());
        financial.put("comment", context.getFinancialComment());
        content.put("financialAnalysis", financial);

        // 四、风险信号
        Map<String, Object> signals = new LinkedHashMap<>();
        BorrowerData.RiskSignals rs = data.getRiskSignals();
        signals.put("multiLending", rs.getMultiLending());
        signals.put("guaranteeChainAbnormal", rs.getGuaranteeChainAbnormal());
        signals.put("litigationRecord", rs.getLitigationRecord());
        signals.put("businessAbnormal", rs.getBusinessAbnormal());
        signals.put("assetTransfer", rs.getAssetTransfer());
        signals.put("deduction", context.getRiskSignalDeduction());
        signals.put("comment", context.getRiskSignalComment());
        content.put("riskSignals", signals);

        // 五、外部数据
        Map<String, Object> external = new LinkedHashMap<>();
        if (data.getExternalData() != null) {
            external.put("creditScore", data.getExternalData().getCreditScore());
            external.put("creditScoreChange", data.getExternalData().getCreditScoreChange());
            external.put("courtFilingCount", data.getExternalData().getCourtFilingCount());
            external.put("taxArrears", data.getExternalData().getTaxArrears());
        }
        content.put("externalData", external);

        // 六、风险评级
        Map<String, Object> rating = new LinkedHashMap<>();
        rating.put("riskScore", context.getRiskScore());
        rating.put("riskGrade", context.getRiskGrade());
        rating.put("previousGrade", context.getPreviousGrade());
        rating.put("conclusion", context.getConclusion());
        rating.put("suggestions", context.getSuggestions());
        content.put("riskRating", rating);

        return content;
    }
}
