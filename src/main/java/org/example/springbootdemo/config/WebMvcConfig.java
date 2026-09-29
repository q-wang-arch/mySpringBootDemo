package org.example.springbootdemo.config;

import org.example.springbootdemo.auth.AuthInterceptor;
import org.example.springbootdemo.auth.RoleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册鉴权拦截器。
 *
 * <p>顺序很关键：先认证（AuthInterceptor，order=1）再授权（RoleInterceptor，order=2）。
 * 反过来会出现"角色有了但我们还不知道你是谁"的错乱，也就会把本该 401 的请求误报成 403。
 *
 * <p>注意这里没有加 {@code @EnableWebMvc}——那会关掉 Spring Boot 的 MVC 自动配置，
 * 连带静态资源和默认消息转换器一起失效。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RoleInterceptor roleInterceptor;
    private final AuthProperties authProperties;

    public WebMvcConfig(AuthInterceptor authInterceptor,
                        RoleInterceptor roleInterceptor,
                        AuthProperties authProperties) {
        this.authInterceptor = authInterceptor;
        this.roleInterceptor = roleInterceptor;
        this.authProperties = authProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(authProperties.getExcludePaths())
                .order(1);

        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(authProperties.getExcludePaths())
                .order(2);
    }
}
