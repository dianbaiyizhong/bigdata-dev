package com.zhenmei.plugin.dto;

import lombok.Data;

@Data
public class JobSubmitRequest {

    private String jobName;
    private String jobType;
    private String mainClass;
    private String appArgs;
    private String sparkProperties;
    private String deployMode;
    private String master;
    private Integer driverMemory;
    private Integer driverCores;
    private Integer executorMemory;
    private Integer executorCores;
    private Integer numExecutors;
    private String dependencyIds;
    private Long pySparkZipId;
}
