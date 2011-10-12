package interdroid.vdb.content.avro;

import java.io.IOException;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.pm.ProviderInfo;

import interdroid.vdb.content.GenericContentProvider;
import interdroid.vdb.content.metadata.Metadata;
import interdroid.vdb.persistence.api.VdbInitializer;

public class AvroContentProvider extends GenericContentProvider {
	private static final Logger logger = LoggerFactory.getLogger(AvroContentProvider.class);

	private final Schema schema_;

	public static final String VALUE_COLUMN_NAME = GenericContentProvider.SEPARATOR + "value";
	public static final String KEY_COLUMN_NAME = GenericContentProvider.SEPARATOR + "key";
	public static final String TYPE_COLUMN_NAME = GenericContentProvider.SEPARATOR + "type";
	public static final String ID_COLUMN_NAME = GenericContentProvider.SEPARATOR + "id";
	public static final String ARRAY_TABLE_INFIX = GenericContentProvider.SEPARATOR;
	public static final String MAP_TABLE_INFIX = GenericContentProvider.SEPARATOR;
	public static final String TYPE_NAME_COLUMN_NAME = TYPE_COLUMN_NAME + GenericContentProvider.SEPARATOR + "name";
	public static final String TYPE_URI_COLUMN_NAME = TYPE_COLUMN_NAME + GenericContentProvider.SEPARATOR + "uri";

	public AvroContentProvider(Schema schema) {
		super(schema.getNamespace(), makeMetadata(schema));
		schema_ = schema;
	}

	private static Metadata makeMetadata(Schema schema) {
		return new AvroMetadata(schema);
	}

	public AvroContentProvider(String schema) {
		this(Schema.parse(schema));
	}

	public VdbInitializer buildInitializer() {
		logger.debug("Building initializer.");
		return new DatabaseInitializer(namespace_, metadata_, schema_.toString());
	}

	@Override
	public void attachInfo(Context context, ProviderInfo info) {
		super.attachInfo(context, info);

		// Make sure we are registered.
		logger.debug("attachInfo");
		logger.debug("Registering schema: {}", schema_.getName());
		try {
			AvroSchemaRegistrationHandler.registerSchema(context, schema_);
		} catch (IOException e) {
			logger.error("Unexpected exception registering", e);
		}
		logger.debug("Schema registered.");
	}
}
