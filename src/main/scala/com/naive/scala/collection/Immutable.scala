package com.naive.scala.collection

import java.util.Date
import java.text.SimpleDateFormat

/**
  * Created by guoning on 15/12/18.
  */


object Immutable {

    def main(args: Array[String]): Unit = {
        println(new Rest().today)
        println(new Rest().getDate("2011-01-09"))
    }
}

// 注： 必须将object Rest的定义放在class Rest之前，或者显式指出str2Date的返回类型,否则编译不通过
object Rest {

    class RichDate(str: String) {
        def toDate: Date = new SimpleDateFormat("yyyy-MM-dd").parse(str)
    }

    implicit def str2Date(str: String): RichDate = new RichDate(str)
}

class Rest {

    import Rest._

    // 必须把demo.util包下的伴随对象DateWrapper中的所有成员引进来
    def today = new Date().formatted("yyyy-MM-dd")

    // 伴随对象 Rest中定义了一个隐式转换函数
    def getDate(str: String): Date = str.toDate
}

