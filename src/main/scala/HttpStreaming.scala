import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentType, HttpCharsets, HttpEntity, HttpMethods, HttpRequest, HttpResponse, MediaTypes, StatusCodes, Uri}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString

import scala.concurrent.TimeoutException
import scala.concurrent.duration.DurationLong
import scala.util.control.NonFatal

object HttpStreaming {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("TweetsWordCount")
    val logger = Logging(system, getClass)
    val parallelism = 10

    val decider: Supervision.Decider = {
      case _: TimeoutException => Supervision.Restart
      case NonFatal(e) =>
        logger.error(s"Stream failed with ${e.getMessage}, going to resume")
        Supervision.Resume
    }

    // Akka-Streams materializer instance
    implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system)
      .withSupervisionStrategy(decider))

    implicit val executionContext = system.dispatcher

    val httpRequest = HttpRequest(
      method = HttpMethods.GET,
      uri = Uri("https://randomuser.me/api/"),
      entity = HttpEntity(
        contentType = ContentType(MediaTypes.`application/json`),
        ""
      ).withoutSizeLimit()
    )


    val source: Source[HttpResponse, NotUsed] = Source.fromFuture(Http().singleRequest(httpRequest))


    def extractEntityData(response: HttpResponse): Source[ByteString, _] =
      response match {
        case HttpResponse(StatusCodes.OK, _, entity, _) => entity.dataBytes
        case notOkResponse =>
          Source.failed(new RuntimeException(s"illegal response $notOkResponse"))
      }

//    source.flatMapConcat(extractEntityData).map(_.utf8String).toMat(Sink.foreach(println))(Keep.both).run()
    Source.tick(1.seconds, 4.seconds, httpRequest).mapAsync(1)(Http().singleRequest(_))
      .flatMapConcat(extractEntityData).map(_.utf8String).toMat(Sink.foreach(println))(Keep.both).run()
  }
}
