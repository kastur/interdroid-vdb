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
package interdroid.vdb.content;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;

/**
 * Content change handlers that can be used to hook and edit data before
 * various database operations.
 *
 * This class is both a static interface for registering with and an adapter.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class ContentChangeHandler {
	/**
	 * The registered handlers.
	 */
	private static Map<String, ContentChangeHandler>handlers =
			new HashMap<String, ContentChangeHandler>();

	/**
	 * Returns a handler for the given name in the given namespace.
	 * @param namespace the namespace to check in
	 * @param name the name to check for
	 * @return the content change handler
	 */
	public static ContentChangeHandler getHandler(final String namespace,
			final String name) {
		return handlers.get(namespace + "." + name);
	}

	/**
	 * The pre-insert hook to modify values (like adding defaults)
	 * before an insert is done.
	 * @param values the values to be inserted
	 */
	public void preInsertHook(final ContentValues values) {
		// Intentionally Blank as we anticipate more methods
		// which makes this an adapter
	}

	/**
	 * Register a content change handler for the given name.
	 * It is assumed the name includes a namespace.
	 * @param name the name to register with
	 * @param contentChangeHandler the content change handler
	 */
	private static void register(final String name,
			final ContentChangeHandler contentChangeHandler) {
		handlers.put(name, contentChangeHandler);
	}

	/**
	 * Register a content change handler for the given name in
	 * the given namespace.
	 * @param namespace the namespace to register in
	 * @param name the name to register
	 * @param contentChangeHandler the handler for this type
	 */
	public static void register(final String namespace, final String name,
			final ContentChangeHandler contentChangeHandler) {
		register(namespace + "." + name, contentChangeHandler);
	}
}
