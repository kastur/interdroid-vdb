/**
 * This class holds constants for triggering actions in the vdb-ui package.
 * It has to live here due to the way the android tools structure Library
 * packages. We can't make vdb-ui a Library to import this class into
 * other UI packages that want to launch ui activities so it lives down
 * in this layer even though it really belongs in the vdb-ui system.
 */
package interdroid.vdb;

/**
 *
 * @author nick
 *
 */
public final class Actions {
	public static final String ACTION_COMMIT = "interdroid.vdb.action.COMMIT";
	public static final String ACTION_ADD_BRANCH = "interdroid.vdb.action.ADD_BRANCH";
	public static final String ACTION_EDIT_REMOTE = "interdroid.vdb.action.EDIT_REMOTE";
	public static final String ACTION_ADD_REMOTE = "interdroid.vdb.action.ADD_REMOTE";
	public static String ACTION_MANAGE_LOCAL_BRANCHES = "interdroid.vdb.action.MANAGE_LOCAL_BRANCHES";
	public static final String ACTION_MANAGE_REMOTES = "interdroid.vdb.action.MANAGE_REMOTES";
	public static String ACTION_MANAGE_REPOSITORY = "interdroid.vdb.action.MANAGE_REPOSITORY";
}
