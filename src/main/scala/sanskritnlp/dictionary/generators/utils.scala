package sanskritnlp.dictionary.generators

import scala.collection.immutable.ListMap
import scala.io.Source

object utils {
  def tsvToMapIterator(infileStr: String, delimiter: String = "\t"): Iterator[Map[String, String]] = {
    val columnNames = Source.fromFile(infileStr, "utf8").bufferedReader().readLine().split(delimiter).toList
    val rowIterator = Source.fromFile(infileStr, "utf8").getLines().drop(1).map(line => {
      val values = line.split(delimiter).toList
      ListMap(columnNames.zip(values): _*)
    })
    rowIterator
  }
}
