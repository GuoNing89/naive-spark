package com.naive.spark.sql

import org.apache.spark.sql.Row
import org.apache.spark.{SparkContext, SparkConf}

/**
  * Copyright: 版权所有 ( c ) 北京启明星辰信息技术有限公司 2015 保留所有权利。
  * Created by 郭宁 on 15/7/27 下午3:52
  */
object Movie {

    // 创建一个表示Movie的自定义类
    case class Movie(movieId: Int, title: String, genres: String)

    def main(args: Array[String]) {

        val base_path = this.getClass.getResource("/").getPath

        val conf = new SparkConf().setAppName("Spark Sql Demo").setMaster("local[3]")
        val sc = new SparkContext(conf)
        // 首先用已有的Spark Context对象创建SQLContext对象
        val sqlContext = new org.apache.spark.sql.SQLContext(sc)
        // 导入语句，可以隐式地将RDD转化成DataFrame
        import sqlContext.implicits._
        // 用数据集文本文件创建一个Movie对象的DataFrame
        val dfMovies = sc.textFile(s"$base_path/data/movies.csv").filter(!_.startsWith("movieId"))
                .map(_.split(","))
                .map(p => Movie(p(0).trim.toInt, p(1), p(2))).toDF()

        val tup = dfMovies.map((row: Row) => {
            (row.getInt(0), row.getString(1))
        })
        tup.foreach(println)

        // 将DataFrame注册为一个表
        dfMovies.registerTempTable("movies")

        val limit10 = sqlContext.sql( """select * from movies limit 10""")



        limit10.show()
        // 显示DataFrame的内容
        dfMovies.show()

        // 打印DF模式
        dfMovies.printSchema()

        // 选择Movie名称列
        dfMovies.select("title").show()

        // 选择Movie名称和类别列
        dfMovies.select("title", "genres").show()

        // 根据id选择Movie
        dfMovies.filter(dfMovies("movieId").equalTo(500)).show()

        // 根据movieId统计Movie数量
        dfMovies.groupBy("movieId").count().show()

    }
}


