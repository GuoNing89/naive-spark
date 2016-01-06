package com.naive.spark.ml

import org.apache.log4j.{Level, Logger}
import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, ALS, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}

/**
 * Naive-GN
 * Created by guoning on 15/11/3.
 */
object MovieALS {

    def main(args: Array[String]) {

        Logger.getRootLogger.setLevel(Level.WARN)

        val conf = new SparkConf().setAppName("MovieALS")
        val sc = new SparkContext(conf)

        val ratingsFile = sc.textFile(args(0))
        val ratings = ratingsFile.filter(!_.startsWith("userId")).map(line => {
            val fields = line.split(",")
            Rating(fields(0).toInt, fields(1).toInt, fields(2).toDouble)
        }).cache()

        val numRatings = ratings.count()
        val numUsers = ratings.map(_.user).distinct().count()
        val numMovies = ratings.map(_.product).distinct().count()

        println(s"Got $numRatings ratings from $numUsers users on $numMovies movies.")

        val splits = ratings.randomSplit(Array(0.8, 0.2))
        val training = splits(0).cache()
        val test = splits(1)

        println(s"Got training ${training.count()} test ${test.count()}")


        val ranks = List(8,12)
        val lambdas = List(0.01, 1.0)
        val numIters = List(10, 20)
        var bestModel: Option[MatrixFactorizationModel] = None
        var bestValidationRmse = Double.MaxValue
        var bestRank = 0
        var bestLambda = -1.0
        var bestNumIter = -1

//        var model = ALS.train(training, 8, args(1).toInt, 0.01)
//        bestModel = Some(model)
        for (rank <- ranks; lambda <- lambdas; numIter <- numIters) {
            val model = ALS.train(training, rank, numIter, lambda)
            val validationRmse = computeRmse(model, training, test)
            println(s"RMSE (validation) ${validationRmse} for the model trained with rank = ${rank}, lambda = ${lambda}, and numIter = ${numIter}")
            if (validationRmse < bestValidationRmse) {
                bestModel = Some(model)
                bestValidationRmse = validationRmse
                bestRank = rank
                bestLambda = lambda
                bestNumIter = numIter
            }
        }

        val testRmse = computeRmse(bestModel.get, ratings, test)

        println("The best model was trained with rank = " + bestRank + " and lambda = " + bestLambda
                + ", and numIter = " + bestNumIter + ", and its RMSE on the test set is " + testRmse + ".")

        var isRuning = true
        while (isRuning) {
            val line = Console.readLine()

            if ("exit".equals(line)) {
                isRuning = false
            } else {
                if (line.contains(",")) {
                    val strs = line.split(",")
                    println(s"UserID : ${strs(0)} MovieId : ${strs(1)} , 评分 : ${bestModel.get.predict(strs(0).toInt,strs(1).toInt)}")

                } else if (line.contains(" ")) {
                    val strs = line.split(" ")
                    println(s"UserID : ${strs(0)}::")
                    bestModel.get.recommendProducts(strs(0).toInt,strs(1).toInt).foreach(println)
                } else {

                }
            }

        }


    }


    def computeRmse(model: MatrixFactorizationModel, ratings: RDD[Rating], test: RDD[Rating]): Double = {

        // Evaluate the model on rating data
        val usersProducts = test.map { case Rating(user, product, rate) =>
            (user, product)
        }
        val predictions =
            model.predict(usersProducts).map { case Rating(user, product, rate) =>
                ((user, product), rate)
            }
        val ratesAndPreds = ratings.map { case Rating(user, product, rate) =>
            ((user, product), rate)
        }.join(predictions)
        val MSE = ratesAndPreds.map { case ((user, product), (r1, r2)) =>
            val err = (r1 - r2)
            err * err
        }.mean()
        println("Mean Squared Error = " + MSE)
        MSE
    }


}


