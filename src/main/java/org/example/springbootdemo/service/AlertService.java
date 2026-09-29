package org.example.springbootdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.springbootdemo.agent.AnalysisContext;
import org.example.springbootdemo.agent.steps.ScoringStep;
import org.example.springbootdemo.dto.BorrowerData;
import org.example.springbootdemo.entity.Alert;
import org.example.springbootdemo.entity.AlertRule;
import org.example.springbootdemo.mapper.AlertMapper;
import org.example.springbootdemo.mapper.AlertRuleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    @Autowired
    private AlertRuleMapper alertRuleMapper;

    @Autowired
    private AlertMapper alertMapper;

    @Autowired
    private ScoringStep scoringStep;

    /** 规则缓存（线程安全），避免每次任务都查库 */
    private volatile List<AlertRule> ruleCache = new CopyOnWriteArrayList<>();

    /**
     * 规则缓存是否已成功加载。
     * 为 false 表示启动时未能从数据库读到规则——应用仍可运行，但**预警匹配会静默失效**，
     * 每次分析都不会产生任何预警工单，必须排查。
     */
    private volatile boolean ruleCacheReady = false;

    /** 是否已经就"规则缓存不可用"告警过，避免批量分析时刷屏 */
    private volatile boolean staleWarned = false;

    /**
     * 启动时加载规则缓存。
     *
     * <p>这里刻意<b>不</b>让异常向上抛出：数据库暂时不可用不应该导致整个应用起不来。
     * 代价是预警功能会静默降级，因此失败时打一条醒目的 ERROR 日志，方便第一时间发现。
     */
    @PostConstruct
    public void loadRules() {
        try {
            refreshRules();
            log.info("[预警规则] 规则缓存加载完成，生效规则 {} 条", ruleCache.size());
        } catch (Exception e) {
            ruleCache = new CopyOnWriteArrayList<>();
            ruleCacheReady = false;
            log.error("================================================================");
            log.error("[预警规则] 规则缓存加载失败！应用将继续启动，但**预警功能已失效**：");
            log.error("[预警规则] 后续所有分析任务都不会产生预警工单，直到规则被成功加载。");
            log.error("[预警规则] 失败原因: {}", e.toString());
            log.error("[预警规则] 请检查数据库连接与 alert_rule 表，修复后重启应用，");
            log.error("[预警规则] 或调用 AlertService.refreshRules() 重载规则。");
            log.error("================================================================");
        }
    }

    /** 刷新规则缓存（规则变更时调用；也可用于启动失败后的手动重载） */
    public void refreshRules() {
        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRule::getEnabled, 1);
        ruleCache = new CopyOnWriteArrayList<>(alertRuleMapper.selectList(wrapper));
        ruleCacheReady = true;
        staleWarned = false;
    }

    /** 当前生效的规则条数（供健康检查/排障使用） */
    public int getRuleCount() {
        return ruleCache.size();
    }

    /** 规则缓存是否已成功加载（供健康检查/排障使用） */
    public boolean isRuleCacheReady() {
        return ruleCacheReady;
    }

    /**
     * 匹配预警规则，触发预警
     */
    public void checkAndTriggerAlerts(AnalysisContext context) {
        List<AlertRule> rules = ruleCache;

        if (rules.isEmpty()) {
            // 规则为空有两种可能：启动时加载失败（故障），或库里确实没有启用的规则（配置问题）。
            // 两者都意味着预警不会触发，只在首次出现时告警一次，避免批量分析刷屏。
            if (!staleWarned) {
                staleWarned = true;
                if (ruleCacheReady) {
                    log.warn("[预警规则] 当前没有任何启用的预警规则，任务 {} 不会产生预警工单；"
                            + "请确认 alert_rule 表中的规则已置 enabled=1。", context.getTaskId());
                } else {
                    log.warn("[预警规则] 规则缓存不可用（启动时加载失败），任务 {} 未执行预警匹配；"
                            + "预警功能处于失效状态，详见启动日志中的 ERROR 详情。", context.getTaskId());
                }
            }
            return;
        }

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
