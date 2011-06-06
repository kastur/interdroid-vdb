package interdroid.vdb.content;

import java.util.List;

import interdroid.vdb.content.EntityUriMatcher.MatchType;

import android.net.Uri;

public class EntityUriBuilder {

	public static Uri repositoryUri(String authority, String repoName)
	{
		String uriStr = "content://" + authority + "/"
				+ (repoName.equals(authority) ? "" : repoName);
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

	public static Uri toNative(Uri uri) {
		Uri.Builder builder = uri.buildUpon();
		List<String> path = uri.getPathSegments();
		String pathString = "";
		for (int i = 1; i < path.size(); i++) {
			pathString += "/" + path.get(i);
		}
		builder.authority(path.get(0));
		builder.path(pathString);

		return builder.build();
	}
}
