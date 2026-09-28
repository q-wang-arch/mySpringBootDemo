package org.example.springbootdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.springbootdemo.agent.AnalysisContext;
import org.example.springbootdemo.agent.steps.ScoringStep;
import org.example.springbootdemo.dto.BorrowerData;
import org.example.springbootdemo.entity.Alert;
import org.example.springbootdemo.entity.AlertRule;
import org.example.springbootdemo.mapper.AlertMapper;
import org.example.springbootdemo.mapper.AlertRuleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 预警匹配与推送服务
 */
@Service
public class AlertService {

    @Autowired
    private AlertRuleMapper alertRuleMapper;

    @Autowired
    private AlertMapper alertMapper;

    @Autowired
    private ScoringStep scoringStep;

    /** 规则缓存（线程安全），避免每次任务都查库 */
    private volatile List<AlertRule> ruleCache = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void loadRules() {
        refreshRules();
    }

    /** 刷新规则缓存（规则变更时调用） */
    public void refreshRules() {
        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRule::getEnabled, 1);
        ruleCache = new CopyOnWriteArrayList<>(alertRuleMapper.selectList(wrapper));
    }

    /**
     * 匹配预警规则，触发预警
     */
    public void checkAndTriggerAlerts(AnalysisContext context) {
        List<AlertRule> rules = ruleCache;

        BorrowerData data = context.getBorrowerData();
        String grade = context.getRiskGrade();

        for (AlertRule rule : rules) {
            boolean triggered = evaluateRule(rule, context, data, grade);
            if (triggered) {
                createAlert(context, rule, data);
            }
        }
    }

    /**
     * 评估单条规则是否触发
     */
    private boolean evaluateRule(AlertRule rule, AnalysisContext context, BorrowerData data, String grade) {
        String ruleId = rule.getRuleId();

        switch (ruleId) {
            case "RULE-001": // 评级下降 >= 2级
                if (context.getPreviousGrade() != null) {
                    int drop = scoringStep.getGradeDrop(context.getPreviousGrade(), grade);
                    return drop >= 2;
                }
                return false;

            case "RULE-002": // 评级为D
                return "D".equals(grade);

            case "RULE-003": // 评级为E
                return "E".equals(grade);

            case "RULE-004": // 逾期超限 > 30天
                return data.getRepaymentInfo().getCurrentOverdueDays() != null
                        && data.getRepaymentInfo().getCurrentOverdueDays() > 30;

            case "RULE-005": // 多头借贷
                return Boolean.TRUE.equals(data.getRiskSignals().getMultiLending());

            case "RULE-006": // 收入下降 > 20%
                return data.getFinancialInfo().getMonthlyIncomeChange() != null
                        && data.getFinancialInfo().getMonthlyIncomeChange().doubleValue() < -0.2;

            case "RULE-007": // 负债率 > 60%
                return data.getFinancialInfo().getDebtRatio() != null
                        && data.getFinancialInfo().getDebtRatio().doubleValue() > 0.6;

            default:
                return false;
        }
    }

    /**
     * 创建预警工单
     */
    private void createAlert(AnalysisContext context, AlertRule rule, BorrowerData data) {
        Alert alert = new Alert();
        alert.setAlertId("ALT" + System.currentTimeMillis() + (int)(Math.random() * 1000));
        alert.setTaskId(context.getTaskId());
        alert.setBorrowerId(data.getBorrowerId());
        alert.setBorrowerName(data.getBorrowerName());
        alert.setRuleId(rule.getRuleId());
        alert.setRuleName(rule.getRuleName());
        alert.setLevel(rule.getLevel());
        alert.setStatus("TRIGGERED");
        alert.setCreateTime(LocalDateTime.now());

        alertMapper.insert(alert);
        context.getAlerts().add(alert);

        System.out.println("[预警触发] " + rule.getRuleName()
                + " | 借款人=" + data.getBorrowerName()
                + " | 级别=" + rule.getLevel()
                + " | 通知方式=" + rule.getNotifyMethods());
    }
}
