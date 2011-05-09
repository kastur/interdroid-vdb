package interdroid.vdb.content;

import interdroid.vdb.content.EntityUriMatcher.MatchType;

import android.net.Uri;

public class EntityUriBuilder {

	public static Uri repositoryUri(String authority, String repoName)
	{
		String uriStr = "content://" + authority + "/"
				+ repoName;
		return Uri.parse(uriStr);
	}

	public static Uri branchUri(String authority, String repoName, String name)
	{
		return Uri.withAppendedPath(repositoryUri(authority, repoName),
				MatchType.LOCAL_BRANCH.shortString_ + "/" + name);
	}

	public static Uri remoteBranchUri(String authority, String repoName, String name)
	{
		return Uri.withAppendedPath(repositoryUri(authority, repoName),
				MatchType.REMOTE_BRANCH.shortString_ + "/" + name);
	}

	public static Uri remoteUri(String authority, String repoName, String name)
	{
		return Uri.withAppendedPath(repositoryUri(authority, repoName),
				MatchType.REMOTE.shortString_ + "/" + name);
	}

	public static Uri commitUri(String authority, String repoName, String sha1)
	{
		return Uri.withAppendedPath(repositoryUri(authority, repoName),
				MatchType.COMMIT.shortString_ + "/" + sha1);
	}
}
