from pyspark.sql import SparkSession
spark = SparkSession.builder.appName("wordcount").enableHiveSupport().getOrCreate()
df = spark.sql("select * from t_right limit 10")
df.show()
spark.stop()