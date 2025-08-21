package com.example.demo.controller;

import com.example.demo.model.AchievementTable;
import com.example.demo.model.DownloadRecords;
import com.example.demo.service.AchievementTableService;
import com.example.demo.service.DownloadRecordsService;
import com.example.demo.service.PermissionService;
import com.example.demo.utils.JSONResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 成果下载控制器
 * 处理成果文件下载、下载记录管理、下载统计等功能
 */
@RestController
@Slf4j
@RequestMapping("/api/achievement/download")
public class AchievementDownloadController {

    @Autowired
    private AchievementTableService achievementTableService;

    @Autowired
    private DownloadRecordsService downloadRecordsService;

    @Autowired
    private PermissionService permissionService;

    /**
     * 下载成果文件
     */
    @GetMapping("/file/{achievementId}")
    @PreAuthorize("hasAuthority('ROLE_0') or hasAuthority('ROLE_1') or hasAuthority('ROLE_2')")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Integer achievementId,
            HttpServletRequest request) {
        
        try {
            // 检查权限
            if (!permissionService.canAccessAchievement(achievementId, "DOWNLOAD")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

            // 获取成果信息
            AchievementTable achievement = achievementTableService.queryById(achievementId);
            if (achievement == null || !achievement.getTableStatus()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // 检查审核状态
            if (achievement.getAuditFlag() != 1) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

            // 获取文件路径（实际项目中需要从文件存储服务获取）
            String filePath = getAchievementFilePath(achievementId);
            File file = new File(filePath);
            
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // 记录下载记录
            DownloadRecords downloadRecord = new DownloadRecords();
            downloadRecord.setAchievementId(achievementId);
            downloadRecord.setUserId(getCurrentUserId());
            downloadRecord.setDownloadPath(filePath);
            downloadRecord.setDownloadStatus(true);
            downloadRecordsService.insert(downloadRecord);

            // 更新下载计数
            achievement.setAchievementDownloadCount(achievement.getAchievementDownloadCount() + 1);
            achievementTableService.update(achievement);

            // 返回文件
            Resource resource = new FileSystemResource(file);
            String fileName = URLEncoder.encode(achievement.getAchievementName() + ".zip", StandardCharsets.UTF_8.toString());
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(file.length())
                    .body(resource);

        } catch (Exception e) {
            log.error("下载文件失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 获取用户下载历史
     */
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('ROLE_0') or hasAuthority('ROLE_1') or hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> getDownloadHistory(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        try {
            List<DownloadRecords> downloadHistory = downloadRecordsService.getUserDownloadHistory(
                    getCurrentUserId(), pageNum, pageSize);
            
            String msg = "获取下载历史成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, downloadHistory);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取下载历史失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取下载统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<JSONResult> getDownloadStatistics() {
        try {
            Map<String, Object> statistics = downloadRecordsService.getDownloadStatistics();
            String msg = "获取下载统计成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, statistics);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取下载统计失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取热门下载成果
     */
    @GetMapping("/popular")
    public ResponseEntity<JSONResult> getPopularDownloads(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            List<Map<String, Object>> popularDownloads = downloadRecordsService.getPopularDownloads(pageNum, pageSize);
            String msg = "获取热门下载成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, popularDownloads);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取热门下载失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 检查下载权限
     */
    @GetMapping("/check-permission/{achievementId}")
    public ResponseEntity<JSONResult> checkDownloadPermission(@PathVariable Integer achievementId) {
        try {
            boolean hasPermission = permissionService.canAccessAchievement(achievementId, "DOWNLOAD");
            
            if (hasPermission) {
                String msg = "有下载权限";
                JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, null);
                return ResponseEntity.ok(jsonResult);
            } else {
                String msg = "无下载权限";
                JSONResult jsonResult = new JSONResult("error", HttpStatus.FORBIDDEN.value(), msg, null);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(jsonResult);
            }
        } catch (Exception e) {
            log.error("检查下载权限失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 批量下载
     */
    @PostMapping("/batch-download")
    @PreAuthorize("hasAuthority('ROLE_0') or hasAuthority('ROLE_1') or hasAuthority('ROLE_2')")
    public ResponseEntity<JSONResult> batchDownload(@RequestBody List<Integer> achievementIds) {
        try {
            // 检查所有成果的下载权限
            for (Integer achievementId : achievementIds) {
                if (!permissionService.canAccessAchievement(achievementId, "DOWNLOAD")) {
                    String msg = "部分成果无下载权限";
                    JSONResult jsonResult = new JSONResult("error", HttpStatus.FORBIDDEN.value(), msg, null);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(jsonResult);
                }
            }

            // 创建批量下载任务（实际项目中需要实现异步下载）
            String downloadTaskId = createBatchDownloadTask(achievementIds);
            
            String msg = "批量下载任务已创建";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, downloadTaskId);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("批量下载失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取下载任务状态
     */
    @GetMapping("/download-task/{taskId}")
    public ResponseEntity<JSONResult> getDownloadTaskStatus(@PathVariable String taskId) {
        try {
            Map<String, Object> taskStatus = getBatchDownloadTaskStatus(taskId);
            String msg = "获取任务状态成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, taskStatus);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取下载任务状态失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    private String getAchievementFilePath(Integer achievementId) {
        // 实际项目中需要从文件存储服务获取正确的文件路径
        return "/tmp/achievements/" + achievementId + ".zip";
    }

    private Integer getCurrentUserId() {
        // 从SecurityContext获取当前用户ID
        return 1;
    }

    private String createBatchDownloadTask(List<Integer> achievementIds) {
        // 实际项目中需要实现异步下载任务
        return "task_" + System.currentTimeMillis();
    }

    private Map<String, Object> getBatchDownloadTaskStatus(String taskId) {
        // 实际项目中需要查询任务状态
        return Map.of(
                "taskId", taskId,
                "status", "completed",
                "progress", 100,
                "downloadUrl", "/api/download/batch/" + taskId
        );
    }
}