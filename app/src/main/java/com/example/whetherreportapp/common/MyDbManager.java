package com.example.whetherreportapp.common;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDbManager extends SQLiteOpenHelper 
{

	public MyDbManager(Context context) 
	{
		super(context, "MY_DB", null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}
	
	public void executeDDLQuery(String query)
	{
		getWritableDatabase().execSQL(query);
	}
	
	public Cursor executeRawQuery(String query)
	{
		return getReadableDatabase().rawQuery(query, null);
	}

	public void insertData(String tablename, String data, String date)
	{
		ContentValues cv = new ContentValues();
		cv.put("DATE", date);
		cv.put("data", data);
		getWritableDatabase().insert(tablename, null, cv);
	}
}
