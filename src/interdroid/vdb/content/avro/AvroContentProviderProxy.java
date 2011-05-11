package interdroid.vdb.content.avro;

import interdroid.vdb.content.ContentChangeHandler;
import interdroid.vdb.content.EntityUriMatcher;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;
import interdroid.vdb.content.VdbMainContentProvider;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class AvroContentProviderProxy extends ContentProvider {

	private static final Logger logger = LoggerFactory.getLogger(AvroContentProviderProxy.class);

	// TODO: Need to proxy content change delete update etc notifications.
	// TODO: Need to check for installation of vdb-ui and make sure we are all set.

	protected final Schema schema_;

	public AvroContentProviderProxy(String schema) {
		this(Schema.parse(schema));
	}

	public AvroContentProviderProxy(Schema schema) {
		schema_ = schema;
	}

	private Uri remapUri(Uri uri) {
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(uri.getScheme());
		builder.authority(VdbMainContentProvider.AUTHORITY);
		builder.path(uri.getAuthority() + uri.getPath());
		builder.query(uri.getQuery());
		Uri built = builder.build();
		logger.debug("remapped: {} to {}", uri, built);
		return built;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return getContext().getContentResolver().delete(remapUri(uri), selection, selectionArgs);
	}

	@Override
	public String getType(Uri uri) {
		return getContext().getContentResolver().getType(remapUri(uri));
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final UriMatch result = EntityUriMatcher.getMatch(uri);
		ContentChangeHandler handler = ContentChangeHandler.getHandler(result.entityName);
		if (handler != null) {
			handler.preInsertHook(values);
		}
		Context context = getContext();
		ContentResolver resolver = context.getContentResolver();
		Uri mappedUri = remapUri(uri);
		logger.debug("Inserting into:" + mappedUri + " values: " + values);

		return resolver.insert(mappedUri, values);
	}

	@Override
	public boolean onCreate() {
		// Make sure we are registered.
		logger.debug("onCreate");
		logger.debug("Registering schema: {}", schema_.getName());
		AvroProviderRegistry.registerSchema(getContext(), schema_);
		logger.debug("Schema registered.");
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		return getContext().getContentResolver().query(remapUri(uri), projection, selection, selectionArgs, sortOrder);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return getContext().getContentResolver().update(remapUri(uri), values, selection, selectionArgs);
	}

}
