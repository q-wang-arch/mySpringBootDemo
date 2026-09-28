package org.example.springbootdemo.agent;

/**
 * 智能体步骤接口
 */
public interface AgentStep {

    /** 步骤名称 */
    String getStepName();

    /** 步骤序号 */
    int getStepOrder();

    /** 执行步骤 */
    StepResult execute(AnalysisContext context);
}
