package com.zhenmei.plugin.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class JobTemplateSaveRequest {

    private String templateName;

    private String description;

    private String jobType;

    private MultipartFile file;

    private MultipartFile pyScript;

    private String mainClass;

    private String entryFile;

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
