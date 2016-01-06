package com.naive.scala

/**
  * Created by guoning on 15/12/17.
  */
object HelloScala extends App {

    println("-" * 50 + "HelloScala" + "-" * 50)

    /**
      * 值参
      * @param word
      */
    def splitLine(word: Any): Unit = {
        println("-" * 50 + word + "-" * 50)
    }

    /**
      * 求和函数
      * @param a
      * @param b
      * @return
      */
    def addFun(a: Int, b: Int): Int = {
        a + b
    }

    /**
      * 最小值函数
      * @param a
      * @param b
      * @return
      */
    def minFun(a: Int, b: Int): Int = {
        if (a < b) a else b
    }

    /**
      * 接收一个函数作为参数的函数
      * @param f
      */
    def receverFun(f: (Int, Int) => Int) = {
        println(f(1, 7))
    }

    splitLine("addFun")
    receverFun(addFun)
    splitLine("minFun")
    receverFun(minFun)

    /**
      * 部分应用（Partial application）
      * 你可以使用下划线“_”部分应用一个函数，结果将得到另一个函数。
      * Scala使用下划线表示不同上下文中的不同事物，你通常可以把它看作是一个没有命名的神奇通配符。
      * 在{ _ + 2 }的上下文中，它代表一个匿名参数。你可以这样使用它：
      * @param m
      * @param n
      * @return
      */
    def adder(m: Int, n: Int) = m + n

    val add2 = adder(2, _: Int)
    splitLine(add2(3))

    /**
      * 构造函数
      * 构造函数不是特殊的方法，他们是除了类的方法定义之外的代码。让我们扩展计算器的例子，增加一个构造函数参数，并用它来初始化内部状态
      * @param brand
      */
    class Calculator(brand: String) {
        /* A constructor. */
        val color: String = if (brand == "TI") {
            "blue"
        } else if (brand == "HP") {
            "black"
        } else {
            "white"
        }

        // An instance method.
        def add(m: Int, n: Int): Int = m + n
    }

    class C {
        var acc = 0

        def minc() = {
            acc += 1
        }

        val finc = () => acc += 1
    }
    /* 函数 VS 方法 函数对无参的 可以直接调用 c.minc  而 方法 即便是无参 也需要加() 才能实际执行函数体 */
    val c = new C
    println(c.acc)
    c.minc()
    println(c.acc)
    c.finc()
    println(c.acc)

}


