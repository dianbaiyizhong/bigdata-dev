FROM docker.elastic.co/elasticsearch/elasticsearch:7.10.2

# 安装 S3 Repository 插件
RUN bin/elasticsearch-plugin install --batch repository-s3

# 创建 Elasticsearch keystore（用于存储S3凭证）
RUN bin/elasticsearch-keystore create

