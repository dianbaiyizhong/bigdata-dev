package com.zhenmei.plugin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_job_template")
public class JobTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateName;

    private String description;

    private String jarPath;

    private String jarOriginalName;

    private Long jarFileSize;

    private String scriptPath;

    private String scriptOriginalName;

    private Long scriptFileSize;

    private String jobType;

    private String mainClass;

    private String entryFile;

    private Long pySparkZipId;

    private String dependencyIds;

    private String appArgs;

    private String sparkProperties;

    private String deployMode;

    private String master;

    private Integer driverMemory;

    private Integer driverCores;

    private Integer executorMemory;

    private Integer executorCores;

    private Integer numExecutors;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
