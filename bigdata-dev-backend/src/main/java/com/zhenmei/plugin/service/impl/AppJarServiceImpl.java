package com.zhenmei.plugin.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhenmei.plugin.entity.AppJar;
import com.zhenmei.plugin.mapper.AppJarMapper;
import com.zhenmei.plugin.service.AppJarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppJarServiceImpl implements AppJarService {

    private final AppJarMapper appJarMapper;

    private static final String APP_JAR_DIR = FileUtil.getTmpDirPath() + "plugin-jars" + File.separator;

    @Override
    public List<AppJar> upload(MultipartFile[] files, String description) {
        List<AppJar> result = new ArrayList<>();
        File dir = new File(APP_JAR_DIR);
        if (!dir.exists()) dir.mkdirs();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String originalFilename = file.getOriginalFilename();
            String savedName = System.currentTimeMillis() + "_" + originalFilename;
            File dest = new File(dir, savedName);
            try {
                file.transferTo(dest);
            } catch (IOException e) {
                throw new RuntimeException("上传失败: " + originalFilename);
            }
            AppJar jar = new AppJar();
            jar.setName(originalFilename);
            jar.setJarPath(dest.getAbsolutePath());
            jar.setFileSize(file.getSize());
            jar.setDescription(description);
            appJarMapper.insert(jar);
            result.add(jar);
        }
        return result;
    }

    @Override
    public AppJar getById(Long id) {
        return appJarMapper.selectById(id);
    }

    @Override
    public Page<AppJar> list(int page, int size, String keyword) {
        Page<AppJar> p = new Page<>(page, size);
        LambdaQueryWrapper<AppJar> w = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            w.like(AppJar::getName, keyword);
        }
        w.orderByDesc(AppJar::getCreateTime);
        return appJarMapper.selectPage(p, w);
    }

    @Override
    public void delete(Long id) {
        AppJar jar = appJarMapper.selectById(id);
        if (jar != null && StrUtil.isNotBlank(jar.getJarPath())) {
            new File(jar.getJarPath()).delete();
        }
        appJarMapper.deleteById(id);
    }
}
