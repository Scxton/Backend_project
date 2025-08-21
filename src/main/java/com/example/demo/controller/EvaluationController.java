package com.example.demo.controller;

import com.example.demo.model.InteractionEvaluation;
import com.example.demo.service.EvaluationService;
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
 * 评价和互动控制器
 * 处理成果评价、评分、互动相关操作
 */
@RestController
@Slf4j
@RequestMapping("/api/evaluation")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;

    /**
     * 提交评价
     */
    @PostMapping("/submit")
    @PreAuthorize("hasAnyAuthority('ROLE_1', 'ROLE_2')")
    public ResponseEntity<JSONResult> submitEvaluation(@RequestBody InteractionEvaluation evaluation) {
        try {
            Integer userId = getCurrentUserId();
            evaluation.setUserId(userId);
            
            boolean success = evaluationService.submitEvaluation(evaluation);
            
            if (success) {
                String msg = "评价提交成功";
                JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, null);
                return ResponseEntity.ok(jsonResult);
            } else {
                String msg = "您已经评价过该成果";
                JSONResult jsonResult = new JSONResult("error", HttpStatus.BAD_REQUEST.value(), msg, null);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonResult);
            }
        } catch (Exception e) {
            log.error("提交评价失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取成果的评价列表
     */
    @GetMapping("/achievement/{achievementId}")
    public ResponseEntity<JSONResult> getAchievementEvaluations(
            @PathVariable Integer achievementId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            List<InteractionEvaluation> evaluations = evaluationService.getAchievementEvaluations(achievementId, pageNum, pageSize);
            int total = evaluationService.getEvaluationCount(achievementId);
            
            Map<String, Object> data = Map.of(
                "evaluations", evaluations,
                "total", total,
                "averageRating", evaluationService.getAverageRating(achievementId)
            );
            
            String msg = "获取评价列表成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, data);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取评价列表失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取我的评价
     */
    @GetMapping("/my-evaluations")
    @PreAuthorize("hasAnyAuthority('ROLE_1', 'ROLE_2')")
    public ResponseEntity<JSONResult> getMyEvaluations(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Integer userId = getCurrentUserId();
            List<InteractionEvaluation> evaluations = evaluationService.getUserEvaluations(userId, pageNum, pageSize);
            int total = evaluationService.getUserEvaluationCount(userId);
            
            Map<String, Object> data = Map.of(
                "evaluations", evaluations,
                "total", total
            );
            
            String msg = "获取我的评价成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, data);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取我的评价失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 更新评价
     */
    @PutMapping("/update/{evaluationId}")
    @PreAuthorize("hasAnyAuthority('ROLE_1', 'ROLE_2')")
    public ResponseEntity<JSONResult> updateEvaluation(@PathVariable Integer evaluationId, @RequestBody InteractionEvaluation evaluation) {
        try {
            Integer userId = getCurrentUserId();
            evaluation.setInteractionId(evaluationId);
            evaluation.setUserId(userId);
            
            boolean success = evaluationService.updateEvaluation(evaluation);
            
            if (success) {
                String msg = "评价更新成功";
                JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, null);
                return ResponseEntity.ok(jsonResult);
            } else {
                String msg = "评价不存在或无权限修改";
                JSONResult jsonResult = new JSONResult("error", HttpStatus.FORBIDDEN.value(), msg, null);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(jsonResult);
            }
        } catch (Exception e) {
            log.error("更新评价失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 删除评价
     */
    @DeleteMapping("/delete/{evaluationId}")
    @PreAuthorize("hasAnyAuthority('ROLE_1', 'ROLE_2')")
    public ResponseEntity<JSONResult> deleteEvaluation(@PathVariable Integer evaluationId) {
        try {
            Integer userId = getCurrentUserId();
            boolean success = evaluationService.deleteEvaluation(evaluationId, userId);
            
            if (success) {
                String msg = "评价删除成功";
                JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, null);
                return ResponseEntity.ok(jsonResult);
            } else {
                String msg = "评价不存在或无权限删除";
                JSONResult jsonResult = new JSONResult("error", HttpStatus.FORBIDDEN.value(), msg, null);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(jsonResult);
            }
        } catch (Exception e) {
            log.error("删除评价失败", e);
            String msg = "系统错误，请稍后重试";
            JSONResult jsonResult = new JSONResult("error", HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(jsonResult);
        }
    }

    /**
     * 获取评价统计
     */
    @GetMapping("/statistics/{achievementId}")
    public ResponseEntity<JSONResult> getEvaluationStatistics(@PathVariable Integer achievementId) {
        try {
            Map<String, Object> statistics = evaluationService.getEvaluationStatistics(achievementId);
            
            String msg = "获取评价统计成功";
            JSONResult jsonResult = new JSONResult("success", HttpStatus.OK.value(), msg, statistics);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("获取评价统计失败", e);
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