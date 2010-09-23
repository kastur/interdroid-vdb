package interdroid.vdb.content;

import interdroid.vdb.content.EntityUriMatcher.MatchType;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;
import interdroid.vdb.content.VdbConfig.RepositoryConf;
import interdroid.vdb.persistence.api.VdbInitializer;
import interdroid.vdb.persistence.api.VdbInitializerFactory;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class VdbMainContentProvider extends ContentProvider {
	private static final Logger logger = LoggerFactory.getLogger(VdbMainContentProvider.class);

	public static final String AUTHORITY = VdbMainContentProvider.class.getName().toLowerCase();
	public static final String BASE_TYPE = "vnd." + VdbMainContentProvider.class.getPackage().getName().toLowerCase();

	private VdbConfig config_;
	private final Map<String,RepositoryInfo> repoInfos_
			= new HashMap<String,RepositoryInfo>();

	private static class RepositoryInfo {
		public final ContentProvider provider_;
		public final VdbInitializer initializer_;
		public final String name_;

		public RepositoryInfo(RepositoryConf conf)
		{
			try {
				if (logger.isDebugEnabled())
					logger.debug("Constructing Content Provider: " + conf.contentProvider_);
				provider_ = (ContentProvider) Class.forName(conf.contentProvider_).newInstance();
				initializer_ = ((VdbInitializerFactory)provider_).buildInitializer();
				name_ = conf.name_;
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public boolean onCreate()
	{
		if (config_ == null) {
			config_ = new VdbConfig(getContext());
			// Initialize all the child content providers, one for each repository.
			for (RepositoryConf repoConf : config_.getRepositories()) {
				RepositoryInfo repoInfo = new RepositoryInfo(repoConf);
				if (repoInfos_.containsKey(repoInfo.name_)) {
					throw new RuntimeException("Invalid configuration, duplicate repository name "
							+ repoInfo.name_);
				}
				try {
					if (logger.isDebugEnabled())
						logger.debug("Adding repository: " + repoInfo.name_);
					VdbRepositoryRegistry.getInstance().addRepository(getContext(),
							repoInfo.name_, repoInfo.initializer_);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				repoInfos_.put(repoInfo.name_, repoInfo);

				// Do this at the end, since onCreate will be called in the child
				// We want everything to be registered prior to this happening.
				repoInfo.provider_.attachInfo(getContext(), null);
			}
		}
		return true;
	}

	private void validateUri(Uri uri, RepositoryInfo info, UriMatch match)
	{
		if (info == null) {
			throw new IllegalArgumentException("Bad URI: unregistered repository. " + uri);
		}
		if (match.type == MatchType.REPOSITORY) {
			throw new IllegalArgumentException("Bad URI: only repository was specified. " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		if (logger.isDebugEnabled())
			logger.debug("delete: " + uri);
		UriMatch match = EntityUriMatcher.getMatch(uri);
		RepositoryInfo info = repoInfos_.get(match.repositoryName);
		validateUri(uri, info, match);
		return info.provider_.delete(uri, selection, selectionArgs);
	}

	@Override
	public String getType(Uri uri)
	{
		if (logger.isDebugEnabled())
			logger.debug("getType : " + uri);
		UriMatch match = EntityUriMatcher.getMatch(uri);
		RepositoryInfo info = repoInfos_.get(match.repositoryName);
		if (info == null) {
			throw new IllegalArgumentException("Bad URI: unregistered repository. " + uri);
		}
		if (match.entityName == null) { // points to actual commit/branch
			switch(match.type) {
			case REPOSITORY:
				return BASE_TYPE + "/repository";
			case COMMIT:
				return BASE_TYPE + "/commit";
			case LOCAL_BRANCH:
				return BASE_TYPE + "/branch.local";
			case REMOTE_BRANCH:
				return BASE_TYPE + "/branch.remote";
			case REMOTE:
				return BASE_TYPE + "/remote";
			}
		}
		return info.provider_.getType(uri);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		if (logger.isDebugEnabled())
			logger.debug("insert: " + uri);
		UriMatch match = EntityUriMatcher.getMatch(uri);
		RepositoryInfo info = repoInfos_.get(match.repositoryName);
		validateUri(uri, info, match);
		return info.provider_.insert(uri, values);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{
		if (logger.isDebugEnabled())
			logger.debug("query: " + uri);
		UriMatch match = EntityUriMatcher.getMatch(uri);
		RepositoryInfo info = repoInfos_.get(match.repositoryName);
		validateUri(uri, info, match);
		return info.provider_.query(uri, projection, selection, selectionArgs, sortOrder);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs)
	{
		if (logger.isDebugEnabled())
			logger.debug("update: " + uri);
		UriMatch match = EntityUriMatcher.getMatch(uri);
		RepositoryInfo info = repoInfos_.get(match.repositoryName);
		validateUri(uri, info, match);
		return info.provider_.update(uri, values, selection, selectionArgs);
	}
}
