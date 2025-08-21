package com.example.demo.config.securityConfig;

import com.example.demo.model.UserRole;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 基于角色的权限评估器
 * 实现细粒度的权限控制
 */
@Component
public class RoleBasedPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String requiredPermission = permission.toString();
        
        // 检查用户角色权限
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            UserRole userRole = UserRole.fromAuthority(authority.getAuthority());
            
            switch (userRole) {
                case ADMINISTRATOR:
                    // 管理员拥有所有权限
                    return true;
                case PUBLISHER:
                    // 发布者权限检查
                    return checkPublisherPermission(requiredPermission, targetDomainObject);
                case GENERAL_USER:
                    // 普通用户权限检查
                    return checkGeneralUserPermission(requiredPermission, targetDomainObject);
                default:
                    return false;
            }
        }
        
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        // 基于ID和类型的权限检查
        return hasPermission(authentication, null, permission);
    }

    private boolean checkPublisherPermission(String permission, Object targetDomainObject) {
        switch (permission) {
            case "READ_ACHIEVEMENT":
            case "SEARCH_ACHIEVEMENT":
            case "DOWNLOAD_ACHIEVEMENT":
            case "COMMENT_ACHIEVEMENT":
            case "RATE_ACHIEVEMENT":
                return true;
            case "CREATE_ACHIEVEMENT":
            case "UPDATE_OWN_ACHIEVEMENT":
            case "DELETE_OWN_ACHIEVEMENT":
                return true;
            case "UPDATE_ANY_ACHIEVEMENT":
            case "DELETE_ANY_ACHIEVEMENT":
            case "APPROVE_ACHIEVEMENT":
            case "MANAGE_USERS":
            case "MANAGE_ROLES":
                return false;
            default:
                return false;
        }
    }

    private boolean checkGeneralUserPermission(String permission, Object targetDomainObject) {
        switch (permission) {
            case "READ_ACHIEVEMENT":
            case "SEARCH_ACHIEVEMENT":
            case "DOWNLOAD_ACHIEVEMENT":
            case "COMMENT_ACHIEVEMENT":
            case "RATE_ACHIEVEMENT":
                return true;
            case "CREATE_ACHIEVEMENT":
            case "UPDATE_ACHIEVEMENT":
            case "DELETE_ACHIEVEMENT":
            case "APPROVE_ACHIEVEMENT":
            case "MANAGE_USERS":
            case "MANAGE_ROLES":
                return false;
            default:
                return false;
        }
    }
}