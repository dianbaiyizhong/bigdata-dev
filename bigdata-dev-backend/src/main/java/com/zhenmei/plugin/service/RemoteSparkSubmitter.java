package com.zhenmei.plugin.service;

import cn.hutool.core.util.StrUtil;
import com.zhenmei.plugin.config.SparkConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteSparkSubmitter {

    private static final String SPARK_YARN_JAR_HDFS = "/user/spark/jars/spark-yarn_2.12-3.2.1.jar";
    private static final String SPARK_JARS_ARCHIVE_HDFS = "/user/spark/spark-jars.zip";

    private final SparkConfig sparkConfig;

    public Configuration buildHadoopConf() {
        Configuration conf = new Configuration();
        if (StrUtil.isNotBlank(sparkConfig.getFsDefaultFs())) {
            conf.set("fs.defaultFS", sparkConfig.getFsDefaultFs());
        }
        if (StrUtil.isNotBlank(sparkConfig.getYarnResourceManagerAddress())) {
            conf.set("yarn.resourcemanager.address", sparkConfig.getYarnResourceManagerAddress());
        }
        if (StrUtil.isNotBlank(sparkConfig.getHadoopConfDir())) {
            conf.set("hadoop.conf.dir", sparkConfig.getHadoopConfDir());
        }
        return conf;
    }

    public String uploadToHdfs(String localPath, String hdfsDir) {
        try {
            Configuration conf = buildHadoopConf();
            FileSystem fs = FileSystem.get(conf);
            Path dir = new Path(hdfsDir);
            if (!fs.exists(dir)) fs.mkdirs(dir);
            String fileName = new File(localPath).getName();
            Path dest = new Path(hdfsDir + "/" + fileName);
            fs.copyFromLocalFile(false, true, new Path(localPath), dest);
            fs.close();
            log.info("上传到 HDFS: {} -> {}", localPath, dest);
            return dest.toString();
        } catch (IOException e) {
            log.error("上传到 HDFS 失败: {}", localPath, e);
            throw new RuntimeException("HDFS上传失败: " + e.getMessage(), e);
        }
    }

    public String getAppState(String appId) {
        try {
            YarnClient yarnClient = YarnClient.createYarnClient();
            yarnClient.init(buildHadoopConf());
            yarnClient.start();
            String[] parts = appId.split("_");
            ApplicationId applicationId = ApplicationId.newInstance(
                    Long.parseLong(parts[1]), Integer.parseInt(parts[2]));
            ApplicationReport report = yarnClient.getApplicationReport(applicationId);
            yarnClient.close();
            return report.getYarnApplicationState().name();
        } catch (Exception e) {
            log.error("获取 YARN 状态失败: {}", appId, e);
            return "UNKNOWN";
        }
    }

    public void killYarnApp(String appId) {
        try {
            YarnClient yarnClient = YarnClient.createYarnClient();
            yarnClient.init(buildHadoopConf());
            yarnClient.start();
            String[] parts = appId.split("_");
            ApplicationId applicationId = ApplicationId.newInstance(
                    Long.parseLong(parts[1]), Integer.parseInt(parts[2]));
            yarnClient.killApplication(applicationId);
            yarnClient.close();
            log.info("YARN application {} killed via API", appId);
        } catch (Exception e) {
            log.error("kill YARN 失败: {}", appId, e);
            throw new RuntimeException("kill失败: " + e.getMessage(), e);
        }
    }

    public String submitYarnApplication(
            String appName,
            String mainHdfsPath,
            String mainClass,
            String appArgs,
            boolean isPython,
            String pyZipHdfsPath,
            List<String> dependencyJarHdfsPaths,
            String stagingDir,
            Map<String, String> sparkConfMap,
            String hiveSiteHdfsPath,
            Integer driverMemory,
            Integer driverCores,
            Integer executorMemory,
            Integer executorCores,
            Integer numExecutors
    ) throws Exception {
        Configuration conf = buildHadoopConf();
        FileSystem fs = FileSystem.get(conf);

        YarnClient yarnClient = YarnClient.createYarnClient();
        yarnClient.init(conf);
        yarnClient.start();

        try {
            YarnClientApplication app = yarnClient.createApplication();
            ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
            appContext.setApplicationName(appName);
            appContext.setApplicationType("SPARK");
            appContext.setMaxAppAttempts(1);

            Map<String, LocalResource> localResources = new LinkedHashMap<>();

            // 用户 jar/script
            Path userMain = new Path(mainHdfsPath);
            String mainKey = isPython ? "__main__" : "__app__";
            addLocalResource(localResources, mainKey, userMain, LocalResourceType.FILE, LocalResourceVisibility.APPLICATION, fs);

            // spark-yarn jar (AM 必须)
            Path sparkYarnJar = new Path(SPARK_YARN_JAR_HDFS);
            addLocalResource(localResources, "__spark_yarn__", sparkYarnJar, LocalResourceType.FILE, LocalResourceVisibility.APPLICATION, fs);

            // spark-jars 归档 — 解压到 ./__spark_libs__/
            Path sparkJarsArchive = new Path(SPARK_JARS_ARCHIVE_HDFS);
            addLocalResource(localResources, "__spark_libs__", sparkJarsArchive, LocalResourceType.ARCHIVE, LocalResourceVisibility.APPLICATION, fs);

            // 依赖 jars
            if (dependencyJarHdfsPaths != null) {
                for (int i = 0; i < dependencyJarHdfsPaths.size(); i++) {
                    addLocalResource(localResources, "dep_" + i, new Path(dependencyJarHdfsPaths.get(i)),
                            LocalResourceType.FILE, LocalResourceVisibility.APPLICATION, fs);
                }
            }

            // Hive 配置文件
            if (StrUtil.isNotBlank(hiveSiteHdfsPath)) {
                addLocalResource(localResources, "hive-site.xml", new Path(hiveSiteHdfsPath),
                        LocalResourceType.FILE, LocalResourceVisibility.APPLICATION, fs);
            }

            // Spark 配置 zip
            Path sparkConfZip = createSparkConfZip(fs, stagingDir, sparkConfMap, driverMemory, driverCores, executorMemory, executorCores, numExecutors, isPython, pyZipHdfsPath, dependencyJarHdfsPaths, hiveSiteHdfsPath);
            addLocalResource(localResources, "__spark_conf__", sparkConfZip, LocalResourceType.ARCHIVE, LocalResourceVisibility.APPLICATION, fs);

            ContainerLaunchContext amContainer = Records.newRecord(ContainerLaunchContext.class);
            amContainer.setLocalResources(localResources);

            // 环境变量
            Map<String, String> env = new HashMap<>();
            StringBuilder cp = new StringBuilder();
            cp.append("./__spark_conf__").append(File.pathSeparator);
            cp.append("./__spark_yarn__").append(File.pathSeparator);
            cp.append("./__spark_libs__/*").append(File.pathSeparator);
            cp.append("./").append(mainKey);
            if (dependencyJarHdfsPaths != null) {
                for (int i = 0; i < dependencyJarHdfsPaths.size(); i++) {
                    cp.append(File.pathSeparator).append("./dep_").append(i);
                }
            }
            // 添加本地 Spark jars 作为 AM 兜底 classpath
            cp.append(File.pathSeparator).append(sparkConfig.getSparkHome()).append("/jars/*");
            env.put("CLASSPATH", cp.toString());
            env.put("SPARK_YARN_MODE", "true");
            env.put("SPARK_HOME", sparkConfig.getSparkHome());
            // 让 AM 容器能读取 Hadoop 配置（core-site.xml 等），从而解析 HDFS 路径
            if (StrUtil.isNotBlank(sparkConfig.getHadoopConfDir())) {
                env.put("HADOOP_CONF_DIR", sparkConfig.getHadoopConfDir());
            }
            amContainer.setEnvironment(env);

            // AM 启动命令
            List<String> commands = new ArrayList<>();
            StringBuilder cmd = new StringBuilder();
            cmd.append("$JAVA_HOME/bin/java -server -Xmx1024m");
            cmd.append(" -Dspark.yarn.app.container.log.dir={{LOG_DIR}}");
            cmd.append(" org.apache.spark.deploy.yarn.ApplicationMaster");
            cmd.append(" --properties-file ./__spark_conf__/spark.conf");

            if (isPython) {
                cmd.append(" --primary-py-file ").append(mainHdfsPath);
                if (StrUtil.isNotBlank(pyZipHdfsPath)) {
                    cmd.append(" --arg --py-files --arg ").append(pyZipHdfsPath);
                }
            } else {
                cmd.append(" --jar ").append(mainHdfsPath);
                if (StrUtil.isNotBlank(mainClass)) {
                    cmd.append(" --class ").append(mainClass);
                }
            }

            if (StrUtil.isNotBlank(appArgs)) {
                for (String arg : appArgs.split("\\s+")) {
                    if (StrUtil.isNotBlank(arg)) {
                        cmd.append(" --arg \"").append(arg).append("\"");
                    }
                }
            }

            commands.add(cmd.toString());
            amContainer.setCommands(commands);

            int amMemory = driverMemory != null ? driverMemory : 1024;
            int amCores = driverCores != null ? driverCores : 1;
            Resource capability = Resource.newInstance(amMemory, amCores);
            appContext.setResource(capability);
            appContext.setAMContainerSpec(amContainer);

            yarnClient.submitApplication(appContext);
            String appIdStr = appContext.getApplicationId().toString();
            log.info("YARN 提交成功: appId={}, name={}", appIdStr, appName);
            return appIdStr;

        } finally {
            yarnClient.close();
            fs.close();
        }
    }

    @SuppressWarnings("deprecation")
    private void addLocalResource(Map<String, LocalResource> resources, String key, Path path,
                                  LocalResourceType type, LocalResourceVisibility visibility, FileSystem fs) throws IOException {
        FileStatus status = fs.getFileStatus(path);
        LocalResource resource = Records.newRecord(LocalResource.class);
        resource.setType(type);
        resource.setVisibility(visibility);
        resource.setResource(ConverterUtils.getYarnUrlFromPath(status.getPath()));
        resource.setTimestamp(status.getModificationTime());
        resource.setSize(status.getLen());
        resources.put(key, resource);
    }

    private Path createSparkConfZip(FileSystem fs, String stagingDir, Map<String, String> sparkConfMap,
                                    Integer driverMemory, Integer driverCores,
                                    Integer executorMemory, Integer executorCores, Integer numExecutors,
                                    boolean isPython, String pyZipHdfsPath,
                                    List<String> dependencyJarHdfsPaths, String hiveSiteHdfsPath) throws IOException {
        Path zipPath = new Path(stagingDir + "/__spark_conf__.zip");
        File tempFile = File.createTempFile("spark-conf-", ".zip");

        try (FileOutputStream fos = new FileOutputStream(tempFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            zos.putNextEntry(new ZipEntry("spark.conf"));

            Properties props = new Properties();

            props.setProperty("spark.app.name", sparkConfMap.getOrDefault("spark.app.name", "SparkJob"));
            props.setProperty("spark.master", sparkConfMap.getOrDefault("spark.master", "yarn"));
            props.setProperty("spark.submit.deployMode", sparkConfMap.getOrDefault("spark.submit.deployMode", "cluster"));

            if (driverMemory != null) props.setProperty("spark.driver.memory", driverMemory + "m");
            if (driverCores != null) props.setProperty("spark.driver.cores", String.valueOf(driverCores));
            if (executorMemory != null) props.setProperty("spark.executor.memory", executorMemory + "m");
            if (executorCores != null) props.setProperty("spark.executor.cores", String.valueOf(executorCores));
            if (numExecutors != null) props.setProperty("spark.executor.instances", String.valueOf(numExecutors));

            // spark.yarn.archive: AM 会将此归档分发给所有 executor 容器
            String fsDefault = sparkConfig.getFsDefaultFs();
            if (StrUtil.isNotBlank(fsDefault)) {
                props.setProperty("spark.yarn.archive", fsDefault + SPARK_JARS_ARCHIVE_HDFS);
            } else {
                props.setProperty("spark.yarn.archive", SPARK_JARS_ARCHIVE_HDFS);
            }
            // 本地 Spark jars 路径（兜底: 如果 archive 分发失败，executor 可以直接从本地加载）
            String sparkHome = sparkConfig.getSparkHome();
            props.setProperty("spark.yarn.jars", "local:" + sparkHome + "/jars/*");
            props.setProperty("spark.executor.extraClassPath", sparkHome + "/jars/*");
            props.setProperty("spark.yarn.stagingDir", stagingDir);

            if (StrUtil.isNotBlank(sparkConfig.getFsDefaultFs())) {
                props.setProperty("spark.hadoop.fs.defaultFS", sparkConfig.getFsDefaultFs());
            }
            if (StrUtil.isNotBlank(sparkConfig.getYarnResourceManagerAddress())) {
                String rmAddr = sparkConfig.getYarnResourceManagerAddress();
                props.setProperty("spark.hadoop.yarn.resourcemanager.address", rmAddr);
                String rmHost = rmAddr.contains(":") ? rmAddr.split(":")[0] : rmAddr;
                props.setProperty("spark.hadoop.yarn.resourcemanager.hostname", rmHost);
            }

            for (Map.Entry<String, String> entry : sparkConfMap.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                if (!props.containsKey(k) && StrUtil.isNotBlank(v)) {
                    props.setProperty(k, v);
                }
            }

            if (isPython && StrUtil.isNotBlank(pyZipHdfsPath)) {
                props.setProperty("spark.yarn.dist.pyFiles", pyZipHdfsPath);
                props.setProperty("spark.submit.pyFiles", pyZipHdfsPath);
            }

            if (dependencyJarHdfsPaths != null && !dependencyJarHdfsPaths.isEmpty()) {
                props.setProperty("spark.yarn.dist.jars", String.join(",", dependencyJarHdfsPaths));
            }

            if (StrUtil.isNotBlank(hiveSiteHdfsPath)) {
                props.setProperty("spark.yarn.dist.files", hiveSiteHdfsPath);
                props.setProperty("spark.sql.catalogImplementation", "hive");
            }

            StringWriter sw = new StringWriter();
            props.store(sw, null);
            zos.write(sw.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        try {
            Path dest = new Path(zipPath.toString());
            fs.copyFromLocalFile(false, true, new Path(tempFile.getAbsolutePath()), dest);
            log.info("Spark conf zip 已上传: {}", dest);
            return dest;
        } finally {
            tempFile.delete();
        }
    }
}
