package sanskritnlp.dictionary

import java.io.{PrintWriter, StringWriter}

import org.slf4j.{Logger, LoggerFactory}
import sanskritnlp.transliteration.indic.{devanaagarii, kannada, telugu}
import sanskritnlp.transliteration.transliterator

object headwordTransformers{
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)

  def addDevanaagariiFromOtherIndic(headwords_original:Array[String]) = (headwords_original ++ headwords_original.map(
    x => try {
      transliterator.getDevanagariiFromOtherIndicString(x).getOrElse("")
    } catch {
      case ex: Exception => {
        val sw = new StringWriter
        log.error(headwords_original.toString)
        log.error(sw.toString)
        log.error(x)
        ""
      }
    }))

  def addOptitransFromDevanaagarii(headwords_original:Array[String]) = (headwords_original ++ headwords_original.map(
    x => try {
      transliterator.transliterate(x, "dev", "optitrans")
    } catch {
      case ex: Exception => {
        val sw = new StringWriter
        ex.printStackTrace(new PrintWriter(sw))
        log.error(sw.toString)
        log.error(x)
        ""
      }
    }))


  def addIndicScriptsFromDevanaagarii(headwords_original:Array[String]) = (headwords_original ++ headwords_original.map(
    x => try {
      transliterator.transliterate(x, "dev", "iast")
    } catch {
      case ex: Exception => {
        val sw = new StringWriter
        ex.printStackTrace(new PrintWriter(sw))
        log.error(sw.toString)
        log.error(x)
        ""
      }
    }))

  def addNonAnsusvaaraVariantsFromDevanaagarii(headwords_original:Array[String]) = (headwords_original ++ headwords_original.map(
    x => try {
      transliterator.getNonAnusvaaraVariant(x)
    } catch {
      case ex: Exception => {
        val sw = new StringWriter
        ex.printStackTrace(new PrintWriter(sw))
        log.error(sw.toString)
        log.error(x)
        ""
      }
    }))
  def headwordSorter(a: String, b: String): Boolean = {
    def checkDistinctProperty(fn: String => Boolean): Boolean = fn(a) && !fn(b)

    def getScore(x: String): Float = {
      var score = x.length
      if (x.contains("_")) score += -1000
      if (transliterator.getScriptHandler(x) == Some(devanaagarii)) score += 1000
      if (transliterator.getScriptHandler(x) == Some(kannada)) score += 70
      if (transliterator.getScriptHandler(x) == Some(telugu)) score += 69
      score
    }

    if (getScore(a) == getScore(b)) {
      a < b
    } else {
      getScore(a) > getScore(b)
    }
  }

}