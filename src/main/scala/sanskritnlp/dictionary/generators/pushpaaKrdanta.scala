package sanskritnlp.dictionary.generators

import java.io.{File, PrintWriter}
import java.nio.charset.StandardCharsets

import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization
import org.slf4j.{Logger, LoggerFactory}
import sanskritnlp.transliteration.roman.harvardKyoto

import scala.collection.mutable
import scala.io.Source

object pushpaaKrdanta {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)

  def tsvToJsonBabylon(): Unit = {
    implicit val formats: Formats = DefaultFormats
    val infileStr = "/home/vvasuki/stardict-sanskrit/sa-vyAkaraNa/puShpA-ArdhadhAtuka/mUlam/ArdhadhAtuka-kRdanta-koshaH.tsv"
    val outfileStr = "/home/vvasuki/stardict-sanskrit/sa-vyAkaraNa/puShpA-ArdhadhAtuka/puShpA-ArdhadhAtuka.babylon"
    val src = utils.tsvToMapIterator(infileStr)
    val outFileObj = new File(outfileStr)
    new File(outFileObj.getParent).mkdirs
    val destination = new PrintWriter(outFileObj)

    src.map(valueMap=> valueMap.filter(tuple2 => tuple2._2.trim != "" && tuple2._1 != "क्रमाङ्कः")
      .mapValues(_.split("/").map(_.trim)))
      .foreach(valueMap => {
      val headers = valueMap.values.flatten.toList.distinct.filterNot(_.isEmpty)
      val headersLine = headers.mkString("|")
      val meaningLine =  Serialization.writePretty(valueMap).replace("\n", "<BR>")
      destination.println(headersLine)
      destination.println(meaningLine)
      destination.println("")
      // println(line)
      println(headersLine)
      println(meaningLine)
    })
    destination.close()
    println("")
  }

  def main(args: Array[String]): Unit = {
    tsvToJsonBabylon()
  }
}
