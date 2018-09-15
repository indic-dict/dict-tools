package stardict_sanskrit
import akka.pattern.ask
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import org.slf4j.{Logger, LoggerFactory}
import sanskrit_coders.RichHttpClient
import akka.http.scaladsl.model._
import akka.stream.scaladsl.FileIO
import akka.util.Timeout

import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Success}

case class DictIndex(indexUrl: String, var downloadPath: String = "", var dictTarUrls: List[String] = List()) {
  downloadPath = indexUrl.replaceAllLiterally("https://raw.githubusercontent.com/", "").replaceAllLiterally("master/", "").replaceAllLiterally("tars/tars.MD", "")
  dictTarUrls = DictIndex.getUrlsFromIndexMd(url=indexUrl).toList
//
//  override def toString: String = s"indexUrl: ${indexUrl}\ndownloadPath: ${downloadPath}\ndictTarUrls: ${dictTarUrls.mkString("\n")}"
}

object DictIndex {
  def getUrlsFromIndexMd(url: String): Array[String] = {
    Source.fromURL(url).mkString.split("\n").map(_.replaceAll("[<>]", ""))
  }
}

class InstallerActor extends Actor with ActorLogging {
  import context.dispatcher // Provides ExecutionContext - required below.
  import akka.pattern.pipe // For pipeTo() below.

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  private val simpleClient: HttpRequest => Future[HttpResponse] = Http(context.system).singleRequest(_: HttpRequest)
  private val redirectingClient: HttpRequest => Future[HttpResponse] = RichHttpClient.httpClientWithRedirect(simpleClient)

  def receive: PartialFunction[Any, Unit] = {
    case Tuple2(dictTarUrl: String, destinationPath: String) => {
      log.debug(dictTarUrl)
      log.debug(destinationPath)
      val fileSink = FileIO.toPath(Paths.get(destinationPath))
      redirectingClient(HttpRequest(uri = dictTarUrl)).map(_.entity.dataBytes.to(fileSink).run()).pipeTo(sender())
      // TODO: To be continued.
    }
  }
}

object installer {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  val system = ActorSystem("installerActorSystem")


  def install(destination: String, indexOfIndicesUrl: String): Unit = {
    val indices = DictIndex.getUrlsFromIndexMd(indexOfIndicesUrl)
    val installerActorRef = system.actorOf(Props[InstallerActor], "installerActor")
    // Actor ask timeout
    implicit val timeout: Timeout = Timeout(10, TimeUnit.MINUTES)
    import scala.concurrent.ExecutionContext.Implicits.global

    //    log.debug(indices.mkString(","))
    indices.map(new DictIndex(_)).take(1).foreach(index => {
      log.debug(index.toString)
      index.dictTarUrls.foreach(dictUrl => {
        ask(installerActorRef, Tuple2(dictUrl, index.downloadPath)).onComplete{
          case Success(value) => log.info("All done!")
          case Failure(exception) => log.error(s"An error occurred: ${exception.getMessage}, with stacktrace:\n${exception.getStackTrace.mkString("\n")}")
        }
      })
    })
  }

  def main(args: Array[String]): Unit = {
    install(destination = "/home/vvasuki/sanskrit-coders/stardict-dicts-installed/", indexOfIndicesUrl = "https://raw.githubusercontent.com/sanskrit-coders/stardict-dictionary-updater/master/dictionaryIndices.md")
  }
}
