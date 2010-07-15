package com.google.provider.versioned.orm;

import interdroid.vdb.content.orm.ORMGenericContentProvider;

public class NotePadProvider extends ORMGenericContentProvider {
	public NotePadProvider() {
		super("notes", Notes.class);
	}
}
