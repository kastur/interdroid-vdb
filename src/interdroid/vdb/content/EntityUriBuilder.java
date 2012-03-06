package interdroid.vdb.content;

import java.util.List;

import interdroid.vdb.Authority;
import interdroid.vdb.content.EntityUriMatcher.MatchType;

import android.net.Uri;

/**
 * A utility class for building VDB URIs.
 * <br/>
 * URIs come in with either a native authority used by the package
 * to expose data to all other applications or an internal authority
 * which VDB offers the equivalent data from.
 * <br/>
 * Internal URIs are of the form: <br/>
 * content://VDB_AUTHORITY/NATIVE_AUTHORITY/TYPE/ENTITY <br/>
 * Native URIs are of the form: <br/>
 * content://NATIVE_AUTHORITY/TYPE/ENTITY <br/>
 * <br/>
 * URI types can either be for the local master, a local branch, a
 * remote master, a remote branch or a commit. EntityUriMatcher.MatchType
 * for the strings for these different type specifiers. For local master
 * no type need be specified at all.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * @see EntityUriMatcher.MatchType
 */
public final class EntityUriBuilder {

    /**
     * No construction of this utility class.
     */
    private EntityUriBuilder() { }

    /**
     * Returns a native URI for the given entity in the given authority.
     * @param authority the authority for the repository
     * @param entity the entity in the repository
     * @return a URI
     */
    public static Uri nativeUri(final String authority, final String entity) {
        return Uri.withAppendedPath(repositoryUri(authority, authority),
                MatchType.LOCAL_BRANCH.toString() + "/master/" + entity);
    }

    /**
     * Returns a URI for the given repository in the given authority.
     * @param authority the authority for the repository
     * @param repoName the repository
     * @return a URI
     */
    public static Uri repositoryUri(final String authority,
            final String repoName) {
        String uriStr;
        if (repoName.equals(authority)) {
            uriStr = "content://" + authority + "/";
        } else {
            uriStr = "content://" + authority + "/" + repoName;
        }
        return Uri.parse(uriStr);
    }

    /**
     * Returns a URI for a branch.
     * @param authority the authority for the repository
     * @param repoName the repository name
     * @param name the branch name
     * @return a URI
     */
    public static Uri branchUri(final String authority, final String repoName,
            final String name) {
        return Uri.withAppendedPath(repositoryUri(authority, repoName),
                MatchType.LOCAL_BRANCH.toString() + "/" + name);
    }

    /**
     * Returns a URI for a remote branch.
     * @param authority The authority for the repository
     * @param repoName the repository name
     * @param name the remote branch name
     * @return a URI
     */
    public static Uri remoteBranchUri(final String authority,
            final String repoName, final String name) {
        return Uri.withAppendedPath(repositoryUri(authority, repoName),
                MatchType.REMOTE_BRANCH.toString() + "/" + name);
    }

    /**
     * Returns a URI for a remote repository.
     * @param authority the authority for the repository
     * @param repoName the repository name
     * @param name the remote name
     * @return a URI
     */
    public static Uri remoteUri(final String authority, final String repoName,
            final String name) {
        return Uri.withAppendedPath(repositoryUri(authority, repoName),
                MatchType.REMOTE.toString() + "/" + name);
    }

    /**
     * Returns a URI for a commit.
     * @param authority the authority for the repository
     * @param repoName the repository name
     * @param sha1 the SHA1 of the commit
     * @return a URI
     */
    public static Uri commitUri(final String authority, final String repoName,
            final String sha1) {
        return Uri.withAppendedPath(repositoryUri(authority, repoName),
                MatchType.COMMIT.toString() + "/" + sha1);
    }

    /**
     * Converts an internal URI for the given native URI.
     * @param uri the native URI to convert
     * @return a URI with internal authority
     */
    public static Uri toInternal(final Uri uri) {
        Uri.Builder builder = uri.buildUpon();
        StringBuffer pathString = new StringBuffer();
        pathString.append("/");
        pathString.append(uri.getAuthority());
        pathString.append(uri.getPath());
        builder.authority(Authority.VDB);
        builder.path(pathString.toString());

        return builder.build();
    }

    /**
     * Converts an internal URI to a native URI for the given repository.
     * @param uri the internal URI to convert
     * @return a URI with native authority
     */
    public static Uri toNative(final Uri uri) {
        Uri.Builder builder = uri.buildUpon();
        List<String> path = uri.getPathSegments();
        StringBuffer pathString = new StringBuffer();
        for (int i = 1; i < path.size(); i++) {
            pathString.append("/");
            pathString.append(path.get(i));
        }
        builder.authority(path.get(0));
        builder.path(pathString.toString());

        return builder.build();
    }
}

