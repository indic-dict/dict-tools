package sanskritnlp.dictionary.generators

import scala.io.Source

object utils {
  def tsvToMapIterator(infileStr: String, delimiter: String = "\t"): Stream[Map[String, String]] = {
    val src = Source.fromFile(infileStr, "utf8").bufferedReader()
    val columnNames = src.readLine().split(delimiter)
    val rowIterator = src.lines().map(line => {
      val values = line.split(delimiter)
      columnNames.zip(values).toMap
    }).asInstanceOf[Stream[Map[String, String]]]
    rowIterator
  }
}
