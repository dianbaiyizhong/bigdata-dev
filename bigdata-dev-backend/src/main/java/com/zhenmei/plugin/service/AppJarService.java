package com.zhenmei.plugin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhenmei.plugin.entity.AppJar;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AppJarService {

    List<AppJar> upload(MultipartFile[] files, String description);

    AppJar getById(Long id);

    Page<AppJar> list(int page, int size, String keyword);

    void delete(Long id);
}
