package com.example.demo.model;

import java.io.Serializable;

/**
 * 用户角色枚举类
 * 定义系统中的三种用户角色：普通用户、发布者、管理员
 */
public enum UserRole implements Serializable {
    
    GENERAL_USER("普通用户", "ROLE_0", 0),
    PUBLISHER("发布者", "ROLE_1", 1),
    ADMINISTRATOR("管理员", "ROLE_2", 2);

    private final String roleName;
    private final String authority;
    private final Integer roleId;

    UserRole(String roleName, String authority, Integer roleId) {
        this.roleName = roleName;
        this.authority = authority;
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getAuthority() {
        return authority;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public static UserRole fromRoleId(Integer roleId) {
        for (UserRole role : UserRole.values()) {
            if (role.getRoleId().equals(roleId)) {
                return role;
            }
        }
        return GENERAL_USER;
    }

    public static UserRole fromAuthority(String authority) {
        for (UserRole role : UserRole.values()) {
            if (role.getAuthority().equals(authority)) {
                return role;
            }
        }
        return GENERAL_USER;
    }
}