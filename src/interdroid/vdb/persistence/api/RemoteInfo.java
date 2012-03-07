package interdroid.vdb.persistence.api;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * Container for all the information we care about a remote device.
 */
public class RemoteInfo {
    /**
     * The section this information is saved under in the preferences.
     */
    public static final String SECTION = "remote";
    /**
     * The key for the description.
     */
    private static final String KEY_DESCRIPTION = "vdb-description";
    /**
     * The key for the local name on the remote.
     */
    private static final String KEY_OUR_NAME_ON_REMOTE =
            "vdb-local-name-on-remote";
    /**
     * The key for the type of remote.
     */
    private static final String KEY_TYPE = "vdb-type";

    /**
     * The types of remotes we suport.
     *
     * @author nick &lt;palmer@cs.vu.nl&gt;
     *
     */
    public enum RemoteType {
        /** A merge point remote. */
        MERGE_POINT,
        /** A hub remote. */
        HUB;
    }

    /**
     * The type of remote this represents.
     */
    private RemoteType mType;
    /**
     * The name of the remote.
     */
    private String mName;
    /**
     * The description of the remote.
     */
    private String mDescription;
    /**
     * The URI for the remote.
     */
    private URIish mRemoteUri;
    /**
     * Our name on the remote when in HUB mode.
     */
    private String mOurNameOnRemote;

    /**
     * Construct a new remote info for a merge point.
     */
    public RemoteInfo() {
        mType = RemoteType.MERGE_POINT;
    }
    /**
     * Construct a new remote info for a merge point
     * with the given name.
     * @param name the name of the remote.
     */
    public RemoteInfo(final String name) {
        mName = name;
        mType = RemoteType.MERGE_POINT;
    }

    /**
     * Load a remote from a configuration.
     * @param rc the configuration to load from
     * @throws URISyntaxException if the Remote URI is invalid
     */
    public final void load(final Config rc) throws URISyntaxException {
        try {
            setType(RemoteType.valueOf(rc.getString(SECTION, mName, KEY_TYPE)));
        } catch (IllegalArgumentException e) {
            setType(RemoteType.HUB);
        } catch (NullPointerException e) {
            setType(RemoteType.HUB);
        }

        RemoteConfig remoteCfg = new RemoteConfig(rc, mName);
        List<URIish> allURIs = remoteCfg.getURIs();
        if (allURIs.size() > 0) {
            setRemoteUri(allURIs.get(0));
        } else {
            setRemoteUri(null);
        }
        setDescription(rc.getString(SECTION, mName, KEY_DESCRIPTION));

        switch (mType) {
        case HUB:
            setOurNameOnRemote(rc.getString(SECTION,
                    mName, KEY_OUR_NAME_ON_REMOTE));
            break;
        default:
        case MERGE_POINT:
        }
    }

    /**
     * Save the information to the configuration.
     * @param rc the config to save to
     * @throws URISyntaxException if the URI syntax is invalid
     */
    public final void save(final Config rc) throws URISyntaxException {
        rc.setString(SECTION, mName, KEY_TYPE, mType.name());
        rc.setString(SECTION, mName, KEY_DESCRIPTION, mDescription);

        RemoteConfig remoteCfg = new RemoteConfig(rc, mName);
        switch (mType) {
        case HUB:
            // From hubs we may fetch all the remote references in refs/remotes
            // TODO: test that the wildcard works for multiple directories
            remoteCfg.setFetchRefSpecs(new ArrayList<RefSpec>());
            remoteCfg.addFetchRefSpec(new RefSpec()
                    .setSourceDestination("refs/remotes/*", "refs/remotes/*"));

            // And we push our local branches to the refs/remotes/ourname
            remoteCfg.setPushRefSpecs(new ArrayList<RefSpec>());
            if (mOurNameOnRemote != null) {
                String pushPath = "refs/remotes/" + mOurNameOnRemote + "/*";
                remoteCfg.addPushRefSpec(new RefSpec()
                        .setSourceDestination("refs/heads/*",  pushPath));
            }
            break;
        case MERGE_POINT:
            // We fetch merge points into refs/remotes/remote-name
            remoteCfg.setFetchRefSpecs(new ArrayList<RefSpec>());
            String fetchPath = "refs/remotes/" + mName +  "/*";
            remoteCfg.addFetchRefSpec(new RefSpec()
                    .setSourceDestination("refs/heads/*", fetchPath));

            // We push directly into the heads of the merge-point remote
            remoteCfg.setPushRefSpecs(new ArrayList<RefSpec>());
            remoteCfg.addPushRefSpec(new RefSpec()
                    .setSourceDestination("refs/heads/*", "refs/heads/*"));
        default:
        }

        for (URIish uri : remoteCfg.getURIs()) {
            remoteCfg.removeURI(uri);
        }
        if (mRemoteUri != null) {
            remoteCfg.addURI(mRemoteUri);
        }
        remoteCfg.update(rc);

        switch (mType) {
        case HUB:
            rc.setString(SECTION, mName, KEY_OUR_NAME_ON_REMOTE,
                    mOurNameOnRemote);
            break;
        case MERGE_POINT:
            rc.unset(SECTION, mName, KEY_OUR_NAME_ON_REMOTE);
        default:
        }
    }

    @Override
    public final RemoteInfo clone() {
        RemoteInfo copy = new RemoteInfo();
        copy.mType = mType;
        copy.mName = mName;
        copy.mOurNameOnRemote = mOurNameOnRemote;
        copy.mDescription = mDescription;
        copy.mRemoteUri = mRemoteUri;
        return copy;
    }

    /**
     * Sets the type for this remote.
     * @param type the type to set it to
     */
    public final void setType(final RemoteType type) {
        mType = type;
        if (mType == null) {
            throw new IllegalArgumentException("Type must not be null.");
        }
        switch (mType) {
        default:
        case HUB:
            break;
        case MERGE_POINT:
            mOurNameOnRemote = null;
            break;
        }
    }

    /**
     * @return the type for this remote
     */
    public final RemoteType getType() {
        return mType;
    }

    /**
     * Sets the name for this remote.
     * @param name the name for this remote
     */
    public final void setName(final String name) {
        mName = name;
    }

    /**
     * @return the name for this remote
     */
    public final String getName() {
        return mName;
    }

    /**
     * Sets the description for this remote.
     * @param description the description
     */
    public final void setDescription(final String description) {
        mDescription = description;
    }

    /**
     * @return the description for this remote
     */
    public final String getDescription() {
        return mDescription;
    }

    /**
     * Set the URI for this remote.
     * @param remoteUri the remote uri
     */
    public final void setRemoteUri(final URIish remoteUri) {
        mRemoteUri = remoteUri;
    }

    /**
     * @return the remote uri
     */
    public final URIish getRemoteUri() {
        return mRemoteUri;
    }

    /**
     * Set our name on the remote. This can only be done for remotes
     * which are in HUB mode.
     * @param ourNameOnRemote the name to set
     */
    public final void setOurNameOnRemote(final String ourNameOnRemote) {
        if (mType != RemoteType.HUB) {
            throw new IllegalArgumentException(
                    "ourNameOnRemote is only valid for hubs.");
        }
        mOurNameOnRemote = ourNameOnRemote;
    }

    /**
     * @return our name on the remote or null.
     */
    public final String getOurNameOnRemote() {
        return mOurNameOnRemote;
    }
}
