package com.tagstory.api.auth;

import com.tagstory.core.domain.user.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PrincipalDetails implements UserDetails, OAuth2User {

    private Long userId;
    private String pendingId;
    private Role role;
    private Map<String, Object> userInfoMap;


    /* 생성자 */
    public PrincipalDetails(Long userId) {
        this.userId = userId;
    }

    public PrincipalDetails(String pendingId) {
        this.pendingId = pendingId;
    }

    public PrincipalDetails(Long userId, Role role) {
        this.userId = userId;
        this.role = role;
    }

    public PrincipalDetails(Long userId, Role role, Map<String, Object> attributes) {
        this.userId = userId;
        this.role = role;
        this.userInfoMap = attributes;
    }

    public PrincipalDetails(String pendingId, Role role, Map<String, Object> attributes) {
        this.pendingId = pendingId;
        this.role = role;
        this.userInfoMap = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return userInfoMap;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collect = new ArrayList<>();
        collect.add((GrantedAuthority) () -> role.toString());
        return collect;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return null;
    }
}
