package com.example.demo.controller;

import com.example.demo.model.AchievementTable;
import com.example.demo.model.UserRole;
import com.example.demo.service.AchievementTableService;
import com.example.demo.service.PermissionService;
import com.example.demo.utils.JSONResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 成果发布控制器
 * 处理成果上传、编辑、审核等发布相关功能
 */
@RestController
@Slf4j
@RequestMapping("/api/achievement/publish")
public class AchievementPublishController {

    @Autowired
    private AchievementTableService achievementTableService;

    @Autowired
    private PermissionService permissionService;

    /**
     * 提交成果发布申请
     */
    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('ROLE_1') or hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> submitAchievement(@RequestBody AchievementTable achievement, HttpServletRequest request) {
        try {
            // 设置初始状态为待审核
            achievement.setAuditFlag(0); // 0:待审核, 1:审核通过, 2:审核拒绝
            achievement.setTableStatus(true);
            
            Integer result = achievementTableService.insert(achievement);
            
            if (result > 0) {
                String msg = "成果发布申请已提交，等待管理员审核";
                JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, result);
                return ResponseEntity.ok(jsonResult);
            } else {
                String msg = "成果发布申请提交失败";
                JSONResult jsonResult = new JSONResult("error", HttpStatus.BAD_REQUEST.value(), msg, null);
                return ResponseEntity.badRequest().body(jsonResult);
            }
        } catch (Exception e) {
            log.error("提交成果发布申请失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 更新成果信息（仅发布者可更新自己的成果）
     */
    @PostMapping("/update/{achievementId}")
    @PreAuthorize("hasAuthority('ROLE_1') or hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> updateAchievement(
            @PathVariable Integer achievementId,
            @RequestBody AchievementTable achievement) {
        
        // 检查权限
        if (!permissionService.canAccessAchievement(achievementId, "UPDATE")) {
            String msg = "无权限更新该成果";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.FORBIDDEN.value(), msg, null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(jsonResult);
        }

        try {
            achievement.setAchievementId(achievementId);
            // 更新后需要重新审核
            achievement.setAuditFlag(0);
            
            Integer result = achievementTableService.update(achievement);
            
            if (result > 0) {
                String msg = "成果信息更新成功，等待重新审核";
                JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, result);
                return ResponseEntity.ok(jsonResult);
            } else {
                String msg = "成果信息更新失败";
                JSONResult jsonResult = new JSONResult("error", HttpStatus.BAD_REQUEST.value(), msg, null);
                return ResponseEntity.badRequest().body(jsonResult);
            }
        } catch (Exception e) {
            log.error("更新成果信息失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取当前用户的成果列表
     */
    @GetMapping("/my-achievements")
    @PreAuthorize("hasAuthority('ROLE_1') or hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> getMyAchievements(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        try {
            List<AchievementTable> achievements = achievementTableService.queryByUserId(getCurrentUserId(), pageNum, pageSize);
            String msg = "获取我的成果列表成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, achievements);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取我的成果列表失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取待审核的成果列表（管理员专用）
     */
    @GetMapping("/pending-review")
    @PreAuthorize("hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> getPendingReviewAchievements(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        try {
            List<AchievementTable> achievements = achievementTableService.queryAllWithPaginationForApproval(pageNum, pageSize);
            String msg = "获取待审核成果列表成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, achievements);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取待审核成果列表失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 上传成果文件
     */
    @PostMapping("/upload-file")
    @PreAuthorize("hasAuthority('ROLE_1') or hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> uploadAchievementFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("achievementId") Integer achievementId) {
        
        try {
            // 检查文件大小和类型
            if (file.getSize() > 100 * 1024 * 1024) { // 100MB限制
                String msg = "文件大小不能超过100MB";
                JSONResult jsonResult = new JSONResult("error", HttpStatus.BAD_REQUEST.value(), msg, null);
                return ResponseEntity.badRequest().body(jsonResult);
            }

            // 保存文件逻辑（需要实现文件服务）
            String fileUrl = saveFile(file, achievementId);
            
            String msg = "文件上传成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, fileUrl);
            return ResponseEntity.ok(jsonResult);
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            String msg = "文件上传失败，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取成果发布统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('ROLE_1') or hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> getPublishStatistics() {
        try {
            Map<String, Object> statistics = achievementTableService.getPublishStatistics(getCurrentUserId());
            String msg = "获取发布统计信息成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, statistics);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取发布统计信息失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    private Integer getCurrentUserId() {
        // 从SecurityContext获取当前用户ID
        // 实际实现需要从JWT或SecurityContext获取
        return 1;
    }

    private String saveFile(MultipartFile file, Integer achievementId) {
        // 实际文件保存逻辑，需要集成文件存储服务
        // 返回文件URL
        return "/uploads/" + achievementId + "/" + file.getOriginalFilename();
    }
}