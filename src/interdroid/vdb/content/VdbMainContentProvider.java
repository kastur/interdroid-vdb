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

import interdroid.vdb.Authority;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;

/**
 * This content provider wrapps access to all other content providers
 * in the VDB system.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class VdbMainContentProvider extends ContentProvider {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(VdbMainContentProvider.class);

	/**
	 * @deprecated Use {@link Authority#VDB} instead
	 */
	public static final String AUTHORITY = Authority.VDB;

	/**
	 * The registry of content providers we know of.
	 */
	private VdbProviderRegistry mRegistry;

	@Override
	public final boolean onCreate() {
		LOG.debug("OnCreate called.");

		return true;
	}

	/**
	 * Called when the provider is attached to the context.
	 * @param context the context being attached to
	 * @param info the info on this provider.
	 */
	public final void attachInfo(final Context context,
			final ProviderInfo info) {
		super.attachInfo(context, info);

		try {
			mRegistry = new VdbProviderRegistry(context);
		} catch (IOException e) {
			LOG.error("Caught an exception while building registry", e);
			e.printStackTrace();
		}
	}

	@Override
	public final int delete(final Uri uri, final String selection,
			final String[] selectionArgs) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("delete: " + uri);
		}
		ContentProvider provider = mRegistry.get(uri);
		return provider.delete(uri, selection, selectionArgs);
	}

	@Override
	public final String getType(final Uri uri) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getType : " + uri);
		}
		return mRegistry.getType(uri);
	}

	@Override
	public final Uri insert(final Uri uri, final ContentValues values) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("insert: " + uri);
		}
		ContentProvider provider = mRegistry.get(uri);
		return provider.insert(uri, values);
	}

	@Override
	public final Cursor query(final Uri uri, final String[] projection,
			final String selection, final String[] selectionArgs,
			final String sortOrder) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("query: " + uri);
		}
		ContentProvider provider = mRegistry.get(uri);
		return provider.query(uri, projection,
				selection, selectionArgs, sortOrder);
	}

	@Override
	public final int update(final Uri uri, final ContentValues values,
			final String selection, final String[] selectionArgs) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("update: " + uri);
		}
		ContentProvider provider = mRegistry.get(uri);
		return provider.update(uri, values, selection, selectionArgs);
	}
}
