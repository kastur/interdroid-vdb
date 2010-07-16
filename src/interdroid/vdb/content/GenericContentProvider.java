package interdroid.vdb.content;

import java.io.IOException;
import java.util.ArrayList;

import interdroid.vdb.content.EntityUriMatcher.UriMatch;
import interdroid.vdb.content.metadata.DatabaseFieldType;
import interdroid.vdb.content.metadata.EntityInfo;
import interdroid.vdb.content.metadata.FieldInfo;
import interdroid.vdb.content.metadata.Metadata;
import interdroid.vdb.persistence.api.VdbCheckout;
import interdroid.vdb.persistence.api.VdbInitializer;
import interdroid.vdb.persistence.api.VdbInitializerFactory;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public abstract class GenericContentProvider extends ContentProvider implements VdbInitializerFactory {
	private static final String TAG = "VDB";
	protected final Metadata metadata_;
	protected final String name_;
	protected VdbRepository vdbRepo_;

	// TODO: (nick) Support for multiple key tables?

	private String escapeName(EntityInfo entity) {
		// Don't include the namespace for entities that match our name for simplicity
		if (name_.equals(entity.namespace())) {
			return DatabaseUtils.sqlEscapeString(entity.name().replace('.', '_'));
		} else {
			// But entities in other namespaces we use the full namespace. This shouldn't happen often.
			return DatabaseUtils.sqlEscapeString(entity.namespace().replace('.', '_') + "_" + entity.name().replace('.', '_'));
		}
	}

	public class DatabaseInitializer implements VdbInitializer {
		@Override
		public void onCreate(SQLiteDatabase db)
		{
			Log.d(TAG, "Initializing database for: " + name_);
			for (EntityInfo entity : metadata_.getEntities()) {
				// Only handle root entities.
				// Children get recursed so foreign key constraints all point up
				if (entity.parentEntity == null) {
					buildTables(db, entity);
				}
			}
		}

		private void buildTables(SQLiteDatabase db, EntityInfo entity) {
			boolean firstField = true;
			ArrayList<EntityInfo>children = new ArrayList<EntityInfo>();

			Log.d(TAG, "Creating table for: " + entity.namespace() + " : " + entity.name() + ":" + escapeName(entity));
			db.execSQL("DROP TABLE IF EXISTS " + escapeName(entity));

			StringBuilder createSql = new StringBuilder("CREATE TABLE ");
			createSql.append(escapeName(entity));
			createSql.append("(");
			for (FieldInfo field : entity.getFields()) {
				switch (field.dbType) {
				case ONE_TO_MANY_INT:
				case ONE_TO_MANY_STRING:
					// Skip these since they are handled by putting the key for this one in the targetEntity
					// but queue the child to be handled when we are done with this table.
					children.add(field.targetEntity);
					break;
				case ONE_TO_ONE:
					// First we need to build the child table so we can do the foreign key on this one
					buildTables(db, field.targetEntity);

					if (!firstField) {
						createSql.append(",\n");
					} else {
						firstField = false;
					}
					createSql.append(DatabaseUtils.sqlEscapeString(field.fieldName));
					createSql.append(" ");
					createSql.append(DatabaseFieldType.INTEGER);
					createSql.append(" REFERENCES ");
					createSql.append(escapeName(field.targetEntity));
					createSql.append("(");
					createSql.append(field.targetField.fieldName);
					createSql.append(")");
					createSql.append(" DEFERRABLE");
					break;
				default:
					if (!firstField) {
						createSql.append(",\n");
					} else {
						firstField = false;
					}
					createSql.append(DatabaseUtils.sqlEscapeString(field.fieldName));
					createSql.append(" ");
					createSql.append(field.dbTypeName());
					if (field.targetEntity != null) {
						createSql.append(" REFERENCES ");
						createSql.append(escapeName(field.targetEntity));
						createSql.append("(");
						createSql.append(field.targetField.fieldName);
						createSql.append(") DEFERRABLE");
					}
					break;
				}
			}

			// Now add the primary key constraint
			createSql.append(", ");
			createSql.append(" PRIMARY KEY (");
			firstField = true;
			for(FieldInfo field : entity.key) {
				if (!firstField) {
					createSql.append(", ");
				} else {
					firstField = false;
				}
				createSql.append(field.fieldName);
			}
			createSql.append(")");

			// Close the table
			createSql.append(")");

			Log.d(TAG, "Creating: " + createSql.toString());
			db.execSQL(createSql.toString());

			// Now process any remaining children
			for (EntityInfo child : children) {
				buildTables(db, child);
			}

			// Now fill in any enumeration values
			if (entity.enumValues != null) {
				ContentValues values = new ContentValues();
				for (Integer ordinal : entity.enumValues.keySet()) {
					String value = entity.enumValues.get(ordinal);
					values.clear();
					values.put("_id", ordinal);
					values.put("_value", value);
					db.insert(escapeName(entity), "_id", values);
				}
			}
		}
	}

	public VdbInitializer buildInitializer() {
		return new DatabaseInitializer();
	}

	@Override
	public boolean onCreate() {
		vdbRepo_ = VdbRepositoryRegistry.getInstance().getRepository(name_);
		return true;
	}

	public GenericContentProvider(String name, Metadata metadata)
	{
		name_ = name;
		metadata_ = metadata;
	}

	@Override
	public String getType(Uri uri)
	{
		final UriMatch result = EntityUriMatcher.getMatch(uri);
		final EntityInfo info = metadata_.getEntity(result.entityName);
		return result.entityIdentifier != null ? info.itemContentType()
				: info.contentType();
	}

	private VdbCheckout getCheckoutFor(Uri uri, UriMatch result) {
		try {
			switch(result.type) {
			case LOCAL_BRANCH:
				return vdbRepo_.getBranch(result.reference);
			case COMMIT:
				return vdbRepo_.getCommit(result.reference);
			case REMOTE_BRANCH:
				return vdbRepo_.getRemoteBranch(result.reference);
			default:
				throw new RuntimeException("Unsupported uri type " + uri);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues userValues)
	{
		final UriMatch result = EntityUriMatcher.getMatch(uri);
		if (result.entityIdentifier != null) { /* don't accept ID queries */
			throw new IllegalArgumentException("Invalid item URI " + uri);
		}
		Log.d(TAG, "Getting entity: " + result.entityName);
		final EntityInfo entityInfo = metadata_.getEntity(result.entityName);
		if (entityInfo == null) {
			throw new RuntimeException("Unable to find entity for: " + result.entityName);
		}
		Log.d(TAG, "Got info: " + entityInfo.name());
		Log.d(TAG, "Getting checkout for: " + uri);
		VdbCheckout vdbBranch = getCheckoutFor(uri, result);

		ContentValues values;
		if (userValues != null) {
			values = new ContentValues(userValues);
		} else {
			values = new ContentValues();
		}

		// Propogate the change to the preInsertHook if there is one
		ContentChangeHandler handler = ContentChangeHandler.getHandler(entityInfo.name());
		if (handler != null) {
			handler.preInsertHook(values);
		}

		SQLiteDatabase db;
		try {
			db = vdbBranch.getReadWriteDatabase();
		} catch (IOException e) {
			throw new RuntimeException("getReadWriteDatabase failed", e);
		}

		try {
			Log.d(TAG, "Inserting: " + entityInfo.name() + " : " + entityInfo.key.get(0).fieldName + ":" + values.getAsString(entityInfo.key.get(0).fieldName) + " : " + values.size());
			long rowId = db.insert(escapeName(entityInfo), entityInfo.key.get(0).fieldName, values);
			if (rowId > 0) {
				Uri noteUri = ContentUris.withAppendedId(uri, rowId);
				getContext().getContentResolver().notifyChange(noteUri, null);
				return noteUri;
			}

			throw new SQLException("Failed to insert row into " + uri);
		} finally {
			vdbBranch.releaseDatabase();
		}

	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder)
	{
		// Validate the requested uri
		final UriMatch result = EntityUriMatcher.getMatch(uri);
		Log.d(TAG, "Query for: " + result.entityName + " " + getClass().getCanonicalName());
		final EntityInfo entityInfo = metadata_.getEntity(result.entityName);
		if (entityInfo == null) {
			throw new RuntimeException("Unable to find entity for: " + result.entityName);
		}
		VdbCheckout vdbBranch = getCheckoutFor(uri, result);

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		if (result.entityIdentifier != null) {
			qb.setTables(escapeName(entityInfo));
			// qb.setProjectionMap(sNotesProjectionMap);
			qb.appendWhere(entityInfo.key.get(0).fieldName + "=" + result.entityIdentifier);
		} else {
			qb.setTables(escapeName(entityInfo));
		}

		// Get the database and run the query
		SQLiteDatabase db;
		try {
			db = vdbBranch.getReadOnlyDatabase();
		} catch (IOException e) {
			throw new RuntimeException("getReadOnlyDatabase failed", e);
		}

		// TODO(emilian): default sort order

		try {
			Log.d(TAG, "Querying with: " + qb.buildQuery(projection, selection, selectionArgs, null, null, sortOrder, null));
			Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

			// Tell the cursor what uri to watch, so it knows when its source data changes
			c.setNotificationUri(getContext().getContentResolver(), uri);
			return c;
		} finally {
			vdbBranch.releaseDatabase();
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs)
	{
		// Validate the requested uri
		final UriMatch result = EntityUriMatcher.getMatch(uri);
		final EntityInfo entityInfo = metadata_.getEntity(result.entityName);
		VdbCheckout vdbBranch = getCheckoutFor(uri, result);

		SQLiteDatabase db;
		try {
			db = vdbBranch.getReadWriteDatabase();
		} catch (IOException e) {
			throw new RuntimeException("getReadWriteDatabase failed", e);
		}

		int count = 0;
		try {
			if (result.entityIdentifier != null) {
				Log.d(TAG, "Updating: " + entityInfo.name() + " : " + values);
				count = db.update(escapeName(entityInfo), values,
						entityInfo.key.get(0).fieldName + "=" + result.entityIdentifier
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			} else {
				count = db.update(escapeName(entityInfo), values, where, whereArgs);
			}
		} finally {
			vdbBranch.releaseDatabase();
		}

		/*
		 TODO(emilian) implement auto commit support
        try {
        	vdbBranch.commitBranch();
        } catch(IOException e) {
        	Log.v(GenericContentProvider.class.getSimpleName(), e.toString());
        }
		 */

		getContext().getContentResolver().notifyChange(uri, null);
		return count;

	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs)
	{
		// Validate the requested uri
		final UriMatch result = EntityUriMatcher.getMatch(uri);
		final EntityInfo entityInfo = metadata_.getEntity(result.entityName);
		if (entityInfo == null) {
			throw new RuntimeException("Unable to find entity for: " + result.entityName);
		}
		VdbCheckout vdbBranch = getCheckoutFor(uri, result);

		SQLiteDatabase db;
		try {
			db = vdbBranch.getReadWriteDatabase();
		} catch (IOException e) {
			throw new RuntimeException("getReadWriteDatabase failed", e);
		}

		try {
			int count;
			if (result.entityIdentifier != null) {
				Log.d(TAG, "Deleting: " + entityInfo.name());

				count = db.delete(escapeName(entityInfo),
						entityInfo.key.get(0).fieldName + "=" + result.entityIdentifier
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			} else {
				count = db.delete(escapeName(entityInfo), where, whereArgs);
			}

			getContext().getContentResolver().notifyChange(uri, null);
			return count;
		} finally {
			vdbBranch.releaseDatabase();
		}
	}
}
