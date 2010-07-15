package interdroid.vdb.content.avro;

import interdroid.vdb.content.GenericContentProvider;
import interdroid.vdb.content.metadata.Metadata;

public class AvroContentProvider extends GenericContentProvider {

    public AvroContentProvider(String name, String schema) {
    	super(name, makeMetadata(schema));
    }

	private static Metadata makeMetadata(String schema) {
		return new AvroMetadata(schema);
	}

}
