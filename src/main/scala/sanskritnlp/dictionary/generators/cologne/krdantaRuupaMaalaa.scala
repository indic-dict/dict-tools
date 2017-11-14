package sanskritnlp.dictionary.generators.cologne

import java.io.{File, PrintWriter}
import java.nio.charset.StandardCharsets

import org.slf4j.{Logger, LoggerFactory}
import sanskritnlp.transliteration.harvardKyoto

import scala.collection.immutable.ListSet
import scala.collection.mutable
import scala.io.Source

object krdantaRuupaMaalaa {
  val log: Logger = LoggerFactory.getLogger(getClass.getName)

  def toDevanaagarii = {
    val infileStr = "/home/vvasuki/stardict-sanskrit/sa-head/kRdanta-rUpa-mAlA/mUlam/kRdanta-rUpa-mAlA.tsv"
    val outfileStr = "/home/vvasuki/stardict-sanskrit/sa-head/kRdanta-rUpa-mAlA/kRdanta-rUpa-mAlA-test.tsv"
    val src = Source.fromFile(infileStr, "utf8")
    val outFileObj = new File(outfileStr)
    new File(outFileObj.getParent).mkdirs
    val destination = new PrintWriter(outFileObj)

    src.getLines.foreach(line => {
      var newLine = harvardKyoto.toDevanagari(line).get
      newLine = harvardKyoto.restoreEscapeSequences(newLine)
      destination.println(newLine)
      // println(line)
      // println(newLine)
    })
    destination.close()
    println("")
  }

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

  def getWordList(line: String): Seq[String] = {
    val cleanedLine = line.replaceAll(" *(\\{.+?\\}|\\([^\\)]+?\\)|‘.+?’|इति.+?।|१|२|३|४|५|६|७|८|९|०) *", "")
      .replaceAll(" +", " ")
      .replaceAll("-+", "-")
      .replaceAll(" *<sup.+?sup> *", "")
    cleanedLine.split("[ ;,]").map(_.trim).filterNot(_.isEmpty)
  }

  def makeBabylon(): Unit = {
    val babylonHtmlPreamble =
      """
        |#stripmethod=keep
        |#sametypesequence=h
        |#
        |#
        |
      """.stripMargin
    val baseTsvStr = "/home/vvasuki/stardict-sanskrit/sa-vyAkaraNa/kRdanta-rUpa-mAlA/mUlam/kRdanta-rUpa-mAlA.tsv"
    val babylonFile = "/home/vvasuki/stardict-sanskrit/sa-vyAkaraNa/kRdanta-rUpa-mAlA/kRdanta-rUpa-mAlA.babylon"
    val baseTsvSource = Source.fromFile(name = baseTsvStr, enc = StandardCharsets.UTF_8.name())

    val destination = new PrintWriter(new File(babylonFile))
    destination.println(babylonHtmlPreamble)
    baseTsvSource.getLines.map(_.split("\t")).zipWithIndex.
      map({case (Array(key: String, content: String), index: Int) =>
      var (contentWithFootnoteMarkers, footnotesMap) = extractFootnotes(content, f"krm_$index%04d")


      // We don't directly split below to accommodate the [^०-९] condition.
      val finalContentItems = contentWithFootnoteMarkers.replace("\\n\\n", "").replaceAll("([^०-९]। |;)", "$1<BR><BR>").split("<BR><BR>").filterNot(_.isEmpty)
      val headwords = Seq(key) ++ finalContentItems.filter(_.endsWith(";")).map(getWordList).flatten.toSeq
      val finalContentItemsHtml =  finalContentItems.map(x => s"""<li>$x</li>""")
      val footnoteItems = footnotesMap.sortBy(_._1).map({ case (fullFootnoteId: String, content: String) => //noinspection RedundantBlock
      {
        //noinspection RedundantBlock
        s"""<li id=\"foot_${fullFootnoteId}\"><a href=\"#ref_${fullFootnoteId}\">↑</a> $content</li>"""
      }})
      val footnoteHtml = if (footnoteItems.isEmpty) "" else s"<p>प्रासङ्गिक्यः <ul>${footnoteItems.mkString(" ")}</ul></p>"
      s"${headwords.distinct.mkString("|")}\n<ul>${finalContentItemsHtml.mkString(" ")}</ul>$footnoteHtml"
    }).foreach(itemEntry => {
      destination.println(itemEntry)
      destination.println()
    })
    destination.close()
  }

  def main(args: Array[String]): Unit = {
    makeBabylon()
  }
}
