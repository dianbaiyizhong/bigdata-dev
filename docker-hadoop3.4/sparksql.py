from pyspark.sql import SparkSession
# from util import printHello
import pandas as pd
import numpy as np

# 创建支持Hive的SparkSession
spark = SparkSession.builder \
    .appName("Read Hive Table") \
    .enableHiveSupport() \
    .getOrCreate()

spark.conf.set("spark.sql.execution.arrow.pyspark.enabled", "true")

# 直接读取Hive表为DataFrame

# # 或者使用SQL查询
# df = spark.sql("SELECT * FROM t_right")

# # 显示数据
# df.show()

# 2. 创建一个 Pandas DataFrame
pdf = pd.DataFrame(np.random.rand(100, 3))

# 3. 将 Pandas DataFrame 转换为 PySpark DataFrame（使用 Arrow）
df = spark.createDataFrame(pdf)

# 4. 将 PySpark DataFrame 转换回 Pandas DataFrame（使用 Arrow）
result_pdf = df.select("*").toPandas()

# 停止SparkSession（可选，在应用程序结束时）
spark.stop()