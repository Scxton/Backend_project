package com.example.demo.service;

import com.example.demo.model.UserRole;
import com.example.demo.model.AchievementTable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 权限服务类
 * 提供细粒度的权限检查和验证
 */
@Service
public class PermissionService {

    /**
     * 检查用户是否有权限访问成果
     */
    public boolean canAccessAchievement(Integer achievementId, String action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String authority = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse("");

        UserRole userRole = UserRole.fromAuthority(authority);

        switch (action) {
            case "READ":
                return canReadAchievement(userRole, achievementId);
            case "UPDATE":
                return canUpdateAchievement(userRole, achievementId);
            case "DELETE":
                return canDeleteAchievement(userRole, achievementId);
            case "DOWNLOAD":
                return canDownloadAchievement(userRole, achievementId);
            default:
                return false;
        }
    }

    /**
     * 检查用户是否有权限发布成果
     */
    public boolean canPublishAchievement() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String authority = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse("");

        UserRole userRole = UserRole.fromAuthority(authority);
        return userRole == UserRole.PUBLISHER || userRole == UserRole.ADMINISTRATOR;
    }

    /**
     * 检查用户是否有权限审核成果
     */
    public boolean canApproveAchievement() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String authority = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse("");

        UserRole userRole = UserRole.fromAuthority(authority);
        return userRole == UserRole.ADMINISTRATOR;
    }

    /**
     * 检查用户是否有权限管理用户
     */
    public boolean canManageUsers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String authority = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse("");

        UserRole userRole = UserRole.fromAuthority(authority);
        return userRole == UserRole.ADMINISTRATOR;
    }

    private boolean canReadAchievement(UserRole userRole, Integer achievementId) {
        switch (userRole) {
            case ADMINISTRATOR:
                return true;
            case PUBLISHER:
            case GENERAL_USER:
                return true; // 所有用户都可以查看已发布的成果
            default:
                return false;
        }
    }

    private boolean canUpdateAchievement(UserRole userRole, Integer achievementId) {
        switch (userRole) {
            case ADMINISTRATOR:
                return true;
            case PUBLISHER:
                // 发布者只能修改自己的成果
                return isOwnerOfAchievement(achievementId);
            case GENERAL_USER:
                return false;
            default:
                return false;
        }
    }

    private boolean canDeleteAchievement(UserRole userRole, Integer achievementId) {
        switch (userRole) {
            case ADMINISTRATOR:
                return true;
            case PUBLISHER:
                // 发布者只能删除自己的成果
                return isOwnerOfAchievement(achievementId);
            case GENERAL_USER:
                return false;
            default:
                return false;
        }
    }

    private boolean canDownloadAchievement(UserRole userRole, Integer achievementId) {
        switch (userRole) {
            case ADMINISTRATOR:
            case PUBLISHER:
            case GENERAL_USER:
                return true; // 所有用户都可以下载已发布的成果
            default:
                return false;
        }
    }

    private boolean isOwnerOfAchievement(Integer achievementId) {
        // 这里需要从数据库中查询成果的所有者，并与当前用户进行比较
        // 实际实现需要注入AchievementTableService
        return true; // 简化实现
    }
}