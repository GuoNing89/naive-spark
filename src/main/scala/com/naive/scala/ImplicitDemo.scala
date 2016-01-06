package com.naive.scala

/**
  * Created by guoning on 15/12/18.
  * 隐式转换
  *
  *
  */

object ImplicitDemo {

    def display(input: String): Unit = println(input)

    implicit def typeConvertor(input: Int): String = input.toString

    implicit def typeConvertor(input: Boolean): String = if (input) "true" else "false"

    //  implicit def booleanTypeConvertor(input:Boolean):String = if(input) "true" else "false"
List
    def main(args: Array[String]): Unit = {
        display("1212")
        display(12)
        display(true)
    }

}
