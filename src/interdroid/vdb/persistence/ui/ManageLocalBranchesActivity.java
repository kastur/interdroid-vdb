package interdroid.vdb.persistence.ui;

import interdroid.vdb.content.EntityUriMatcher;
import interdroid.vdb.content.EntityUriMatcher.MatchType;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;
import interdroid.vdb.persistence.ui.RevisionsView.OnRevisionClickListener;

import java.io.IOException;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class ManageLocalBranchesActivity extends Activity implements OnRevisionClickListener {
	public static String ACTION_MANAGE_LOCAL_BRANCHES = "interdroid.vdb.action.MANAGE_LOCAL_BRANCHES";
	private VdbRepository vdbRepo_;
	private static int REQUEST_ADD_BRANCH = 1;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
        UriMatch match = EntityUriMatcher.getMatch(intent.getData());

        if (match.type != MatchType.REPOSITORY) {
        	throw new RuntimeException("Invalid URI, can only add branches to a repository. "
        			+ intent.getData());
        }
        vdbRepo_ = VdbRepositoryRegistry.getInstance().getRepository(match.repositoryName);

        buildUI();
	}

	private RevisionsView revView_;

	private void buildUI()
	{
		revView_ = new RevisionsView(this, vdbRepo_);
		setContentView(revView_);
		revView_.setOnCreateContextMenuListener(this);
		revView_.setOnRevisionClickListener(this);
	}

    // Menu item ids
	public static final int MENU_ITEM_DELETE = Menu.FIRST;
    public static final int MENU_ITEM_VIEW = Menu.FIRST + 1;
    public static final int MENU_ITEM_ADD = Menu.FIRST + 2;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_ITEM_VIEW, 0, "View")
	        .setShortcut('3', 'v')
	        .setIcon(android.R.drawable.ic_menu_view);

        menu.add(0, MENU_ITEM_ADD, 0, "Add branch")
                .setShortcut('3', 'a')
                .setIcon(android.R.drawable.ic_menu_add);

        menu.add(1, MENU_ITEM_DELETE, 0, "Delete branch")
	        .setShortcut('0', 'd')
	        .setIcon(android.R.drawable.ic_menu_delete);

        return true;
    }

    @Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		if (requestCode == REQUEST_ADD_BRANCH && resultCode == RESULT_OK) {
			revView_.refresh();
		}
	}

    private void runViewActivity(Uri uri)
    {
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == MENU_ITEM_ADD) {
        	Intent addIntent = new Intent(AddBranchActivity.ACTION_ADD_BRANCH,
        			getIntent().getData());
            startActivityForResult(addIntent, REQUEST_ADD_BRANCH);
            return true;
    	}
    	Uri selectedUri = revView_.getSelectedUri();
    	if (selectedUri == null) {
    		return true;
    	}
    	UriMatch match = EntityUriMatcher.getMatch(selectedUri);
        switch (item.getItemId()) {
        case MENU_ITEM_VIEW:
			runViewActivity(selectedUri);
			return true;
        case MENU_ITEM_DELETE:
        	if (match.type != MatchType.LOCAL_BRANCH) {
        		return true;
        	}
        	try {
				vdbRepo_.deleteBranch(match.reference);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			revView_.refresh();
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void onRevisionClick(RevisionsView view, Uri revUri, SelectAction type)
	{
		runViewActivity(revUri);
	}

}
