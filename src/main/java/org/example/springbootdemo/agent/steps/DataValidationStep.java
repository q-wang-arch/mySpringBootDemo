package org.example.springbootdemo.agent.steps;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.springbootdemo.agent.AgentStep;
import org.example.springbootdemo.agent.AnalysisContext;
import org.example.springbootdemo.agent.StepResult;
import org.example.springbootdemo.dto.BorrowerData;
import org.example.springbootdemo.entity.Borrower;
import org.example.springbootdemo.entity.Loan;
import org.example.springbootdemo.mapper.BorrowerMapper;
import org.example.springbootdemo.mapper.LoanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Step1: 数据完整性校验
 */
@Component
public class DataValidationStep implements AgentStep {

    @Autowired
    private BorrowerMapper borrowerMapper;

    @Autowired
    private LoanMapper loanMapper;

    @Override
    public String getStepName() { return "数据完整性校验"; }

    @Override
    public int getStepOrder() { return 1; }

    @Override
    public StepResult execute(AnalysisContext context) {
        BorrowerData data = context.getBorrowerData();

        // 查询借款人
        LambdaQueryWrapper<Borrower> bw = new LambdaQueryWrapper<>();
        bw.eq(Borrower::getBorrowerId, data.getBorrowerId());
        Borrower borrower = borrowerMapper.selectOne(bw);
        if (borrower == null) {
            return StepResult.fail("借款人不存在: " + data.getBorrowerId());
        }
        context.setBorrower(borrower);

        // 查询贷款合同
        LambdaQueryWrapper<Loan> lw = new LambdaQueryWrapper<>();
        lw.eq(Loan::getLoanId, data.getLoanId());
        Loan loan = loanMapper.selectOne(lw);
        if (loan == null) {
            return StepResult.fail("贷款合同不存在: " + data.getLoanId());
        }
        context.setLoan(loan);

        return StepResult.success("数据校验通过，借款人=" + borrower.getBorrowerName() + "，贷款=" + loan.getLoanId());
    }
}
