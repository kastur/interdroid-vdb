package interdroid.vdb.content;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;

public class ContentChangeHandler {
	private static Map<String, ContentChangeHandler>handlers = new HashMap<String, ContentChangeHandler>();

	public static ContentChangeHandler getHandler(String namespace, String name) {
		return handlers.get(namespace + "." + name);
	}

	public void preInsertHook(ContentValues values) {
		// Intentionally Blank as we anticipate more methods which makes this an adapter
	}

	private static void register(String name,
			ContentChangeHandler contentChangeHandler) {
		handlers.put(name, contentChangeHandler);
	}

	public static void register(String namespace, String name,
			ContentChangeHandler contentChangeHandler) {
		register(namespace + "." + name, contentChangeHandler);
	}
}
