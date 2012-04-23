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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.CursorWrapper;

/**
 * A wrapper for cursors that are used across processes.
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class CrossProcessCursorWrapper extends CursorWrapper
	implements CrossProcessCursor {
	/**
	 * The logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(CrossProcessCursorWrapper.class);

	/**
	 * Construct a cross process cursor.
	 * @param cursor the cursor to wrap.
	 */
	public CrossProcessCursorWrapper(final Cursor cursor) {
		super(cursor);
		LOG.debug("Built cross process cursor.");
	}

	@Override
	public final CursorWindow getWindow() {
		return null;
	}

	@Override
	public final void fillWindow(final int position,
			final CursorWindow window) {
		if (position < 0 || position > getCount()) {
			return;
		}
		window.acquireReference();
		try {
			moveToPosition(position - 1);
			window.clear();
			window.setStartPosition(position);
			int columnNum = getColumnCount();
			window.setNumColumns(columnNum);
			while (moveToNext() && window.allocRow()) {
				for (int i = 0; i < columnNum; i++) {
					String field = getString(i);
					if (field != null) {
						if (!window.putString(field, getPosition(), i)) {
							window.freeLastRow();
							break;
						}
					} else {
						if (!window.putNull(getPosition(), i)) {
							window.freeLastRow();
							break;
						}
					}
				}
			}
		} catch (IllegalStateException e) {
			LOG.error("Exception with wrapped cursor", e);
		} finally {
			window.releaseReference();
		}
	}

	@Override
	public final boolean onMove(final int oldPosition, final int newPosition) {
		return true;
	}
}
