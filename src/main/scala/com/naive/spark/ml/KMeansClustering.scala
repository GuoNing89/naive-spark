package com.naive.spark.ml

/**
 * Copyright: 版权所有 ( c ) 北京启明星辰信息技术有限公司 2015 保留所有权利。
 * Created by 郭宁 on 15/10/27
 */

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import org.apache.spark.mllib.linalg.Vectors

object KMeansClustering {
    def main(args: Array[String]) {
//        if (args.length < 5) {
//            println("Usage:KMeansClustering trainingDataFilePath testDataFilePath numClusters numIterations runTimes")
//            sys.exit(1)
//        }

        val conf = new SparkConf().setAppName("Spark MLlib Exercise:K-Means Clustering").setMaster("local[*]")
        val sc = new SparkContext(conf)

        /**
         * Channel Region Fresh Milk Grocery Frozen Detergents_Paper Delicassen
         * 2 3  12669 9656 7561 214 2674 1338
         * 2 3 7057 9810 9568 1762 3293 1776
         * 2 3 6353 8808 7684 2405 3516 7844
         */

        val rawTrainingData = sc.textFile("/Users/guoning/Documents/IdeaProjects/git/Study/spark/src/main/resources/train.csv")
        val parsedTrainingData = rawTrainingData.filter(!isColumnNameLine(_)).map(line => {
                Vectors.dense(line.split(",").map(_.trim).filter(!"".equals(_)).map(_.toDouble))
            }).cache()

        // Cluster the data into two classes using KMeans

        val numClusters = 8
        val numIterations = 30
        val runTimes = 1
        var clusterIndex: Int = 0

        val ks:Array[Int] = Array(3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20)
        ks.foreach(cluster => {
            val model:KMeansModel = KMeans.train(parsedTrainingData, cluster,30,1)
            val ssd = model.computeCost(parsedTrainingData)
            println("sum of squared distances of points to their nearest center when k=" + cluster + " -> "+ ssd)
        })

        val clusters: KMeansModel = KMeans.train(parsedTrainingData, numClusters, numIterations, runTimes)

        println("Cluster Number:" + clusters.clusterCenters.length)

        println("Cluster Centers Information Overview:")
        clusters.clusterCenters.foreach(
            x => {
                println("Center Point of Cluster " + clusterIndex + ":")
                println(x)
                clusterIndex += 1
            })

        //begin to check which cluster each test data belongs to based on the clustering result

        val rawTestData = sc.textFile("/Users/guoning/Documents/IdeaProjects/git/Study/spark/src/main/resources/test.csv")
        val parsedTestData = rawTestData.map(line => {
            Vectors.dense(line.split(",").map(_.trim).filter(!"".equals(_)).map(_.toDouble))
        })
        parsedTestData.collect().foreach(testDataLine => {
            val predictedClusterIndex:
            Int = clusters.predict(testDataLine)
            println("The data " + testDataLine.toString + " belongs to cluster " + predictedClusterIndex)
        })
        println("Spark MLlib K-means clustering test finished.")
    }

    private def isColumnNameLine(line: String): Boolean = {
        if (line != null && line.contains("Channel")) true
        else false
    }
}
