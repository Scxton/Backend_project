package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.*;
import com.example.demo.utils.FileTools;
import com.example.demo.utils.FileZip;
import com.example.demo.utils.JSONResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@RestController
@Slf4j
public class FileController {

    @Resource
    private DownloadRecordsService downloadRecordsService;
    @Resource
    private FileRecordService fileRecordService;
    @Resource
    private AchievementTableService achievementTableService;
    @Resource
    private AchievementCheckTemplateService achievementCheckTemplateService;
    @Resource
    private VersionHistoryService versionHistoryService;
    @Resource
    private ApprovalRecordsService approvalRecordsService;

    private String templatePath = "C:/data/template/";
    private String achievementPath = "C:/data/achievement/";
    private String auditPath = "C:/auditspace/";
    /*
     * 模板上传
     * 输入：文件（不可多文件）
     * 返回：jsonResult
     */
    @PostMapping("/template/upload")
    public JSONResult uploadTemplate(@RequestParam("file") MultipartFile file) {
        JSONResult jsonResult = FileTools.upload(file, templatePath, 1);
        return jsonResult;
    }
    /*
     * 模板更新
     * 输入：文件（不可多文件）、模板id
     * 输出：jsonResult
     */
    @PostMapping("/template/update")
    public JSONResult updateTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("templateId") Integer templateId){
        String fileName = file.getOriginalFilename();
        AchievementCheckTemplate achievementCheckTemplate = new AchievementCheckTemplate();
        achievementCheckTemplate.setTemplateStoragepath(fileName);
        achievementCheckTemplate.setTemplateId(templateId);
        Integer res = this.achievementCheckTemplateService.update(achievementCheckTemplate);
        JSONResult jsonResult = FileTools.upload(file, templatePath, 1);
        if(jsonResult.getType()=="success"){
            jsonResult.setResultMsg("更新成功");
            jsonResult.setData(res);
        }
        return jsonResult;
    }
    /*
     * 模板下载记录提交
     * 输入：模板id，用户id
     * 输出：返回jsonResult，提交成功后jsonResult.data中包含文件名用于下载
     */
    @PostMapping("/template/downloadPage")
    public JSONResult templatedownloadPage(
            @RequestParam("templateId") Integer templateId,
            @RequestParam("userId") Integer userId){
        JSONResult jsonResult = new JSONResult();
        DownloadRecords downloadRecords = new DownloadRecords();
        AchievementCheckTemplate res = this.achievementCheckTemplateService.queryById(templateId);
        String fileName = res.getTemplateStoragepath();
        File dest = new File(templatePath+fileName);
        int statusCode;
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        downloadRecords.setDownloadTime(now.format(formatter2));
        downloadRecords.setTemplateId(templateId);
        downloadRecords.setUserId(userId);
        if(!dest.exists()){
            statusCode = HttpStatus.OK.value();
            jsonResult = new JSONResult("fail",statusCode,"表单提交失败","模板文件不存在");
            return jsonResult;
        }else{
            statusCode = HttpStatus.OK.value();
            Integer res_insert = this.downloadRecordsService.insert(downloadRecords);
            if(res_insert == 0){
                jsonResult = new JSONResult("fail",statusCode,"表单提交失败","下载记录表插入失败");
                return jsonResult;
            }
            jsonResult = new JSONResult("success",statusCode,"表单提交成功",fileName);
            return jsonResult;
        }
    }
    /*
     * 模板下载
     * 输入：文件名（从模板下载记录提交接口获得）
     * 输出：下载成功直接返回文件
     */
    @PostMapping("/template/download")
    public void downloadTemplate(HttpServletResponse response,
                                 @RequestParam("fileName") String fileName){
        try {
            FileTools.downloadfile(response, templatePath, fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*
     * 成功上传至待审核区
     * 输入：文件（可多文件）、成果表
     * 输出：jsonResult
     */
    @PostMapping("/achievement/uploadtoAudit")
    public JSONResult uploadtoAudit(
            @RequestParam("file") List<MultipartFile> files,
            @RequestPart(value = "achievementTable") AchievementTable achievementTable
    ){
        JSONResult jsonResult = new JSONResult();
        int statusCode = HttpStatus.OK.value();
        achievementTable.setAuditFlag(0);
        jsonResult.setResultCode(statusCode);
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        achievementTable.setUploadTime(now.format(formatter1));
        Integer res = this.achievementTableService.insert(achievementTable);
        if (res == 0) {
            jsonResult.setType("fail");
            jsonResult.setResultMsg("上传失败");
            jsonResult.setData("数据库写入失败");
            return jsonResult;
        }
        String achievementName = achievementTable.getAchievementName();
        String filePath = auditPath+achievementName+"/";

        MultipartFile file = null;

        for (int i = 0; i < files.size(); ++i) {
            file = files.get(i);
            jsonResult = FileTools.upload(file, filePath, i+1);
            if(jsonResult.getType() == "fail"){
                return jsonResult;
            }
        }
        String zipFilePath = auditPath+achievementName+now.format(formatter2)+".zip";
        ApprovalRecords approvalRecords = new ApprovalRecords();

        approvalRecords.setAchievementId(achievementTable.getAchievementId());
        approvalRecords.setApprovalStatus(0);
        approvalRecords.setApprovalType(achievementName+now.format(formatter2)+".zip");
        approvalRecords.setUserId(achievementTable.getUserId());
        approvalRecords.setTableStatus(true);
        File inputFile = new File(filePath);
        File outputFile = new File(zipFilePath);
        statusCode = HttpStatus.OK.value();
        try {
            FileZip.zipCompress(inputFile, outputFile);
            FileTools.forceDelete(inputFile);
            this.approvalRecordsService.insert(approvalRecords);
            jsonResult = new JSONResult("success", statusCode, "上传成功，等待审核", achievementName+now.format(formatter2));
            return jsonResult;
        } catch (IOException e) {
            jsonResult = new JSONResult("fail", statusCode, "上传失败", e.getMessage());
            e.printStackTrace();
            return jsonResult;
        }
    }

    /*
     * 更新文件上传至待审核区
     * 输入：文件（可多文件）、成果ID、新版本号
     * 输出：jsonResult
     */
    @PostMapping("/achievement/upgradetoAudit")
    public JSONResult upgradetoAudit(
            @RequestParam("file") List<MultipartFile> files,
            @RequestPart(value = "achievementId") Integer achievementId,
            @RequestPart(value = "newVersion") String newVersion
    ){
        JSONResult jsonResult = new JSONResult();
        int statusCode = HttpStatus.OK.value();
        AchievementTable achievementTable = this.achievementTableService.queryById(achievementId);
        achievementTable.setAuditFlag(2);
        achievementTable.setAchievementVersion(newVersion);
        jsonResult.setResultCode(statusCode);
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        achievementTable.setUploadTime(now.format(formatter1));
        Integer res = this.achievementTableService.update(achievementTable);
        if (res == 0) {
            jsonResult.setType("fail");
            jsonResult.setResultMsg("上传失败");
            jsonResult.setData("数据库写入失败");
            return jsonResult;
        }
        String achievementName = achievementTable.getAchievementName();
        String filePath = auditPath+achievementName+"/";

        MultipartFile file = null;

        for (int i = 0; i < files.size(); ++i) {
            file = files.get(i);
            jsonResult = FileTools.upload(file, filePath, i+1);
            if(jsonResult.getType() == "fail"){
                return jsonResult;
            }
        }
        String zipFilePath = auditPath+achievementName+now.format(formatter2)+".zip";
        ApprovalRecords approvalRecords = new ApprovalRecords();

        approvalRecords.setAchievementId(achievementTable.getAchievementId());
        approvalRecords.setApprovalStatus(3);
        approvalRecords.setApprovalType(achievementName+now.format(formatter2)+".zip");
        approvalRecords.setUserId(achievementTable.getUserId());
        approvalRecords.setTableStatus(true);
        File inputFile = new File(filePath);
        File outputFile = new File(zipFilePath);
        statusCode = HttpStatus.OK.value();
        try {
            FileZip.zipCompress(inputFile, outputFile);
            FileTools.forceDelete(inputFile);
            this.approvalRecordsService.insert(approvalRecords);
            jsonResult = new JSONResult("success", statusCode, "上传成功，等待审核", achievementName+now.format(formatter2));
            return jsonResult;
        } catch (IOException e) {
            jsonResult = new JSONResult("fail", statusCode, "上传失败", e.getMessage());
            e.printStackTrace();
            return jsonResult;
        }
    }
    /*
     * 待审核文件下载
     * 输入：文件名
     * 输出：成功直接返回文件
     */
    @PostMapping("/achievement/downloadtoAudit")
    public void downloadtoAudit(HttpServletResponse response,
                                @RequestParam("fileName") String fileName){
        log.info("downloadtoAudit");
        log.info("fileName:"+fileName);
        try {
            log.info("auditPath:"+auditPath);
            log.info("fileName:"+fileName);
            log.info("response"+response);
            FileTools.downloadfile(response, auditPath, fileName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*
     * 查询成果最新版本文件列表
     * 输入：成果id
     * 输出：jsonResult，其中包含一个list列出所有文件名
     */
    @SuppressWarnings("resource")
    @PostMapping("/achievement/getfile")
    public JSONResult getfile(@RequestParam("achievementId") Integer achievementId){
        JSONResult jsonResult = new JSONResult();
        int statusCode;
        FileRecord fileRecord = new FileRecord();
        fileRecord.setAchievementId(achievementId);
        List<FileRecord> allFileRecords = this.fileRecordService.queryAllByLimit(fileRecord);
        FileRecord neddRecord = allFileRecords.get(allFileRecords.size() - 1);
        String zipName = neddRecord.getFileName();
        List<String> fileList = new ArrayList<>();
        try {
            ZipFile zipFile = new ZipFile(achievementPath+zipName);
            statusCode = HttpStatus.OK.value();
            for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();){
                ZipEntry entry=e.nextElement();
                fileList.add(entry.getName());
            }
            jsonResult = new JSONResult("success", statusCode, "查询成功", fileList);
        } catch (IOException e) {
            e.printStackTrace();
            statusCode = HttpStatus.OK.value();
            jsonResult = new JSONResult("fail", statusCode, "查询失败", e.getMessage());
            return jsonResult;
        }
        return jsonResult;

    }
    /*
     * 审核完成文件归档
     * 输入：
     * 成果名、用户id、
     * 操作id（0新增成果归档、1更新文件审核、3为未通过审核文件删除）
     * 审核完成文件名、新成果版本号（添加和修改文件时需要）、修改文件名（修改文件时需要）、更新内容（修改文件时需要）
     * 输出：jsonResult
     */
    @PostMapping("/achievement/AuditComplete")
    public JSONResult AuditComplete(
            @RequestParam("operationId") Integer operationId,
            @RequestParam("approvalId") Integer approvalId
    ){
        log.info("auditComplete");
        JSONResult jsonResult = new JSONResult();
        int statusCode;
        statusCode = HttpStatus.OK.value();

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        ApprovalRecords approvalRecord = this.approvalRecordsService.queryById(approvalId);
        String AuditFileName = approvalRecord.getApprovalType();
        Integer achievementId = approvalRecord.getAchievementId();
        String zipFilePath = auditPath+AuditFileName;
        approvalRecord.setApprovalTime(now.format(formatter2));
        approvalRecord.setApprovalStatus(1);

        AchievementTable achievement = this.achievementTableService.queryById(achievementId);
        File startFile=new File(zipFilePath);
        Integer ProjectId = achievement.getProjectId();
        String achievementName = achievement.getAchievementName();

        if(operationId == 0){
            Integer userId = achievement.getUserId();
            String achievementVersion = achievement.getAchievementVersion();
            File endDirection=new File(achievementPath);
            log.info("startFile:"+startFile.getPath());
            log.info("endDirection:"+endDirection.getPath());
            if(!endDirection.exists()) {
                endDirection.mkdirs();
            }

            FileRecord fileRecord = new FileRecord();
            fileRecord.setAchievementId(achievementId);
            fileRecord.setProjectId(ProjectId);
            fileRecord.setUserId(userId);
            fileRecord.setUploadTime(now.format(formatter2));

            VersionHistory versionHistory = new VersionHistory();
            versionHistory.setAchievementId(achievementId);
            versionHistory.setUserId(userId);
            versionHistory.setUpdateTime(now.format(formatter2));
            versionHistory.setVersionNumber(achievementVersion);
            log.info("operationId == 0");

            achievement.setAuditFlag(1);
            log.info("approvalRecord finished");
            String endfilename = achievementName+achievementVersion+".zip";
            File endFile=new File(endDirection+File.separator+endfilename);
            startFile.renameTo(endFile);
            fileRecord.setFileName(endfilename);
            log.info("end file name "+endfilename);
            log.info("endFile.renameTo "+endfilename);
            if(!endFile.exists()) {
                jsonResult = new JSONResult("fail", statusCode, "操作失败", "文件归档失败");
                return jsonResult;
            }
            Integer res_vh = this.versionHistoryService.insert(versionHistory);
            Integer res_fr = this.fileRecordService.insert(fileRecord);
            Integer res_ar = this.approvalRecordsService.update(approvalRecord);
            Integer res_at = this.achievementTableService.update(achievement);
            jsonResult = new JSONResult("success", statusCode, "操作成功", "成果上传成功");
            return jsonResult;

        }else if(operationId == 1){
            return jsonResult;
        }else if(operationId == 2){
            return jsonResult;
        }else if(operationId ==3){
            if(achievement.getAuditFlag() == 0){
                this.achievementTableService.deleteById(achievementId);
                approvalRecord.setApprovalStatus(2);
                log.info("approvalRecord finished");
                this.approvalRecordsService.update(approvalRecord);
                try {
                    FileTools.forceDelete(startFile);
                    jsonResult = new JSONResult("success", statusCode, "操作成功", "未通过审核文件已删除");
                } catch (IOException e) {
                    jsonResult = new JSONResult("fail", statusCode, "操作失败", e.getMessage());
                    e.printStackTrace();
                    return jsonResult;
                }
                return jsonResult;
            }else if(achievement.getAuditFlag() == 2){
                achievement.setAuditFlag(1);
                VersionHistory versionHistory = new VersionHistory();
                versionHistory.setAchievementId(achievementId);
                List<VersionHistory> veList = this.versionHistoryService.querylimitWithPagination(versionHistory, 1, 10000);
                achievement.setAchievementVersion(veList.get(veList.size()-1).getVersionNumber());
                achievement.setUploadTime(veList.get(veList.size()-1).getUpdateTime());
                this.achievementTableService.update(achievement);
                approvalRecord.setApprovalStatus(2);
                log.info("approvalRecord finished");
                this.approvalRecordsService.update(approvalRecord);
                try {
                    FileTools.forceDelete(startFile);
                    jsonResult = new JSONResult("success", statusCode, "操作成功", "未通过审核文件已删除");
                } catch (IOException e) {
                    jsonResult = new JSONResult("fail", statusCode, "操作失败", e.getMessage());
                    e.printStackTrace();
                    return jsonResult;
                }
                return jsonResult;
            }else{
                jsonResult = new JSONResult("fail", statusCode, "操作失败", "operationId无效");
                return jsonResult;
            }

        }else{
            jsonResult = new JSONResult("fail", statusCode, "操作失败", "operationId无效");
            return jsonResult;
        }
    }
    /*
     * 提交成果下载表单
     * 输入：成果id，支持多输入，用户id
     * 输出：jsonResult，提交成功则jsonResult.data中包含下载文件名，用于后续下载操作
     */
    @PostMapping("/achievement/downloadPage")
    public JSONResult achievementdownloadPage(
            @RequestParam(value = "achievementIds", required = false) List<Integer> achievementIds,
            @RequestParam("userId") Integer userId){
        log.info("downloadPage start");
        JSONResult jsonResult = new JSONResult();
        int statusCode;
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<String> fileNames = new ArrayList<>();

        String fileName;
        for(Integer achievementId : achievementIds){

            AchievementTable res = this.achievementTableService.queryById(achievementId);

            if (res == null) {
                statusCode = HttpStatus.NOT_FOUND.value();
                jsonResult = new JSONResult("fail", statusCode, "表单提交失败", "未找到成果数据");
                return jsonResult;
            }
            fileName = res.getAchievementName()+res.getAchievementVersion()+".zip";

            File dest = new File(achievementPath+fileName);

            if(!dest.exists()){
                statusCode = HttpStatus.OK.value();
                jsonResult = new JSONResult("fail",statusCode,"表单提交失败",res.getAchievementName()+"文件不存在");
                return jsonResult;
            }
        }
        for(Integer achievementId : achievementIds){
            DownloadRecords downloadRecords = new DownloadRecords();
            AchievementTable res = this.achievementTableService.queryById(achievementId);

            if (res == null) {
                statusCode = HttpStatus.NOT_FOUND.value();
                jsonResult = new JSONResult("fail", statusCode, "表单提交失败", "未找到成果数据");
                return jsonResult;
            }

            downloadRecords.setDownloadTime(now.format(formatter2));
            downloadRecords.setUserId(userId);
            fileName = res.getAchievementName()+res.getAchievementVersion()+".zip";

            fileNames.add(fileName);

            downloadRecords.setAchievementId(achievementId);


            Integer res_insert = this.downloadRecordsService.insert(downloadRecords);//

            if(res_insert == 0){
                statusCode = HttpStatus.OK.value();
                jsonResult = new JSONResult("fail",statusCode,"表单提交失败","下载记录表插入失败");
                return jsonResult;
            }

            res.setAchievementDownloadCount(res.getAchievementDownloadCount()+1);

            Integer res_edit = this.achievementTableService.update(res);

            if(res_edit == 0){
                statusCode = HttpStatus.OK.value();
                jsonResult = new JSONResult("fail",statusCode,"表单提交失败","下载统计次数修改失败");
                return jsonResult;
            }
        }

        statusCode = HttpStatus.OK.value();
        jsonResult = new JSONResult("success",statusCode,"表单提交成功",fileNames);
        log.info("success");
        log.info("jsonResult",jsonResult.getData());
        return jsonResult;

    }
    /*
     * 成果下载
     * 输入：文件名，支持输入多个文件名（下载表单提交后获得）
     * 输出：调用成功直接返回文件
     */
    @PostMapping("/achievement/download")
    public void downloadachievement(HttpServletResponse response,
                                    @RequestParam("fileNames") List<String> fileNames) throws IOException {
        log.info("downloadachievement");
        System.out.println(fileNames);
        log.info("fileNames "+fileNames);
        if(fileNames.size() == 1){
            try {
                log.info("fileNames ==1");

                FileTools.downloadfile(response, achievementPath, fileNames.get(0));
                log.info("单文件下载成功");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            try {
                log.info("fileNames !=1");
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
                String destDir = achievementPath+File.separator+now.format(formatter2);
                File destDirF = new File(destDir);
                destDirF.mkdirs();
                for(String fileName : fileNames){
                    File source = new File(achievementPath+File.separator+fileName);
                    File dest = new File(destDir+File.separator+fileName);
                    FileUtils.copyFile(source, dest);
                }
                File outputFile = new File(destDir+".zip");
                FileZip.zipCompress(destDirF, outputFile);
                FileTools.downloadfile(response, achievementPath, now.format(formatter2)+".zip");
                FileTools.forceDelete(destDirF);
                FileTools.forceDelete(outputFile);
                log.info("多个文件下载成功");
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
