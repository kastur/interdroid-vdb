package interdroid.vdb.content;

import interdroid.vdb.content.EntityUriMatcher.MatchType;

import android.net.Uri;

public class EntityUriBuilder {

	public static Uri repositoryUri(String repoName)
	{
		String uriStr = "content://" + VdbMainContentProvider.AUTHORITY + "/"
				+ repoName;
		return Uri.parse(uriStr);
	}

	public static Uri branchUri(String repoName, String name)
	{
		return Uri.withAppendedPath(repositoryUri(repoName),
				MatchType.LOCAL_BRANCH.shortString_ + "/" + name);
	}

	public static Uri remoteBranchUri(String repoName, String name)
	{
		return Uri.withAppendedPath(repositoryUri(repoName),
				MatchType.REMOTE_BRANCH.shortString_ + "/" + name);
	}

	public static Uri remoteUri(String repoName, String name)
	{
		return Uri.withAppendedPath(repositoryUri(repoName),
				MatchType.REMOTE.shortString_ + "/" + name);
	}

	public static Uri commitUri(String repoName, String sha1)
	{
		return Uri.withAppendedPath(repositoryUri(repoName),
				MatchType.COMMIT.shortString_ + "/" + sha1);
	}
}
