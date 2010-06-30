package interdroid.vdb.persistence.impl;

import java.io.IOException;

import android.database.sqlite.SQLiteDatabase;

public class VdbCommitImpl extends VdbCheckoutImpl {

	public VdbCommitImpl(VdbRepositoryImpl parentRepo, String branchName)
	{
		super(parentRepo, branchName);
	}

	@Override
	public synchronized SQLiteDatabase getReadWriteDatabase() throws IOException
	{
		throw new RuntimeException("Commit checkouts are readonly");
	}

	@Override
	public void commit(String authorName, String authorEmail, String msg)
	throws IOException
	{
		throw new RuntimeException("Commit checkouts are readonly");
	}
}
