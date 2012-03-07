package interdroid.vdb.content.avro;

import java.io.IOException;

import interdroid.vdb.Authority;
import interdroid.vdb.content.ContentChangeHandler;
import interdroid.vdb.content.CrossProcessCursorWrapper;
import interdroid.vdb.content.EntityUriMatcher;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;

/**
 * Proxies for a remote content provider that is using an avro content provider.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public final class AvroContentProviderProxy extends ContentProvider {

    /**
     * Access to logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(AvroContentProviderProxy.class);

    // TODO: Need to proxy content change delete update etc notifications.
    // TODO: Need to check for installation of vdb-ui.

    /**
     * The schema for the provider.
     */
    private final Schema mSchema;

    /**
     * Constructs a proxy for a provider using the given schema.
     * @param schema the schema for the provider
     */
    public AvroContentProviderProxy(final String schema) {
        this(Schema.parse(schema));
    }

    /**
     * Constructs a proxy for a provider using the given schema.
     * @param schema the schema for the provider
     */
    public AvroContentProviderProxy(final Schema schema) {
        LOG.debug("Constructing provider proxy.");
        mSchema = schema;
    }

    /**
     * Remaps a URI from the native URI to the internal URI.
     * @param uri the uri to remap to an internal URI
     * @return the internal URI equivalent
     */
    private Uri remapUri(final Uri uri) {
        // TODO: This should be done with EntityURIBuilder no?
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(uri.getScheme());
        builder.authority(Authority.VDB);
        builder.path(uri.getAuthority() + uri.getPath());
        builder.query(uri.getQuery());
        Uri built = builder.build();
        LOG.debug("remapped: {} to {}", uri, built);
        return built;
    }

    @Override
    public int delete(final Uri uri, final String selection,
            final String[] selectionArgs) {
        return getContext().getContentResolver().delete(
                remapUri(uri), selection, selectionArgs);
    }

    @Override
    public String getType(final Uri uri) {
        return getContext().getContentResolver().getType(remapUri(uri));
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        final UriMatch result = EntityUriMatcher.getMatch(uri);
        ContentChangeHandler handler =
                ContentChangeHandler.getHandler(
                        result.authority, result.entityName);
        if (handler != null) {
            handler.preInsertHook(values);
        }
        Context context = getContext();
        ContentResolver resolver = context.getContentResolver();
        Uri mappedUri = remapUri(uri);
        LOG.debug("Inserting into:" + mappedUri + " values: " + values);

        return resolver.insert(mappedUri, values);
    }

    @Override
    public void attachInfo(final Context context, final ProviderInfo info) {
        super.attachInfo(context, info);

        // Make sure we are registered.
        LOG.debug("attachInfo");
        LOG.debug("Registering schema: {}", mSchema.getName());

        try {
            AvroSchemaRegistrationHandler.registerSchema(context, mSchema);
        } catch (IOException e) {
            LOG.error("Caught IOException while registering.", e);
        }

    }

    @Override
    public Cursor query(final Uri uri, final String[] projection,
            final String selection, final String[] selectionArgs,
            final String sortOrder) {
        return new CrossProcessCursorWrapper(getContext()
                .getContentResolver().query(
                        remapUri(uri), projection,
                        selection, selectionArgs, sortOrder));
    }

    @Override
    public int update(final Uri uri, final ContentValues values,
            final String selection, final String[] selectionArgs) {
        return getContext().getContentResolver().update(
                remapUri(uri), values, selection, selectionArgs);
    }

    @Override
    public boolean onCreate() {
        return false;
    }

}
