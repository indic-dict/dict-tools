package sanskritnlp.dictionary.generators

import java.io.{File, PrintWriter}

import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, Formats}
import org.slf4j.{Logger, LoggerFactory}
import sanskritnlp.dictionary.generators.pushpaaKrdanta.getClass
import sanskritnlp.transliteration.transliterator

object keshirAja {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  def tsvToJsonBabylon(): Unit = {
    implicit val formats: Formats = DefaultFormats
    val keshirAjaDir = "/home/vvasuki/indic-dict/stardict-kannada/kn-head/keshirAja/"
    val infileStr = s"${keshirAjaDir}/mUlam/mUlam.tsv"
    val outfileStr = s"${keshirAjaDir}/keshirAja.babylon"
    val src = utils.tsvToMapIterator(infileStr)
    val outFileObj = new File(outfileStr)
    new File(outFileObj.getParent).mkdirs
    val destination = new PrintWriter(outFileObj)
//    Header row: kn_root	sa_meaning	kn_root_latin	eng_meaning
//     Typical line: ಈ	प्रदाने	ī	to give
    src.foreach(valueMap => {
        val headersKannada = valueMap.getOrElse("kn_root", "").split(",").map(_.trim)
        val headersSanskritMeaning = valueMap.getOrElse("sa_meaning", "").split(",").map(_.trim)
        val headers = headersKannada ++ headersKannada.map(transliterator.transliterate(_, "kannada", "optitrans")) ++ headersKannada.map(transliterator.transliterate(_, "kannada", "dev")) ++ headersSanskritMeaning ++ headersSanskritMeaning.map(transliterator.transliterate(_, "dev", "kannada"))
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
