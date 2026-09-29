package org.example.springbootdemo.auth;

import org.example.springbootdemo.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 授权拦截器：读取接口上的 {@link RequireRoles} 注解，判断"你能不能碰这个接口"。
 *
 * <p>不满足时返回 403 而不是 401——调用者身份是明确的，只是权限不足。
 * 排查问题时这两个状态码必须分清：401 查令牌，403 查角色配置。
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RoleInterceptor.class);

    private final AuthProperties authProperties;

    public RoleInterceptor(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!authProperties.isEnabled() || !(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        // 方法级注解优先，没有则回退到类级注解
        RequireRoles requireRoles = handlerMethod.getMethodAnnotation(RequireRoles.class);
        if (requireRoles == null) {
            requireRoles = handlerMethod.getBeanType().getAnnotation(RequireRoles.class);
        }
        // 未声明角色约束的接口只要求认证通过
        if (requireRoles == null || requireRoles.value().length == 0) {
            return true;
        }

        AuthPrincipal principal = AuthContext.get();
        if (principal == null) {
            AuthResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED, "未认证");
            return false;
        }

        for (String role : requireRoles.value()) {
            if (principal.hasRole(role)) {
                return true;
            }
        }

        String message = "无权访问该接口：当前身份 " + principal.getUsername()
                + "（角色 " + principal.getRolesText() + "）不具备所需角色 "
                + String.join("/", requireRoles.value());
        log.warn("[auth] 403 {} {} - {}", request.getMethod(), request.getRequestURI(), message);
        AuthResponses.write(response, HttpServletResponse.SC_FORBIDDEN, message);
        return false;
    }
}
