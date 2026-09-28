package org.example.springbootdemo.agent;

import org.example.springbootdemo.dto.BorrowerData;
import org.example.springbootdemo.entity.Alert;
import org.example.springbootdemo.entity.Borrower;
import org.example.springbootdemo.entity.Loan;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 分析上下文 - 在智能体各步骤间传递数据
 */
public class AnalysisContext {

    private String taskId;
    private BorrowerData borrowerData;
    private Borrower borrower;
    private Loan loan;

    // 各步骤分析结果
    private BigDecimal repaymentScore;
    private String repaymentComment;

    private BigDecimal financialScore;
    private String financialComment;

    private BigDecimal riskSignalDeduction;
    private String riskSignalComment;

    // 最终评分
    private BigDecimal riskScore;
    private String riskGrade;
    private String previousGrade;

    // 报告
    private String reportId;
    private String conclusion;
    private List<String> suggestions;

    // 预警
    private List<Alert> alerts = new ArrayList<>();

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public BorrowerData getBorrowerData() { return borrowerData; }
    public void setBorrowerData(BorrowerData borrowerData) { this.borrowerData = borrowerData; }
    public Borrower getBorrower() { return borrower; }
    public void setBorrower(Borrower borrower) { this.borrower = borrower; }
    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }
    public BigDecimal getRepaymentScore() { return repaymentScore; }
    public void setRepaymentScore(BigDecimal repaymentScore) { this.repaymentScore = repaymentScore; }
    public String getRepaymentComment() { return repaymentComment; }
    public void setRepaymentComment(String repaymentComment) { this.repaymentComment = repaymentComment; }
    public BigDecimal getFinancialScore() { return financialScore; }
    public void setFinancialScore(BigDecimal financialScore) { this.financialScore = financialScore; }
    public String getFinancialComment() { return financialComment; }
    public void setFinancialComment(String financialComment) { this.financialComment = financialComment; }
    public BigDecimal getRiskSignalDeduction() { return riskSignalDeduction; }
    public void setRiskSignalDeduction(BigDecimal riskSignalDeduction) { this.riskSignalDeduction = riskSignalDeduction; }
    public String getRiskSignalComment() { return riskSignalComment; }
    public void setRiskSignalComment(String riskSignalComment) { this.riskSignalComment = riskSignalComment; }
    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }
    public String getRiskGrade() { return riskGrade; }
    public void setRiskGrade(String riskGrade) { this.riskGrade = riskGrade; }
    public String getPreviousGrade() { return previousGrade; }
    public void setPreviousGrade(String previousGrade) { this.previousGrade = previousGrade; }
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    public List<Alert> getAlerts() { return alerts; }
    public void setAlerts(List<Alert> alerts) { this.alerts = alerts; }
}
