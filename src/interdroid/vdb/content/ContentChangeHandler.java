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
