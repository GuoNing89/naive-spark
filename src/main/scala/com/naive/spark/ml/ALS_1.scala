package com.naive.spark.ml

import com.naive.spark.StreamingExamples
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Copyright: 版权所有 ( c ) 北京启明星辰信息技术有限公司 2015 保留所有权利。
  * Created by 郭宁 on 15/8/8 上午10:29
  * ALS交替最小二乘法的协同过滤算法
  */
object ALS_1 extends App {

    StreamingExamples.setStreamingLogLevels()

    val conf = new SparkConf().setAppName("MLDEMO").setMaster("local[*]")
    val sc = new SparkContext(conf)
    val data = sc.textFile("/Users/guoning/Documents/IdeaProjects/venus/cupid-streaming/test/data/ratings.csv")

    //data中每条数据经过map的split后会是一个数组，模式匹配后，会new一个Rating对象
    val baseData = data.filter(!_.startsWith("userId")).map(_.split(",") match { case Array(user, item, rate, ts) =>
        Rating(user.toInt, item.toInt, rate.toDouble)
    })

    val Array(ratings, tests) = baseData.randomSplit(Array(0.8, 0.2))
    val rank = 20
    // 设置潜在因子个数为10
    val numIterations = 10
    // 要迭代计算30次
    val lambda = 0.01
    // 正则化因子
    //接下来调用ALS.train()方法，进行模型训练：
    val model = ALS.train(ratings, rank, numIterations, lambda)
    //训练完后，我们要对比一下预测的结果来进行对比测试：
    val usersProducts = tests.map { case Rating(user, product, rate) =>
        (user, product)
    }
    //预测后的用户，电影，评分
    val predictions =
        model.predict(usersProducts).map { case Rating(user, product, rate) =>
            ((user, product), rate)
        }

    //原始{(用户，电影)，评分} join  预测后的{(用户，电影)，评分}
    val ratesAndPreds = baseData.map { case Rating(user, product, rate) =>
        ((user, product), rate)
    }.join(predictions)

    ratesAndPreds.collect take 10 foreach println
}
