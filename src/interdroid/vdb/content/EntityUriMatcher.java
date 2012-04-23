/*
 * Copyright (c) 2008-2012 Vrije Universiteit, The Netherlands All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the Vrije Universiteit nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package interdroid.vdb.content;

import interdroid.vdb.Authority;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.net.Uri;

/**
 * A utility class which knows how to digest VDB URIs into component parts.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public final class EntityUriMatcher {
	/**
	 * The logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(EntityUriMatcher.class);

	/**
	 * The types of URIs supported.
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 *
	 */
	public static enum MatchType {
		/** For master in a repository no type is required. */
		REPOSITORY(""),
		/** For a local branch "branches" is required. */
		LOCAL_BRANCH("branches"),
		/** For a commit "Commits" is required. */
		COMMIT("commits"),
		/** For a remote master "remote" is required. */
		REMOTE("remote"),
		/** For a remote branch "remote-branches" is required. */
		REMOTE_BRANCH("remote-branches");

		/**
		 * The short string for this match type.
		 */
		private final String shortString;

		/**
		 * Constructs a MatchType for the given string.
		 * @param shortValue the string value for this MatchType.
		 */
		MatchType(final String shortValue) {
			shortString = shortValue;
		}

		/**
		 * Convert from a short string to the MatchType.
		 * @param shortString the string to convert
		 * @return the matched type or null if there is no match.
		 */
		public static MatchType fromShortString(final String shortString) {
			for (MatchType type : MatchType.values()) {
				if (type.toString().equals(shortString)) {
					return type;
				}
			}
			return null;
		}

		/**
		 * @return the string version of this type.
		 */
		public String toString() {
			return shortString;
		}
	}

	/**
	 * Utility classes can not be constructed.
	 */
	private EntityUriMatcher() { }

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
		 * True if the match is for a native URI content://provider.
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
		 * @return true if this points to a checkout.
		 */
		public final boolean isCheckout() {
			return type == MatchType.LOCAL_BRANCH
					|| type == MatchType.REMOTE_BRANCH
					|| type == MatchType.COMMIT;
		}

		/**
		 * Returns whether the checkout this match points to is read only
		 * or not. Note that this method throws the unchecked
		 * IllegalStateException when this is not a checkout match
		 * so please call {@link #isCheckout()} first.
		 * @return true if this is a read only checkout
		 */
		public final boolean isReadOnlyCheckout() {
			if (!isCheckout()) {
				throw new IllegalStateException(
						"This UriMatch is not a checkout.");
			}
			return type != MatchType.LOCAL_BRANCH;
		}

		/**
		 * Constructs a blank match.
		 **/
		public UriMatch() { }

		/**
		 * Copy constructor.
		 * @param other the match to copy
		 **/
		public UriMatch(final UriMatch other) {
			isNative = other.isNative;
			authority = other.authority;
			repositoryName = other.repositoryName;
			type = other.type;
			reference = other.reference;
			entityName = other.entityName;
			entityIdentifier = other.entityIdentifier;
		}

		/**
		 * Builds an URI with the components specified in this object.
		 * @return the built URI
		 */
		public final Uri buildUri() {
			Uri.Builder b = new Uri.Builder().scheme("content")
					.authority(authority);
			if (!isNative) {
					b.appendPath(repositoryName);
			}
			if (type == MatchType.REPOSITORY) {
				return b.build();
			}
			b.appendPath(type.toString()).appendEncodedPath(reference);
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
		 * Note that this throws the unchecked IllegalStateException
		 * when this is not a checkout match
		 * so please call {@link #isCheckout()} first.
		 * @return the stripped checkout Uri
		 */
		public final Uri getCheckoutUri() {
			if (!isCheckout()) {
				throw new IllegalStateException(
						"This UriMatch is not a checkout.");
			}
			UriMatch copy = new UriMatch(this);
			copy.entityName = null;
			copy.entityIdentifier = null;
			return copy.buildUri();
		}
	}


	/*
	 * Various types of URIs we match.
	 * content://authority/repository_name/metadata/entity
	 * content://authority/repository_name/metadata/entity/id
	 *
	 * content://authority/repository_name/branches/branch_name/entity
	 * content://authority/repository_name/branches/branch_name/
	 *                                              [entity/id]+[/entity]
	 * content://authority/repository_name/remote/remote_name
	 * content://authority/repository_name/remote-branches/remote_name/entity
	 * content://authority/repository_name/remote-branches/remote_name/
	 *                                              [entity/id]+[/entity]?
	 * content://authority/repository_name/commits/sha1/entity
	 * content://authority/repository_name/commits/sha1/[entity/id]+[/entity]?
	 */
	/**
	 * Returns a match for the given URI.
	 * @param uri the uri to be matched
	 * @return the match result
	 */
	public static UriMatch getMatch(final Uri uri) {
		final UriMatch match = new UriMatch();

		match.authority = uri.getAuthority();
		LOG.debug("Match authority: {}", match.authority);

		ListIterator<String> pathIterator =
				uri.getPathSegments().listIterator();

		if (match.authority.equals(Authority.VDB)) {
			match.repositoryName = pathIterator.next();
			match.isNative = false;
		} else {
			match.repositoryName = match.authority;
			match.isNative = true;
		}
		LOG.debug("Match repository: {}", match.repositoryName);

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
				throw new IllegalArgumentException(
						"Unknown URI, no reference. " + uri);
			}
			match.reference = pathIterator.next();
			if (match.type == MatchType.REMOTE_BRANCH) {
				if (!pathIterator.hasNext()) {
					throw new IllegalArgumentException(
							"Unknown URI, no branch. " + uri);
				}
				match.reference = match.reference + "/" + pathIterator.next();
			}
			if (!pathIterator.hasNext()) {
				// no entity .. points to the branch/commit only
				return match;
			}
			break;
		default:
			// Nothing to be done.
			break;
		}

		// Now we have to handle the (possibly multiple levels of) entities
		String lastEntityName = null;
		String lastEntityId = null;
		while (pathIterator.hasNext()) {
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
