package com.example.demo.service;

import com.example.demo.mapper.InteractionEvaluationMapper;
import com.example.demo.model.InteractionEvaluation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 评价服务
 * 处理成果评价、评分、互动相关业务逻辑
 */
@Service
@Slf4j
public class EvaluationService {

    @Autowired
    private InteractionEvaluationMapper evaluationMapper;

    /**
     * 提交评价
     */
    public boolean submitEvaluation(InteractionEvaluation evaluation) {
        try {
            // 检查用户是否已经评价过该成果
            List<InteractionEvaluation> existing = evaluationMapper.queryAll();
            boolean alreadyEvaluated = existing.stream()
                .anyMatch(e -> e.getAchievementId().equals(evaluation.getAchievementId()) 
                    && e.getUserId().equals(evaluation.getUserId()));
            
            if (alreadyEvaluated) {
                return false;
            }

            evaluation.setInteractionTime(new Date().toString());
            evaluation.setTableStatus(true);
            
            int result = evaluationMapper.insert(evaluation);
            return result > 0;
        } catch (Exception e) {
            log.error("提交评价失败", e);
            throw new RuntimeException("提交评价失败", e);
        }
    }

    /**
     * 获取成果的评价列表
     */
    public List<InteractionEvaluation> getAchievementEvaluations(Integer achievementId, int pageNum, int pageSize) {
        try {
            int offset = (pageNum - 1) * pageSize;
            List<InteractionEvaluation> evaluations = evaluationMapper.queryAll();
            
            return evaluations.stream()
                .filter(e -> e.getAchievementId().equals(achievementId) && e.getTableStatus())
                .sorted((e1, e2) -> e2.getInteractionTime().compareTo(e1.getInteractionTime()))
                .skip(offset)
                .limit(pageSize)
                .toList();
        } catch (Exception e) {
            log.error("获取评价列表失败", e);
            throw new RuntimeException("获取评价列表失败", e);
        }
    }

    /**
     * 获取用户评价
     */
    public List<InteractionEvaluation> getUserEvaluations(Integer userId, int pageNum, int pageSize) {
        try {
            int offset = (pageNum - 1) * pageSize;
            List<InteractionEvaluation> evaluations = evaluationMapper.queryAll();
            
            return evaluations.stream()
                .filter(e -> e.getUserId().equals(userId))
                .sorted((e1, e2) -> e2.getInteractionTime().compareTo(e1.getInteractionTime()))
                .skip(offset)
                .limit(pageSize)
                .toList();
        } catch (Exception e) {
            log.error("获取用户评价失败", e);
            throw new RuntimeException("获取用户评价失败", e);
        }
    }

    /**
     * 更新评价
     */
    public boolean updateEvaluation(InteractionEvaluation evaluation) {
        try {
            InteractionEvaluation existing = evaluationMapper.queryById(evaluation.getInteractionId());
            if (existing == null || !existing.getUserId().equals(evaluation.getUserId())) {
                return false;
            }

            evaluation.setInteractionTime(new Date().toString());
            int result = evaluationMapper.update(evaluation);
            return result > 0;
        } catch (Exception e) {
            log.error("更新评价失败", e);
            throw new RuntimeException("更新评价失败", e);
        }
    }

    /**
     * 删除评价
     */
    public boolean deleteEvaluation(Integer evaluationId, Integer userId) {
        try {
            InteractionEvaluation existing = evaluationMapper.queryById(evaluationId);
            if (existing == null || !existing.getUserId().equals(userId)) {
                return false;
            }

            existing.setTableStatus(false);
            int result = evaluationMapper.update(existing);
            return result > 0;
        } catch (Exception e) {
            log.error("删除评价失败", e);
            throw new RuntimeException("删除评价失败", e);
        }
    }

    /**
     * 获取评价总数
     */
    public int getEvaluationCount(Integer achievementId) {
        try {
            return (int) evaluationMapper.queryAll().stream()
                .filter(e -> e.getAchievementId().equals(achievementId) && e.getTableStatus())
                .count();
        } catch (Exception e) {
            log.error("获取评价总数失败", e);
            throw new RuntimeException("获取评价总数失败", e);
        }
    }

    /**
     * 获取用户评价总数
     */
    public int getUserEvaluationCount(Integer userId) {
        try {
            return (int) evaluationMapper.queryAll().stream()
                .filter(e -> e.getUserId().equals(userId))
                .count();
        } catch (Exception e) {
            log.error("获取用户评价总数失败", e);
            throw new RuntimeException("获取用户评价总数失败", e);
        }
    }

    /**
     * 获取平均评分
     */
    public double getAverageRating(Integer achievementId) {
        try {
            List<InteractionEvaluation> evaluations = evaluationMapper.queryAll().stream()
                .filter(e -> e.getAchievementId().equals(achievementId) && e.getTableStatus())
                .toList();
            
            if (evaluations.isEmpty()) {
                return 0.0;
            }
            
            double average = evaluations.stream()
                .mapToInt(InteractionEvaluation::getRating)
                .average()
                .orElse(0.0);
            
            return Math.round(average * 10) / 10.0;
        } catch (Exception e) {
            log.error("获取平均评分失败", e);
            throw new RuntimeException("获取平均评分失败", e);
        }
    }

    /**
     * 获取评价统计
     */
    public Map<String, Object> getEvaluationStatistics(Integer achievementId) {
        try {
            List<InteractionEvaluation> evaluations = evaluationMapper.queryAll().stream()
                .filter(e -> e.getAchievementId().equals(achievementId) && e.getTableStatus())
                .toList();
            
            int totalCount = evaluations.size();
            double averageRating = totalCount > 0 ? 
                evaluations.stream().mapToInt(InteractionEvaluation::getRating).average().orElse(0.0) : 0.0;
            
            Map<Integer, Integer> ratingDistribution = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                ratingDistribution.put(i, 0);
            }
            
            for (InteractionEvaluation evaluation : evaluations) {
                int rating = evaluation.getRating();
                ratingDistribution.put(rating, ratingDistribution.get(rating) + 1);
            }
            
            return Map.of(
                "totalCount", totalCount,
                "averageRating", Math.round(averageRating * 10) / 10.0,
                "ratingDistribution", ratingDistribution
            );
        } catch (Exception e) {
            log.error("获取评价统计失败", e);
            throw new RuntimeException("获取评价统计失败", e);
        }
    }

    /**
     * 检查用户是否已经评价过该成果
     */
    public boolean hasUserEvaluated(Integer userId, Integer achievementId) {
        try {
            return evaluationMapper.queryAll().stream()
                .anyMatch(e -> e.getUserId().equals(userId) 
                    && e.getAchievementId().equals(achievementId) 
                    && e.getTableStatus());
        } catch (Exception e) {
            log.error("检查评价状态失败", e);
            throw new RuntimeException("检查评价状态失败", e);
        }
    }
}