package stardict_sanskrit

import java.io.File
import java.net.URL
import akka.actor.ActorSystem
import cats.data.NonEmptyList
import github4s.{GHError, GHResponse, Github}
import github4s.domain.{Commit, Content}
import cats.effect.IO
import org.http4s.client.{Client, JavaNetClientBuilder}
import org.slf4j.{Logger, LoggerFactory}
import sanskrit_coders.RichHttpAkkaClient

import java.lang
import java.net.http.HttpResponse
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._


class GithubRepo(val githubOrg: String, val githubRepo: String, val githubToken: Option[String] = None, branch: Option[String]=None) {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  val httpClient: Client[IO] = {
    JavaNetClientBuilder[IO].create // You can use any http4s backend
  }
  val githubClient: Github[IO] = Github(httpClient, githubToken)
  
  def getGitPath(filePath: String) = {
    filePath.replaceFirst(s".+/$githubRepo/", "")
  }

  def getDirContents(dirPath: String): Either[GHError, NonEmptyList[Content]] = {
    val contentsResponseFuture = githubClient.repos.getContents(owner = githubOrg, repo = githubRepo, path = (getGitPath(filePath = dirPath)), ref = branch)
    import cats.effect.unsafe.implicits.global
    return contentsResponseFuture.unsafeRunSync().result
  }

  /**
   * Get timestamp from the name of a file whose prefix has been passed in.
   * 
   * @param fileNamePrefix
   * @param dirPath
   * @return
   */
  def getFileNameTimestampFromGithub(fileNamePrefix: String, dirPath: String): Option[String] = {
    getDirContents(dirPath = dirPath) match {
      case Right(contents) =>
        // Assuming that the first commit is the latest. TODO: Do something more robust.
        val fileContent = contents.filter(_.name.startsWith(fileNamePrefix + "__"))
        if (fileContent.headOption.isEmpty) {
          return None
        } else {
          return tarProcessor.getTimestampFromName(fileContent.headOption.get.name)
        }
      case Left(e) => log error s"${e.getMessage}: ${getGitPath(dirPath)}, ${dirPath}, ${fileNamePrefix}"
        return None
    }
  }

  def getContentList(tarDirFilePath: String, extension:String ="tar.gz"): List[Content] = {
    getDirContents(tarDirFilePath) match {
      case Right(contents) =>
        // Assuming that the first commit is the latest. TODO: Do something more robust.
        val tarContent = contents.filter(_.name.endsWith(extension))
        return tarContent
      case Left(e) => log error e.getMessage
        return List()
    }
  }
  
  def downloadFileByPrefix(fileName: String, dirPath: String): Unit = {
    getDirContents(dirPath = dirPath) match {
      case Right(contents) =>
        // Assuming that the first commit is the latest. TODO: Do something more robust.
        val tarContent = contents.filter(_.name.startsWith(fileName))
        if (tarContent.headOption.isEmpty) {
          log error s"Did not find tar file for ${fileName}!"
        } else {
          import sys.process._
          val destPath = new File(dirPath, tarContent.head.name)
          log info s"Downloading ${tarContent.head.download_url.get} to ${destPath}"
          implicit val actorSystem = ActorSystem("HttpAkka")
          try {
            Await.ready(RichHttpAkkaClient.dumpToFile(tarContent.head.download_url.get, destPath.toString), Duration(3, MINUTES))
            log info s"Downloaded ${tarContent.head.download_url.get} to ${destPath}. Now terminating actor system."
          } catch {
            case e : Exception => log error e.getMessage
              e.printStackTrace()
          } finally {
            log info "Terminating actor system"
            actorSystem.terminate()
          }
          
//          new URL(tarContent.head.download_url.get) #> destPath !!
        }
      case Left(e) => log error e.getMessage
    }
  }
  
  def getGithubUpdateTime(filePath: String, branch:Option[String]=None): Option[String] = {
    val relativePath = filePath.replaceFirst(s".+$githubRepo/", "")
    import cats.effect.unsafe.implicits.global
    val commitsResponse: Either[GHError, List[Commit]] = githubClient.repos.listCommits(owner = githubOrg, repo = githubRepo, path=Some(relativePath), sha=branch).unsafeRunSync().result
    commitsResponse match {
      case Right(commits) =>
        if (commits.isEmpty) {
          return None
        } else {
          // Assuming that the first commit is the latest. TODO: Do something more robust.
          val lastestCommit = commits.head
          return Some(lastestCommit.date.replace("T", "_").replace(":", "-"))
        }
      case Left(e) => log error e.getMessage
        return None
    }
  }
}


object GithubRepo {

  def fromUrl(url: String, githubToken: Option[String] = None): GithubRepo = {
    val (githubOrg, githubRepo, githubBranch) = GithubRepo.getGithubOrgRepoBranch(url)
    return new GithubRepo(githubOrg=githubOrg, githubRepo=githubRepo, branch=Some(githubBranch), githubToken=githubToken)
  }
  
  def getGithubOrgRepoBranch(someUrl: String): (String, String, String) = {
    val repoUrlPattern = ".+github.com/(.+?)/(.+?)/raw/(.+?)/.*".r("org", "repo", "branch")
    val githubOrgRepoMatch = repoUrlPattern.findFirstMatchIn(someUrl)
    assert(githubOrgRepoMatch != None)
    val githubOrg = githubOrgRepoMatch.get.group("org")
    val githubRepo = githubOrgRepoMatch.get.group("repo")
    val githubBranch = githubOrgRepoMatch.get.group("branch")
    return (githubOrg, githubRepo, githubBranch)
  }
  
}