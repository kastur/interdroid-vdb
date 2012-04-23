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

import interdroid.vdb.content.DatabaseInitializer;
import interdroid.vdb.content.GenericContentProvider;
import interdroid.vdb.persistence.api.VdbInitializer;

/**
 * This content provider supports ORM style decleration of a table.
 * Note that this system does not support related tables.
 *
 * This support could be added but given the power and flexibility of the
 * Avro based content providers it isn't a high priority.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class ORMGenericContentProvider extends GenericContentProvider {

	/**
	 * Construct an ORM content provider for the given information.
	 * @param namespace the namespace for the database.
	 * @param schemaClasses the class with annotations of bd info.
	 */
	public ORMGenericContentProvider(final String namespace,
			final Class<?> schemaClasses) {
		super(namespace, new ORMMetadata(namespace, schemaClasses));
	}

	@Override
	public final VdbInitializer buildInitializer() {
		return new DatabaseInitializer(mNamespace, mMetadata);
	}
}
