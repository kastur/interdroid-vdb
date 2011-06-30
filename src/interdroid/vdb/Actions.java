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
	private static final String ACTION_BASE = "interdroid.vdb.action.";
	public static final String ACTION_COMMIT = ACTION_BASE + "COMMIT";
	public static final String ACTION_ADD_BRANCH = ACTION_BASE + "ADD_BRANCH";
	public static final String ACTION_EDIT_REMOTE = ACTION_BASE + "EDIT_REMOTE";
	public static final String ACTION_ADD_REMOTE = ACTION_BASE + "ADD_REMOTE";
	public static String ACTION_MANAGE_LOCAL_BRANCHES = ACTION_BASE + "MANAGE_LOCAL_BRANCHES";
	public static final String ACTION_MANAGE_REMOTES = ACTION_BASE + "MANAGE_REMOTES";
	public static final String ACTION_ADD_PEER = ACTION_BASE + "ADD_PEER";
	public static final String ACTION_MANAGE_PEERS = ACTION_BASE + "MANAGE_PEERS";
	public static String ACTION_MANAGE_REPOSITORY = ACTION_BASE + "MANAGE_REPOSITORY";
	public static String ACTION_MANAGE_REPOSITORIES = ACTION_BASE + "MANAGE_REPOSITORIES";

}
