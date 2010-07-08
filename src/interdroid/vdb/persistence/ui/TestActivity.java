package interdroid.vdb.persistence.ui;


import interdroid.vdb.content.EntityUriBuilder;
//import interdroid.vdb.content.VdbMainContentProvider;
//import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;
import interdroid.vdb.persistence.impl.MergeHelper;
import interdroid.vdb.persistence.impl.VdbRepositoryImpl;

import java.io.IOException;
//import java.net.URISyntaxException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Properties;

//import org.eclipse.jgit.lib.NullProgressMonitor;
//import org.eclipse.jgit.lib.Repository;
//import org.eclipse.jgit.transport.RefSpec;
//import org.eclipse.jgit.transport.RemoteConfig;
//import org.eclipse.jgit.transport.SshConfigSessionFactory;
//import org.eclipse.jgit.transport.SshSessionFactory;
//import org.eclipse.jgit.transport.Transport;
//import org.eclipse.jgit.transport.URIish;
//import org.eclipse.jgit.transport.OpenSshConfig.Host;
//
//import com.jcraft.jsch.Session;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

public class TestActivity extends Activity {
	private static final int REQUEST_PICK_VERSION = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

        // Intent intent = new Intent(Intent.ACTION_PICK,
        //		Uri.parse("content://" + VdbMainContentProvider.AUTHORITY + "/notes"));
        // startActivityForResult(intent, REQUEST_PICK_VERSION);

		Intent intent = new Intent(ManageRepositoryActivity.ACTION_MANAGE_REPOSITORY,
				EntityUriBuilder.repositoryUri("notes"));
		startActivity(intent);
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK && requestCode == REQUEST_PICK_VERSION) {
			TextView v = new TextView(this);
			v.setText(data.getDataString());
			setContentView(v);

			try {
				MergeHelper helper = new MergeHelper(
						VdbRepositoryRegistry.getInstance().getRepository("notes")
						.getBranch("temp"));
				//helper.diff2("notes", "notes");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			VdbRepositoryImpl reg = (VdbRepositoryImpl)
					VdbRepositoryRegistry.getInstance().getRepository("notes");

			Intent intent;

			intent = new Intent(ManageRepositoryActivity.ACTION_MANAGE_REPOSITORY,
					EntityUriBuilder.repositoryUri("notes"));
			startActivity(intent);

			if (false) {
				Uri notesUri = Uri.withAppendedPath(data.getData(), "notes");

				intent = new Intent(Intent.ACTION_PICK, notesUri);
				startActivity(intent);
			}
		}
	}
}
