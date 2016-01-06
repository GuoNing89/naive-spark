package com.naive.spark.ml

import com.naive.spark.StreamingExamples
import org.apache.spark.mllib.linalg.{Vectors, DenseVector}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.classification.SVMWithSGD
import org.apache.spark.mllib.regression.LabeledPoint


/**
  * Created by guoning on 15/12/24.
  *
  * 支持向量机(SVM)
  */
object SVM_1 extends App{

    StreamingExamples.setStreamingLogLevels()

    val conf = new SparkConf().setAppName("MLDEMO").setMaster("local[*]")
    val sc = new SparkContext(conf)

    // Load and parse the data file
    val data = sc.textFile("/Users/guoning/Documents/IdeaProjects/naive-spark/src/main/resources/data/sample_svm_data.txt")
    val parsedData = data.map { line =>
        val parts = line.split(' ')
        LabeledPoint(parts(0).toDouble,Vectors.dense(parts.tail.map(x => x.toDouble)) )
    }

    // Run training algorithm to build the model
    val numIterations = 20
    val model = SVMWithSGD.train(parsedData, numIterations)

    // Evaluate model on training examples and compute training error
    val labelAndPreds = parsedData.map { point =>
        val prediction = model.predict(point.features)
        (point.label, prediction)
    }
    val trainErr = labelAndPreds.filter(r => r._1 != r._2).count.toDouble / parsedData.count
    println("Training Error = " + trainErr)
}
