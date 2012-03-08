/*
 * Copyright (c) 2008-2012 Vrije Universiteit, The Netherlands All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the Vrije Universiteit nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package interdroid.vdb.content.avro;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;

import interdroid.vdb.content.metadata.DatabaseFieldType;
import interdroid.vdb.content.metadata.FieldInfo;

/**
 * A FieldInfo for fields from a parsed Avro schema.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class AvroFieldInfo extends FieldInfo {

    /**
     * The schema for the field.
     */
    private Schema mSchema;

    /**
     * @return the schema for this field.
     */
    public Schema getSchema() {
        return mSchema;
    }

    /**
     * Construct from an Avro Field.
     *
     * @param field the field to represent
     */
    public AvroFieldInfo(final Field field) {
        this(field, false);
    }

    /**
     * Construct from a field which is a key.
     *
     * @param field the field to represent
     * @param isKey true if this is a key field
     */
    protected AvroFieldInfo(final Field field, final boolean isKey) {
        super(field.name(), getFieldType(field.schema()), isKey);
        mSchema = field.schema();
    }

    /**
     * Construct from a schema for a field.
     * @param schema the schema for the field
     */
    protected AvroFieldInfo(final Schema schema) {
        super(schema.getName(), getFieldType(schema), false);
        mSchema = schema;
    }

    /**
     * Returns the field type for the given schema.
     * @param schema the schema the type is desired for
     * @return the type of database field used to represent this schema
     */
    private static DatabaseFieldType getFieldType(final Schema schema) {
        switch (schema.getType()) {
        case BYTES:
        case FIXED:
            // TODO: (nick) These should probably be handled using streams
            return DatabaseFieldType.BLOB;
        case DOUBLE:
        case FLOAT:
            return DatabaseFieldType.REAL_NUMBER;
        case INT:
        case LONG:
        case BOOLEAN:
            return DatabaseFieldType.INTEGER;
        case STRING:
            return DatabaseFieldType.TEXT;
        case ARRAY:
            return DatabaseFieldType.ONE_TO_MANY_INT;
        case RECORD:
        case ENUM:
            return DatabaseFieldType.ONE_TO_ONE;
        case MAP:
            return DatabaseFieldType.ONE_TO_MANY_STRING;
        case UNION:
            return DatabaseFieldType.BLOB;
        case NULL:
            return DatabaseFieldType.INTEGER;
        default:
            throw new RuntimeException("Unsupported Avro Field Type: "
                    + schema.toString());
        }
    }

}
