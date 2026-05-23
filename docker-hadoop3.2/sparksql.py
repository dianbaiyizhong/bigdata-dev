from pyspark.sql import SparkSession

# 创建支持Hive的SparkSession
spark = SparkSession.builder \
    .appName("Read Hive Table") \
    .enableHiveSupport() \
    .getOrCreate()

# 直接读取Hive表为DataFrame

# 或者使用SQL查询
df = spark.sql("SELECT * FROM t_right")

# 显示数据
df.show()

# 停止SparkSession（可选，在应用程序结束时）
spark.stop()