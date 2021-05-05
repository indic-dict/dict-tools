package sanskritnlp.dictionary

import java.io.File

object mdTools {
  def getFilePath(destPath: String, word: String, prefixPathDepth: Int = 4): File = {
    var filePath = new File(destPath)
    Range(0, prefixPathDepth, 1).foreach(i => {
      if (word.length > i) {
        filePath = new File(filePath, word.substring(0, i+1))
      }
    })
    filePath = new File(filePath, word + ".md")
    return filePath
  }
}
