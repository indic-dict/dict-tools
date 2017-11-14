package sanskritnlp.dictionary.generators.cologne

import java.nio.charset.StandardCharsets

import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.io.Source

object krdantaRuupaMaalaa {
  val log: Logger = LoggerFactory.getLogger(getClass.getName)
  def extractFootnotes(content: String, tagPrefix: String): (String, List[(String, String)]) = {
    val footnotePattern = "\\[\\[?[^\\]]*\\]\\]?|\\([^\\)]+\\)".r
    var contentWithFootnoteMarkers = content
    var footnotesMap = mutable.ListMap[String, String]()
    var footnoteId = 1
    var nextFootnote = footnotePattern.findFirstIn(contentWithFootnoteMarkers)
    while(nextFootnote.isDefined) {
      val fullFootnoteId = f"""${tagPrefix}_$footnoteId%02d"""
      footnotesMap.put(fullFootnoteId, nextFootnote.get)
      val footnoteTag = s"""<sup id=\"ref_$fullFootnoteId\"><a href=\"#foot_${
        //noinspection RedundantBlock
        fullFootnoteId}\">${footnoteId}</a></sup>"""
      contentWithFootnoteMarkers = footnotePattern.replaceFirstIn(contentWithFootnoteMarkers, footnoteTag)
      nextFootnote = footnotePattern.findFirstIn(contentWithFootnoteMarkers)
      footnoteId = footnoteId + 1
    }
    (contentWithFootnoteMarkers, footnotesMap.toList)
  }

  def getHeadwords(line: String): Set[String] = {
    val cleanedLine = line.replaceAll(" *(\\{.+?\\}|\\([^\\)]+?\\)|‘.+?’|इति.+?।|१|२|३|४|५|६|७|८|९|०) *", "")
      .replaceAll("( |-|,|;)+", "$1")
      .replaceAll("<sup.+?sup>", "")
    cleanedLine.split("[ ;,]+").map(_.trim).filterNot(_.isEmpty).toSet
  }

  def makeBabylon(): Unit = {
    val baseTsvStr = "/home/vvasuki/stardict-sanskrit/sa-vyAkaraNa/kRdanta-rUpa-mAlA/mUlam/kRdanta-rUpa-mAlA.tsv"
    val baseTsvSource = Source.fromFile(name = baseTsvStr, enc = StandardCharsets.UTF_8.name())
    baseTsvSource.getLines.take(10).map(_.split("\t")).zipWithIndex.
      foreach({case (Array(key: String, content: String), index: Int) =>
      var (contentWithFootnoteMarkers, footnotesMap) = extractFootnotes(content, index.toString)


      // We don't directly split below to accommodate the [^०-९] condition.
      val finalContentItems = contentWithFootnoteMarkers.replace("\\n\\n", "").replaceAll("([^०-९]। |;)", "$1<BR><BR>").split("<BR><BR>").filterNot(_.isEmpty)
      val headwords = Seq(key) ++ finalContentItems.filter(_.endsWith(";")).map(getHeadwords).flatten.toSeq
      val finalContentItemsHtml =  finalContentItems.map(x => s"""<li>$x</li>""")
      val footnoteItems = footnotesMap.sortBy(_._1).map({ case (fullFootnoteId: String, content: String) => //noinspection RedundantBlock
      {
        //noinspection RedundantBlock
        s"""<li id=\"foot_${fullFootnoteId}\"><a href=\"#ref_${fullFootnoteId}\">↑</a> $content</li>"""
      }})
      val footnoteHtml = if (footnoteItems.isEmpty) "" else s"<p>प्रासङ्गिक्यः <ul>${footnoteItems.mkString(" ")}</ul></p>"
      val finalContents = s"${headwords.mkString("|")}\n<ul>${finalContentItemsHtml.mkString(" ")}</ul>$footnoteHtml"
//      log.debug(index.toString)
//      log.debug(contentWithFootnoteMarkers)
      log.debug(finalContents)
//      val potentialTableRows = content.split(";\\n\\n")
//      potentialTableRows.foreach(row => {
//        row.take
//      })
    })
  }

  def main(args: Array[String]): Unit = {
    makeBabylon()
  }
}
