package com.zhenmei.plugin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_pyspark_zip")
public class PySparkZip {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String hdfsPath;

    private Long fileSize;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
