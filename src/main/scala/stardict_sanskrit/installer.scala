package stardict_sanskrit
import java.io.File

import akka.pattern.ask
import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Status}
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


case class DictInfo(dictTarUrl: String, destinationFolder: String, var dictName: String = null, var tarFilename : String = null) {
  tarFilename = dictTarUrl.split("/").last
  dictName = tarFilename.split("__").head
}


case class DictIndex(indexUrl: String, downloadPathPrefix: String, var downloadPath: String = "", var dictTarUrls: List[String] = List()) {
  downloadPath = Paths.get(downloadPathPrefix, indexUrl.replaceAllLiterally("https://raw.githubusercontent.com/", "").replaceAllLiterally("master/", "").replaceAllLiterally("tars/tars.MD", "")).toString
  dictTarUrls = DictIndex.getUrlsFromIndexMd(url=indexUrl).toList

  val dictionaries: List[DictInfo] = dictTarUrls.map(dictTarUrl => DictInfo(dictTarUrl=dictTarUrl, destinationFolder = downloadPath))
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
    case dict: DictInfo => {
      log.info(dict.toString)
      val httpResponseFuture = redirectingClient(HttpRequest(uri = dict.dictTarUrl))

      if (new java.io.File(dict.dictTarUrl).exists()) {
        log.warning(s"Skipping pre-existing $dict")
        Future.fromTry(Success(s"Dict already exists: $dict")).pipeTo(sender())
      } else {
        // Download the file.
        val destinationTarPath = Paths.get(dict.destinationFolder, dict.dictName, dict.tarFilename)
        new java.io.File(destinationTarPath.getParent.toString).mkdirs()
        val fileSink = FileIO.toPath(destinationTarPath)
        val downloadResultFuture = httpResponseFuture.flatMap(response => {
          response.entity.dataBytes.runWith(fileSink)
        })
        downloadResultFuture.foreach(result => log.debug(s"Download result for $dict: ${result}"))
        val extractionResult = downloadResultFuture.map( ioResult => ioResult.status match {
          case Success(value) => tarProcessor.extractFile(archiveFileName = destinationTarPath.toString, destinationPath = destinationTarPath.getParent.toString)
            new java.io.File(destinationTarPath.toString).delete()
            Success(s"Done with $dict")
          case Failure(exception) => Failure(new Exception(s"Failed to deal with $dict", exception))
        }
        ).flatMap(Future.fromTry)
        extractionResult.pipeTo(sender())
      }
    }
  }
}

object installer {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  val system = ActorSystem("installerActorSystem")


  def install(destination: String, indexOfIndicesUrl: String): Unit = {
    val indices = DictIndex.getUrlsFromIndexMd(indexOfIndicesUrl)
    val dictionaries = indices.map(new DictIndex(_, downloadPathPrefix = destination)).flatMap(index => index.dictionaries)

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
