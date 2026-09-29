package org.example.springbootdemo.auth;

/**
 * 角色常量，与 docs/03-接口设计文档.md 中的角色定义保持一致。
 *
 * <p>用常量而不是散落的字符串，避免拼写错误导致"注解写了但永远不生效"这类隐蔽问题。
 */
public final class Roles {

    /** 风险管理员：查看/导出报告、处置预警 */
    public static final String RISK_ADMIN = "RISK_ADMIN";

    /** 风险审批人：审批预警处置结果、查看全部报告 */
    public static final String RISK_APPROVER = "RISK_APPROVER";

    /** 系统管理员：配置规则模板、管理用户权限 */
    public static final String SYS_ADMIN = "SYS_ADMIN";

    /** 接口调用方：源头系统，推送贷后数据、获取分析结果 */
    public static final String API_CLIENT = "API_CLIENT";

    private Roles() {
    }
}
