package org.example.springbootdemo.dto;

import java.math.BigDecimal;

/**
 * 源头系统推送的贷后数据
 */
public class BorrowerData {

    private String borrowerId;
    private String borrowerName;
    private String loanId;
    private String reportPeriod;

    private LoanInfo loanInfo;
    private RepaymentInfo repaymentInfo;
    private FinancialInfo financialInfo;
    private RiskSignals riskSignals;
    private ExternalData externalData;

    public static class LoanInfo {
        private BigDecimal loanAmount;
        private BigDecimal loanBalance;
        private String loanType;
        private String loanStartDate;
        private String loanEndDate;
        private BigDecimal interestRate;

        public BigDecimal getLoanAmount() { return loanAmount; }
        public void setLoanAmount(BigDecimal loanAmount) { this.loanAmount = loanAmount; }
        public BigDecimal getLoanBalance() { return loanBalance; }
        public void setLoanBalance(BigDecimal loanBalance) { this.loanBalance = loanBalance; }
        public String getLoanType() { return loanType; }
        public void setLoanType(String loanType) { this.loanType = loanType; }
        public String getLoanStartDate() { return loanStartDate; }
        public void setLoanStartDate(String loanStartDate) { this.loanStartDate = loanStartDate; }
        public String getLoanEndDate() { return loanEndDate; }
        public void setLoanEndDate(String loanEndDate) { this.loanEndDate = loanEndDate; }
        public BigDecimal getInterestRate() { return interestRate; }
        public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    }

    public static class RepaymentInfo {
        private Integer totalTerms;
        private Integer completedTerms;
        private Integer ontimeCount;
        private Integer overdueCount;
        private Integer maxOverdueDays;
        private Integer currentOverdueDays;

        public Integer getTotalTerms() { return totalTerms; }
        public void setTotalTerms(Integer totalTerms) { this.totalTerms = totalTerms; }
        public Integer getCompletedTerms() { return completedTerms; }
        public void setCompletedTerms(Integer completedTerms) { this.completedTerms = completedTerms; }
        public Integer getOntimeCount() { return ontimeCount; }
        public void setOntimeCount(Integer ontimeCount) { this.ontimeCount = ontimeCount; }
        public Integer getOverdueCount() { return overdueCount; }
        public void setOverdueCount(Integer overdueCount) { this.overdueCount = overdueCount; }
        public Integer getMaxOverdueDays() { return maxOverdueDays; }
        public void setMaxOverdueDays(Integer maxOverdueDays) { this.maxOverdueDays = maxOverdueDays; }
        public Integer getCurrentOverdueDays() { return currentOverdueDays; }
        public void setCurrentOverdueDays(Integer currentOverdueDays) { this.currentOverdueDays = currentOverdueDays; }
    }

    public static class FinancialInfo {
        private BigDecimal monthlyIncome;
        private BigDecimal monthlyIncomeChange;
        private BigDecimal debtRatio;
        private BigDecimal debtRatioChange;
        private String cashFlowStatus;

        public BigDecimal getMonthlyIncome() { return monthlyIncome; }
        public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
        public BigDecimal getMonthlyIncomeChange() { return monthlyIncomeChange; }
        public void setMonthlyIncomeChange(BigDecimal monthlyIncomeChange) { this.monthlyIncomeChange = monthlyIncomeChange; }
        public BigDecimal getDebtRatio() { return debtRatio; }
        public void setDebtRatio(BigDecimal debtRatio) { this.debtRatio = debtRatio; }
        public BigDecimal getDebtRatioChange() { return debtRatioChange; }
        public void setDebtRatioChange(BigDecimal debtRatioChange) { this.debtRatioChange = debtRatioChange; }
        public String getCashFlowStatus() { return cashFlowStatus; }
        public void setCashFlowStatus(String cashFlowStatus) { this.cashFlowStatus = cashFlowStatus; }
    }

    public static class RiskSignals {
        private Boolean multiLending;
        private Boolean guaranteeChainAbnormal;
        private Boolean litigationRecord;
        private Boolean businessAbnormal;
        private Boolean assetTransfer;

        public Boolean getMultiLending() { return multiLending; }
        public void setMultiLending(Boolean multiLending) { this.multiLending = multiLending; }
        public Boolean getGuaranteeChainAbnormal() { return guaranteeChainAbnormal; }
        public void setGuaranteeChainAbnormal(Boolean guaranteeChainAbnormal) { this.guaranteeChainAbnormal = guaranteeChainAbnormal; }
        public Boolean getLitigationRecord() { return litigationRecord; }
        public void setLitigationRecord(Boolean litigationRecord) { this.litigationRecord = litigationRecord; }
        public Boolean getBusinessAbnormal() { return businessAbnormal; }
        public void setBusinessAbnormal(Boolean businessAbnormal) { this.businessAbnormal = businessAbnormal; }
        public Boolean getAssetTransfer() { return assetTransfer; }
        public void setAssetTransfer(Boolean assetTransfer) { this.assetTransfer = assetTransfer; }
    }

    public static class ExternalData {
        private Integer creditScore;
        private Integer creditScoreChange;
        private Integer courtFilingCount;
        private Boolean taxArrears;

        public Integer getCreditScore() { return creditScore; }
        public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
        public Integer getCreditScoreChange() { return creditScoreChange; }
        public void setCreditScoreChange(Integer creditScoreChange) { this.creditScoreChange = creditScoreChange; }
        public Integer getCourtFilingCount() { return courtFilingCount; }
        public void setCourtFilingCount(Integer courtFilingCount) { this.courtFilingCount = courtFilingCount; }
        public Boolean getTaxArrears() { return taxArrears; }
        public void setTaxArrears(Boolean taxArrears) { this.taxArrears = taxArrears; }
    }

    public String getBorrowerId() { return borrowerId; }
    public void setBorrowerId(String borrowerId) { this.borrowerId = borrowerId; }
    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
    public String getLoanId() { return loanId; }
    public void setLoanId(String loanId) { this.loanId = loanId; }
    public String getReportPeriod() { return reportPeriod; }
    public void setReportPeriod(String reportPeriod) { this.reportPeriod = reportPeriod; }
    public LoanInfo getLoanInfo() { return loanInfo; }
    public void setLoanInfo(LoanInfo loanInfo) { this.loanInfo = loanInfo; }
    public RepaymentInfo getRepaymentInfo() { return repaymentInfo; }
    public void setRepaymentInfo(RepaymentInfo repaymentInfo) { this.repaymentInfo = repaymentInfo; }
    public FinancialInfo getFinancialInfo() { return financialInfo; }
    public void setFinancialInfo(FinancialInfo financialInfo) { this.financialInfo = financialInfo; }
    public RiskSignals getRiskSignals() { return riskSignals; }
    public void setRiskSignals(RiskSignals riskSignals) { this.riskSignals = riskSignals; }
    public ExternalData getExternalData() { return externalData; }
    public void setExternalData(ExternalData externalData) { this.externalData = externalData; }
}
