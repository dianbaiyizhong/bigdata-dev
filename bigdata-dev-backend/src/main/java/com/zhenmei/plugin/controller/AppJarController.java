package com.zhenmei.plugin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhenmei.plugin.entity.AppJar;
import com.zhenmei.plugin.response.ApiResponse;
import com.zhenmei.plugin.service.AppJarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/app-jar")
@RequiredArgsConstructor
public class AppJarController {

    private final AppJarService appJarService;

    @PostMapping("/upload")
    public ApiResponse<List<AppJar>> upload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "description", required = false) String description) {
        return ApiResponse.success(appJarService.upload(files, description));
    }

    @GetMapping("/list")
    public ApiResponse<Page<AppJar>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(appJarService.list(page, size, keyword));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        appJarService.delete(id);
        return ApiResponse.success();
    }
}
