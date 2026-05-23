package com.zhenmei.plugin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhenmei.plugin.config.SparkConfig;
import com.zhenmei.plugin.dto.JobSubmitRequest;
import com.zhenmei.plugin.entity.DependencyJar;
import com.zhenmei.plugin.entity.PySparkZip;
import com.zhenmei.plugin.entity.SparkJob;
import com.zhenmei.plugin.mapper.SparkJobMapper;
import com.zhenmei.plugin.service.DependencyService;
import com.zhenmei.plugin.service.PySparkZipService;
import com.zhenmei.plugin.service.RemoteSparkSubmitter;
import com.zhenmei.plugin.service.SparkJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SparkJobServiceImpl implements SparkJobService {

    private final SparkJobMapper sparkJobMapper;
    private final DependencyService dependencyService;
    private final SparkConfig sparkConfig;
    private final PySparkZipService pySparkZipService;
    private final RemoteSparkSubmitter remoteSubmitter;

    @Override
    public SparkJob submitJob(JobSubmitRequest request, String jarPath, String scriptPath) {
        boolean isPython = "PYTHON".equalsIgnoreCase(request.getJobType());

        String pyZipHdfsPath = null;
        if (isPython && request.getPySparkZipId() != null) {
            PySparkZip zip = pySparkZipService.getById(request.getPySparkZipId());
            if (zip != null) {
                pyZipHdfsPath = zip.getHdfsPath();
            }
        }

        log.info("pyZipHdfsPath:{}", pyZipHdfsPath);
        SparkJob job = new SparkJob();
        job.setJobName(request.getJobName());
        job.setJobType(StrUtil.blankToDefault(request.getJobType(), "JAR"));
        job.setJarPath(jarPath);
        job.setScriptPath(scriptPath);
        job.setPyZipPath(pyZipHdfsPath);
        job.setMainClass(request.getMainClass());
        job.setAppArgs(request.getAppArgs());
        job.setSparkProperties(request.getSparkProperties());
        job.setDeployMode(StrUtil.blankToDefault(request.getDeployMode(), sparkConfig.getDeployMode()));
        job.setMaster(StrUtil.blankToDefault(request.getMaster(), sparkConfig.getMaster()));
        job.setDriverMemory(request.getDriverMemory());
        job.setDriverCores(request.getDriverCores());
        job.setExecutorMemory(request.getExecutorMemory());
        job.setExecutorCores(request.getExecutorCores());
        job.setNumExecutors(request.getNumExecutors());
        job.setDependencyIds(request.getDependencyIds());
        job.setStatus("SUBMITTING");
        sparkJobMapper.insert(job);

        Long jobId = job.getId();
        String stagingDir = sparkConfig.getYarnStagingDir() + jobId;

        try {
            // 1. 上传 main jar/script 到 HDFS
            String mainResource = isPython ? scriptPath : jarPath;
            String mainHdfsPath = remoteSubmitter.uploadToHdfs(mainResource, stagingDir);

            // 2. 上传依赖 jars 到 HDFS
            List<String> dependencyHdfsPaths = new ArrayList<>();
            if (StrUtil.isNotBlank(request.getDependencyIds())) {
                List<Long> depIds = Arrays.stream(request.getDependencyIds().split(","))
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
                for (Long depId : depIds) {
                    List<DependencyJar> jars = dependencyService.getJars(depId);
                    for (DependencyJar depJar : jars) {
                        if (StrUtil.isNotBlank(depJar.getJarPath())) {
                            String depHdfsPath = remoteSubmitter.uploadToHdfs(depJar.getJarPath(), stagingDir + "/jars");
                            dependencyHdfsPaths.add(depHdfsPath);
                        }
                    }
                }
            }

            // 3. 上传 hive-site.xml
            String hiveSiteHdfsPath = null;
            if (StrUtil.isNotBlank(sparkConfig.getHiveSiteXml())) {
                hiveSiteHdfsPath = remoteSubmitter.uploadToHdfs(sparkConfig.getHiveSiteXml(), stagingDir);
            }

            // 4. 构建 Spark 配置 map
            Map<String, String> sparkConfMap = new HashMap<>();
            sparkConfMap.put("spark.app.name", job.getJobName());
            sparkConfMap.put("spark.master", job.getMaster());
            sparkConfMap.put("spark.submit.deployMode", job.getDeployMode());

            if (StrUtil.isNotBlank(request.getSparkProperties())) {
                for (String prop : request.getSparkProperties().split("\n")) {
                    prop = prop.trim();
                    if (prop.isEmpty() || prop.startsWith("#")) continue;
                    String[] kv = prop.split("=", 2);
                    if (kv.length == 2) {
                        sparkConfMap.put(kv[0].trim(), kv[1].trim());
                    }
                }
            }

            // 5. 通过 YARN API 提交
            log.info("[JOB-{}] 通过 YARN API 提交: {}", jobId, mainHdfsPath);
            String appId = remoteSubmitter.submitYarnApplication(
                    job.getJobName(),
                    mainHdfsPath,
                    request.getMainClass(),
                    request.getAppArgs(),
                    isPython,
                    pyZipHdfsPath,
                    dependencyHdfsPaths,
                    stagingDir,
                    sparkConfMap,
                    hiveSiteHdfsPath,
                    request.getDriverMemory(),
                    request.getDriverCores(),
                    request.getExecutorMemory(),
                    request.getExecutorCores(),
                    request.getNumExecutors()
            );

            job.setAppId(appId);
            job.setStatus("RUNNING");
            sparkJobMapper.updateById(job);

            // 6. 后台定期轮询 YARN 状态
            Long finalJobId = jobId;
            String finalAppId = appId;
            new Thread(() -> monitorYarnStatus(finalJobId, finalAppId), "yarn-monitor-" + jobId).start();

        } catch (Exception e) {
            log.error("[JOB-{}] 提交Spark任务失败", jobId, e);
            job.setStatus("FAILED");
            job.setErrorMsg("提交失败: " + e.getMessage());
            sparkJobMapper.updateById(job);
        }

        return job;
    }

    private void monitorYarnStatus(Long jobId, String appId) {
        try {
            while (true) {
                String state = remoteSubmitter.getAppState(appId);
                SparkJob job = sparkJobMapper.selectById(jobId);
                if (job == null) break;

                log.info("[JOB-{}] YARN 状态: {}, appId: {}", jobId, state, appId);

                switch (state) {
                    case "FINISHED":
                    case "SUCCEEDED":
                        job.setStatus("FINISHED");
                        sparkJobMapper.updateById(job);
                        return;
                    case "FAILED":
                    case "KILLED":
                        job.setStatus("FAILED");
                        job.setErrorMsg("YARN 状态: " + state);
                        sparkJobMapper.updateById(job);
                        return;
                    case "RUNNING":
                    case "ACCEPTED":
                    case "SUBMITTED":
                        if (!"RUNNING".equals(job.getStatus())) {
                            job.setStatus("RUNNING");
                            sparkJobMapper.updateById(job);
                        }
                        break;
                    default:
                        // UNKNOWN 等 - 继续等待
                        break;
                }

                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("[JOB-{}] 轮询 YARN 状态异常", jobId, e);
        }
    }

    @Override
    public SparkJob getJobById(Long id) {
        return sparkJobMapper.selectById(id);
    }

    @Override
    public Page<SparkJob> listJobs(int page, int size) {
        Page<SparkJob> p = new Page<>(page, size);
        LambdaQueryWrapper<SparkJob> w = new LambdaQueryWrapper<>();
        w.orderByDesc(SparkJob::getCreateTime);
        return sparkJobMapper.selectPage(p, w);
    }

    @Override
    public void killJob(Long id) {
        SparkJob job = sparkJobMapper.selectById(id);
        if (job != null && "RUNNING".equals(job.getStatus())) {
            if (StrUtil.isNotBlank(job.getAppId())) {
                try {
                    remoteSubmitter.killYarnApp(job.getAppId());
                    log.info("[JOB-{}] YARN application {} killed", id, job.getAppId());
                } catch (Exception e) {
                    log.error("[JOB-{}] kill YARN 失败: {}", id, e.getMessage());
                }
            }
            job.setStatus("KILLED");
            sparkJobMapper.updateById(job);
        }
    }
}
