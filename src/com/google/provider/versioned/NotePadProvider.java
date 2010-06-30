package com.google.provider.versioned;

import interdroid.vdb.content.GenericContentProvider;

public class NotePadProvider extends GenericContentProvider {
	public NotePadProvider() {
		super("notes", Notes.class); 
	}
}
