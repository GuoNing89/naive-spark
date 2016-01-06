package com.naive.spark.ml

// General purpose library

import org.apache.spark.sql.Column
import org.apache.spark.{SparkContext, SparkConf}


/**
 * Naive-GN
 * Created by guoning on 15/11/2.
 */
object BinaryDemo {

    import scala.xml._

    // Spark data manipulation libraries

    import org.apache.spark.sql.catalyst.plans._
    import org.apache.spark.sql._
    import org.apache.spark.sql.types._
    import org.apache.spark.sql.functions._

    // Spark machine learning libraries

    import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
    import org.apache.spark.ml.classification.LogisticRegression
    import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
    import org.apache.spark.ml.Pipeline

    def main(args: Array[String]) {

        val conf = new SparkConf().setAppName("BinaryDemo").setMaster("local[*]")
        val sc = new SparkContext(conf)

        val sqlContext = new org.apache.spark.sql.SQLContext(sc)


        val fileName = "/Users/guoning/Documents/IdeaProjects/git/Study/spark/src/main/resources/Posts.small.xml"
        val textFile = sc.textFile(fileName)
        val postsXml = textFile.map(_.trim).
                filter(!_.startsWith("<?xml version=")).
                filter(_ != "<posts>").
                filter(_ != "</posts>").filter(_.startsWith("<row "))

        val postsRDD = postsXml.map { s =>
            val xml = XML.loadString(s)

            val id = (xml \ "@Id").text
            val tags = (xml \ "@Tags").text

            val title = (xml \ "@Title").text
            val body = (xml \ "@Body").text
            val bodyPlain = ("<\\S+>".r).replaceAllIn(body, " ")
            val text = (title + " " + bodyPlain).replaceAll("\n", " ").replaceAll("( )+", " ");

            Row(id, tags, text)
        }

        val schemaString = "Id Tags Text"
        val schema = StructType(
            schemaString.split(" ").map(fieldName => StructField(fieldName, StringType, true)))

        val postsDf = sqlContext.createDataFrame(postsRDD, schema)

        postsDf.show()

        val targetTag = "java"
        val myudf: (String => Double) = (str: String) => {
            if (str.contains(targetTag)) 1.0 else 0.0
        }
        val sqlfunc = udf(myudf)
        val postsLabeled = postsDf.withColumn("Label", sqlfunc(col("Tags")))


        val positive = postsLabeled.filter("Label > 0.0")
        val negative = postsLabeled.filter("Label < 1.0")

        // 抽样
        val positiveTrain = positive.sample(false, 0.9)
        val negativeTrain = negative.sample(false, 0.9)
        //抽样数据 合并,作为训练数据
        val training = positiveTrain.unionAll(negativeTrain)

        val negativeTrainTmp = negativeTrain.withColumnRenamed("Label", "Flag").select("Id", "Flag")
        // 通过做外关联 过滤出训练数据剩下的数据 作为测试数据
        val negativeTest = negative.join(negativeTrainTmp, negative("Id") === negativeTrainTmp("Id"), "LeftOuter").
                filter("Flag is null").select(negative("Id"), new Column("Tags"), new Column("Text"), new Column("Label"))


        val positiveTrainTmp = positiveTrain.withColumnRenamed("Label", "Flag").select("Id", "Flag")
        val positiveTest = positive.join(positiveTrainTmp, positive("Id") === positiveTrainTmp("Id"), "LeftOuter").
                filter("Flag is null").select(negative("Id"), new Column("Tags"), new Column("Text"), new Column("Label"))
        val testing = negativeTest.unionAll(positiveTest)


        //// 建模
        //        特征的数量 Number of features
        val numFeatures = 64000
        //        梯度下降的世代数Number of epoch for gradient decent
        val numEpochs = 30
        //        回归参数Regression parameters
        val regParam = 0.02

        val tokenizer = new Tokenizer().setInputCol("Text").setOutputCol("Words")
        val hashingTF = new org.apache.spark.ml.feature.HashingTF().setNumFeatures(numFeatures).
                setInputCol(tokenizer.getOutputCol).setOutputCol("Features")
        val lr = new LogisticRegression().setMaxIter(numEpochs).setRegParam(regParam).
                setFeaturesCol("Features").setLabelCol("Label").
                setRawPredictionCol("Score").setPredictionCol("Prediction")
        val pipeline = new Pipeline().setStages(Array(tokenizer, hashingTF, lr))

        val start = System.currentTimeMillis()
        // this comand takes a time
        val model = pipeline.fit(training)

        println("模型训练 耗时::" + (System.currentTimeMillis() - start))

        val testTitle = "Easiest way to merge a release into one JAR file"
        val testBody =
            """Is there a tool or script which easily merges a bunch of
    href=&quot;http://en.wikipedia.org/wiki/JAR_%28file_format%29&quot;
    &gt;JAR&lt;/a&gt; files into one JAR file? A bonus would be to easily set the main-file manifest
    and make it executable. I would like to run it with something like:
    &lt;/p&gt;&#xA;&#xA;&lt;blockquote&gt;&#xA;  &lt;p&gt;java -jar
    rst.jar&lt;/p&gt;&#xA;&lt;/blockquote&gt;&#xA;&#xA;&lt;p&gt;
    As far as I can tell, it has no dependencies which indicates that it shouldn't be an easy
    single-file tool, but the downloaded ZIP file contains a lot of libraries."""
        val testText = testTitle + testBody

        val testDF = sqlContext.createDataFrame(Seq((99.0, testText))).toDF("Label", "Text")
        val result = model.transform(testDF)
        val prediction = result.collect()(0)(6).asInstanceOf[Double]
        println("Prediction: " + prediction)


        val testingResult = model.transform(testing)
        val testingResultScores = testingResult.select("Prediction", "Label").rdd.
                map(r => (r(0).asInstanceOf[Double], r(1).asInstanceOf[Double]))
        val bc = new BinaryClassificationMetrics(testingResultScores)
        val roc = bc.areaUnderROC
        println("Area under the ROC:" + roc)


    }

}
