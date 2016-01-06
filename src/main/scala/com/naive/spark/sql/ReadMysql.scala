package com.naive.spark.sql

import org.apache.spark.sql.{Row, SQLContext}
import org.apache.spark.{SparkContext, SparkConf}

/**
  * Created by guoning on 15/12/18.
  */



case class Exprs(node_id:String,
                 expr_name:String,
                 expr_id:String,
                 expr:String,
                 index_id:String,
                 serial_id:String,
                 var params:AnyRef //Tuple3[AnyVal,AnyVal,AnyVal]
                 , var expr_eval:String)

object ReadMysql {




    def main(args: Array[String]) {


        val conf = new SparkConf().setAppName("ReadMysql").setMaster("local[3]")
        val sc = new SparkContext(conf)
        // 首先用已有的Spark Context对象创建SQLContext对象
        val sqlContext = new SQLContext(sc)
        // 导入语句，可以隐式地将RDD转化成DataFrame
        import sqlContext.implicits._
        sqlContext.load("/tmp/movies.csv")
        val rowsDf = sqlContext.read.format("jdbc").options(
            Map("url" -> "jdbc:mysql://localhost:3306/tsoc?user=tsoc&password=fake.PWD.fool.4.U",
                "driver" -> "com.mysql.jdbc.Driver",
                "dbtable" -> "(select count(ip) from kb_botnet) t")).load()

        println(rowsDf.count())
        rowsDf.registerTempTable("temp")



//        println("1 rowsDf.size = " + rowsDf.count()) //打印1
//
//        var result = ""//这个用来收集所有查询结果打印一下
//        val exprsDf =  rowsDf.map {
//            (r: Row)
//            => Exprs(r.getAs[Int]("jobflow_node_id").toString, r.getAs[String]("name"), r.getAs[Int]("cal_expression_id").toString, r.getAs[String]("expression"),
//                r.getAs[Int]("index_id").toString, r.getAs[Int]("serial_id").toString, (1,2,3), """""")
//        }
//        exprsDf.foreach(
//            _ match{
//                case x:Exprs => result += x.node_id + "," + x.expr_name + "," + x.expr_id + "," + x.expr + "," + x.index_id + "," + x.serial_id + "," + x.params + "," + x.expr_eval + ";"
//            }
//        )
//
//        println("2 rowsDf.size = " + rowsDf.count())//也是1
//        println("结果值：" + result) //打印为空，空行
//        println("rowsDf = " + exprsDf)// rowsDf



    }



}
