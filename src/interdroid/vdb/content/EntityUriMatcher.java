package interdroid.vdb.content;


import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.net.Uri;

public class EntityUriMatcher {
	private static final Logger logger = LoggerFactory.getLogger(EntityUriMatcher.class);

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
		 * True if the match is for a native URI content://provider
		 */
		public boolean isNative = false;

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
		 * Name of the last entity in the path.
		 * Can be null if it was not present.
		 */
		public String entityName;

		/**
		 * Identifier of the last entity in the path.
		 * Can be null if it was not present.
		 */
		public String entityIdentifier;

		/**
		 * Name of any parent entities.
		 * Can be null if none were present.
		 */
		public List<String> parentEntityNames;

		/**
		 * Parent entity identifiers which match the names.
		 * Can be null if none were present.
		 */
		public List<String> parentEntityIdentifiers;

		/**
		 * The authority for this entity URI.
		 */
		public String authority;

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
			isNative = other.isNative;
			authority = other.authority;
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
					.authority(authority);
			if (!isNative) {
					b.appendPath(repositoryName);
			}
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
	 * content://authority/repository_name/branches/branch_name/[entity/id]+[/entity]
	 * content://authority/repository_name/remote/remote_name
	 * content://authority/repository_name/remote-branches/remote_name/entity
	 * content://authority/repository_name/remote-branches/remote_name/[entity/id]+[/entity]?
	 * content://authority/repository_name/commits/sha1/entity
	 * content://authority/repository_name/commits/sha1/[entity/id]+[/entity]?
	 *
	 * @param uri
	 * @return
	 */
	public static UriMatch getMatch(Uri uri) {
		final UriMatch match = new UriMatch();

		match.authority = uri.getAuthority();
		logger.debug("Match authority: {}", match.authority);

		ListIterator<String> pathIterator = uri.getPathSegments().listIterator();

		if (match.authority.equals(VdbMainContentProvider.AUTHORITY)) {
			match.repositoryName = pathIterator.next();
			match.isNative = false;
		} else {
			match.repositoryName = match.authority;
			match.isNative = true;
		}
		logger.debug("Match repository: {}", match.repositoryName);

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

		// Now we have to handle the (possibly multiple levels of) entities
		String lastEntityName = null;
		String lastEntityId = null;
		while(pathIterator.hasNext()) {
			lastEntityName = pathIterator.next();
			// Do we have an Id for this one?
			if (pathIterator.hasNext()) {
				lastEntityId = pathIterator.next();
			} else {
				lastEntityId = null;
			}
			// Are we done?
			if (!pathIterator.hasNext()) {
				match.entityName = lastEntityName;
				match.entityIdentifier = lastEntityId;
			} else {
				// Do we need to init the lists?
				if (match.parentEntityNames == null) {
					match.parentEntityNames = new ArrayList<String>();
					match.parentEntityIdentifiers = new ArrayList<String>();
				}
				match.parentEntityNames.add(lastEntityName);
				match.parentEntityIdentifiers.add(lastEntityId);
			}
		}

		return match;
	}
}
