package com.zhenmei.plugin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_app_jar")
public class AppJar {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String jarPath;

    private String description;

    private Long fileSize;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
