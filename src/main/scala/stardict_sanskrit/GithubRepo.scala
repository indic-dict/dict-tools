package stardict_sanskrit

import github4s.Github
import github4s.Github._
import github4s.GithubResponses.GHResult
import github4s.free.domain.Commit
import github4s.jvm.Implicits._
import org.slf4j.{Logger, LoggerFactory}
import scalaj.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class GithubRepo(val githubOrg: String, val githubRepo: String, val githubToken: Option[String] = None, tarFileBranch: Option[String]=None) {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)

  val githubClient: Github = Github(githubToken)
  
  def getGitPath(filePath: String) = filePath.replaceFirst(s".+/${githubRepo}/", "")

  def getTarFileNameTimestampFromGithub(dictionaryFolder: DictionaryFolder): Option[String] = {
    val contentsResponseFuture = githubClient.repos.getContents(owner = githubOrg, repo = githubRepo, path = (getGitPath(filePath = dictionaryFolder.getTarDirFile.getAbsolutePath) + "/tars"), ref = tarFileBranch).exec[Future, HttpResponse[String]]()
    val contentsResponse = Await.result(contentsResponseFuture, 20.seconds)
    contentsResponse match {
      case Right(GHResult(contents, status, headers)) =>
        // Assuming that the first commit is the latest. TODO: Do something more robust.
        val tarContent = contents.filter(_.name.startsWith(dictionaryFolder.name))
        if (tarContent.headOption.isEmpty) {
          return None
        } else {
          return tarProcessor.getTimestampFromName(tarContent.headOption.get.name)
        }
      case Left(e) => log error e.getMessage
        return None
    }
  }

  def getGithubUpdateTime(filePath: String, branch:Option[String]=None): Option[String] = {
    val relativePath = filePath.replaceFirst(s".+${githubRepo}/", "")
    val commitsResponseFuture = githubClient.repos.listCommits(owner = githubOrg, repo = githubRepo, path=Some(relativePath), sha=branch).exec[Future, HttpResponse[String]]()
    val commitsResponse = Await.result(commitsResponseFuture, 20.seconds)
    commitsResponse match {
      case Right(GHResult(commits, status, headers)) =>
        if (commits.isEmpty) {
          return None
        } else {
          // Assuming that the first commit is the latest. TODO: Do something more robust.
          val lastestCommit: Commit = commits.head
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
    return new GithubRepo(githubOrg=githubOrg, githubRepo=githubRepo, tarFileBranch=Some(githubBranch), githubToken=githubToken)
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