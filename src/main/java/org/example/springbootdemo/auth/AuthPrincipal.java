package org.example.springbootdemo.auth;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 认证通过后的身份信息（谁在调用 + 他有哪些角色）。
 *
 * <p>不可变对象：一旦创建就不再修改，可安全地在一次请求内传递。
 */
public class AuthPrincipal {

    private final String username;
    private final Set<String> roles;

    public AuthPrincipal(String username, Collection<String> roles) {
        this.username = username;
        Set<String> normalized = new LinkedHashSet<>();
        if (roles != null) {
            for (String role : roles) {
                if (role != null && !role.trim().isEmpty()) {
                    normalized.add(role.trim().toUpperCase());
                }
            }
        }
        this.roles = Collections.unmodifiableSet(normalized);
    }

    public String getUsername() {
        return username;
    }

    public Set<String> getRoles() {
        return roles;
    }

    /** 是否具备指定角色（大小写不敏感） */
    public boolean hasRole(String role) {
        return role != null && roles.contains(role.trim().toUpperCase());
    }

    /** 便于日志与错误信息展示 */
    public String getRolesText() {
        return roles.isEmpty() ? "(无角色)" : String.join("/", roles);
    }

    @Override
    public String toString() {
        return username + "[" + getRolesText() + "]";
    }
}
