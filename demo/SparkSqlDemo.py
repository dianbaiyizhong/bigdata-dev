from pyspark.sql import SparkSession
spark = SparkSession.builder.appName("wordcount").enableHiveSupport().getOrCreate()
df = spark.sql("select * from tb_demo_2 limit 10")
df.show()
spark.stop()