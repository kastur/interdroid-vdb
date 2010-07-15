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


import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class VdbMainContentProvider extends ContentProvider {
	public static final String AUTHORITY = VdbMainContentProvider.class.getName().toLowerCase();
	public static final String BASE_TYPE = "vnd." + VdbMainContentProvider.class.getPackage().getName().toLowerCase();

	private VdbConfig config_;
	private static final Map<String,RepositoryInfo> repoInfos_
			= new HashMap<String,RepositoryInfo>();

	private static final String TAG = "VdbMCP";

	private static class RepositoryInfo {
		public final ContentProvider provider_;
		public final VdbInitializer initializer_;
		public final String name_;

		public RepositoryInfo(RepositoryConf conf)
		{
			try {
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
		config_ = new VdbConfig(getContext());
		// Initialize all the child content providers, one for each repository.
		for (RepositoryConf repoConf : config_.getRepositories()) {
			RepositoryInfo repoInfo = new RepositoryInfo(repoConf);
			if (repoInfos_.containsKey(repoInfo.name_)) {
				throw new RuntimeException("Invalid configuration, duplicate repository name "
						+ repoInfo.name_);
			}
			try {
				Log.d(TAG, "Adding repository: " + repoInfo.name_);
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
		UriMatch match = EntityUriMatcher.getMatch(uri);
		RepositoryInfo info = repoInfos_.get(match.repositoryName);
		validateUri(uri, info, match);
		return info.provider_.delete(uri, selection, selectionArgs);
	}

	@Override
	public String getType(Uri uri)
	{
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
		UriMatch match = EntityUriMatcher.getMatch(uri);
		RepositoryInfo info = repoInfos_.get(match.repositoryName);
		validateUri(uri, info, match);
		return info.provider_.insert(uri, values);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{
		UriMatch match = EntityUriMatcher.getMatch(uri);
		RepositoryInfo info = repoInfos_.get(match.repositoryName);
		validateUri(uri, info, match);
		return info.provider_.query(uri, projection, selection, selectionArgs, sortOrder);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs)
	{
		UriMatch match = EntityUriMatcher.getMatch(uri);
		RepositoryInfo info = repoInfos_.get(match.repositoryName);
		validateUri(uri, info, match);
		return info.provider_.update(uri, values, selection, selectionArgs);
	}
}
