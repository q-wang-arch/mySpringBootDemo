package org.example.springbootdemo.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明接口所需的角色，由 {@link RoleInterceptor} 在进入 Controller 之前校验。
 *
 * <p>认证（401）由 {@link AuthInterceptor} 负责，本注解只负责授权（403）——
 * 也就是"已确认你是谁，但判断你能不能碰这个接口"。
 *
 * <p>可标注在类上作为默认约束，标注在方法上则针对该方法生效，且<b>方法级优先</b>
 * （方法上有注解时不再叠加类上的注解）。
 *
 * <pre>
 * &#64;RequireRoles({Roles.RISK_ADMIN, Roles.SYS_ADMIN})
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRoles {

    /** 允许访问的角色，满足其中任意一个即放行 */
    String[] value();
}
