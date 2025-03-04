package teksturepako.pakku.api.models.gh

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Full Repository
 *
 * @property id Unique identifier of the repository (Example: 1296269)
 * @property nodeId (Example: "MDEwOlJlcG9zaXRvcnkxMjk2MjY5")
 * @property name The name of the repository (Example: "Hello-World")
 * @property fullName (Example: "octocat/Hello-World")
 * @property owner A GitHub user
 * @property private Whether the repository is private or public
 * @property htmlUrl (Example: "https://github.com/octocat/Hello-World")
 * @property description (Example: "This your first repo!")
 * @property fork
 * @property url (Example: "https://api.github.com/repos/octocat/Hello-World")
 * @property archiveUrl (Example: "http://api.github.com/repos/octocat/Hello-World/{archive_format}{/ref}")
 * @property assigneesUrl (Example: "http://api.github.com/repos/octocat/Hello-World/assignees{/user}")
 * @property blobsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/git/blobs{/sha}")
 * @property branchesUrl (Example: "http://api.github.com/repos/octocat/Hello-World/branches{/branch}")
 * @property collaboratorsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/collaborators{/collaborator}")
 * @property commentsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/comments{/number}")
 * @property commitsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/commits{/sha}")
 * @property compareUrl (Example: "http://api.github.com/repos/octocat/Hello-World/compare/{base}...{head}")
 * @property contentsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/contents/{+path}")
 * @property contributorsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/contributors")
 * @property deploymentsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/deployments")
 * @property downloadsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/downloads")
 * @property eventsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/events")
 * @property forksUrl (Example: "http://api.github.com/repos/octocat/Hello-World/forks")
 * @property gitCommitsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/git/commits{/sha}")
 * @property gitRefsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/git/refs{/sha}")
 * @property gitTagsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/git/tags{/sha}")
 * @property gitUrl (Example: "git:github.com/octocat/Hello-World.git")
 * @property issueCommentUrl (Example: "http://api.github.com/repos/octocat/Hello-World/issues/comments{/number}")
 * @property issueEventsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/issues/events{/number}")
 * @property issuesUrl (Example: "http://api.github.com/repos/octocat/Hello-World/issues{/number}")
 * @property keysUrl (Example: "http://api.github.com/repos/octocat/Hello-World/keys{/key_id}")
 * @property labelsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/labels{/name}")
 * @property languagesUrl (Example: "http://api.github.com/repos/octocat/Hello-World/languages")
 * @property mergesUrl (Example: "http://api.github.com/repos/octocat/Hello-World/merges")
 * @property milestonesUrl (Example: "http://api.github.com/repos/octocat/Hello-World/milestones{/number}")
 * @property notificationsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/notifications{?since,all,participating}")
 * @property pullsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/pulls{/number}")
 * @property releasesUrl (Example: "http://api.github.com/repos/octocat/Hello-World/releases{/id}")
 * @property sshUrl (Example: "git@github.com:octocat/Hello-World.git")
 * @property stargazersUrl (Example: "http://api.github.com/repos/octocat/Hello-World/stargazers")
 * @property statusesUrl (Example: "http://api.github.com/repos/octocat/Hello-World/statuses/{sha}")
 * @property subscribersUrl (Example: "http://api.github.com/repos/octocat/Hello-World/subscribers")
 * @property subscriptionUrl (Example: "http://api.github.com/repos/octocat/Hello-World/subscription")
 * @property tagsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/tags")
 * @property teamsUrl (Example: "http://api.github.com/repos/octocat/Hello-World/teams")
 * @property treesUrl (Example: "http://api.github.com/repos/octocat/Hello-World/git/trees{/sha}")
 * @property cloneUrl (Example: "https://github.com/octocat/Hello-World.git")
 * @property mirrorUrl (Example: "git:git.example.com/octocat/Hello-World")
 * @property hooksUrl (Example: "http://api.github.com/repos/octocat/Hello-World/hooks")
 * @property svnUrl (Example: "https://svn.github.com/octocat/Hello-World")
 * @property homepage (Example: "https://github.com")
 * @property forksCount (Example: 9)
 * @property stargazersCount (Example: 80)
 * @property watchersCount (Example: 80)
 * @property size The size of the repository, in kilobytes. Size is calculated hourly. When a repository is initially created, the size is 0. (Example: 108)
 * @property defaultBranch The default branch of the repository (Example: "master")
 * @property openIssuesCount (Example: 0)
 * @property isTemplate Whether this repository acts as a template that can be used to generate new repositories (Example: true)
 * @property topics (Example: ["octocat", "atom", "electron", "API"])
 * @property hasIssues Whether issues are enabled (Example: true)
 * @property hasProjects Whether projects are enabled (Example: true)
 * @property hasWiki Whether the wiki is enabled (Example: true)
 * @property hasPages
 * @property hasDownloads Whether downloads are enabled (Example: true)
 * @property hasDiscussions Whether discussions are enabled (Example: true)
 * @property archived Whether the repository is archived
 * @property disabled Returns whether this repository disabled
 * @property visibility The repository visibility: public, private, or internal (Example: "public")
 * @property pushedAt (Example: "2011-01-26T19:06:43Z")
 * @property createdAt (Example: "2011-01-26T19:01:12Z")
 * @property updatedAt (Example: "2011-01-26T19:14:43Z")
 * @property permissions
 * @property allowRebaseMerge Whether to allow rebase merges for pull requests (Example: true)
 * @property tempCloneToken
 * @property allowSquashMerge Whether to allow squash merges for pull requests (Example: true)
 * @property allowAutoMerge Whether to allow Auto-merge to be used on pull requests (Example: false)
 * @property deleteBranchOnMerge Whether to delete head branches when pull requests are merged (Example: false)
 * @property allowMergeCommit Whether to allow merge commits for pull requests (Example: true)
 * @property allowForking Whether to allow forking this repo
 * @property openIssues
 * @property watchers
 */
