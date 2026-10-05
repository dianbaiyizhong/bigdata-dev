package com.zhenmei.plugin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhenmei.plugin.entity.Dependency;
import com.zhenmei.plugin.entity.DependencyJar;
import com.zhenmei.plugin.response.ApiResponse;
import com.zhenmei.plugin.service.DependencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/dependency")
@RequiredArgsConstructor
public class DependencyController {

    private final DependencyService dependencyService;

    @PostMapping
    public ApiResponse<Dependency> create(
            @RequestParam String name,
            @RequestParam(required = false) String description) {
        return ApiResponse.success(dependencyService.create(name, description));
    }

    @GetMapping("/list")
    public ApiResponse<Page<Dependency>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(dependencyService.list(page, size, keyword));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dependencyService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/upload")
    public ApiResponse<List<DependencyJar>> uploadJars(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files) {
        return ApiResponse.success(dependencyService.uploadJars(id, files));
    }

    @GetMapping("/{id}/jars")
    public ApiResponse<List<DependencyJar>> getJars(@PathVariable Long id) {
        return ApiResponse.success(dependencyService.getJars(id));
    }

    @DeleteMapping("/jar/{jarId}")
    public ApiResponse<Void> deleteJar(@PathVariable Long jarId) {
        dependencyService.deleteJar(jarId);
        return ApiResponse.success();
    }
}
