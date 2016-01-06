package com.naive.spark.ml

/**
 * Copyright: 版权所有 ( c ) 北京启明星辰信息技术有限公司 2015 保留所有权利。
 * Created by 郭宁 on 15/10/27
 */

import org.apache.log4j.{Level, Logger}
import scopt.OptionParser

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.linalg.Vectors

/**
 * An example k-means app. Run with
 * {{{
 * ./bin/run-example org.apache.spark.examples.mllib.DenseKMeans [options] <input>
 * }}}
 * If you use it as a template to create your own app, please use `spark-submit` to submit your app.
 */
object DenseKMeans {

    object InitializationMode extends Enumeration {
        type InitializationMode = Value
        val Random, Parallel = Value
    }

    import InitializationMode._

    case class Params(input: String = null,
                      k: Int = -1,
                      numIterations: Int = 10,
                      initializationMode: InitializationMode = Parallel) extends AbstractParams[Params]

    def main(args: Array[String]) {
        val defaultParams = Params()

        val parser = new OptionParser[Params]("DenseKMeans") {
            head("DenseKMeans: an example k-means app for dense data.")
            opt[Int]('k', "k")
                    .required()
                    .text(s"number of clusters, required")
                    .action((x, c) => c.copy(k = x))
            opt[Int]("numIterations")
                    .text(s"number of iterations, default: ${defaultParams.numIterations}")
                    .action((x, c) => c.copy(numIterations = x))
            opt[String]("initMode")
                    .text(s"initialization mode (${InitializationMode.values.mkString(",")}), " +
                            s"default: ${defaultParams.initializationMode}")
                    .action((x, c) => c.copy(initializationMode = InitializationMode.withName(x)))
            arg[String]("<input>")
                    .text("input paths to examples")
                    .required()
                    .action((x, c) => c.copy(input = x))
        }

        parser.parse(args, defaultParams).map { params =>
            run(params)
        }.getOrElse {
            sys.exit(1)
        }
    }

    def run(params: Params) {
        val conf = new SparkConf().setAppName(s"DenseKMeans with $params")
        val sc = new SparkContext(conf)

        Logger.getRootLogger.setLevel(Level.WARN)

        val examples = sc.textFile(params.input).map { line =>
            Vectors.dense(line.split(' ').map(_.toDouble))
        }.cache()

        val numExamples = examples.count()

        println(s"numExamples = $numExamples.")

        val initMode = params.initializationMode match {
            case Random => KMeans.RANDOM
            case Parallel => KMeans.K_MEANS_PARALLEL
        }

        val model = new KMeans()
                .setInitializationMode(initMode)
                .setK(params.k)
                .setMaxIterations(params.numIterations)
                .run(examples)

        val cost = model.computeCost(examples)
        model.clusterCenters.foreach(v => {
            println(v)
        })
        println(s"Total cost = $cost.")

        sc.stop()
    }
}
