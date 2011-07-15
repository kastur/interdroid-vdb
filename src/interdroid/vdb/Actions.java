package interdroid.vdb;

import android.content.Intent;

/**
 * This class holds constants for triggering actions in the vdb-ui package.
 * It has to live here due to the way the android tools structure Library
 * packages. We can't make vdb-ui a Library to import this class into
 * other UI packages that want to launch ui activities so it lives down
 * in this layer even though it really belongs in the vdb-ui system.
 *
 * @author nick
 *
 */
public final class Actions {
    /** Don't allow construction of this class. **/
    private Actions() { }

    /** Base for all actions. **/
    private static final String ACTION_BASE =
            "interdroid.vdb.action.";

    /** The Commit activity action. **/
    public static final String ACTION_COMMIT =
            ACTION_BASE + "COMMIT";

    /** Add a branch action. **/
    public static final String ACTION_ADD_BRANCH =
            ACTION_BASE + "ADD_BRANCH";

    /** Edit a remote hub action. **/
    public static final String ACTION_EDIT_REMOTE =
            ACTION_BASE + "EDIT_REMOTE";

    /** Add a peer action. **/
    public static final String ACTION_ADD_REMOTE =
            ACTION_BASE + "ADD_REMOTE";

    /** Manage local branches action. **/
    public static final String ACTION_MANAGE_LOCAL_BRANCHES =
            ACTION_BASE + "MANAGE_LOCAL_BRANCHES";

    /** Manage all remote repositories action. **/
    public static final String ACTION_MANAGE_REMOTES =
            ACTION_BASE + "MANAGE_REMOTES";

    /** Add a peer action. **/
    public static final String ACTION_ADD_PEER =
            ACTION_BASE + "ADD_PEER";

    /** Manage all peers action. **/
    public static final String ACTION_MANAGE_PEERS =
            ACTION_BASE + "MANAGE_PEERS";

    /** Manage the properties for a repository. **/
    public static final String ACTION_MANAGE_REPOSITORY_PROPERTIES =
            ACTION_BASE + "MANAGE_REPOSITORY_PROPERTIES";

    /** Manage a particular repository action. **/
    public static final String ACTION_MANAGE_REPOSITORY =
            ACTION_BASE + "MANAGE_REPOSITORY";

    /** Manage all repositories action. **/
    public static final String ACTION_MANAGE_REPOSITORIES =
            ACTION_BASE + "MANAGE_REPOSITORIES";

    /** Manage peer information action. **/
	public static final String ACTION_MANAGE_PEER_INFO =
			ACTION_BASE + "MANAGE_PEER_INFO";

	/** Manage local sharing action. **/
	public static final String ACTION_MANAGE_LOCAL_SHARING =
			ACTION_BASE + "MANAGE_LOCAL_SHARING";

	/** Manage remote sharing action. **/
	public static final String ACTION_MANAGE_REMOTE_SHARING =
			ACTION_BASE + "MANAGE_REMOTE_SHARING";

	/** The git service. **/
	public static final String GIT_SERVICE = "interdroid.vdb.GIT_SERVICE";

}
