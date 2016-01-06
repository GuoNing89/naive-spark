package com.naive.spark.ml

import com.naive.spark.StreamingExamples
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.mllib.tree.model.DecisionTreeModel
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Naive-GN  
  * Created by guoning on 15/11/10.
  *
  * 决策树
  */
object MyDecisionTree {

    //本地测试
    val rootDir = "/Users/guoning/Documents/data"
    def main(args: Array[String]) {

        StreamingExamples.setStreamingLogLevels()

        val conf = new SparkConf().setAppName("SparkInAction").setMaster("local[4]")
        val sc = new SparkContext(conf)
        val covTypeData = sc.textFile(rootDir + "/covtype.data")
        val data = dataPrepare(covTypeData)
        //选择测试集合和训练集合
        val Array(train, cvData, test) =
            data.randomSplit(Array(0.8, 0.1, 0.1))
        train.cache()
        cvData.cache()
        test.cache()

        val model = buildDecisionTree(train, cvData)
        evaluate(train, cvData)
    }

    /**
      * Spark MLlib 表示特征向量的对象是 LabeledPoint
      * 这个对象由表示特征的向量和目标变量组成
      */
    def dataPrepare(data: RDD[String]) = {
        val sample = data.map {
            line =>
                //全部数据转化为 double
                val array = line.split(",").map(_.toDouble)
                //前面54列是特征向量，后面一列是目标变量 label
                val featureVector = Vectors.dense(array.init)
                //决策树目标变量必须从0开始，按照1递增
                LabeledPoint(array.last - 1, featureVector)
        }
        sample
    }


    /**
      * 决策树模型建立，这里很容易改进为十择交叉验证。
      * 对每一份数据建立模型时，都需要随机选出部分数据来调整模型参数到最优。
      * 通过交叉验证的方式调整参数。
      * @param train
      * @param cvData
      */
    def buildDecisionTree(train: RDD[LabeledPoint], cvData: RDD[LabeledPoint]) = {
        def getMetrics(model: DecisionTreeModel, data: RDD[LabeledPoint]) = {
            val predictionsAndLabels = data.map {
                example =>
                    (model.predict(example.features), example.label)
            }
            new MulticlassMetrics(predictionsAndLabels)
        }
        val model = DecisionTree.trainClassifier(
            train, 7, Map[Int, Int](), "gini", 4, 100
        )
        val matrics = getMetrics(model, cvData)
        println(matrics.confusionMatrix)
        (0 until 7).map(
            cat => (matrics.precision(cat), matrics.recall(cat))
        ).foreach(println)
    }

    /**
      * 获取模型瞎猜的概率
      * @param train 测试数据集
      * @param cvData 验证数据集
      */
    def guessProb(train: RDD[LabeledPoint], cvData: RDD[LabeledPoint]) {
        /**
          * 返回数据集合中，每一个类别的概率
          * @param data 训练数据集
          */
        def labelProb(data: RDD[LabeledPoint]): Array[Double] = {
            val labelCnt = data.map(_.label).countByValue()
            val labelProb = labelCnt.toArray.sortBy(_._1).map(_._2)
            labelProb.map(_.toDouble / labelProb.sum)
        }

        val trainProb = labelProb(train)
        val cvProb = labelProb(cvData)
        val prob = trainProb.zip(cvProb).map {
            case (a, b) => a * b
        }.sum
        println(prob)
    }

    /**
      * 模型评估
      * @param trainData 训练数据
      * @param cvData 交叉验证数据
      */
    def evaluate(trainData: RDD[LabeledPoint], cvData: RDD[LabeledPoint]): Unit = {
        val evaluations =
            for (impurity <- Array("gini", "entropy");
                 depth <- Array(1, 20);
                 bins <- Array(10, 300))
                yield {
                    val model = DecisionTree.trainClassifier(
                        trainData, 7, Map[Int, Int](), impurity, depth, bins)
                    val predictionsAndLabels = cvData.map(example =>
                        (model.predict(example.features), example.label)
                    )
                    val accuracy =
                        new MulticlassMetrics(predictionsAndLabels).precision
                    ((impurity, depth, bins), accuracy)
                }
        evaluations.sortBy(_._2).reverse.foreach(println)
    }

}
