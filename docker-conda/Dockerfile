# 使用官方 Miniconda3 基础镜像
FROM continuumio/miniconda3:latest

# 设置工作目录
WORKDIR /app

# （可选）将环境设为默认激活，或把 conda 命令加入 PATH
ENV PATH /opt/conda/envs/myenv/bin:$PATH

# 设置容器启动时的默认命令
CMD ["bash"]