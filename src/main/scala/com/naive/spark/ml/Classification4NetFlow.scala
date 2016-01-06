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


object Classification4NetFlow {
    case class Kddcup(duration: Double,
//                      protocol_type: String,
//                      service: String,
//                      flag: String,
                      src_bytes: Double,
                      dst_bytes: Double,
                      land: Double,
                      wrong_fragment: Double,
                      urgent: Double,
                      hot: Double,
                      num_failed_logins: Double,
                      logged_in: Double,
                      num_compromised: Double,
                      root_shell: Double,
                      su_attempted: Double,
                      num_root: Double,
                      num_file_creations: Double,
                      num_shells: Double,
                      num_access_files: Double,
                      num_outbound_cmds: Double,
                      is_host_login: Double,
                      is_guest_login: Double,
                      count: Double,
                      srv_count: Double,
                      serror_rate: Double,
                      srv_serror_rate: Double,
                      rerror_rate: Double,
                      srv_rerror_rate: Double,
                      same_srv_rate: Double,
                      diff_srv_rate: Double,
                      srv_diff_host_rate: Double,
                      dst_host_count: Double,
                      dst_host_srv_count: Double,
                      dst_host_same_srv_rate: Double,
                      dst_host_diff_srv_rate: Double,
                      dst_host_same_src_port_rate: Double,
                      dst_host_srv_diff_host_rate: Double,
                      dst_host_serror_rate: Double,
                      dst_host_srv_serror_rate: Double,
                      dst_host_rerror_rate: Double,
                      dst_host_srv_rerror_rate: Double,
                      label: String)

    def main(args: Array[String]) {

        StreamingExamples.setStreamingLogLevels()

        if (args.length < 1){
            println("Usage:ClassificationPipeline inputDataFile")
            sys.exit(1)
        }
        val conf = new SparkConf().setAppName(s"${this.getClass.getSimpleName}")
        val sc = new SparkContext(conf)
        val sqlCtx = new SQLContext(sc)

        val parsedRDD = sc.textFile(args(0)).filter(_.nonEmpty).map(_.split(","))
                .map(p => Kddcup(p(0).toDouble,/* p(1), p(2), p(3),*/ p(4).toDouble, p(5).toDouble, p(6).toDouble, p(7).toDouble, p(8).toDouble, p(9).toDouble
                    , p(10).toDouble, p(11).toDouble, p(12).toDouble, p(13).toDouble, p(14).toDouble, p(15).toDouble, p(16).toDouble, p(17).toDouble, p(18).toDouble, p(19).toDouble
                    , p(20).toDouble, p(21).toDouble, p(22).toDouble, p(23).toDouble, p(25).toDouble, p(25).toDouble, p(26).toDouble, p(27).toDouble, p(28).toDouble, p(29).toDouble
                    , p(30).toDouble, p(31).toDouble, p(32).toDouble, p(33).toDouble, p(34).toDouble, p(35).toDouble, p(36).toDouble, p(37).toDouble, p(38).toDouble, p(39).toDouble
                    , p(40).toDouble,p(41)))

        parsedRDD.take(10).foreach(println)
        val df = sqlCtx.createDataFrame(parsedRDD).toDF("duration","src_bytes","dst_bytes","land","wrong_fragment","urgent","hot","num_failed_logins","logged_in","num_compromised","root_shell","su_attempted","num_root","num_file_creations","num_shells","num_access_files","num_outbound_cmds","is_host_login","is_guest_login","count","srv_count","serror_rate","srv_serror_rate","rerror_rate","srv_rerror_rate","same_srv_rate","diff_srv_rate","srv_diff_host_rate","dst_host_count","dst_host_srv_count","dst_host_same_srv_rate","dst_host_diff_srv_rate","dst_host_same_src_port_rate","dst_host_srv_diff_host_rate","dst_host_serror_rate","dst_host_srv_serror_rate","dst_host_rerror_rate","dst_host_srv_rerror_rate","label").cache()
        val labelIndexer = new StringIndexer().setInputCol("label").setOutputCol("indexedLabel").fit(df)

        val vectorAssembler = new VectorAssembler().setInputCols(Array("duration",/*"protocol_type","service","flag",*/"src_bytes","dst_bytes","land","wrong_fragment","urgent","hot","num_failed_logins","logged_in","num_compromised","root_shell","su_attempted","num_root","num_file_creations","num_shells","num_access_files","num_outbound_cmds","is_host_login","is_guest_login","count","srv_count","serror_rate","srv_serror_rate","rerror_rate","srv_rerror_rate","same_srv_rate","diff_srv_rate","srv_diff_host_rate","dst_host_count","dst_host_srv_count","dst_host_same_srv_rate","dst_host_diff_srv_rate","dst_host_same_src_port_rate","dst_host_srv_diff_host_rate","dst_host_serror_rate","dst_host_srv_serror_rate","dst_host_rerror_rate","dst_host_srv_rerror_rate")).setOutputCol("featureVector")
        // 随机森林分类器
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
        predictionResultDF.select("duration","src_bytes","dst_bytes","land","wrong_fragment","label","predictedLabel").show(20)


        val error = predictionResultDF.filter("label != predictedLabel")

        println("Testing Error = " + (error.count().toDouble / predictionResultDF.count().toDouble))

        val evaluator = new MulticlassClassificationEvaluator().setLabelCol("label").setPredictionCol("prediction").setMetricName("precision")
        val randomForestModel = model.stages(2).asInstanceOf[RandomForestClassificationModel]
        // 打印决策树
        println("Trained Random Forest Model is:\n" + randomForestModel.toDebugString)
    }
}