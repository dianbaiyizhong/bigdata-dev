package com.zhenmei.plugin.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhenmei.plugin.config.SparkConfig;
import com.zhenmei.plugin.dto.JobSubmitRequest;
import com.zhenmei.plugin.entity.DependencyJar;
import com.zhenmei.plugin.entity.JobTemplate;
import com.zhenmei.plugin.entity.PySparkZip;
import com.zhenmei.plugin.entity.SparkJob;
import com.zhenmei.plugin.mapper.SparkJobMapper;
import com.zhenmei.plugin.service.DependencyService;
import com.zhenmei.plugin.service.JobTemplateService;
import com.zhenmei.plugin.service.PySparkZipService;
import com.zhenmei.plugin.service.SparkJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkLauncher;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SparkJobServiceImpl implements SparkJobService {

    private final SparkJobMapper sparkJobMapper;
    private final DependencyService dependencyService;
    private final SparkConfig sparkConfig;
    private final PySparkZipService pySparkZipService;
    private final JobTemplateService jobTemplateService;

    private static final Set<String> RETRYABLE_STATUSES = Set.of("FAILED", "KILLED", "FINISHED");

    @Override
    public SparkJob submitJob(JobSubmitRequest request, String jarPath, String scriptPath) {
        if (request == null) {
            request = new JobSubmitRequest();
        }

        if (request.getTemplateId() != null) {
            JobTemplate template = getTemplate(request.getTemplateId());
            validateTemplateFiles(template);
            request = mergeTemplateRequest(request, template);
            jarPath = template.getJarPath();
            scriptPath = template.getScriptPath();
        } else if (StrUtil.isBlank(request.getJobType())) {
            request.setJobType("JAR");
        }

        if (StrUtil.isBlank(request.getJobName())) {
            throw new IllegalArgumentException("jobName不能为空");
        }

        boolean isPython = "PYTHON".equalsIgnoreCase(request.getJobType());

        String pyZipPath = null;
        if (isPython && request.getPySparkZipId() != null) {
            PySparkZip zip = pySparkZipService.getById(request.getPySparkZipId());
            if (zip != null) {
                pyZipPath = zip.getHdfsPath();
            }
        }

        String entryFile = request.getEntryFile();
        boolean isZipProject = isPython && StrUtil.isNotBlank(entryFile)
                && StrUtil.isNotBlank(scriptPath) && scriptPath.toLowerCase(Locale.ROOT).endsWith(".zip");

        String extractedEntryPath = null;
        if (isZipProject) {
            extractedEntryPath = extractEntryFromZip(scriptPath, entryFile);
            log.info("从 zip 提取入口文件: {} -> {}", entryFile, extractedEntryPath);
        }

        SparkJob job = new SparkJob();
        job.setJobName(request.getJobName());
        job.setJobType(StrUtil.blankToDefault(request.getJobType(), "JAR"));
        job.setJarPath(jarPath);
        job.setScriptPath(scriptPath);
        job.setPyZipPath(pyZipPath);
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
        job.setEntryFile(entryFile);
        job.setStatus("SUBMITTING");

        return launchJob(job, extractedEntryPath);
    }

    private JobTemplate getTemplate(Long templateId) {
        if (jobTemplateService == null) {
            throw new IllegalStateException("任务模板服务未配置");
        }
        JobTemplate template = jobTemplateService.getById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("任务模板不存在: " + templateId);
        }
        return template;
    }

    private void validateTemplateFiles(JobTemplate template) {
        String jobType = StrUtil.blankToDefault(template.getJobType(), "JAR").toUpperCase(Locale.ROOT);
        String filePath;
        String fileType;
        if ("JAR".equals(jobType)) {
            filePath = template.getJarPath();
            fileType = "JAR";
        } else if ("PYTHON".equals(jobType)) {
            filePath = template.getScriptPath();
            fileType = "Python";
            if (StrUtil.isNotBlank(filePath)
                    && filePath.toLowerCase(Locale.ROOT).endsWith(".zip")
                    && StrUtil.isBlank(template.getEntryFile())) {
                throw new IllegalArgumentException("Python ZIP任务模板缺少entryFile");
            }
        } else {
            throw new IllegalArgumentException("任务模板jobType不合法: " + template.getJobType());
        }

        if (StrUtil.isBlank(filePath) || !isRegularFile(filePath)) {
            throw new IllegalArgumentException("任务模板文件不存在: " + template.getTemplateName()
                    + " (" + fileType + ")");
        }
    }

    private boolean isRegularFile(String value) {
        try {
            return Files.isRegularFile(Paths.get(value));
        } catch (RuntimeException e) {
            return false;
        }
    }

    JobSubmitRequest mergeTemplateRequest(JobSubmitRequest request, JobTemplate template) {
        if (template == null) {
            throw new IllegalArgumentException("任务模板不能为空");
        }
        if (request == null) {
            request = new JobSubmitRequest();
        }

        JobSubmitRequest merged = new JobSubmitRequest();
        merged.setTemplateId(request.getTemplateId());
        merged.setJobName(request.getJobName() != null
                ? request.getJobName()
                : StrUtil.blankToDefault(template.getTemplateName(), "template-" + template.getId()));
        merged.setJobType(StrUtil.blankToDefault(template.getJobType(), "JAR"));
        merged.setMainClass(template.getMainClass());
        merged.setEntryFile(template.getEntryFile());
        merged.setPySparkZipId(template.getPySparkZipId());
        merged.setDependencyIds(template.getDependencyIds());
        merged.setAppArgs(request.getAppArgs() != null ? request.getAppArgs() : template.getAppArgs());
        merged.setSparkProperties(request.getSparkProperties() != null
                ? request.getSparkProperties()
                : template.getSparkProperties());
        merged.setDeployMode(template.getDeployMode());
        merged.setMaster(template.getMaster());
        merged.setDriverMemory(request.getDriverMemory() != null
                ? request.getDriverMemory()
                : template.getDriverMemory());
        merged.setDriverCores(request.getDriverCores() != null
                ? request.getDriverCores()
                : template.getDriverCores());
        merged.setExecutorMemory(request.getExecutorMemory() != null
                ? request.getExecutorMemory()
                : template.getExecutorMemory());
        merged.setExecutorCores(request.getExecutorCores() != null
                ? request.getExecutorCores()
                : template.getExecutorCores());
        merged.setNumExecutors(request.getNumExecutors() != null
                ? request.getNumExecutors()
                : template.getNumExecutors());
        return merged;
    }

    private SparkJob launchJob(SparkJob job, String extractedEntryPath) {
        String scriptPath = job.getScriptPath();
        boolean isPython = "PYTHON".equalsIgnoreCase(job.getJobType());
        boolean isZipProject = isPython && StrUtil.isNotBlank(job.getEntryFile())
                && StrUtil.isNotBlank(scriptPath) && scriptPath.endsWith(".zip");

        sparkJobMapper.insert(job);

        try {
            SparkLauncher launcher;

            if (isPython) {
                String mainScript = isZipProject ? extractedEntryPath : scriptPath;
                String archivePath = StrUtil.isNotBlank(job.getPyZipPath())
                        ? "hdfs:" + job.getPyZipPath()
                        : "hdfs:/user/pyspark-libs/pyspark_env.zip";
                launcher = new SparkLauncher()
                        .setAppResource(mainScript)
                        .setMaster(job.getMaster())
                        .addSparkArg("--archives", archivePath + "#PY3")
                        .setConf("spark.executorEnv.PYSPARK_PYTHON", "./PY3/bin/python")
                        .setConf("spark.yarn.appMasterEnv.PYSPARK_PYTHON", "./PY3/bin/python")
                        .setDeployMode(job.getDeployMode())
                        .setAppName(job.getJobName())
                        .setVerbose(true);

                if (isZipProject) {
                    launcher.addPyFile(scriptPath);
                }
            } else {
                launcher = new SparkLauncher()
                        .setAppResource(job.getJarPath())
                        .setMainClass(job.getMainClass())
                        .setMaster(job.getMaster())
                        .setDeployMode(job.getDeployMode())
                        .setAppName(job.getJobName())
                        .setVerbose(true);
            }

            if (StrUtil.isNotBlank(sparkConfig.getSparkHome())) {
                launcher.setSparkHome(sparkConfig.getSparkHome());
            }
            if (StrUtil.isNotBlank(sparkConfig.getHiveSiteXml())) {
                launcher.addFile(sparkConfig.getHiveSiteXml());
                launcher.setConf("spark.sql.catalogImplementation", "hive");
                log.info("[JOB-{}] 添加 hive-site.xml: {}", job.getId(), sparkConfig.getHiveSiteXml());
            }
            if (StrUtil.isNotBlank(job.getAppArgs())) {
                launcher.addAppArgs(job.getAppArgs().split("\\s+"));
            }
            if (job.getDriverMemory() != null) {
                launcher.setConf(SparkLauncher.DRIVER_MEMORY, job.getDriverMemory() + "m");
            }
            if (job.getDriverCores() != null) {
                launcher.setConf("spark.driver.cores", String.valueOf(job.getDriverCores()));
            }
            if (job.getExecutorMemory() != null) {
                launcher.setConf(SparkLauncher.EXECUTOR_MEMORY, job.getExecutorMemory() + "m");
            }
            if (job.getExecutorCores() != null) {
                launcher.setConf(SparkLauncher.EXECUTOR_CORES, String.valueOf(job.getExecutorCores()));
            }
            if (job.getNumExecutors() != null) {
                launcher.setConf("spark.executor.instances", String.valueOf(job.getNumExecutors()));
            }
            if (StrUtil.isNotBlank(job.getSparkProperties())) {
                for (String prop : job.getSparkProperties().split("\n")) {
                    prop = prop.trim();
                    if (prop.isEmpty() || prop.startsWith("#")) continue;
                    String[] kv = prop.split("=", 2);
                    if (kv.length == 2) {
                        launcher.setConf(kv[0].trim(), kv[1].trim());
                    }
                }
            }

            if (StrUtil.isNotBlank(job.getDependencyIds())) {
                List<Long> depIds = Arrays.stream(job.getDependencyIds().split(","))
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
                        if ("KILLED".equals(j.getStatus())) {
                            log.info("[JOB-{}] 任务已被终止，跳过状态更新", jobId);
                        } else if (exitCode == 0) {
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
                    if (j != null && !"KILLED".equals(j.getStatus())) {
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
    public SparkJob retryJob(Long id) {
        SparkJob original = sparkJobMapper.selectById(id);
        if (original == null) {
            throw new RuntimeException("任务不存在: " + id);
        }
        if (!RETRYABLE_STATUSES.contains(original.getStatus())) {
            throw new RuntimeException("仅已结束(失败/终止/已完成)状态的任务可以重试，当前状态: " + original.getStatus());
        }

        SparkJob job = new SparkJob();
        job.setJobName(original.getJobName());
        job.setJobType(original.getJobType());
        job.setJarPath(original.getJarPath());
        job.setScriptPath(original.getScriptPath());
        job.setPyZipPath(original.getPyZipPath());
        job.setEntryFile(original.getEntryFile());
        job.setMainClass(original.getMainClass());
        job.setAppArgs(original.getAppArgs());
        job.setSparkProperties(original.getSparkProperties());
        job.setDeployMode(original.getDeployMode());
        job.setMaster(original.getMaster());
        job.setDriverMemory(original.getDriverMemory());
        job.setDriverCores(original.getDriverCores());
        job.setExecutorMemory(original.getExecutorMemory());
        job.setExecutorCores(original.getExecutorCores());
        job.setNumExecutors(original.getNumExecutors());
        job.setDependencyIds(original.getDependencyIds());
        job.setStatus("SUBMITTING");

        String extractedEntryPath = null;
        boolean isZipProject = "PYTHON".equalsIgnoreCase(job.getJobType())
                && StrUtil.isNotBlank(job.getEntryFile())
                && StrUtil.isNotBlank(job.getScriptPath()) && job.getScriptPath().endsWith(".zip");
        if (isZipProject) {
            extractedEntryPath = extractEntryFromZip(job.getScriptPath(), job.getEntryFile());
            log.info("[JOB-{}] 重试: 从 zip 重新提取入口文件 {} -> {}", id, job.getEntryFile(), extractedEntryPath);
        }

        log.info("[JOB-{}] 重试任务(原状态: {}), 复用配置启动新记录", id, original.getStatus());
        return launchJob(job, extractedEntryPath);
    }

    private String extractEntryFromZip(String zipPath, String entryFile) {
        File tmpDir = new File(FileUtil.getTmpDirPath(), "pyspark-extracted");
        if (!tmpDir.exists()) tmpDir.mkdirs();

        String entryName = new File(entryFile).getName();
        File output = new File(tmpDir, System.currentTimeMillis() + "_" + entryName);
        String normalizedEntry = entryFile.replace("\\", "/");

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                String name = ze.getName().replace("\\", "/");
                if (name.equals(normalizedEntry) || name.endsWith("/" + normalizedEntry)) {
                    Files.copy(zis, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    return output.getAbsolutePath();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("解压入口文件失败: " + e.getMessage(), e);
        }

        throw new RuntimeException("在压缩包中未找到入口文件: " + entryFile);
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
    public String killJob(Long id) {
        SparkJob job = sparkJobMapper.selectById(id);
        if (job == null) {
            return "任务不存在: " + id;
        }
        if (!"RUNNING".equals(job.getStatus())) {
            return "当前状态 " + job.getStatus() + ", 无法终止";
        }
        if (StrUtil.isBlank(job.getAppId())) {
            return "尚未捕获到 appId, 无法通过 yarn 终止";
        }
        try {
            ProcessBuilder pb = new ProcessBuilder("yarn", "application", "-kill", job.getAppId());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = p.waitFor();
            if (exitCode == 0) {
                log.info("[JOB-{}] YARN application {} killed: {}", id, job.getAppId(), output.trim());
                job.setStatus("KILLED");
                sparkJobMapper.updateById(job);
                return null;
            } else {
                log.error("[JOB-{}] yarn kill 失败, exitCode={}, output:\n{}", id, exitCode, output);
                job.setErrorMsg("终止 YARN application 失败: " + output.trim());
                sparkJobMapper.updateById(job);
                return "终止 YARN application 失败: " + output.trim();
            }
        } catch (IOException e) {
            log.error("[JOB-{}] kill YARN 失败, yarn 命令不可用或构建失败: {}", id, e.getMessage());
            job.setErrorMsg("终止失败: yarn 命令执行异常 - " + e.getMessage());
            sparkJobMapper.updateById(job);
            return "终止失败: yarn 命令执行异常 - " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[JOB-{}] kill YARN 被中断: {}", id, e.getMessage());
            job.setErrorMsg("终止失败: " + e.getMessage());
            sparkJobMapper.updateById(job);
            return "终止失败: " + e.getMessage();
        }
    }
}
