package interdroid.vdb.content;

import interdroid.vdb.content.EntityUriMatcher.UriMatch;
import interdroid.vdb.content.metadata.EntityInfo;
import interdroid.vdb.content.metadata.FieldInfo;
import interdroid.vdb.content.metadata.Metadata;
import interdroid.vdb.persistence.api.VdbCheckout;
import interdroid.vdb.persistence.api.VdbInitializer;
import interdroid.vdb.persistence.api.VdbInitializerFactory;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.jgit.lib.Constants;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class GenericContentProvider extends ContentProvider implements VdbInitializerFactory {
    private final Metadata metadata_;
    private final String name_;
	private VdbRepository vdbRepo_;
		
	public class DatabaseInitializer implements VdbInitializer {
	    @Override
		public void onCreate(SQLiteDatabase db)
	    {	    	
	    	// TODO(emilian): sql encoding
	    	for (EntityInfo entity : metadata_.getEntities()) {
	    		boolean firstField = true;
	    		db.execSQL("DROP TABLE IF EXISTS " + entity.name());
	    		
	    		StringBuilder createSql = new StringBuilder("CREATE TABLE ");
	    		createSql.append(entity.name());
	    		createSql.append("(");
	    		for (FieldInfo field : entity.fields.values()) {
	    			if (!firstField) {
	    				createSql.append(",\n");
	    			} else {
	    				firstField = false;
	    			}
	    			createSql.append(field.fieldName);
	    			createSql.append(" ");
	    			createSql.append(field.dbType.name());
	    			if (field.isID) {
	    				createSql.append(" PRIMARY KEY");
	    			}
	    		}
	    		createSql.append(")");
	    		db.execSQL(createSql.toString());
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

	public GenericContentProvider(String name, Class<?>... schemaClasses)
	{
		name_ = name;
		metadata_ = new Metadata(schemaClasses);
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
		final EntityInfo entityInfo = metadata_.getEntity(result.entityName);
		VdbCheckout vdbBranch = getCheckoutFor(uri, result);

        ContentValues values;
        if (userValues != null) {
            values = new ContentValues(userValues);
        } else {
            values = new ContentValues();
        }
        
        try {
	        // TODO(emilian): make this cleaner
	        Method m = entityInfo.clazz.getMethod("preInsertHook", ContentValues.class);
	        if (m != null) {
	        	m.invoke(null, values);
	        }
        } catch(NoSuchMethodException e) {
        	// ignore .. user has no hook
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
        
        SQLiteDatabase db;
		try {
			db = vdbBranch.getReadWriteDatabase();
		} catch (IOException e) {
			throw new RuntimeException("getReadWriteDatabase failed", e);
		}

		try {
			long rowId = db.insert(entityInfo.name(), entityInfo.idField.fieldName, values);
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
		final EntityInfo entityInfo = metadata_.getEntity(result.entityName);
		VdbCheckout vdbBranch = getCheckoutFor(uri, result);
		
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        if (result.entityIdentifier != null) {
			qb.setTables(entityInfo.name());
			// qb.setProjectionMap(sNotesProjectionMap);
			qb.appendWhere(entityInfo.idField.fieldName + "=" + result.entityIdentifier);
        } else {
        	qb.setTables(entityInfo.name());
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
	            count = db.update(entityInfo.name(), values, 
	            		entityInfo.idField.fieldName + "=" + result.entityIdentifier
	                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
	        } else {
	        	count = db.update(entityInfo.name(), values, where, whereArgs);
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
	            count = db.delete(entityInfo.name(), 
	            		entityInfo.idField.fieldName + "=" + result.entityIdentifier
	                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
	        } else {
	        	count = db.delete(entityInfo.name(), where, whereArgs);
	        }

	        getContext().getContentResolver().notifyChange(uri, null);
	        return count;
		} finally {
			vdbBranch.releaseDatabase();
		}
	}

}
