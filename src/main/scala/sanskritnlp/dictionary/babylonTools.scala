package sanskritnlp.dictionary

/*
Run this:
PATH_TO_SANSKRITNLPJAVA=/home/vvasuki/sanskritnlpjava/target
scala -classpath "$PATH_TO_SANSKRITNLPJAVA/sanskritnlp-1.0-SNAPSHOT/WEB-INF/lib/*:$PATH_TO_SANSKRITNLPJAVA/sanskritnlp-1.0-SNAPSHOT/WEB-INF/classes" sanskritnlp.transliteration.dictTools.sutraNumbersToDevanagari  nyasa/nyasa.babylon
 */*/

import java.io._

import org.slf4j.{Logger, LoggerFactory}
import sanskritnlp.transliteration.indic._
import sanskritnlp.transliteration.roman.{as, iast, optitrans}
import sanskritnlp.transliteration.transliterator
import sanskritnlp.vyAkaraNa.devanAgarI
import stardict_sanskrit.babylonProcessor.log

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
 */
object babylonTools {

  val log: Logger = LoggerFactory.getLogger("babylonTools")
  
  def addStandardHeadwords(infileStr: String) = {
    val headwordTransformerBasic = (headwords_original: Array[String]) => (
      headwordTransformers.addOptitransFromDevanaagarii(
        headwordTransformers.addNonAnsusvaaraVariantsFromDevanaagarii(headwordTransformers.addDevanaagariiFromOtherIndic(headwords_original)))
      ).filterNot(_.isEmpty).distinct
    var headwordTransformer = headwordTransformerBasic
    if (infileStr.contains("sa-head")) {
       headwordTransformer = (headwords_original: Array[String]) => headwordTransformers.addIndicScriptsFromDevanaagarii(headwordTransformerBasic(headwords_original))
    }
    fixHeadwords(infileStr = infileStr, outputExt = "babylon_final", headwordTransformer=headwordTransformer)
  }


  def fixHeadwords(infileStr: String, outputExt: String, headwordTransformer: (Array[String]) => Array[String], sort: Boolean = true): Unit = {
    log info ("Processing " + infileStr)
    val outfileStr = infileStr.replaceFirst("\\.[^.]+$", "." + outputExt)
    log info ("Will produce " + outfileStr)
    val outFileObj = new File(outfileStr)
    new File(outFileObj.getParent).mkdirs
    val destination = new PrintWriter(outFileObj)
    
    def isHeadLine(x: String) = x.startsWith("#") || x.trim.isEmpty

    var src = Source.fromFile(infileStr, "utf8")
    src.getLines.takeWhile(isHeadLine).foreach(destination.println)

    src = Source.fromFile(infileStr, "utf8")
    src.getLines.dropWhile(isHeadLine).sliding(3, 3).foreach(t => {
      if (t.filterNot(_.isEmpty).isEmpty) {
        log.warn("Got empty lines - probably end of file. Skipping.")
      } else {
        val headwordsLine = t(0)
        val definitionLine = t(1)
        try {
          val headwordsOriginal = headwordsLine.split('|')
          var headwordsFixed = headwordTransformer(headwordsOriginal).toList.distinct
          if (sort) {
            headwordsFixed = headwordsFixed.sortWith(headwordTransformers.headwordSorter)
          }
          // Sorting with sortwith is risky - can fail and produce no output line. Skipping that.
          destination.println(headwordsFixed.mkString("|"))
          destination.println(definitionLine)
          destination.println()
        } catch {
          case ex: Exception => {
            log error ex.toString
            val sw = new StringWriter
            ex.printStackTrace(new PrintWriter(sw))
            log.error(sw.toString)
            log error "line: " + t.toString()
          }
        }
      }
    })
    destination.close()
    log info ("Produced " + outfileStr)
  }

  def fixEntries(infileStr: String, outputExt: String, entryTransformer: String => String): Unit = {
    log info ("Processing " + infileStr)
    val outfileStr = infileStr.replaceFirst("\\.[^.]+$", outputExt)
    log info ("Will produce " + outfileStr)
    val outFileObj = new File(outfileStr)
    new File(outFileObj.getParent).mkdirs
    val destination = new PrintWriter(outFileObj)


    def isHeadLine(x: String) = x.startsWith("#") || x.trim.isEmpty

    var src = Source.fromFile(infileStr, "utf8")
    src.getLines.takeWhile(isHeadLine).foreach(destination.println)

    src = Source.fromFile(infileStr, "utf8")
    src.getLines.dropWhile(isHeadLine).zipWithIndex.foreach(t => {
      val line = t._1
      val index = t._2
      try {
        if ((index + 1) % 2 == 0) {
          val entryOriginal = line
          val entryTransliterated = entryTransformer(entryOriginal)
          destination.println(entryTransliterated.toSet.toList.sorted.mkString("|"))
        } else {
          destination.println(line)
        }
      } catch {
        case ex: Exception => {
          log error ex.toString
          log error "line: " + t.toString()
        }
      }
    })
    destination.close()
    log info ("Produced " + outfileStr)
  }

  /**
   * Add optitrans and devanAgarI headwords from IAST. 
   *
   * @param indicWordSet
   */
  def getDevanagariOptitransFromIastIfIndic(infileStr: String, outputExt: String, indicWordSet: mutable.HashSet[String] = mutable.HashSet[String]()): Unit = {
    log info "=======================Adding optitrans headwords, making final babylon file."

    val indicWordSetDev = indicWordSet.filter(devanAgarI.isEncoding)

    def isIndic(word: String) = indicWordSetDev.contains(iast.fromDevanagari(word)) || iast.isEncoding(word)

    def transliterateIfIndic(x: String, destSchema: String) = if (isIndic(x)) {
      transliterator.transliterate(x, "iast", destSchema)
    } else {
      x
    }

    val toDevanAgarIAndOptitrans = (headwords_original: Array[String]) => headwords_original.map(
      x => transliterateIfIndic(x, "dev")) ++ headwords_original.map(x => transliterateIfIndic(x, "optitrans"))

    fixHeadwords(infileStr=infileStr, outputExt=outputExt, headwordTransformer = toDevanAgarIAndOptitrans)
  }
  
}
