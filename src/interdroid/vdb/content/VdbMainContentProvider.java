package interdroid.vdb.content;

import interdroid.vdb.Authority;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;

public class VdbMainContentProvider extends ContentProvider {
	private static final Logger logger = LoggerFactory.getLogger(VdbMainContentProvider.class);

	/**
	 * @deprecated Use {@link Authority#VDB} instead
	 */
	public static final String AUTHORITY = Authority.VDB;

	private VdbProviderRegistry registry_;

	@Override
	public boolean onCreate()
	{
		logger.debug("OnCreate called.");

		return true;
	}

	public void attachInfo(Context context, ProviderInfo info) {
		super.attachInfo(context, info);

		try {
			registry_ = new VdbProviderRegistry(context);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		if (logger.isDebugEnabled())
			logger.debug("delete: " + uri);
		ContentProvider provider = registry_.get(uri);
		return provider.delete(uri, selection, selectionArgs);
	}

	@Override
	public String getType(Uri uri)
	{
		if (logger.isDebugEnabled())
			logger.debug("getType : " + uri);
		return registry_.getType(uri);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		if (logger.isDebugEnabled())
			logger.debug("insert: " + uri);
		ContentProvider provider = registry_.get(uri);
		return provider.insert(uri, values);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{
		if (logger.isDebugEnabled())
			logger.debug("query: " + uri);
		ContentProvider provider = registry_.get(uri);
		return provider.query(uri, projection, selection, selectionArgs, sortOrder);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs)
	{
		if (logger.isDebugEnabled())
			logger.debug("update: " + uri);
		ContentProvider provider = registry_.get(uri);
		return provider.update(uri, values, selection, selectionArgs);
	}
}
