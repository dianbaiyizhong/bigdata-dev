package com.zhenmei.plugin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhenmei.plugin.dto.JobTemplateSaveRequest;
import com.zhenmei.plugin.entity.JobTemplate;

public interface JobTemplateService {

    JobTemplate save(JobTemplateSaveRequest request);

    JobTemplate getById(Long id);

    Page<JobTemplate> list(int page, int size, String keyword);

    void delete(Long id);
}
