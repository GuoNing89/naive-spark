package com.naive.scala.actor

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import akka.event.Logging
import akka.util.Timeout
import org.apache.spark.streaming.Duration

import scala.concurrent.{Future, duration, Await}

/**
  * Created by guoning on 15/12/27.
  */
class MyActor extends Actor {
    val log = Logging(context.system, this)
    def receive = {
        case "test" ⇒ log.info("received test")
        case "hello" ⇒ {
            log.info("hello - 1")
//            Thread.sleep(500)
            log.info("hello - 2")
        }
        case _ ⇒ log.info("received unknown message")
    }

    override def preStart(): Unit = {
        log.info("preStart")
    }
    override def preRestart(reason: Throwable, message: Option[Any]) {
        log.info("preRestart")
        context.children foreach context.stop
        postStop()
    }
    override def postRestart(reason: Throwable) { preStart() }
    override def postStop(): Unit = {
        log.info("postStop")
    }

}
object Main extends App {
    val system = ActorSystem("MySystem")
    val myActor = system.actorOf(Props[MyActor], name = "myactor")

    myActor ! "test"

    val firstActor = system.actorOf(Props[FirstActor] , name = "firstActor")

    firstActor ! DoIt("wo cao")

    val future = myActor.ask("hello")(Timeout(2,TimeUnit.SECONDS))




}
case class DoIt(msg : String)
class FirstActor extends Actor {
    def receive = {
        case m: DoIt =>
            context.actorOf(Props(new Actor {
                def receive = {
                    case DoIt(msg) ⇒
                        val replyMsg = doSomeDangerousWork(msg)
                        sender ! replyMsg
                        context.stop(self)
                }
                def doSomeDangerousWork(msg: String): String = { println(s"-----------$msg-----------"); msg }
            })) forward m

    }
}