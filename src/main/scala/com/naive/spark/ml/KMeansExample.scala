package com.naive.spark.ml

import com.naive.spark.StreamingExamples
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.linalg.{VectorUDT, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{StructField, StructType}
import org.apache.spark.sql.{Row, SQLContext}
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Copyright: 版权所有 ( c ) 北京启明星辰信息技术有限公司 2015 保留所有权利。
  * Created by 郭宁 on 15/10/27
  */

/*
*
* 0,tcp,http,SF,238,1282,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,4,4,0.00,0.00,0.00,0.00,1.00,0.00,0.00,5,5,1.00,0.00,0.20,0.00,0.00,0.00,0.00,0.00,normal.
* */



object KMeansExample {

    def main(args: Array[String]): Unit = {

        StreamingExamples.setStreamingLogLevels()

        if (args.length != 2) {
            System.err.println("Usage: ml.KMeansExample <file> <k>")
            System.exit(1)
        }
        val input = args(0)
        val k = args(1).toInt

        val conf = new SparkConf().setAppName(s"${this.getClass.getSimpleName}")
        val sc = new SparkContext(conf)
        val sqlContext = new SQLContext(sc)

        // 加载数据
        val kddcupData = sc.textFile(input).filter(_.nonEmpty)

        //数据初步探索
        dataExplore(kddcupData)
        val data = dataPrepare(kddcupData).values
        data.cache()
        //参数是随意设置的，两个类，迭代次数50次
        val numClusters = 12
        val numIterations = 50
        val clusters = KMeans.train(data, numClusters, numIterations)
        //查看一下模型得到的每一个类中心
        clusters.clusterCenters.foreach(println)
        //使用均方误差查看模型效果
        val wssse = clusters.computeCost(data)
        println("Within Set Sum of Squared Errors = " + wssse)

        sc.stop()
    }


    /**
      * 将原始数据去掉分类变量，并且转成带标签的 double 向量
      * @param data 原始数据集合
      * @return
      */
    def dataPrepare(data: RDD[String]) = {
        val labelsAndData = data.map {
            line =>
                val buffer = line.split(",").toBuffer
                //从索引1开始，总共去掉三个数据（即去掉 1， 2， 3 列）
                buffer.remove(1, 3)
                val label = buffer.remove(buffer.length - 1)
                val vector = Vectors.dense(buffer.map(_.toDouble).toArray)
                (label, vector)
        }
        labelsAndData
    }

    /**
      * kddcup.data.corrected 数据简单探索
      */
    def dataExplore(data: RDD[String]) = {
        val splitData = data.map{
            line => line.split(",")
        }
        splitData.take(10).foreach{
            line => println(line.mkString(","))
        }
        val sample = splitData.map(_.last).countByValue().toSeq.sortBy(_._2).reverse
        sample.foreach(println)
    }
}
