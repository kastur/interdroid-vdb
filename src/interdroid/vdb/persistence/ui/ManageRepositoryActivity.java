package interdroid.vdb.persistence.ui;

import interdroid.vdb.content.EntityUriMatcher;
import interdroid.vdb.content.EntityUriMatcher.MatchType;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class ManageRepositoryActivity extends TabActivity {
	public static String ACTION_MANAGE_REPOSITORY = "interdroid.vdb.action.MANAGE_REPOSITORY";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
        UriMatch match = EntityUriMatcher.getMatch(intent.getData());
        
        if (match.type != MatchType.REPOSITORY) {
        	throw new RuntimeException("Not a vdb repository URI: "
        			+ intent.getData());
        }

		final TabHost tabHost = getTabHost();

	    tabHost.addTab(tabHost.newTabSpec("tab1")                                                                                   
	            .setIndicator("Branches")
	            .setContent(new Intent(
	            		ManageLocalBranchesActivity.ACTION_MANAGE_LOCAL_BRANCHES,
	            		intent.getData())));
	    
	    tabHost.addTab(tabHost.newTabSpec("tab2")                                                                                   
	            .setIndicator("Peers")
	            .setContent(new Intent(
	            		ManageRemotesActivity.ACTION_MANAGE_REMOTES,
	            		intent.getData())));	    
	}
    
}
