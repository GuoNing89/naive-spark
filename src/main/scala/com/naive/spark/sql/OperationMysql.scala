package com.naive.spark.sql

import java.util.Properties

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkContext, SparkConf}


/**
  * Created by guoning on 16/3/15.
  *
  * spark 操作 Mysql
  *
  */
object OperationMysql {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("OperationMysql").setMaster("local[3]")
    val sc = new SparkContext(conf)
    // 首先用已有的Spark Context对象创建SQLContext对象
    val sqlContext = new SQLContext(sc)

    val rowsDf = sqlContext.read.format("jdbc").options(
      Map("url" -> "jdbc:mysql://localhost:3306/tsoc?user=tsoc&password=fake.PWD.fool.4.U",
        "driver" -> "com.mysql.jdbc.Driver",
        "dbtable" -> "behavior_config")).load().toDF()

    rowsDf.printSchema()
    rowsDf.show()

    // 导入语句，可以隐式地将RDD转化成DataFrame
    import sqlContext.implicits._

    val prop = new Properties()
    prop.put("user", "root")
    prop.put("password", "")
    val df = sc.parallelize(Array(BehaviorConfig(6, 1, 1, 1, 4), BehaviorConfig(7, 2, 2, 2, 5))).toDF()
    df.printSchema()
    val dfWriter = df.write.mode("append")

    dfWriter.jdbc("jdbc:mysql://127.0.0.1:3306/tsoc", "behavior_config", prop)

  }
}

case class BehaviorConfig(modifytime: Int, modifyuser: Int, isopenalert: Int, isopenevent: Int, id: Int)
