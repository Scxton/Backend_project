package com.example.demo.controller;

import com.example.demo.service.AchievementApprovalService;
import com.example.demo.utils.JSONResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 成果审核控制器
 * 处理成果审核相关操作
 */
@RestController
@Slf4j
@RequestMapping("/api/achievement/approval")
public class AchievementApprovalController {

    @Autowired
    private AchievementApprovalService approvalService;

    /**
     * 获取待审核列表
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> getPendingApprovals(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String achievementName,
            @RequestParam(required = false) String achievementCategory,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            List<? extends Object> approvals = approvalService.getPendingApprovals(
                    pageNum, pageSize, achievementName, achievementCategory, priority, startDate, endDate);
            
            int total = approvalService.getPendingApprovalsCount(
                    achievementName, achievementCategory, priority, startDate, endDate);
            
            String msg = "获取待审核列表成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, 
                    Map.of("list", approvals, "total", total));
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取待审核列表失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取审核统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> getApprovalStatistics() {
        try {
            Map<String, Object> statistics = approvalService.getApprovalStatistics();
            String msg = "获取审核统计成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, statistics);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取审核统计失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取审核详情
     */
    @GetMapping("/detail/{achievementId}")
    @PreAuthorize("hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> getApprovalDetail(@PathVariable Integer achievementId) {
        try {
            Object detail = approvalService.getApprovalDetail(achievementId);
            if (detail == null) {
                String msg = "成果不存在";
                JSONResult jsonResult = new JSONResult("error", HttpStatus.NOT_FOUND.value(), msg, null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonResult);
            }
            
            String msg = "获取审核详情成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, detail);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取审核详情失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 审核通过
     */
    @PostMapping("/approve/{achievementId}")
    @PreAuthorize("hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> approveAchievement(@PathVariable Integer achievementId) {
        try {
            Integer auditorId = getCurrentUserId();
            boolean success = approvalService.approveAchievement(achievementId, auditorId);
            
            if (success) {
                String msg = "审核通过成功";
                JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, null);
                return ResponseEntity.ok(jsonResult);
            } else {
                String msg = "审核失败，成果不存在或状态不正确";
                JSONResult jsonResult = new JSONResult("error", HttpStatus.BAD_REQUEST.value(), msg, null);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonResult);
            }
        } catch (Exception e) {
            log.error("审核通过失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 审核拒绝
     */
    @PostMapping("/reject")
    @PreAuthorize("hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> rejectAchievement(@RequestBody Map<String, Object> params) {
        try {
            Integer achievementId = (Integer) params.get("achievementId");
            String reason = (String) params.get("reason");
            
            if (reason == null || reason.trim().isEmpty()) {
                String msg = "拒绝理由不能为空";
                JSONResult jsonResult = new JSONResult("error", HttpStatus.BAD_REQUEST.value(), msg, null);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonResult);
            }
            
            Integer auditorId = getCurrentUserId();
            boolean success = approvalService.rejectAchievement(achievementId, auditorId, reason);
            
            if (success) {
                String msg = "审核拒绝成功";
                JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, null);
                return ResponseEntity.ok(jsonResult);
            } else {
                String msg = "审核失败，成果不存在或状态不正确";
                JSONResult jsonResult = new JSONResult("error", HttpStatus.BAD_REQUEST.value(), msg, null);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonResult);
            }
        } catch (Exception e) {
            log.error("审核拒绝失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 批量审核通过
     */
    @PostMapping("/batch-approve")
    @PreAuthorize("hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> batchApprove(@RequestBody List<Integer> achievementIds) {
        try {
            Integer auditorId = getCurrentUserId();
            int successCount = approvalService.batchApprove(achievementIds, auditorId);
            
            String msg = String.format("批量审核成功，共通过%d个成果", successCount);
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, successCount);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("批量审核失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取审核历史
     */
    @GetMapping("/history/{achievementId}")
    public ResponseEntity<JSONResult> getAuditHistory(@PathVariable Integer achievementId) {
        try {
            List<? extends Object> history = approvalService.getAuditHistory(achievementId);
            String msg = "获取审核历史成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, history);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取审核历史失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取我的审核记录
     */
    @GetMapping("/my-history")
    @PreAuthorize("hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> getMyAuditHistory(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Integer auditorId = getCurrentUserId();
            List<? extends Object> history = approvalService.getMyAuditHistory(auditorId, pageNum, pageSize);
            String msg = "获取审核记录成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, history);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取审核记录失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    private Integer getCurrentUserId() {
        // 从SecurityContext获取当前用户ID
        return 1;
    }
}