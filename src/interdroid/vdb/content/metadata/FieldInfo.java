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
package interdroid.vdb.content.metadata;

/**
 * The information for a field in a table.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public abstract class FieldInfo {
    /**
     * The name of the field.
     */
    public final String fieldName;

    /**
     * The type of this field in the database.
     */
    public final DatabaseFieldType dbType;

    /**
     * True if this field is part of the key in the table.
     */
    public final boolean isKey;

    /**
     * The entity this field targets if it is a relation field.
     */
    public EntityInfo targetEntity;

    /**
     * The field this field targets if it is a relation field.
     */
    public FieldInfo targetField;

    /**
     * Construct a FieldInfo.
     * @param fieldName the name of the field
     * @param dbType the type of the field
     * @param isKey true if this is a key field.
     */
    protected FieldInfo(final String fieldName,
            final DatabaseFieldType dbType, final boolean isKey) {
        this.dbType = dbType;
        this.fieldName = fieldName;
        this.isKey = isKey;
    }

    /**
     * @return the name of the database type
     */
    public final String dbTypeName() {
        return dbType.name();
    }

}
