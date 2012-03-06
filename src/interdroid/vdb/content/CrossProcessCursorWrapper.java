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
