package com.zhenmei.plugin.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhenmei.plugin.dto.JobTemplateSaveRequest;
import com.zhenmei.plugin.entity.JobTemplate;
import com.zhenmei.plugin.mapper.JobTemplateMapper;
import com.zhenmei.plugin.service.JobTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobTemplateServiceImpl implements JobTemplateService {

    private static final Path TEMPLATE_DIR = Paths.get(FileUtil.getTmpDirPath())
            .toAbsolutePath().normalize().resolve("plugin-job-templates");

    private final JobTemplateMapper jobTemplateMapper;

    @Override
    @Transactional
    public JobTemplate save(JobTemplateSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("模板请求不能为空");
        }

        String templateName = trimToNull(request.getTemplateName());
        if (templateName == null) {
            throw new IllegalArgumentException("模板名不能为空");
        }
        if (templateName.length() > 255) {
            throw new IllegalArgumentException("模板名长度不能超过255");
        }

        String jobType = determineJobType(request);
        ensureNameAvailable(templateName);

        JobTemplate template = new JobTemplate();
        template.setTemplateName(templateName);
        template.setDescription(trimToNull(request.getDescription()));
        template.setJobType(jobType);
        template.setMainClass(trimToNull(request.getMainClass()));
        template.setEntryFile(trimToNull(request.getEntryFile()));
        template.setPySparkZipId(request.getPySparkZipId());
        template.setDependencyIds(trimToNull(request.getDependencyIds()));
        template.setAppArgs(request.getAppArgs());
        template.setSparkProperties(request.getSparkProperties());
        template.setDeployMode(trimToNull(request.getDeployMode()));
        template.setMaster(trimToNull(request.getMaster()));
        template.setDriverMemory(request.getDriverMemory());
        template.setDriverCores(request.getDriverCores());
        template.setExecutorMemory(request.getExecutorMemory());
        template.setExecutorCores(request.getExecutorCores());
        template.setNumExecutors(request.getNumExecutors());

        Path savedPath = null;
        try {
            if ("JAR".equals(jobType)) {
                String originalName = originalName(request.getFile());
                String extension = extension(originalName);
                savedPath = copyFile(request.getFile(), extension);
                template.setJarPath(savedPath.toString());
                template.setJarOriginalName(originalName);
                template.setJarFileSize(request.getFile().getSize());
            } else {
                String originalName = originalName(request.getPyScript());
                String extension = extension(originalName);
                savedPath = copyFile(request.getPyScript(), extension);
                template.setScriptPath(savedPath.toString());
                template.setScriptOriginalName(originalName);
                template.setScriptFileSize(request.getPyScript().getSize());
            }

            if (jobTemplateMapper.insert(template) != 1) {
                throw new IllegalStateException("任务模板保存失败");
            }
            return template;
        } catch (RuntimeException e) {
            deleteSavedPath(savedPath);
            throw e;
        }
    }

    @Override
    public JobTemplate getById(Long id) {
        return jobTemplateMapper.selectById(id);
    }

    @Override
    public Page<JobTemplate> list(int page, int size, String keyword) {
        Page<JobTemplate> result = new Page<>(page, size);
        LambdaQueryWrapper<JobTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(JobTemplate::getTemplateName, keyword.trim());
        }
        wrapper.orderByDesc(JobTemplate::getCreateTime);
        return jobTemplateMapper.selectPage(result, wrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        JobTemplate template = jobTemplateMapper.selectById(id);
        if (template == null) {
            return;
        }
        jobTemplateMapper.deleteById(id);
        deleteTemplateFile(template.getJarPath());
        deleteTemplateFile(template.getScriptPath());
    }

    private String determineJobType(JobTemplateSaveRequest request) {
        boolean hasJar = hasFile(request.getFile());
        boolean hasScript = hasFile(request.getPyScript());
        String jobType = normalizeJobType(request.getJobType());
        if (jobType == null) {
            if (hasJar && !hasScript) {
                jobType = "JAR";
            } else if (hasScript && !hasJar) {
                jobType = "PYTHON";
            } else {
                throw new IllegalArgumentException("必须指定jobType并上传对应文件");
            }
        }

        if ("JAR".equals(jobType)) {
            if (hasScript) {
                throw new IllegalArgumentException("JAR模板不能同时上传Python脚本");
            }
            if (!hasJar) {
                throw new IllegalArgumentException("JAR模板必须上传.jar文件");
            }
            String name = originalName(request.getFile());
            if (!".jar".equals(extension(name))) {
                throw new IllegalArgumentException("JAR模板文件必须是.jar格式");
            }
            if (StrUtil.isBlank(request.getMainClass())) {
                throw new IllegalArgumentException("JAR模板必须指定mainClass");
            }
        } else {
            if (hasJar) {
                throw new IllegalArgumentException("Python模板不能同时上传JAR文件");
            }
            if (!hasScript) {
                throw new IllegalArgumentException("Python模板必须上传.py或.zip文件");
            }
            String name = originalName(request.getPyScript());
            String extension = extension(name);
            if (!".py".equals(extension) && !".zip".equals(extension)) {
                throw new IllegalArgumentException("Python模板文件必须是.py或.zip格式");
            }
            String entryFile = trimToNull(request.getEntryFile());
            if (entryFile != null && !isSafeEntryFile(entryFile)) {
                throw new IllegalArgumentException("Python入口文件路径不合法");
            }
            if (".zip".equals(extension) && entryFile == null) {
                throw new IllegalArgumentException("Python ZIP模板必须指定entryFile");
            }
        }
        return jobType;
    }

    private String normalizeJobType(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!"JAR".equals(normalized) && !"PYTHON".equals(normalized)) {
            throw new IllegalArgumentException("jobType只支持JAR或PYTHON");
        }
        return normalized;
    }

    private void ensureNameAvailable(String templateName) {
        LambdaQueryWrapper<JobTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobTemplate::getTemplateName, templateName);
        if (jobTemplateMapper.selectOne(wrapper) != null) {
            throw new IllegalArgumentException("模板名已存在");
        }
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private String originalName(MultipartFile file) {
        String originalName = safeOriginalName(file == null ? null : file.getOriginalFilename());
        if (originalName == null) {
            throw new IllegalArgumentException("上传文件名称不能为空");
        }
        return originalName;
    }

    private String safeOriginalName(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        if (separator >= 0) {
            normalized = normalized.substring(separator + 1);
        }
        return trimToNull(normalized);
    }

    private String extension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index).toLowerCase(Locale.ROOT);
    }

    private boolean isSafeEntryFile(String entryFile) {
        String normalized = entryFile.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.indexOf('\0') >= 0) {
            return false;
        }
        for (String part : normalized.split("/")) {
            if ("..".equals(part)) {
                return false;
            }
        }
        return true;
    }

    private Path copyFile(MultipartFile file, String extension) {
        try {
            Files.createDirectories(TEMPLATE_DIR);
            Path target = TEMPLATE_DIR.resolve(UUID.randomUUID() + extension).normalize();
            if (!target.startsWith(TEMPLATE_DIR) || target.equals(TEMPLATE_DIR)) {
                throw new IllegalStateException("模板文件路径不合法");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } catch (IOException e) {
            throw new IllegalStateException("模板文件保存失败", e);
        }
    }

    private void deleteSavedPath(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private void deleteTemplateFile(String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        try {
            Path path = Paths.get(value).toAbsolutePath().normalize();
            if (!path.startsWith(TEMPLATE_DIR) || path.equals(TEMPLATE_DIR)) {
                return;
            }
            Files.deleteIfExists(path);
        } catch (IOException | RuntimeException ignored) {
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
