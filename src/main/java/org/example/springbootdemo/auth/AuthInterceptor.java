package org.example.springbootdemo.auth;

import org.example.springbootdemo.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 认证拦截器：校验访问令牌，确认"你是谁"。
 *
 * <p>职责边界很窄——只做认证，不判断权限。校验通过后把身份写入 {@link AuthContext}，
 * 授权由 {@link RoleInterceptor} 依 {@link RequireRoles} 注解决定，两者失败分别返回 401 / 403。
 *
 * <p>支持的令牌传递方式（二选一）：
 * <ul>
 *   <li>{@code Authorization: Bearer <token>} —— 前端与人工调用</li>
 *   <li>{@code X-Auth-Token: <token>} —— 源头系统对接时更省事</li>
 * </ul>
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private static final String BEARER_PREFIX = "bearer ";
    private static final String TOKEN_HEADER = "X-Auth-Token";

    private final AuthProperties authProperties;

    public AuthInterceptor(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 总开关关闭时直接放行（供本地开发使用，UAT 环境必须为开启状态）
        if (!authProperties.isEnabled()) {
            return true;
        }

        // 浏览器 CORS 预检请求不会携带自定义请求头，放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String token = resolveToken(request);
        AuthProperties.TokenUser user = authProperties.match(token);
        if (user == null) {
            String message = (token == null || token.isEmpty())
                    ? "未提供访问令牌，请通过请求头 Authorization: Bearer <token> 或 X-Auth-Token 携带"
                    : "访问令牌无效";
            log.warn("[auth] 401 {} {} - {}", request.getMethod(), request.getRequestURI(), message);
            AuthResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED, message);
            return false;
        }

        AuthContext.set(new AuthPrincipal(user.getUsername(), user.getRoles()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        // 无论成功失败都要清理，否则线程池复用线程会导致身份串号
        AuthContext.clear();
    }

    /**
     * 从请求头中提取令牌，取不到返回 null。
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && !authorization.trim().isEmpty()) {
            String value = authorization.trim();
            if (value.length() > BEARER_PREFIX.length()
                    && value.substring(0, BEARER_PREFIX.length()).equalsIgnoreCase(BEARER_PREFIX)) {
                return value.substring(BEARER_PREFIX.length()).trim();
            }
            // 兼容直接放令牌、不写 Bearer 前缀的写法
            return value;
        }
        String apiToken = request.getHeader(TOKEN_HEADER);
        return apiToken == null ? null : apiToken.trim();
    }
}
