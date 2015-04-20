package com.jimi.oldcam; 

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException; 
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import android.content.ContentValues;
import android.util.Log;
import android.content.ContentResolver;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException; 
import android.widget.Toast;

public class DB extends SQLiteOpenHelper {


	private static final int DATABASE_VERSION = 3;
	private static final String DATABASE_NAME = "oldcam";

	private Context context;

	DB(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	private static String SQLCreatePictures = 
		"create table pictures(" + 
		//"id integer primary key autoincrement, " + 
		"roll integer," +  // id of the roll used
		"pic integer," + // id of the picture in the roll
		"day date," + // id of the picture in the roll
		"aperture text," +
		"shutter_speed integer," + 
		"notes text," + // TODO allow user to handle that
		"primary key (roll, pic))"; // notes about the picture itself, can enter what filter used here for example

	public void onCreate(SQLiteDatabase db) {
		// call on create of database
		createTables(db);
	} 

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < DATABASE_VERSION) {
			db.execSQL("drop table if exists cameras;");
			db.execSQL("drop table if exists pictures;");
			db.execSQL("drop table if exists paramsInt;");
			db.execSQL("drop table if exists paramsStr;");
		}
		createTables(db);
		//fillDefaults();
	}

	public void createTables(SQLiteDatabase db) {
		db.execSQL("create table paramsStr(key varchar(30), value varchar(30));");
		db.execSQL("create table paramsInt(key varchar(30), value integer);");
		//db.execSQL(SQLCreateCamera);
		db.execSQL(SQLCreatePictures);
		//fillDefaults();	
	}

	public void setParamInt(String key, long value) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put("value", value);
		if (db.update("paramsInt", cv, "key=?",
				new String[]{key}) == 0) {
			cv.put("key", key);
			db = this.getWritableDatabase();
			db.insert("paramsInt", "", cv);
		}
	}

	public long getParamInt(String key) {
		Cursor cur = getReadableDatabase().rawQuery("select value from paramsInt where key=?;", new String [] {key});
		Integer res = -1;
		if ((cur !=null) && cur.moveToFirst()) {
			res = cur.getInt(0);
			cur.close();
		}
		return res;
	}

	public void emptyDB() {
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("delete from cameras;");
		db.execSQL("delete from pictures;");
	}

	public void add_pic(long roll, long pic, String date, String aperture, String shutter_speed, String notes) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put("day", date);
		cv.put("shutter_speed", shutter_speed);
		cv.put("aperture", aperture); 
		cv.put("notes", notes);
		if (db.update("pictures", cv, "roll=? and pic=?",
				new String[]{String.valueOf(roll), String.valueOf(pic)}) == 0) {
			cv.put("roll", roll);
			cv.put("pic", pic);
			db = this.getWritableDatabase();
			db.insert("pictures", "", cv);
		}
	}

	public void del_pic(long roll, long pic) { 
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("delete from pictures where roll=? and pic=?;", new String[]{String.valueOf(roll), String.valueOf(pic)});
	}

	public long get_last(long roll) {
		Cursor cur = getReadableDatabase().rawQuery("select max(pic) from pictures where roll=?;", new String [] {String.valueOf(roll)});
		int res = -1;
		if ((cur !=null) && cur.moveToFirst()) {
			if (!(cur.isNull(0))) {
				res = cur.getInt(0); }
		} 
		if (res == -1) { res = 1; }
		return res;
	}

	public String[] get_pic(long roll, long pic) {
		Cursor cur = getReadableDatabase().rawQuery("select day, aperture, shutter_speed, notes from pictures where roll=? and pic=?;", new String [] {String.valueOf(roll), String.valueOf(pic)});
		if ((cur !=null) && cur.moveToFirst()) {
			String date = cur.getString(0);
			String aperture = cur.getString(1);
			String shutter_speed = cur.getString(2);
			String notes = cur.getString(3);
			cur.close();
			if (date == null) { date = ""; };
			if (aperture == null) { aperture = ""; };
			if (shutter_speed == null) { shutter_speed = ""; };
			if (notes == null) { notes = ""; };
			String[] res = {date, aperture, shutter_speed, notes};
			return res;
		}
		else {
			String [] res = {"", "", "", ""};
			return res;
		}  
	}

	public void purge() {
		SQLiteDatabase db = getReadableDatabase();
		db.execSQL("delete from pictures;"); 
	}

	public String writeCSV() {
		try 
		{
			File myFile; 
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
			String TimeStampDB = sdf.format(cal.getTime()); 
			String filename = "/sdcard/oldcam_"+TimeStampDB+".csv"; 
			myFile = new File(filename);
			Log.i("oldcam", "filename = " + filename);
			myFile.createNewFile();
			FileOutputStream fOut = new FileOutputStream(myFile);
			OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
			myOutWriter.append("roll;pic;date;aperture;shutter_speed;notes");
			myOutWriter.append("\n"); 
			SQLiteDatabase db = getReadableDatabase();
			Cursor cur = getWritableDatabase().rawQuery("select roll, pic, day, aperture, shutter_speed, notes from pictures order by roll, pic", null);
			while ((cur != null) && cur.moveToNext()) {
				myOutWriter.append(cur.getString(0) + ";" + cur.getString(1) + ";" + cur.getString(2) + ";" + cur.getString(3) + ";" + cur.getString(4) + ";" + cur.getString(5));
				myOutWriter.append("\n"); 
			} 
			myOutWriter.close();
			return filename;
		} 
		catch (IOException e)
		{
			Toast t = Toast.makeText(context, "Error while writing csv " + e.getMessage(), Toast.LENGTH_LONG);
			t.show();
			return "";
		}
	}


}
