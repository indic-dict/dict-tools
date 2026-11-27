package sanskritnlp.dictionary

import java.io.{BufferedOutputStream, FileInputStream, FileOutputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.scalatest.funsuite.AnyFunSuite
import stardict_sanskrit.DictIndex

class StardictTarTest extends AnyFunSuite {

  private def writeTarGz(tarGzPath: java.nio.file.Path, entries: Map[String, String]): Unit = {
    val fout = new FileOutputStream(tarGzPath.toFile)
    val gzipOut = new GzipCompressorOutputStream(new BufferedOutputStream(fout))
    val tarOut = new TarArchiveOutputStream(gzipOut)
    try {
      entries.foreach { case (name, body) =>
        val bytes = body.getBytes(StandardCharsets.UTF_8)
        val entry = new TarArchiveEntry(name)
        entry.setSize(bytes.length)
        tarOut.putArchiveEntry(entry)
        tarOut.write(bytes)
        tarOut.closeArchiveEntry()
      }
      tarOut.finish()
    } finally {
      tarOut.close()
    }
  }

  test("extract replaces existing file atomically without truncating open file descriptors") {
    val tmpDir = Files.createTempDirectory("stardict-tar-test")
    try {
      val destDir = tmpDir.resolve("dest")
      Files.createDirectories(destDir)
      val existingFile = destDir.resolve("sample.dict.dz")
      Files.write(existingFile, "OLD_INODE_CONTENT_12345".getBytes(StandardCharsets.UTF_8))

      // Hold open FD like a running GoldenDict instance would before tar extraction.
      val openStream = new FileInputStream(existingFile.toFile)
      try {
        val tarGz = tmpDir.resolve("sample.tar.gz")
        writeTarGz(tarGz, Map(
          "sample.dict.dz" -> "NEW_INODE_CONTENT",
          "sample.ifo" -> "bookname=sample\n",
        ))
        new StardictTar(tarGz.toString).extract(destDir.toString)

        val oldBytes = new Array[Byte]("OLD_INODE_CONTENT_12345".length)
        assert(openStream.read(oldBytes) === oldBytes.length)
        assert(new String(oldBytes, StandardCharsets.UTF_8) === "OLD_INODE_CONTENT_12345")
      } finally {
        openStream.close()
      }

      assert(Files.readString(existingFile, StandardCharsets.UTF_8) === "NEW_INODE_CONTENT")
      assert(Files.readString(destDir.resolve("sample.ifo"), StandardCharsets.UTF_8) === "bookname=sample\n")
    } finally {
      Files.walk(tmpDir).sorted(java.util.Comparator.reverseOrder()).forEach(Files.deleteIfExists(_))
    }
  }

  test("DictIndex.getUrlsFromIndexMd parses local index URLs and handles missing URLs gracefully") {
    val tmpDir = Files.createTempDirectory("dict-index-test")
    try {
      val mdFile = tmpDir.resolve("dictionaryIndices.md")
      Files.writeString(mdFile, "# header\n<https://example.com/a/tars.MD>  \nDisabled: <https://example.com/skip/tars.MD>\nhttp://example.com/b/tars.MD\n")
      val urls = DictIndex.getUrlsFromIndexMd(mdFile.toUri.toString)
      assert(urls.toSeq === Seq("https://example.com/a/tars.MD", "http://example.com/b/tars.MD"))

      val missing = DictIndex.getUrlsFromIndexMd(tmpDir.resolve("missing.md").toUri.toString)
      assert(missing.isEmpty)
    } finally {
      Files.walk(tmpDir).sorted(java.util.Comparator.reverseOrder()).forEach(Files.deleteIfExists(_))
    }
  }
}
