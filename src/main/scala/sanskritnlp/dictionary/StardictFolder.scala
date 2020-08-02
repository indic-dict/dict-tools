package sanskritnlp.dictionary

import java.io.{File, PrintWriter}

import com.davidthomasbernal.stardict.Dictionary
import org.slf4j.{Logger, LoggerFactory}


class StardictFolder(val name: String) {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  var dirFile: File = _
  var ifoFile: Option[File] = None
  var tarFile: Option[File] = None
  var dictFile: Option[File] = None
  var dictdzFile: Option[File] = None

  def this(dirFileIn: java.io.File) = {
    this(dirFileIn.getName)
    dirFile = dirFileIn
    ifoFile = dirFile.listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*\\.ifo"))
    dictFile = dirFile.listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*\\.dict"))
    dictdzFile = dirFile.listFiles.map(_.getCanonicalFile).find(_.getName.matches(s".*\\.dict.dz"))
    log debug toString

  }

  def decompileToBabylon() = {
    val stardict = Dictionary.fromIfo(ifoFile.get.getAbsolutePath, true)
    val stardictIterator = stardict.getIterator
    val babylonFile = new File(dirFile.getAbsolutePath, s"${name}.babylon")
    val destination = new PrintWriter(babylonFile)
    while (stardictIterator.hasNext) {
      val dictEntry = stardictIterator.next()
      destination.println(dictEntry.words.toArray().mkString("|"))
      destination.println(dictEntry.definition)
      destination.println()
    }
    destination.close()
  }
}
