package org.example.springbootdemo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 接口鉴权配置（前缀 {@code app.auth}）。
 *
 * <p><b>令牌值一律通过环境变量注入，绝不写入仓库</b>——原因和数据库密码一样，
 * 公开仓库里一旦提交就永久留在 git 历史中。配置形如：
 *
 * <pre>
 * app.auth.enabled=${APP_AUTH_ENABLED:true}
 * app.auth.users[0].username=risk-admin
 * app.auth.users[0].roles=RISK_ADMIN
 * app.auth.users[0].token=${APP_TOKEN_RISK_ADMIN:}
 * </pre>
 *
 * <p>未配置令牌的用户会被直接跳过（视为该账号不可用），因此"漏配环境变量"只会导致
 * 这个账号用不了，而不会退化成一个空令牌就能通过的后门。
 *
 * <p>若鉴权开启却没有任何有效令牌，启动时会打印 ERROR 日志并使所有 {@code /api/**}
 * 请求返回 401——采用"失败即拒绝"而不是"失败即放行"的策略。
 */
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private static final Logger log = LoggerFactory.getLogger(AuthProperties.class);

    /** 鉴权总开关；关闭后 /api/** 不做任何校验（仅供本地开发） */
    private boolean enabled = true;

    /** 免鉴权路径，常用于探活接口 */
    private List<String> excludePaths = new ArrayList<>(Collections.singletonList("/api/hello/**"));

    /** 令牌用户清单 */
    private List<TokenUser> users = new ArrayList<>();

    /** 令牌 -> 用户 索引，构建后只读 */
    private volatile Map<String, TokenUser> tokenIndex;

    @PostConstruct
    public void init() {
        buildIndex();
    }

    /**
     * 构建令牌索引。幂等且线程安全，兜底处理 @PostConstruct 未被调用的极端情况。
     */
    private synchronized void buildIndex() {
        if (tokenIndex != null) {
            return;
        }

        Map<String, TokenUser> index = new LinkedHashMap<>();
        for (TokenUser user : users) {
            String name = user.getUsername() == null ? "(未命名)" : user.getUsername();
            if (!user.isEnabled()) {
                log.info("[auth] 用户 {} 已禁用，跳过", name);
                continue;
            }
            String token = user.getToken() == null ? "" : user.getToken().trim();
            if (token.isEmpty()) {
                log.warn("[auth] 用户 {} 未配置令牌，已跳过（对应环境变量未设置）", name);
                continue;
            }
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                log.warn("[auth] 用户 {} 未配置角色，已跳过", name);
                continue;
            }
            TokenUser previous = index.put(token, user);
            if (previous != null) {
                log.error("[auth] 令牌冲突：用户 {} 与 {} 配置了相同的令牌，请检查环境变量", previous.getUsername(), name);
            }
        }

        this.tokenIndex = index;

        if (!enabled) {
            log.warn("[auth] 接口鉴权已关闭（app.auth.enabled=false），/api/** 不做任何校验，请勿在 UAT/生产环境这样配置");
        } else if (index.isEmpty()) {
            log.error("[auth] 鉴权已开启但没有任何有效令牌，/api/** 的所有请求都会返回 401。"
                    + "请设置环境变量 APP_TOKEN_RISK_ADMIN 等并重启");
        } else {
            String summary = index.values().stream()
                    .map(user -> user.getUsername() + user.getRoles())
                    .collect(Collectors.joining(", "));
            log.info("[auth] 接口鉴权已开启：免鉴权路径={}，令牌用户={}", excludePaths, summary);
        }
    }

    /**
     * 按令牌匹配用户；未配置、被禁用或令牌不存在时返回 null。
     */
    public TokenUser match(String token) {
        if (token == null) {
            return null;
        }
        buildIndex();
        TokenUser user = tokenIndex.get(token);
        return user != null && user.isEnabled() ? user : null;
    }

    /** 有效令牌用户数，便于启动自检 */
    public int getEffectiveUserCount() {
        buildIndex();
        return tokenIndex.size();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public List<TokenUser> getUsers() {
        return users;
    }

    public void setUsers(List<TokenUser> users) {
        this.users = users;
    }

    /**
     * 一个令牌用户：凭令牌识别身份，凭角色判定权限。
     */
    public static class TokenUser {

        private String username;
        private String token;
        private String description;
        private boolean enabled = true;
        private List<String> roles = new ArrayList<>();

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
}
