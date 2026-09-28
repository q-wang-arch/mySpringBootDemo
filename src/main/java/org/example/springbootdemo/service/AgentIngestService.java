package org.example.springbootdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.springbootdemo.dto.BorrowerData;
import org.example.springbootdemo.entity.AnalysisTask;
import org.example.springbootdemo.entity.Borrower;
import org.example.springbootdemo.entity.Loan;
import org.example.springbootdemo.agent.AgentEngine;
import org.example.springbootdemo.mapper.AnalysisTaskMapper;
import org.example.springbootdemo.mapper.BorrowerMapper;
import org.example.springbootdemo.mapper.LoanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 数据接入服务
 * 接收源头数据 -> 校验 -> 入库 -> 创建分析任务
 */
@Service
public class AgentIngestService {

    @Autowired
    private BorrowerMapper borrowerMapper;

    @Autowired
    private LoanMapper loanMapper;

    @Autowired
    private AnalysisTaskMapper analysisTaskMapper;

    @Autowired
    private AgentEngine agentEngine;

    @Autowired
    @Qualifier("agentExecutor")
    private ThreadPoolTaskExecutor agentExecutor;

    /**
     * 数据接入：校验 -> 入库 -> 创建任务 -> 异步触发分析
     * 事务只覆盖数据接入部分，分析在线程池异步执行
     */
    @Transactional
    public String ingest(BorrowerData data) {

        // 1. 数据校验
        validateData(data);

        // 2. 去重：同一借款人同一报告期次不能重复提交
        checkDuplicate(data.getBorrowerId(), data.getReportPeriod());

        // 3. 保存/更新借款人信息
        saveOrUpdateBorrower(data);

        // 4. 保存/更新贷款合同信息
        saveOrUpdateLoan(data);

        // 5. 创建分析任务
        AnalysisTask task = createAnalysisTask(data);

        // 6. 异步触发智能体引擎执行分析（不阻塞当前请求）
        final String taskId = task.getTaskId();
        agentExecutor.execute(() -> {
            try {
                agentEngine.execute(taskId, data);
            } catch (Exception e) {
                System.err.println("[智能体执行异常] taskId=" + taskId + ", error=" + e.getMessage());
            }
        });

        return task.getTaskId();
    }

    /**
     * 数据校验
     */
    private void validateData(BorrowerData data) {
        if (data.getBorrowerId() == null || data.getBorrowerId().trim().isEmpty()) {
            throw new IllegalArgumentException("借款人ID不能为空");
        }
        if (data.getBorrowerName() == null || data.getBorrowerName().trim().isEmpty()) {
            throw new IllegalArgumentException("借款人姓名不能为空");
        }
        if (data.getLoanId() == null || data.getLoanId().trim().isEmpty()) {
            throw new IllegalArgumentException("贷款合同编号不能为空");
        }
        if (data.getReportPeriod() == null || data.getReportPeriod().trim().isEmpty()) {
            throw new IllegalArgumentException("报告期次不能为空");
        }
        if (data.getLoanInfo() == null) {
            throw new IllegalArgumentException("贷款信息不能为空");
        }
        if (data.getRepaymentInfo() == null) {
            throw new IllegalArgumentException("还款信息不能为空");
        }
        if (data.getFinancialInfo() == null) {
            throw new IllegalArgumentException("财务信息不能为空");
        }
        if (data.getRiskSignals() == null) {
            throw new IllegalArgumentException("风险信号信息不能为空");
        }
    }

    /**
     * 去重检查
     */
    private void checkDuplicate(String borrowerId, String reportPeriod) {
        LambdaQueryWrapper<AnalysisTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisTask::getBorrowerId, borrowerId)
               .eq(AnalysisTask::getReportPeriod, reportPeriod)
               .ne(AnalysisTask::getStatus, "FAILED");
        Long count = analysisTaskMapper.selectCount(wrapper);
        if (count > 0) {
            throw new IllegalStateException("借款人[" + borrowerId + "]在[" + reportPeriod + "]已有分析任务");
        }
    }

    /**
     * 保存或更新借款人
     */
    private Borrower saveOrUpdateBorrower(BorrowerData data) {
        LambdaQueryWrapper<Borrower> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Borrower::getBorrowerId, data.getBorrowerId());
        Borrower existing = borrowerMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setBorrowerName(data.getBorrowerName());
            borrowerMapper.updateById(existing);
            return existing;
        } else {
            Borrower borrower = new Borrower();
            borrower.setBorrowerId(data.getBorrowerId());
            borrower.setBorrowerName(data.getBorrowerName());
            borrower.setBorrowerType("个人");
            borrowerMapper.insert(borrower);
            return borrower;
        }
    }

    /**
     * 保存或更新贷款合同
     */
    private void saveOrUpdateLoan(BorrowerData data) {
        BorrowerData.LoanInfo loanInfo = data.getLoanInfo();

        LambdaQueryWrapper<Loan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Loan::getLoanId, data.getLoanId());
        Loan existing = loanMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setLoanAmount(loanInfo.getLoanAmount());
            existing.setLoanBalance(loanInfo.getLoanBalance());
            existing.setLoanType(loanInfo.getLoanType());
            existing.setLoanStartDate(LocalDate.parse(loanInfo.getLoanStartDate()));
            existing.setLoanEndDate(LocalDate.parse(loanInfo.getLoanEndDate()));
            existing.setInterestRate(loanInfo.getInterestRate());
            loanMapper.updateById(existing);
        } else {
            Loan loan = new Loan();
            loan.setLoanId(data.getLoanId());
            loan.setBorrowerId(data.getBorrowerId());
            loan.setLoanAmount(loanInfo.getLoanAmount());
            loan.setLoanBalance(loanInfo.getLoanBalance());
            loan.setLoanType(loanInfo.getLoanType());
            loan.setLoanStartDate(LocalDate.parse(loanInfo.getLoanStartDate()));
            loan.setLoanEndDate(LocalDate.parse(loanInfo.getLoanEndDate()));
            loan.setInterestRate(loanInfo.getInterestRate());
            loan.setLoanStatus("NORMAL");
            loanMapper.insert(loan);
        }
    }

    /**
     * 创建分析任务
     */
    private AnalysisTask createAnalysisTask(BorrowerData data) {
        AnalysisTask task = new AnalysisTask();
        task.setTaskId("TASK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        task.setBorrowerId(data.getBorrowerId());
        task.setLoanId(data.getLoanId());
        task.setReportPeriod(data.getReportPeriod());
        task.setStatus("PENDING");
        task.setCurrentStep("等待执行");
        task.setCreateTime(LocalDateTime.now());
        analysisTaskMapper.insert(task);
        return task;
    }
}
