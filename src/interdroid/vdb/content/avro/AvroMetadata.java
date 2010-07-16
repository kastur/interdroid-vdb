package interdroid.vdb.content.avro;


import org.apache.avro.Schema;

import interdroid.vdb.content.metadata.Metadata;

public class AvroMetadata extends Metadata {

	private Schema schema_;

	public AvroMetadata(Schema schema) {
		super(schema.getNamespace());
		schema_ = schema;
		parseSchema();
	}

	public void setSchema(Schema schema) {
		schema_ = schema;
	}

	public AvroMetadata(String schema) {
		this(Schema.parse(schema));
	}

	private void parseSchema() {
		new AvroEntityInfo(schema_, this);
	}

}
