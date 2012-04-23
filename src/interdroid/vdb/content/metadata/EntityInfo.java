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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a table in the database.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public abstract class EntityInfo {

	/**
	 * The fields for this table as a map of name to field info.
	 */
	public final Map<String,FieldInfo> fields =
			new HashMap<String, FieldInfo>();
	/**
	 * The list of fields for this table.
	 */
	public final List<FieldInfo> key = new ArrayList<FieldInfo>();

	/**
	 * The values for enumerations.
	 */
	public Map<Integer, String> enumValues;

	/**
	 * The parent entity if any.
	 */
	public EntityInfo parentEntity;

	/**
	 * The children of this entity if any.
	 */
	public final List<EntityInfo> children = new ArrayList<EntityInfo>();

	/**
	 * @return the name of this entity.
	 */
	public abstract String name();

	/**
	 * @return the namespace for this entity.
	 */
	public abstract String namespace();

	/**
	 * @return the namespace followed by a dot or the empty string
	 */
	public final String namespaceDot() {
		if (namespace() == null || "".equals(namespace())) {
			return "";
		}
		return namespace() + ".";
	}

	/**
	 * @return the namespace dot name.
	 */
	public final String getFullName() {
		return namespaceDot() + name();
	}

	/**
	 * @return the content type for a list of this table.
	 */
	public abstract String contentType();

	/**
	 * @return the content type for an item in this table.
	 */
	public abstract String itemContentType();

	/**
	 * @return the fields in this table.
	 */
	public final Collection<FieldInfo> getFields() {
		return fields.values();
	}

}
