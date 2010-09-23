package interdroid.vdb.content.avro;

import org.apache.avro.Schema;

import interdroid.vdb.content.GenericContentProvider;
import interdroid.vdb.content.metadata.Metadata;

public class AvroContentProvider extends GenericContentProvider {

	public static final String VALUE_COLUMN_NAME = GenericContentProvider.SEPARATOR + "value";
	public static final String KEY_COLUMN_NAME = GenericContentProvider.SEPARATOR + "key";
	public static final String TYPE_COLUMN_NAME = GenericContentProvider.SEPARATOR + "type";
	public static final String ID_COLUMN_NAME = GenericContentProvider.SEPARATOR + "id";
	public static final String ARRAY_TABLE_INFIX = GenericContentProvider.SEPARATOR;
	public static final String MAP_TABLE_INFIX = GenericContentProvider.SEPARATOR;
	public static final String TYPE_NAME_COLUMN_NAME = TYPE_COLUMN_NAME + GenericContentProvider.SEPARATOR + "name";

	public AvroContentProvider(Schema schema) {
		super(schema.getNamespace(), makeMetadata(schema));
	}

    private static Metadata makeMetadata(Schema schema) {
    	return new AvroMetadata(schema);
	}

	public AvroContentProvider(String schema) {
    	this(Schema.parse(schema));
    }
}
