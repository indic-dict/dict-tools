package stardict_sanskrit
import java.io.File

import akka.pattern.ask
import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, IOResult}
import org.slf4j.{Logger, LoggerFactory}
import sanskrit_coders.{RichHttpClient, Utils}
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.Timeout

import scala.concurrent.Future
import scala.reflect.io.File
import scala.util.{Failure, Success}


case class Dictionary(dictTarUrl: String, destinationFolder: String)


case class DictIndex(indexUrl: String, downloadPathPrefix: String, var downloadPath: String = "", var dictTarUrls: List[String] = List()) {
  downloadPath = downloadPathPrefix + "/" + indexUrl.replaceAllLiterally("https://raw.githubusercontent.com/", "").replaceAllLiterally("master/", "").replaceAllLiterally("tars/tars.MD", "")
  dictTarUrls = DictIndex.getUrlsFromIndexMd(url=indexUrl).toList

  val dictionaries: List[Dictionary] = dictTarUrls.map(dictTarUrl => Dictionary(dictTarUrl=dictTarUrl, destinationFolder = downloadPath))
//
//  override def toString: String = s"indexUrl: ${indexUrl}\ndownloadPath: ${downloadPath}\ndictTarUrls: ${dictTarUrls.mkString("\n")}"
}

object DictIndex {
  def getUrlsFromIndexMd(url: String): Array[String] = {
    import scala.io.Source
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
    case dict: Dictionary => {
      log.debug(dict.toString)
      val destinationPath = Paths.get(dict.destinationFolder,  dict.dictTarUrl.split("/").last)
      assert(new java.io.File(dict.destinationFolder).mkdirs())
//      Files.createFile(destinationPath)
      val fileSink = FileIO.toPath(destinationPath)
      log.debug(fileSink.toString())
      val httpResponseFuture = redirectingClient(HttpRequest(uri = dict.dictTarUrl))
      val ioResultFuture = httpResponseFuture.flatMap(response => {
        response.entity.dataBytes.runWith(fileSink)
      })
      ioResultFuture.flatMap(ioResult => Future.fromTry(ioResult.status)).pipeTo(sender())
    }
  }
}

object installer {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  val system = ActorSystem("installerActorSystem")


  def install(destination: String, indexOfIndicesUrl: String): Unit = {
    val indices = DictIndex.getUrlsFromIndexMd(indexOfIndicesUrl)
    val dictionaries = indices.map(new DictIndex(_, downloadPathPrefix = destination)).flatMap(index => index.dictionaries).take(1)

    val installerActorRef = system.actorOf(Props[InstallerActor], "installerActor")
    // Actor ask timeout
    implicit val timeout: Timeout = Timeout(10, TimeUnit.MINUTES)
    import scala.concurrent.ExecutionContext.Implicits.global

    //    log.debug(indices.mkString(","))
    val resultFutureList = dictionaries.map( dictionary =>  ask(installerActorRef, dictionary) )
    val futureOfResults: Unit = Utils.getFutureOfTrys(resultFutureList). onComplete {
      case Success(resultList) =>
        log.info(resultList.mkString("\n"))
        system.terminate()
    }
/*
.foreach (someTry => someTry match {
        case Success(value) => log.info("All done!")
        case Failure(exception) => log.error(s"An error occurred: ${exception.getMessage}, with stacktrace:\n${exception.getStackTrace.mkString("\n")}")
      }
 */

  }

  def main(args: Array[String]): Unit = {
    install(destination = "/home/vvasuki/sanskrit-coders/stardict-dicts-installed/", indexOfIndicesUrl = "https://raw.githubusercontent.com/sanskrit-coders/stardict-dictionary-updater/master/dictionaryIndices.md")
  }
}