@Serializable
data class GhRepoModel(
    val id: Int,
    @SerialName("node_id") val nodeId: String,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val owner: GhOwnerModel,
    val `private`: Boolean,
    @SerialName("html_url") val htmlUrl: String,
    val description: String? = null,
    val fork: Boolean,
    val url: String,
    @SerialName("archive_url") val archiveUrl: String,
    @SerialName("assignees_url") val assigneesUrl: String,
    @SerialName("blobs_url") val blobsUrl: String,
    @SerialName("branches_url") val branchesUrl: String,
    @SerialName("collaborators_url") val collaboratorsUrl: String,
    @SerialName("comments_url") val commentsUrl: String,
    @SerialName("commits_url") val commitsUrl: String,
    @SerialName("compare_url") val compareUrl: String,
    @SerialName("contents_url") val contentsUrl: String,
    @SerialName("contributors_url") val contributorsUrl: String,
    @SerialName("deployments_url") val deploymentsUrl: String,
    @SerialName("downloads_url") val downloadsUrl: String,
    @SerialName("events_url") val eventsUrl: String,
    @SerialName("forks_url") val forksUrl: String,
    @SerialName("git_commits_url") val gitCommitsUrl: String,
    @SerialName("git_refs_url") val gitRefsUrl: String,
    @SerialName("git_tags_url") val gitTagsUrl: String,
    @SerialName("git_url") val gitUrl: String,
    @SerialName("issue_comment_url") val issueCommentUrl: String,
    @SerialName("issue_events_url") val issueEventsUrl: String,
    @SerialName("issues_url") val issuesUrl: String,
    @SerialName("keys_url") val keysUrl: String,
    @SerialName("labels_url") val labelsUrl: String,
    @SerialName("languages_url") val languagesUrl: String,
    @SerialName("merges_url") val mergesUrl: String,
    @SerialName("milestones_url") val milestonesUrl: String,
    @SerialName("notifications_url") val notificationsUrl: String,
    @SerialName("pulls_url") val pullsUrl: String,
    @SerialName("releases_url") val releasesUrl: String,
    @SerialName("ssh_url") val sshUrl: String,
    @SerialName("stargazers_url") val stargazersUrl: String,
    @SerialName("statuses_url") val statusesUrl: String,
    @SerialName("subscribers_url") val subscribersUrl: String,
    @SerialName("subscription_url") val subscriptionUrl: String,
    @SerialName("tags_url") val tagsUrl: String,
    @SerialName("teams_url") val teamsUrl: String,
    @SerialName("trees_url") val treesUrl: String,
    @SerialName("clone_url") val cloneUrl: String,
    @SerialName("mirror_url") val mirrorUrl: String? = null,
    @SerialName("hooks_url") val hooksUrl: String,
    @SerialName("svn_url") val svnUrl: String,
    val homepage: String? = null,
    @SerialName("forks_count") val forksCount: Int,
    val forks: Int,
    @SerialName("stargazers_count") val stargazersCount: Int,
    @SerialName("watchers_count") val watchersCount: Int,
    val watchers: Int,
    val size: Int,
    @SerialName("default_branch") val defaultBranch: String,
    @SerialName("open_issues_count") val openIssuesCount: Int,
    @SerialName("open_issues") val openIssues: Int,
    @SerialName("is_template") val isTemplate: Boolean = false,
    val topics: List<String> = listOf(),
    @SerialName("has_issues") val hasIssues: Boolean = true,
    @SerialName("has_projects") val hasProjects: Boolean = true,
    @SerialName("has_wiki") val hasWiki: Boolean = true,
    @SerialName("has_pages") val hasPages: Boolean,
    @SerialName("has_downloads") val hasDownloads: Boolean = true,
    @SerialName("has_discussions") val hasDiscussions: Boolean = false,
    val archived: Boolean = false,
    val disabled: Boolean,
    val visibility: String = "public",
    @SerialName("pushed_at") val pushedAt: String? = null, // date-time
    @SerialName("created_at") val createdAt: String? = null, // date-time
    @SerialName("updated_at") val updatedAt: String? = null, // date-time
    val permissions: Permissions? = null,
    @SerialName("allow_rebase_merge") val allowRebaseMerge: Boolean = true,
    @SerialName("temp_clone_token") val tempCloneToken: String? = null,
    @SerialName("allow_squash_merge") val allowSquashMerge: Boolean? = null,
    @SerialName("allow_auto_merge") val allowAutoMerge: Boolean? = null,
    @SerialName("delete_branch_on_merge") val deleteBranchOnMerge: Boolean? = null,
    @SerialName("allow_merge_commit") val allowMergeCommit: Boolean? = null,
    @SerialName("allow_forking") val allowForking: Boolean,
    @SerialName("subscribers_count") val subscribersCount: Int,
    @SerialName("network_count") val networkCount: Int,
    val license: License? = null,
    val organization: GhOwnerModel? = null,
)
{
    @Serializable
    data class Permissions(
        val pull: Boolean, val push: Boolean, val admin: Boolean
    )

    @Serializable
    data class License(
        val key: String,
        val name: String,
        val url: String? = null,
        @SerialName("spdx_id") val spdxId: String? = null,
        @SerialName("node_id") val nodeId: String,
    )
}