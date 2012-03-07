package interdroid.vdb.content.avro;


import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;

import interdroid.vdb.content.metadata.Metadata;

/**
 * Represents metadata for an Avro schema.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class AvroMetadata extends Metadata {

    /**
     * The schema we are representing.
     */
    private Schema mSchema;

    /**
     * Construct from a schema.
     * @param schema the schema to represent
     */
    public AvroMetadata(final Schema schema) {
        super(schema.getNamespace());
        mSchema = schema;
        parseSchema();
    }

    /**
     * Construct from a string with a schema.
     * @param schema the schema to parse and represent
     */
    public AvroMetadata(final String schema) {
        this(Schema.parse(schema));
    }

    /**
     * Parses a schema constructing entities to represent the schema.
     */
    private void parseSchema() {
        if (mSchema.getType() != Type.RECORD) {
            throw new RuntimeException("Root entity must be a record.");
        }
        new AvroEntityInfo(mSchema, this);
    }

}
