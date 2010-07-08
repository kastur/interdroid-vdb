package interdroid.vdb.content;


import java.util.ListIterator;

import android.net.Uri;

public class EntityUriMatcher {
	public static enum MatchType {
		REPOSITORY(""),
		LOCAL_BRANCH("branches"),
		COMMIT("commits"),
		REMOTE("remote"),
		REMOTE_BRANCH("remote-branches");

		public final String shortString_;
		MatchType(String shortValue)
		{
			shortString_ = shortValue;
		}

		public static MatchType fromShortString(String shortString)
		{
			for (MatchType type : MatchType.values()) {
				if (type.shortString_.equals(shortString)) {
					return type;
				}
			}
			return null;
		}
	}

	private EntityUriMatcher() {}

	/**
	 * Value object specifying the results of a successful URI match.
	 */
	public static class UriMatch {
		/**
		 * The name of the repository as specified in the first path segment.
		 * Always present in a valid match.
		 */
		public String repositoryName;

		/**
		 * The type of match.
		 * Always present in a valid match.
		 */
		public MatchType type;

		/**
		 * Name of git reference, always present for all match types except
		 * MatchType.REPOSITORY and MatchType.METADATA.
		 */
		public String reference;

		/**
		 * Name of entity from the uri part right after reference.
		 * Can be null if it was not present.
		 */
		public String entityName;

		/**
		 * Identifier present as the last path segment after entityName.
		 * Can be null if it was not present.
		 */
		public String entityIdentifier;

		/**
		 * Returns whether this URI points to a vdb checkout.
		 *
		 * It may then point to a table in which case {@link #entityName}
		 * is present possibly together with an {@link #entityIdentifier}
		 * indicating a row.
		 */
		public boolean isCheckout()
		{
			return type == MatchType.LOCAL_BRANCH || type == MatchType.REMOTE_BRANCH
				|| type == MatchType.COMMIT;
		}

		/**
		 * Returns whether the checkout this match points to is read only
		 * or not.
		 *
		 * @throws IllegalStateException when this is not a checkout match
		 * so please call {@link #isCheckout()} first.
		 */
		public boolean isReadOnlyCheckout()
		{
			if (!isCheckout()) {
				throw new IllegalStateException("This UriMatch is not a checkout.");
			}
			return type != MatchType.LOCAL_BRANCH;
		}

		/**
		 * Constructs a blank match.
		 **/
		public UriMatch()
		{
		}

		/**
		 * Copy constructor
		 **/
		public UriMatch(UriMatch other)
		{
			repositoryName = other.repositoryName;
			type = other.type;
			reference = other.reference;
			entityName = other.entityName;
			entityIdentifier = other.entityIdentifier;
		}

		/**
		 * Builds an Uri with the components specified in this object.
		 */
		public Uri buildUri()
		{
			Uri.Builder b = new Uri.Builder().scheme("content")
					.authority(VdbMainContentProvider.AUTHORITY)
					.appendPath(repositoryName);
			if (type == MatchType.REPOSITORY) {
				return b.build();
			}
			b.appendPath(type.shortString_).appendEncodedPath(reference);
			if (entityName == null) {
				return b.build();
			}
			b.appendPath(entityName);
			if (entityIdentifier == null) {
				return b.build();
			}
			b.appendPath(entityIdentifier);
			return b.build();
		}

		/**
		 * Returns an Uri pointing to the checkout part of this match
		 * (strips the table and row).
		 *
		 * @throws IllegalStateException when this is not a checkout match
		 * so please call {@link #isCheckout()} first.
		 * @return the stripped checkout Uri
		 */
		public Uri getCheckoutUri()
		{
			if (!isCheckout()) {
				throw new IllegalStateException("This UriMatch is not a checkout.");
			}
			UriMatch copy = new UriMatch(this);
			copy.entityName = copy.entityIdentifier = null;
			return copy.buildUri();
		}
	}

	/**
	 * content://authority/repository_name/metadata/entity
	 * content://authority/repository_name/metadata/entity/id
	 *
	 * content://authority/repository_name/branches/branch_name/entity
	 * content://authority/repository_name/branches/branch_name/entity/id
	 * content://authority/repository_name/remote/remote_name
	 * content://authority/repository_name/remote-branches/remote_name/entity
	 * content://authority/repository_name/remote-branches/remote_name/entity/id
	 * content://authority/repository_name/commits/sha1/entity
	 * content://authority/repository_name/commits/sha1/entity/id
	 *
	 * @param uri
	 * @return
	 */
	public static UriMatch getMatch(Uri uri) {
		final UriMatch match = new UriMatch();

		if (!VdbMainContentProvider.AUTHORITY.equals(uri.getAuthority())) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		ListIterator<String> pathIterator = uri.getPathSegments().listIterator();

		if (!pathIterator.hasNext()) {
			throw new IllegalArgumentException("Unknown URI, no repository. " + uri);
		}
		match.repositoryName = pathIterator.next();

		if (pathIterator.hasNext()) {
			match.type = MatchType.fromShortString(pathIterator.next());
		} else {
			match.type = MatchType.REPOSITORY;
			return match;
		}
		if (match.type == null) {
			throw new IllegalArgumentException("Unknown URI, bad type. " + uri);
		}

		switch(match.type) {
		case COMMIT:
		case LOCAL_BRANCH:
		case REMOTE:
		case REMOTE_BRANCH:
			if (!pathIterator.hasNext()) {
				throw new IllegalArgumentException("Unknown URI, no reference. " + uri);
			}
			match.reference = pathIterator.next();
			if (match.type == MatchType.REMOTE_BRANCH) {
				if (!pathIterator.hasNext()) {
					throw new IllegalArgumentException("Unknown URI, no branch. " + uri);
				}
				match.reference = match.reference + "/" + pathIterator.next();
			}
			if (!pathIterator.hasNext()) {
				// no entity .. points to the branch/commit only
				return match;
			}
			break;

		}
		match.entityName = pathIterator.next();
		if (pathIterator.hasNext()) {
			match.entityIdentifier = pathIterator.next();
		}

		return match;
	}
}
