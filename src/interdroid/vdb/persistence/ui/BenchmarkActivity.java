package interdroid.vdb.persistence.ui;

import interdroid.vdb.persistence.api.MergeInProgressException;
import interdroid.vdb.persistence.api.VdbCheckout;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;

import java.io.IOException;


import com.google.provider.NotePad.Notes;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;

public class BenchmarkActivity extends Activity implements OnClickListener {
	private static final String TAG = BenchmarkActivity.class.getSimpleName();

	private VdbRepository vdbRepo_;

	protected void reportResult(String benchmark, long totalTime, int numRuns)
	{
		String msg = String.format("Benchmark %s ran in %d for %d runs.",
				benchmark, totalTime, numRuns);
		Log.v(TAG, msg);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

        vdbRepo_ = VdbRepositoryRegistry.getInstance().getRepository("notes");

        Button btn = new Button(this);
        btn.setText("Run benchmark");
        btn.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        btn.setOnClickListener(this);

        setContentView(btn);
	}

	@Override
	public void onClick(View v)
	{
		new Thread() {
			@Override
			public void run()
			{
				try{
					benchmarkContentProvider(com.google.provider.NotePad.Notes.CONTENT_URI);
		 			benchmarkContentProvider(com.google.provider.versioned.Notes.CONTENT_URI);

					benchmarkCommit();
		 			benchmarkCheckout();
				} catch(Exception e) {
					throw new RuntimeException(e);
				}

			}
		}.start();
	}


	protected long oneCheckoutRun(String branchName) throws IOException
	{
		long startTime = System.currentTimeMillis();
		VdbCheckout vdbCheckout = vdbRepo_.getBranch(branchName);
		long endTime = System.currentTimeMillis();

		vdbCheckout.delete();
		return endTime - startTime;
	}

	protected void benchmarkCheckout() throws IOException
	{
		final String branchName = "benchmark-checkout";
		if (vdbRepo_.listBranches().contains(branchName)) {
			vdbRepo_.deleteBranch(branchName);
		}
		vdbRepo_.createBranch(branchName, "master");

		long totalTime = 0;
		for (int i = 0; i < 100; ++i) {
			totalTime += oneCheckoutRun(branchName);
		}
		reportResult("checkout", totalTime, 100);
		vdbRepo_.deleteBranch(branchName);
	}

	protected void benchmarkCommit() throws IOException, MergeInProgressException
	{
		final String branchName = "benchmark-commit";
		if (vdbRepo_.listBranches().contains(branchName)) {
			vdbRepo_.deleteBranch(branchName);
		}
		vdbRepo_.createBranch(branchName, "master");
		VdbCheckout vdbCheckout = vdbRepo_.getBranch(branchName);
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < 100; ++i) {
			vdbCheckout.commit("commit-benchmark", "commit@benchmark",
					"Lorem ipsum dolor sit amet\n\nfdsafd");
		}
		long totalTime = System.currentTimeMillis() - startTime;
		reportResult("commit", totalTime, 100);
		vdbRepo_.deleteBranch(branchName);
	}

	protected void benchmarkContentProvider(Uri notesTableUri)
	{
		ContentResolver resolver = getContentResolver();

		resolver.delete(notesTableUri, null, null);

		long startTime = System.currentTimeMillis();
		for (int i = 1; i <= 1000; ++i) {
			ContentValues values = new ContentValues();
			values.put(Notes._ID, i);
			values.put(Notes.TITLE, "title " + i);
			values.put(Notes.CREATED_DATE, System.currentTimeMillis());
			values.put(Notes.MODIFIED_DATE, System.currentTimeMillis());
			values.put(Notes.NOTE, "note " + i);
			resolver.insert(notesTableUri, values);
		}
		long totalTime = System.currentTimeMillis() - startTime;

		reportResult("insert " + notesTableUri, totalTime, 1000);

		startTime = System.currentTimeMillis();
		for (int i = 1; i <= 1000; ++i) {
			ContentValues values = new ContentValues();
			values.put(Notes.TITLE, "title changed " + i);
			values.put(Notes.CREATED_DATE, System.currentTimeMillis());
			values.put(Notes.MODIFIED_DATE, System.currentTimeMillis());
			values.put(Notes.NOTE, "note changed " + i);
			resolver.update(Uri.withAppendedPath(notesTableUri, String.valueOf(i)),
					values,  null, null);
		}
		totalTime = System.currentTimeMillis() - startTime;
		reportResult("update " + notesTableUri, totalTime, 1000);

		String[] projection = new String[]{Notes._ID, Notes.TITLE, Notes.NOTE,
				Notes.MODIFIED_DATE, Notes.CREATED_DATE};
		startTime = System.currentTimeMillis();
		for (int i = 1; i <= 100; ++i) {
			Cursor c = resolver.query(notesTableUri, projection, null, null,
					"modified DESC");
			boolean haveRecord = c.moveToFirst();
			while (haveRecord) {
				// just pull the values from the cursor
				c.getLong(0);
				c.getString(1);
				c.getString(2);
				c.getLong(3);
				c.getLong(4);
				haveRecord = c.moveToNext();
			}
			c.close();
		}
		totalTime = System.currentTimeMillis() - startTime;
		reportResult("query " + notesTableUri, totalTime, 100);

		startTime = System.currentTimeMillis();
		for (int i = 1; i <= 1000; ++i) {
			resolver.delete(Uri.withAppendedPath(notesTableUri, String.valueOf(i)),
					null, null);
		}
		totalTime = System.currentTimeMillis() - startTime;
		reportResult("delete " + notesTableUri, totalTime, 1000);
	}
}
