package com.zhenmei.plugin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhenmei.plugin.entity.Dependency;
import com.zhenmei.plugin.entity.DependencyJar;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DependencyService {

    Dependency create(String name, String description);

    Dependency getById(Long id);

    Page<Dependency> list(int page, int size, String keyword);

    void delete(Long id);

    List<DependencyJar> uploadJars(Long dependencyId, MultipartFile[] files);

    List<DependencyJar> getJars(Long dependencyId);

    void deleteJar(Long jarId);
}
