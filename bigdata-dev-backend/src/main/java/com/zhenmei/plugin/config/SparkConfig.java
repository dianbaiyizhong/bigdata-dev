package com.zhenmei.plugin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spark")
public class SparkConfig {

    private String sparkHome = "/usr/local/spark";
    private String master = "yarn";
    private String deployMode = "cluster";
    private String hadoopConfDir;
    private String yarnStagingDir = "/user/root/.sparkStaging/";
}
