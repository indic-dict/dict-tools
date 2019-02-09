package stardict_sanskrit

import java.io.{PrintWriter, StringWriter}

import org.slf4j.{Logger, LoggerFactory}
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
}