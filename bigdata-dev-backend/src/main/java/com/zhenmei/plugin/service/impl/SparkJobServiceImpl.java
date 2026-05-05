package com.zhenmei.plugin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhenmei.plugin.config.SparkConfig;
import com.zhenmei.plugin.dto.JobSubmitRequest;
import com.zhenmei.plugin.entity.DependencyJar;
import com.zhenmei.plugin.entity.SparkJob;
import com.zhenmei.plugin.mapper.SparkJobMapper;
import com.zhenmei.plugin.service.DependencyService;
import com.zhenmei.plugin.service.SparkJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkLauncher;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SparkJobServiceImpl implements SparkJobService {

    private final SparkJobMapper sparkJobMapper;
    private final DependencyService dependencyService;
    private final SparkConfig sparkConfig;

    @Override
    public SparkJob submitJob(JobSubmitRequest request, String jarPath) {
        SparkJob job = new SparkJob();
        job.setJobName(request.getJobName());
        job.setJarPath(jarPath);
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

        try {
            SparkLauncher launcher = new SparkLauncher()
                    .setAppResource(jarPath)
                    .setMainClass(request.getMainClass())
                    .setMaster(job.getMaster())
                    .setDeployMode(job.getDeployMode())
                    .setAppName(request.getJobName())
                    .setVerbose(true);

            if (StrUtil.isNotBlank(sparkConfig.getSparkHome())) {
                launcher.setSparkHome(sparkConfig.getSparkHome());
            }
            if (StrUtil.isNotBlank(request.getAppArgs())) {
                launcher.addAppArgs(request.getAppArgs().split("\\s+"));
            }
            if (request.getDriverMemory() != null) {
                launcher.setConf(SparkLauncher.DRIVER_MEMORY, request.getDriverMemory() + "m");
            }
            if (request.getDriverCores() != null) {
                launcher.setConf("spark.driver.cores", String.valueOf(request.getDriverCores()));
            }
            if (request.getExecutorMemory() != null) {
                launcher.setConf(SparkLauncher.EXECUTOR_MEMORY, request.getExecutorMemory() + "m");
            }
            if (request.getExecutorCores() != null) {
                launcher.setConf(SparkLauncher.EXECUTOR_CORES, String.valueOf(request.getExecutorCores()));
            }
            if (request.getNumExecutors() != null) {
                launcher.setConf("spark.executor.instances", String.valueOf(request.getNumExecutors()));
            }
            if (StrUtil.isNotBlank(request.getSparkProperties())) {
                for (String prop : request.getSparkProperties().split("\n")) {
                    prop = prop.trim();
                    if (prop.isEmpty() || prop.startsWith("#")) continue;
                    String[] kv = prop.split("=", 2);
                    if (kv.length == 2) {
                        launcher.setConf(kv[0].trim(), kv[1].trim());
                    }
                }
            }

            if (StrUtil.isNotBlank(request.getDependencyIds())) {
                List<Long> depIds = Arrays.stream(request.getDependencyIds().split(","))
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
                for (Long depId : depIds) {
                    List<DependencyJar> jars = dependencyService.getJars(depId);
                    for (DependencyJar jar : jars) {
                        if (StrUtil.isNotBlank(jar.getJarPath())) {
                            launcher.addJar(jar.getJarPath());
                        }
                    }
                }
            }

            Process spark = launcher.launch();
            Long jobId = job.getId();

            Pattern appIdPattern = Pattern.compile("application_\\d+_\\d+");
            AtomicBoolean appIdSet = new AtomicBoolean(false);

            StringBuilder stdoutBuilder = new StringBuilder();
            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(spark.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdoutBuilder.append(line).append("\n");
                        log.info("[JOB-{}][STDOUT] {}", jobId, line);

                        if (!appIdSet.get()) {
                            Matcher m = appIdPattern.matcher(line);
                            if (m.find()) {
                                String appId = m.group();
                                appIdSet.set(true);
                                SparkJob update = new SparkJob();
                                update.setId(jobId);
                                update.setAppId(appId);
                                sparkJobMapper.updateById(update);
                                log.info("[JOB-{}] 捕获到 appId: {}", jobId, appId);
                            }
                        }
                    }
                } catch (IOException e) {
                    log.warn("[JOB-{}] 读取stdout异常", jobId, e);
                }
            }, "stdout-reader-" + jobId);

            StringBuilder stderrBuilder = new StringBuilder();
            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(spark.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderrBuilder.append(line).append("\n");
                        log.warn("[JOB-{}][STDERR] {}", jobId, line);

                        if (!appIdSet.get()) {
                            Matcher m = appIdPattern.matcher(line);
                            if (m.find()) {
                                String appId = m.group();
                                appIdSet.set(true);
                                SparkJob update = new SparkJob();
                                update.setId(jobId);
                                update.setAppId(appId);
                                sparkJobMapper.updateById(update);
                                log.info("[JOB-{}] 捕获到 appId (stderr): {}", jobId, appId);
                            }
                        }
                    }
                } catch (IOException e) {
                    log.warn("[JOB-{}] 读取stderr异常", jobId, e);
                }
            }, "stderr-reader-" + jobId);

            stdoutThread.start();
            stderrThread.start();

            // 后台等待进程结束
            new Thread(() -> {
                try {
                    int exitCode = spark.waitFor();
                    stdoutThread.join(30000);
                    stderrThread.join(30000);

                    log.info("[JOB-{}] 进程退出码: {}", jobId, exitCode);

                    SparkJob j = sparkJobMapper.selectById(jobId);
                    if (j != null) {
                        if (exitCode == 0) {
                            j.setStatus("FINISHED");
                        } else {
                            j.setStatus("FAILED");
                            String errMsg = stderrBuilder.toString();
                            if (errMsg.isEmpty()) {
                                errMsg = "进程退出码: " + exitCode;
                            }
                            // 截取最后5000字符
                            if (errMsg.length() > 5000) {
                                errMsg = "...(前略)\n" + errMsg.substring(errMsg.length() - 5000);
                            }
                            j.setErrorMsg(errMsg);
                            log.error("[JOB-{}] 失败, 退出码: {}, stderr:\n{}", jobId, exitCode, errMsg);
                        }
                        sparkJobMapper.updateById(j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    SparkJob j = sparkJobMapper.selectById(jobId);
                    if (j != null) {
                        j.setStatus("FAILED");
                        j.setErrorMsg("任务被中断: " + e.getMessage());
                        sparkJobMapper.updateById(j);
                    }
                }
            }, "job-waiter-" + jobId).start();

            job.setStatus("RUNNING");
            sparkJobMapper.updateById(job);

        } catch (IOException e) {
            log.error("提交Spark任务失败", e);
            job.setStatus("FAILED");
            job.setErrorMsg("启动失败: " + e.getMessage());
            sparkJobMapper.updateById(job);
        }

        return job;
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
            job.setStatus("KILLED");
            sparkJobMapper.updateById(job);
        }
    }
}
