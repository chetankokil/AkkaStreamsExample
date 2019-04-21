import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, Outlet}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}

import scala.concurrent.Future


object StreamExample {

  def main(args: Array[String]): Unit = {
    implicit val system       = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()


    val numbers = 1 to 1000

    val source : Source[Int, NotUsed] = Source.fromIterator(() => numbers.iterator)

    val isEvenFlow :Flow[Int, Int, NotUsed] = Flow[Int].filter((num) => num % 2 == 0)

    val evenNumberSource: Source[Int,NotUsed] = source.via(isEvenFlow)

    val sink :Sink[Int, Future[Done]] = Sink.foreach[Int](println)

    evenNumberSource.runWith(sink)
  }


//  val g = RunnableGraph.fromGraph(GraphDSL.create() {
//    implicit builder =>
//      import GraphDSL.Implicits._
//
//      val A:Outlet[Int] = builder.add(Source(1 to 10)).out
//
//
//
//      ClosedShape
//  })

}
