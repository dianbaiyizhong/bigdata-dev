package com.zhenmei.plugin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_spark_job")
public class SparkJob {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String jobName;

    private String jobType;

    private String jarPath;

    private String scriptPath;

    private String pyZipPath;

    private String mainClass;

    private String appArgs;

    private String appId;

    private String status;

    private String sparkProperties;

    private String deployMode;

    private String master;

    private Integer driverMemory;

    private Integer driverCores;

    private Integer executorMemory;

    private Integer executorCores;

    private Integer numExecutors;

    private String dependencyIds;

    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
