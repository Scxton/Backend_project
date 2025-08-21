package com.example.demo.controller;

import com.example.demo.model.AchievementTable;
import com.example.demo.service.AchievementTableService;
import com.example.demo.service.PermissionService;
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
 * 成果管理控制器
 * 处理成果的全生命周期管理、版本控制、统计分析等功能
 */
@RestController
@Slf4j
@RequestMapping("/api/achievement/manage")
public class AchievementManageController {

    @Autowired
    private AchievementTableService achievementTableService;

    @Autowired
    private PermissionService permissionService;

    /**
     * 获取成果列表（支持分页和筛选）
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ROLE_0') or hasAuthority('ROLE_1') or hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> getAchievementList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String achievementName,
            @RequestParam(required = false) String achievementCategory,
            @RequestParam(required = false) Integer auditFlag,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            List<AchievementTable> achievements = achievementTableService.queryWithFilters(
                    pageNum, pageSize, achievementName, achievementCategory, auditFlag, startDate, endDate);
            
            String msg = "获取成果列表成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, achievements);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取成果列表失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取成果详情
     */
    @GetMapping("/detail/{achievementId}")
    public ResponseEntity<JSONResult> getAchievementDetail(@PathVariable Integer achievementId) {
        try {
            AchievementTable achievement = achievementTableService.queryById(achievementId);
            if (achievement == null) {
                String msg = "成果不存在";
                JSONResult jsonResult = new JSONResult("error", HttpStatus.NOT_FOUND.value(), msg, null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(jsonResult);
            }

            String msg = "获取成果详情成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, achievement);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取成果详情失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 批量删除成果
     */
    @PostMapping("/batch-delete")
    @PreAuthorize("hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> batchDeleteAchievements(@RequestBody List<Integer> achievementIds) {
        try {
            int successCount = 0;
            for (Integer achievementId : achievementIds) {
                Integer result = achievementTableService.deleteById(achievementId);
                if (result > 0) {
                    successCount++;
                }
            }

            String msg = String.format("成功删除%d个成果", successCount);
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, successCount);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("批量删除成果失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 批量更新成果状态
     */
    @PostMapping("/batch-update-status")
    @PreAuthorize("hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> batchUpdateStatus(@RequestBody Map<String, Object> params) {
        try {
            List<Integer> achievementIds = (List<Integer>) params.get("achievementIds");
            Integer newStatus = (Integer) params.get("newStatus");

            int successCount = 0;
            for (Integer achievementId : achievementIds) {
                AchievementTable achievement = new AchievementTable();
                achievement.setAchievementId(achievementId);
                achievement.setAuditFlag(newStatus);
                
                Integer result = achievementTableService.update(achievement);
                if (result > 0) {
                    successCount++;
                }
            }

            String msg = String.format("成功更新%d个成果的状态", successCount);
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, successCount);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("批量更新成果状态失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取成果统计分析数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<JSONResult> getStatistics() {
        try {
            Map<String, Object> statistics = achievementTableService.getManagementStatistics();
            String msg = "获取统计分析数据成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, statistics);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取统计分析数据失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取成果版本历史
     */
    @GetMapping("/version-history/{achievementId}")
    public ResponseEntity<JSONResult> getVersionHistory(@PathVariable Integer achievementId) {
        try {
            List<Map<String, Object>> versionHistory = achievementTableService.getVersionHistory(achievementId);
            String msg = "获取版本历史成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, versionHistory);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取版本历史失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取热门成果
     */
    @GetMapping("/popular")
    public ResponseEntity<JSONResult> getPopularAchievements(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            List<AchievementTable> achievements = achievementTableService.queryPopularAchievements(pageNum, pageSize);
            String msg = "获取热门成果成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, achievements);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取热门成果失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取推荐成果
     */
    @GetMapping("/recommended")
    public ResponseEntity<JSONResult> getRecommendedAchievements(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            List<AchievementTable> achievements = achievementTableService.queryRecommendedAchievements(pageNum, pageSize);
            String msg = "获取推荐成果成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, achievements);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取推荐成果失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 导出成果数据
     */
    @GetMapping("/export")
    public ResponseEntity<JSONResult> exportAchievements(
            @RequestParam(required = false) String achievementName,
            @RequestParam(required = false) String achievementCategory,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            String filePath = achievementTableService.exportAchievements(achievementName, achievementCategory, startDate, endDate);
            String msg = "导出成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, filePath);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("导出成果数据失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }
}