package com.zhenmei.plugin.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhenmei.plugin.dto.JobSubmitRequest;
import com.zhenmei.plugin.entity.SparkJob;
import com.zhenmei.plugin.response.ApiResponse;
import com.zhenmei.plugin.service.SparkJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class SparkJobController {

    private final SparkJobService sparkJobService;

    private static final String JAR_DIR = FileUtil.getTmpDirPath() + File.separator+ "plugin-app-jars" + File.separator;
    private static final String SCRIPT_DIR = FileUtil.getTmpDirPath() + File.separator+ "plugin-app-scripts" + File.separator;

    @PostMapping("/submit")
    public ApiResponse<SparkJob> submit(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "pyScript", required = false) MultipartFile pyScript,
            @RequestParam String jobName,
            @RequestParam String jobType,
            @RequestParam(required = false) String mainClass,
            @RequestParam(required = false) String appArgs,
            @RequestParam(required = false) String deployMode,
            @RequestParam(required = false) String master,
            @RequestParam(required = false) Integer driverMemory,
            @RequestParam(required = false) Integer driverCores,
            @RequestParam(required = false) Integer executorMemory,
            @RequestParam(required = false) Integer executorCores,
            @RequestParam(required = false) Integer numExecutors,
            @RequestParam(required = false) String sparkProperties,
            @RequestParam(required = false) String dependencyIds,
            @RequestParam(required = false) Long pySparkZipId) throws IOException {

        JobSubmitRequest request = new JobSubmitRequest();
        request.setJobName(jobName);
        request.setJobType(jobType);
        request.setMainClass(mainClass);
        request.setAppArgs(appArgs);
        request.setDeployMode(deployMode);
        request.setMaster(master);
        request.setDriverMemory(driverMemory);
        request.setDriverCores(driverCores);
        request.setExecutorMemory(executorMemory);
        request.setExecutorCores(executorCores);
        request.setNumExecutors(numExecutors);
        request.setSparkProperties(sparkProperties);
        request.setDependencyIds(dependencyIds);
        request.setPySparkZipId(pySparkZipId);

        String jarPath = null;
        String scriptPath = null;

        if (file != null && !file.isEmpty()) {
            File dir = new File(JAR_DIR);
            if (!dir.exists()) dir.mkdirs();
            String savedName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File dest = new File(dir, savedName);
            file.transferTo(dest);
            jarPath = dest.getAbsolutePath();
        }

        if (pyScript != null && !pyScript.isEmpty()) {
            File dir = new File(SCRIPT_DIR);
            if (!dir.exists()) dir.mkdirs();
            String savedName = System.currentTimeMillis() + "_" + pyScript.getOriginalFilename();
            File dest = new File(dir, savedName);
            pyScript.transferTo(dest);
            scriptPath = dest.getAbsolutePath();
        }

        return ApiResponse.success(sparkJobService.submitJob(request, jarPath, scriptPath));
    }

    @GetMapping("/{id}")
    public ApiResponse<SparkJob> getById(@PathVariable Long id) {
        return ApiResponse.success(sparkJobService.getJobById(id));
    }

    @GetMapping("/list")
    public ApiResponse<Page<SparkJob>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(sparkJobService.listJobs(page, size));
    }

    @PostMapping("/{id}/kill")
    public ApiResponse<Void> kill(@PathVariable Long id) {
        sparkJobService.killJob(id);
        return ApiResponse.success();
    }
}
