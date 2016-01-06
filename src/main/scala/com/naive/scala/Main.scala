package com.naive.scala

import scala.collection.mutable.ListBuffer

/**
  * Created by guoning on 15/12/21.
  */
object Main {




    def main(args: Array[String]) {
        val start = System.currentTimeMillis()
        var list  = ListBuffer[Long](0L)
         for(i <- 0L to 1000000L){
              list += i
         }

        val count = list.sum
        val end = System.currentTimeMillis()

        println(s"count = $count use time : ${end - start}")

    }



}
