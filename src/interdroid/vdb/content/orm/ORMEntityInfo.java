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
package interdroid.vdb.content.orm;

import java.lang.reflect.Field;

import interdroid.vdb.content.metadata.EntityInfo;
import interdroid.vdb.content.metadata.FieldInfo;

/**
 * The information for an entity in the database using the ORM system.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class ORMEntityInfo extends EntityInfo {
    /**
     * The information on this entity.
     */
    private final DbEntity options;

    /**
     * The class for this entity.
     */
    private final Class<?> clazz;

    /**
     * @return the name for this entity
     */
    public final String name() {
        return options.name();
    }

    /**
     * @return the namespace for this entity
     */
    public final String namespace() {
        return clazz.getPackage().getName();
    }

    /**
     * @return the content type for this entity
     */
    public final String contentType() {
        return options.contentType();
    }

    /**
     * @return the item content type for this entity
     */
    public final String itemContentType() {
        return options.itemContentType();
    }

    /**
     * Construct entity information from an annotated class.
     * @param table the class to get table information from
     */
    public ORMEntityInfo(final Class<?> table) {
        this.clazz = table;

        DbEntity entityOptions = clazz.getAnnotation(DbEntity.class);
        if (entityOptions == null) {
            throw new IllegalArgumentException(
                    "The class is not annotated with EntityOptions.");
        }
        this.options = entityOptions;

        for (Field f : clazz.getFields()) {
            FieldInfo fieldInfo = ORMFieldInfo.buildInfo(f);
            if (fieldInfo != null) {
                fields.put(fieldInfo.fieldName, fieldInfo);
                if (fieldInfo.isKey) {
                    this.key.add(fieldInfo);
                }
            }
        }
        if (key.size() == 0) {
            throw new IllegalArgumentException(
                    "The class did not specify an id field.");
        }
    }
}

