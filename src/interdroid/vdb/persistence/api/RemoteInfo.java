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
	public static String SECTION = "remote";
	private static String KEY_DESCRIPTION = "vdb-description";
	private static String KEY_OUR_NAME_ON_REMOTE = "vdb-local-name-on-remote";
	private static String KEY_TYPE = "vdb-type";

	public enum RemoteType {
		MERGE_POINT,
		HUB;
	}
	
	private RemoteType type_;
	private String name_;
	private String description_;
	private URIish remoteUri_;
	
	/* only valid when type_ == RemoteType.HUB */
	private String ourNameOnRemote_;
	
	public RemoteInfo() {
		type_ = RemoteType.MERGE_POINT;
	}
	public RemoteInfo(String name) {
		name_ = name;
		type_ = RemoteType.MERGE_POINT;
	}
		
	public void load(Config rc) throws URISyntaxException
	{
		try {
			setType(RemoteType.valueOf(rc.getString(SECTION, name_, KEY_TYPE)));
		} catch(IllegalArgumentException e) {
			setType(RemoteType.HUB);
		} catch(NullPointerException e) {
			setType(RemoteType.HUB);
		}
		
		RemoteConfig remoteCfg = new RemoteConfig(rc, name_);
		List<URIish> allURIs = remoteCfg.getURIs();
		setRemoteUri((allURIs.size() > 0) ? allURIs.get(0) : null);	
		setDescription(rc.getString(SECTION, name_, KEY_DESCRIPTION));
		
		switch(type_) {
		case HUB:
			setOurNameOnRemote(rc.getString(SECTION, name_, KEY_OUR_NAME_ON_REMOTE));
			break;
		case MERGE_POINT:
		}
	}
	
	public void save(Config rc) throws URISyntaxException
	{
		rc.setString(SECTION, name_, KEY_TYPE, type_.name());
		rc.setString(SECTION, name_, KEY_DESCRIPTION, description_);
		
		RemoteConfig remoteCfg = new RemoteConfig(rc, name_);
		switch(type_) {
		case HUB:
			// From hubs we may fetch all the remote references in refs/remotes
			// TODO(emilian): test that the wildcard works for multiple directories
			remoteCfg.setFetchRefSpecs(new ArrayList<RefSpec>());
			remoteCfg.addFetchRefSpec(new RefSpec()
					.setSourceDestination("refs/remotes/*", "refs/remotes/*"));
			
			// And we push our local branches to the refs/remotes/ourname 
			remoteCfg.setPushRefSpecs(new ArrayList<RefSpec>());
			if (ourNameOnRemote_ != null) {
				String pushPath = "refs/remotes/" + ourNameOnRemote_ + "/*";
				remoteCfg.addPushRefSpec(new RefSpec()
						.setSourceDestination("refs/heads/*",  pushPath));
			}
			break;
		case MERGE_POINT:
			// We fetch merge points into refs/remotes/remote-name 
			remoteCfg.setFetchRefSpecs(new ArrayList<RefSpec>());
			String fetchPath = "refs/remotes/" + name_ +  "/*";
			remoteCfg.addFetchRefSpec(new RefSpec()
					.setSourceDestination("refs/heads/*", fetchPath));

			// We push directly into the heads of the merge-point remote
			remoteCfg.setPushRefSpecs(new ArrayList<RefSpec>());
			remoteCfg.addPushRefSpec(new RefSpec()
					.setSourceDestination("refs/heads/*", "refs/heads/*"));
		}		
		
		for (URIish uri : remoteCfg.getURIs()) {
			remoteCfg.removeURI(uri);
		}
		if (remoteUri_ != null) {
			remoteCfg.addURI(remoteUri_);
		}
		remoteCfg.update(rc);
		
		switch(type_) {
		case HUB:
			rc.setString(SECTION, name_, KEY_OUR_NAME_ON_REMOTE, ourNameOnRemote_);
			break;
		case MERGE_POINT:
			rc.unset(SECTION, name_, KEY_OUR_NAME_ON_REMOTE);
		}
	}
	
	@Override
	public RemoteInfo clone()
	{
		RemoteInfo copy = new RemoteInfo();
		copy.type_ = type_;
		copy.name_ = name_;
		copy.ourNameOnRemote_ = ourNameOnRemote_;
		copy.description_ = description_;
		copy.remoteUri_ = remoteUri_;
		return copy;
	}

	public void setType(RemoteType type)
	{
		type_ = type;
		if (type_ == null) {
			throw new IllegalArgumentException("Type must not be null.");
		}
		switch(type_) {
		case HUB:
			break;
		case MERGE_POINT:
			ourNameOnRemote_ = null;
			break;
		}
	}

	public RemoteType getType()
	{
		return type_;
	}

	public void setName(String name)
	{
		name_ = name;
	}

	public String getName()
	{
		return name_;
	}

	public void setDescription(String description)
	{
		description_ = description;
	}

	public String getDescription()
	{
		return description_;
	}

	public void setRemoteUri(URIish remoteUri)
	{
		remoteUri_ = remoteUri;
	}

	public URIish getRemoteUri()
	{
		return remoteUri_;
	}

	public void setOurNameOnRemote(String ourNameOnRemote)
	{
		if (type_ != RemoteType.HUB) {
			throw new IllegalStateException("ourNameOnRemote is only valid for hubs.");
		}
		ourNameOnRemote_ = ourNameOnRemote;
	}

	public String getOurNameOnRemote()
	{
		return ourNameOnRemote_;
	}
}
