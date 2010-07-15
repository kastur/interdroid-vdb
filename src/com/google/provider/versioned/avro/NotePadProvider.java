package com.google.provider.versioned.avro;

import interdroid.vdb.content.avro.AvroContentProvider;

public class NotePadProvider extends AvroContentProvider {
	public NotePadProvider() {
		super("google.notes",
				"{ " +
				" \"type\": \"record\"," +
				" \"name\": \"notes\"," +
				" \"namespace\": \"google\"," +
				" \"fields\" : [" +
				"    {\"name\": \"" + Notes._ID + "\", \"type\": \"int\"}," +
				"    {\"name\": \"" + Notes.TITLE + "\", \"type\": \"string\"}," +
				"    {\"name\": \"" + Notes.NOTE + "\", \"type\": \"string\"}," +
				"    {\"name\": \"" + Notes.CREATED_DATE + "\", \"type\": \"int\"}," +
				"    {\"name\": \"" + Notes.MODIFIED_DATE + "\", \"type\": \"int\"}" +
				" ]" +
				"}");
	}
}
