package interdroid.vdb.content;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;

public class ContentChangeHandler {
	private static Map<String, ContentChangeHandler>handlers = new HashMap<String, ContentChangeHandler>();

	public static ContentChangeHandler getHandler(String name) {
		return handlers.get(name);
	}

	public void preInsertHook(ContentValues values) {
		// Intentionally Blank as we anticipate more methods which makes this an adapter
	}

	public static void register(String name,
			ContentChangeHandler contentChangeHandler) {
		handlers.put(name, contentChangeHandler);
	}
}
