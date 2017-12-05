package sanskritnlp.dictionary.generators

import java.io.{File, PrintWriter}
import java.nio.charset.StandardCharsets

import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization
import org.slf4j.{Logger, LoggerFactory}
import sanskritnlp.transliteration.harvardKyoto

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

    src.take(10).foreach(valueMap => {
      val newLine =  Serialization.writePretty(valueMap)
      destination.println(newLine)
      // println(line)
       println(newLine)
    })
    destination.close()
    println("")
  }

  def main(args: Array[String]): Unit = {
    tsvToJsonBabylon()
  }
}
