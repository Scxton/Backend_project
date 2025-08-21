package com.example.demo.service;

import com.example.demo.model.AchievementTable;
import com.example.demo.model.AuditRecord;
import com.example.demo.mapper.AchievementTableMapper;
import com.example.demo.mapper.AuditRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 成果审核服务
 * 处理成果审核的全流程管理
 */
@Service
@Slf4j
public class AchievementApprovalService {

    @Autowired
    private AchievementTableMapper achievementTableMapper;

    @Autowired
    private AuditRecordMapper auditRecordMapper;

    /**
     * 获取待审核列表
     */
    public List<AchievementTable> getPendingApprovals(int pageNum, int pageSize, 
                                                     String achievementName, String category, 
                                                     String priority, String startDate, String endDate) {
        int offset = (pageNum - 1) * pageSize;
        return achievementTableMapper.selectPendingApprovals(offset, pageSize, 
                                                           achievementName, category, priority, startDate, endDate);
    }

    /**
     * 获取待审核总数
     */
    public int getPendingApprovalsCount(String achievementName, String category, 
                                      String priority, String startDate, String endDate) {
        return achievementTableMapper.countPendingApprovals(achievementName, category, priority, startDate, endDate);
    }

    /**
     * 获取审核统计信息
     */
    public Map<String, Object> getApprovalStatistics() {
        // 待审核总数
        int totalPending = achievementTableMapper.countPendingApprovals(null, null, null, null, null);
        
        // 今日已审核数
        int todayApproved = auditRecordMapper.countTodayApproved();
        
        // 平均处理时间（小时）
        Double avgProcessingTime = auditRecordMapper.getAverageProcessingTime();
        if (avgProcessingTime == null) {
            avgProcessingTime = 0.0;
        }
        
        // 拒绝率
        Double rejectionRate = auditRecordMapper.getRejectionRate();
        if (rejectionRate == null) {
            rejectionRate = 0.0;
        }

        return Map.of(
            "totalPending", totalPending,
            "todayApproved", todayApproved,
            "avgProcessingTime", Math.round(avgProcessingTime * 100) / 100.0,
            "rejectionRate", Math.round(rejectionRate * 100) / 100.0
        );
    }

    /**
     * 获取审核详情
     */
    public AchievementTable getApprovalDetail(Integer achievementId) {
        return achievementTableMapper.selectByIdWithFiles(achievementId);
    }

    /**
     * 审核通过
     */
    @Transactional
    public boolean approveAchievement(Integer achievementId, Integer auditorId) {
        try {
            AchievementTable achievement = achievementTableMapper.selectById(achievementId);
            if (achievement == null) {
                log.warn("成果不存在: {}", achievementId);
                return false;
            }

            if (achievement.getAuditFlag() != 0) {
                log.warn("成果状态不是待审核: {}", achievementId);
                return false;
            }

            // 更新成果状态
            achievement.setAuditFlag(1);
            achievement.setAuditTime(new Date());
            achievement.setAuditorId(auditorId);
            achievementTableMapper.updateById(achievement);

            // 记录审核记录
            AuditRecord auditRecord = new AuditRecord();
            auditRecord.setAchievementId(achievementId);
            auditRecord.setAuditorId(auditorId);
            auditRecord.setAuditResult(1);
            auditRecord.setAuditTime(new Date());
            auditRecord.setAuditComment("审核通过");
            auditRecordMapper.insert(auditRecord);

            log.info("成果审核通过: {}", achievementId);
            return true;
        } catch (Exception e) {
            log.error("审核通过失败: {}", achievementId, e);
            throw new RuntimeException("审核失败", e);
        }
    }

    /**
     * 审核拒绝
     */
    @Transactional
    public boolean rejectAchievement(Integer achievementId, Integer auditorId, String reason) {
        try {
            AchievementTable achievement = achievementTableMapper.selectById(achievementId);
            if (achievement == null) {
                log.warn("成果不存在: {}", achievementId);
                return false;
            }

            if (achievement.getAuditFlag() != 0) {
                log.warn("成果状态不是待审核: {}", achievementId);
                return false;
            }

            // 更新成果状态
            achievement.setAuditFlag(2);
            achievement.setAuditTime(new Date());
            achievement.setAuditorId(auditorId);
            achievementTableMapper.updateById(achievement);

            // 记录审核记录
            AuditRecord auditRecord = new AuditRecord();
            auditRecord.setAchievementId(achievementId);
            auditRecord.setAuditorId(auditorId);
            auditRecord.setAuditResult(2);
            auditRecord.setAuditTime(new Date());
            auditRecord.setAuditComment(reason);
            auditRecordMapper.insert(auditRecord);

            log.info("成果审核拒绝: {}", achievementId);
            return true;
        } catch (Exception e) {
            log.error("审核拒绝失败: {}", achievementId, e);
            throw new RuntimeException("审核拒绝失败", e);
        }
    }

    /**
     * 批量审核
     */
    @Transactional
    public int batchApprove(List<Integer> achievementIds, Integer auditorId) {
        int successCount = 0;
        for (Integer achievementId : achievementIds) {
            if (approveAchievement(achievementId, auditorId)) {
                successCount++;
            }
        }
        return successCount;
    }

    /**
     * 获取审核历史
     */
    public List<AuditRecord> getAuditHistory(Integer achievementId) {
        return auditRecordMapper.selectByAchievementId(achievementId);
    }

    /**
     * 获取我的审核记录
     */
    public List<AuditRecord> getMyAuditHistory(Integer auditorId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return auditRecordMapper.selectByAuditorId(auditorId, offset, pageSize);
    }
}