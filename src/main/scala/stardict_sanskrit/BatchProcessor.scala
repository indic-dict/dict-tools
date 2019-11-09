package stardict_sanskrit

import java.io.File

import org.slf4j.{Logger, LoggerFactory}

trait BatchProcessor {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)

  /**
    * Get a recursive listing of all files underneath the given directory.
    * from stackoverflow.com/questions/2637643/how-do-i-list-all-files-in-a-subdirectory-in-scala
    */
  def getRecursiveListOfFiles(dir: File): Array[File] = {
    val these = dir.listFiles
    these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
  }

  def getRecursiveSetOfDictDirs(basePaths: Seq[String]): Set[DictionaryFolder] = {
    val babylonFiles = basePaths.flatMap(basePath => getRecursiveListOfFiles(new File(basePath))).
      filter(_.getName.matches(".*\\.babylon(_final)?"))
    val dictionaryFolders = babylonFiles.map(x => new DictionaryFolder(x.getParentFile)).toSet
    dictionaryFolders
  }

  def getMatchingDirectories (file_pattern: String = ".*", baseDir: String = "."): List[java.io.File] = {
    log info (s"file_pattern: ${file_pattern}")
    val baseDirFile = new File(baseDir)
    log info (s"Current directory: ${baseDirFile.getCanonicalPath}")

    log info baseDirFile.listFiles.filter(_.isDirectory).mkString("\n")
    val directories = baseDirFile.listFiles.filter(_.isDirectory).filter(x => x.getName.matches(s".*/?$file_pattern")).filterNot(_.getName.matches(".*/?tars"))
    log info (s"Got ${directories.length} directories")
    directories.toList
  }

  def getMatchingDictionaries(file_pattern: String = ".*", baseDir: String = "."): List[DictionaryFolder] = getMatchingDirectories(file_pattern, baseDir).map(new DictionaryFolder(_))

}

