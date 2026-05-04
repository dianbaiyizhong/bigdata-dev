package com.zhenmei.plugin.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhenmei.plugin.entity.Dependency;
import com.zhenmei.plugin.entity.DependencyJar;
import com.zhenmei.plugin.mapper.DependencyJarMapper;
import com.zhenmei.plugin.mapper.DependencyMapper;
import com.zhenmei.plugin.service.DependencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DependencyServiceImpl implements DependencyService {

    private final DependencyMapper dependencyMapper;
    private final DependencyJarMapper dependencyJarMapper;

    private static final String DEP_JAR_DIR = FileUtil.getTmpDirPath()+ File.separator + "plugin-dep-jars" + File.separator;

    @Override
    @Transactional
    public Dependency create(String name, String description) {
        Dependency dep = new Dependency();
        dep.setName(name);
        dep.setDescription(description);
        dependencyMapper.insert(dep);
        return dep;
    }

    @Override
    public Dependency getById(Long id) {
        return dependencyMapper.selectById(id);
    }

    @Override
    public Page<Dependency> list(int page, int size, String keyword) {
        Page<Dependency> p = new Page<>(page, size);
        LambdaQueryWrapper<Dependency> w = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            w.like(Dependency::getName, keyword);
        }
        w.orderByDesc(Dependency::getCreateTime);
        return dependencyMapper.selectPage(p, w);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        List<DependencyJar> jars = getJars(id);
        for (DependencyJar jar : jars) {
            if (StrUtil.isNotBlank(jar.getJarPath())) {
                new File(jar.getJarPath()).delete();
            }
        }
        LambdaQueryWrapper<DependencyJar> jw = new LambdaQueryWrapper<>();
        jw.eq(DependencyJar::getDependencyId, id);
        dependencyJarMapper.delete(jw);
        dependencyMapper.deleteById(id);
    }

    @Override
    public List<DependencyJar> uploadJars(Long dependencyId, MultipartFile[] files) {
        List<DependencyJar> result = new ArrayList<>();
        File dir = new File(DEP_JAR_DIR);
        if (!dir.exists()) dir.mkdirs();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String originalFilename = file.getOriginalFilename();
            String savedName = System.currentTimeMillis() + "_" + originalFilename;
            File dest = new File(dir, savedName);
            try {
                file.transferTo(dest);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("上传失败: " + originalFilename);
            }
            DependencyJar jar = new DependencyJar();
            jar.setDependencyId(dependencyId);
            jar.setName(originalFilename);
            jar.setJarPath(dest.getAbsolutePath());
            jar.setFileSize(file.getSize());
            dependencyJarMapper.insert(jar);
            result.add(jar);
        }
        return result;
    }

    @Override
    public List<DependencyJar> getJars(Long dependencyId) {
        LambdaQueryWrapper<DependencyJar> w = new LambdaQueryWrapper<>();
        w.eq(DependencyJar::getDependencyId, dependencyId);
        w.orderByAsc(DependencyJar::getId);
        return dependencyJarMapper.selectList(w);
    }

    @Override
    public void deleteJar(Long jarId) {
        DependencyJar jar = dependencyJarMapper.selectById(jarId);
        if (jar != null && StrUtil.isNotBlank(jar.getJarPath())) {
            new File(jar.getJarPath()).delete();
        }
        dependencyJarMapper.deleteById(jarId);
    }
}
