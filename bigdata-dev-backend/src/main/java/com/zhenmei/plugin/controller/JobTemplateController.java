package com.zhenmei.plugin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhenmei.plugin.dto.JobTemplateSaveRequest;
import com.zhenmei.plugin.entity.JobTemplate;
import com.zhenmei.plugin.response.ApiResponse;
import com.zhenmei.plugin.service.JobTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-template")
@RequiredArgsConstructor
public class JobTemplateController {

    private final JobTemplateService jobTemplateService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<JobTemplate> save(@ModelAttribute JobTemplateSaveRequest request) {
        return ApiResponse.success(jobTemplateService.save(request));
    }

    @GetMapping("/list")
    public ApiResponse<Page<JobTemplate>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(jobTemplateService.list(page, size, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<JobTemplate> getById(@PathVariable Long id) {
        return ApiResponse.success(jobTemplateService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        jobTemplateService.delete(id);
        return ApiResponse.success();
    }
}
