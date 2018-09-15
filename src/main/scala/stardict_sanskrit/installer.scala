package stardict_sanskrit

import java.nio.file.Paths

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import org.slf4j.{Logger, LoggerFactory}
import sanskrit_coders.RichHttpClient
import akka.http.scaladsl.model._
import akka.stream.scaladsl.FileIO

import scala.concurrent.Future
import scala.io.Source

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
  import context.dispatcher

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  private val simpleClient: HttpRequest => Future[HttpResponse] = Http(context.system).singleRequest(_: HttpRequest)
  private val redirectingClient: HttpRequest => Future[HttpResponse] = RichHttpClient.httpClientWithRedirect(simpleClient)

  def receive: PartialFunction[Any, Unit] = {
    case (dictTarUrl: String, destinationPath: String) => {
      val fileSink = FileIO.toPath(Paths.get(destinationPath))
      redirectingClient(HttpRequest(uri = dictTarUrl)).map(_.entity.dataBytes.to(fileSink))
      // TODO: To be continued.
    }
  }
}

object installer {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)

  def install(destination: String, indexOfIndicesUrl: String): Unit = {
    val indices = DictIndex.getUrlsFromIndexMd(indexOfIndicesUrl)
//    log.debug(indices.mkString(","))
    indices.map(new DictIndex(_)).take(1).foreach(index => {
      log.debug(index.toString)
      // TODO: To be continued.
    })
  }

  def main(args: Array[String]): Unit = {
    install(destination = "/home/vvasuki/sanskrit-coders/stardict-dicts-installed/", indexOfIndicesUrl = "https://raw.githubusercontent.com/sanskrit-coders/stardict-dictionary-updater/master/dictionaryIndices.md")
  }
}
