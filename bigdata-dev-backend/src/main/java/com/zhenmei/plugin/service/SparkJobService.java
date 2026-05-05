package com.zhenmei.plugin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhenmei.plugin.dto.JobSubmitRequest;
import com.zhenmei.plugin.entity.SparkJob;

public interface SparkJobService {

    SparkJob submitJob(JobSubmitRequest request, String jarPath, String scriptPath);

    SparkJob getJobById(Long id);

    Page<SparkJob> listJobs(int page, int size);

    void killJob(Long id);
}
