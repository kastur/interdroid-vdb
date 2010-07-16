package interdroid.vdb.content.avro;

import org.apache.avro.Schema;

import interdroid.vdb.content.GenericContentProvider;
import interdroid.vdb.content.metadata.Metadata;

public class AvroContentProvider extends GenericContentProvider {

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
