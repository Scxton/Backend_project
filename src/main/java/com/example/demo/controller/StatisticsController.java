package com.example.demo.controller;

import com.example.demo.service.StatisticsService;
import com.example.demo.utils.JSONResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统计和分析控制器
 * 处理平台整体数据统计、用户行为分析、成果使用统计等
 */
@RestController
@Slf4j
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 获取平台整体统计
     */
    @GetMapping("/platform-overview")
    @PreAuthorize("hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> getPlatformOverview() {
        try {
            Map<String, Object> overview = statisticsService.getPlatformOverview();
            String msg = "获取平台统计成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, overview);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取平台统计失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取用户行为统计
     */
    @GetMapping("/user-behavior")
    @PreAuthorize("hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> getUserBehaviorStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Map<String, Object> behaviorStats = statisticsService.getUserBehaviorStats(startDate, endDate);
            String msg = "获取用户行为统计成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, behaviorStats);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取用户行为统计失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取成果使用统计
     */
    @GetMapping("/achievement-usage")
    public ResponseEntity<JSONResult> getAchievementUsageStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Map<String, Object> usageStats = statisticsService.getAchievementUsageStats(startDate, endDate, limit);
            String msg = "获取成果使用统计成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, usageStats);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取成果使用统计失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取分类统计
     */
    @GetMapping("/category-stats")
    public ResponseEntity<JSONResult> getCategoryStats() {
        try {
            Map<String, Object> categoryStats = statisticsService.getCategoryStats();
            String msg = "获取分类统计成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, categoryStats);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取分类统计失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取时间趋势分析
     */
    @GetMapping("/time-trends")
    public ResponseEntity<JSONResult> getTimeTrendAnalysis(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "day") String granularity) {
        try {
            Map<String, Object> trends = statisticsService.getTimeTrendAnalysis(startDate, endDate, granularity);
            String msg = "获取时间趋势成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, trends);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取时间趋势失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取用户个人统计
     */
    @GetMapping("/user-personal")
    public ResponseEntity<JSONResult> getUserPersonalStats() {
        try {
            Integer userId = getCurrentUserId();
            Map<String, Object> personalStats = statisticsService.getUserPersonalStats(userId);
            String msg = "获取个人统计成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, personalStats);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取个人统计失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取热门成果排行
     */
    @GetMapping("/popular-achievements")
    public ResponseEntity<JSONResult> getPopularAchievements(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "downloads") String metric) {
        try {
            Map<String, Object> popular = statisticsService.getPopularAchievements(limit, metric);
            String msg = "获取热门成果成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, popular);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取热门成果失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取用户活跃度排行
     */
    @GetMapping("/user-activity")
    @PreAuthorize("hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> getUserActivityRank(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "month") String period) {
        try {
            Map<String, Object> userActivity = statisticsService.getUserActivityRank(limit, period);
            String msg = "获取用户活跃度成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, userActivity);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取用户活跃度失败", e);
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