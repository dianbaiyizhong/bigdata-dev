package com.zhenmei.plugin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_dependency_jar")
public class DependencyJar {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long dependencyId;

    private String name;

    private String jarPath;

    private Long fileSize;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
