package com.naive.spark.ml

/**
  * Naive-GN  
  * Created by guoning on 15/11/30.
  *
  * 随机森林分类
  */

import com.naive.spark.StreamingExamples
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification._
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{IndexToString, StringIndexer, VectorAssembler}
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}


object ClassificationPipeline {
    def main(args: Array[String]) {

        StreamingExamples.setStreamingLogLevels()

        if (args.length < 1){
            println("Usage:ClassificationPipeline inputDataFile")
            sys.exit(1)
        }
        val conf = new SparkConf().setAppName(s"${this.getClass.getSimpleName}")
        val sc = new SparkContext(conf)
        val sqlCtx = new SQLContext(sc)

        val parsedRDD = sc.textFile(args(0)).map(_.split(",")).map(eachRow => {
            val a = eachRow.map(x => x.toDouble)
            (a(0),a(1),a(2),a(3),a(4))
        })
        val df = sqlCtx.createDataFrame(parsedRDD).toDF("f0","f1","f2","f3","label").cache()
        val labelIndexer = new StringIndexer().setInputCol("label").setOutputCol("indexedLabel").fit(df)
        val vectorAssembler = new VectorAssembler().setInputCols(Array("f0","f1","f2","f3")).setOutputCol("featureVector")
        val rfClassifier = new RandomForestClassifier().setLabelCol("indexedLabel").setFeaturesCol("featureVector").setNumTrees(5)
        val labelConverter = new IndexToString().setInputCol("prediction").setOutputCol("predictedLabel").setLabels(labelIndexer.labels)
        // 将数据切分为训练数据和测试数据
        val Array(trainingData, testData) = df.randomSplit(Array(0.8, 0.2))
        // 建立Pipeline,设置Stages
        val pipeline = new Pipeline().setStages(Array(labelIndexer,vectorAssembler,rfClassifier,labelConverter))
        // 拿训练数据集训练模型
        val model = pipeline.fit(trainingData)
        // 拿测试数据集进行预测
        val predictionResultDF = model.transform(testData)
        predictionResultDF.select("f0","f1","f2","f3","label","predictedLabel").show(20)
        val evaluator = new MulticlassClassificationEvaluator().setLabelCol("label").setPredictionCol("prediction").setMetricName("precision")
        // 评估
        val predictionAccuracy = evaluator.evaluate(predictionResultDF)
        println("Testing Error = " + (1.0 - predictionAccuracy))
        val randomForestModel = model.stages(2).asInstanceOf[RandomForestClassificationModel]
        // 打印决策树
        println("Trained Random Forest Model is:\n" + randomForestModel.toDebugString)
    }
}