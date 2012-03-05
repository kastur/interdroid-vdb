package interdroid.vdb.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.CursorWrapper;

public class CrossProcessCursorWrapper extends CursorWrapper
	implements CrossProcessCursor {
	private static final Logger logger = LoggerFactory.getLogger(CrossProcessCursorWrapper.class);


	public CrossProcessCursorWrapper(Cursor cursor) {
		super(cursor);
		logger.debug("Built cross process cursor.");
	}

	@Override
	public CursorWindow getWindow() {
		return null;
	}

	@Override
	public void fillWindow(int position, CursorWindow window) {
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
			logger.error("Exception with wrapped cursor", e);
		} finally {
			window.releaseReference();
		}
	}

	@Override
	public boolean onMove(int oldPosition, int newPosition) {
		return true;
	}
}