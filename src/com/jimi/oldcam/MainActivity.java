package com.jimi.oldcam;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.os.Bundle; 
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener; // what do i do if need view.clicklistener ?
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import android.view.Window;

public class MainActivity extends Activity
{
	DB db;

	long roll;
	long pic;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Thread.setDefaultUncaughtExceptionHandler(
			new java.lang.Thread.UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				Log.e("oldcam", "Unhandled exception: " + ex.getMessage(), ex);
				System.exit(1);
			}}); 

		// this.requestWindowFeature(Window.FEATURE_NO_TITLE); // seems sad without it
		setContentView(R.layout.main);
		init();
	} 
  
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.mainactivity, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_export) {
			String s = db.writeCSV();
			if (s != "") {
				Toast t = Toast.makeText(this, "File exported to " + s, Toast.LENGTH_SHORT);
				t.show();
				return true;
			}
		} 
		if (id == R.id.action_purge) { 
			AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
			dlgAlert.setMessage("Sure ?");
			dlgAlert.setTitle("Purging");
			final Context context = this;
			dlgAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							db.purge();
							Toast t = Toast.makeText(context, "DB purged", Toast.LENGTH_SHORT);
							t.show();
							pic=1;roll=1;
							display();
						}});
			dlgAlert.setNegativeButton("No", null);
			//dlgAlert.setCancelable(false);
			dlgAlert.create().show(); 
			return true;
		} 

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() { // better than onstop
		super.onPause(); 
		save();
	} 

	private void init() {
		db = new DB(this);
		roll = db.getParamInt("roll");
		if (roll == -1) { roll = 1; };
		pic = db.getParamInt("pic");
		if (pic == -1) { pic = 1; };
		Log.i("oldcam", "here");
		display();
	}

	public void btn_change_roll(View view) {
		ask_for_roll();
	}

	public void btn_change_pic(View view) {
		ask_for_pic();
	}

	public void btn_pic_next(View view) {
		save();
		pic += 1;
		display();
	}

	public void btn_pic_prev(View view) {
		save();
		pic -= 1;
		if (pic < 1) {
			roll -= 1;
			if (roll < 1) { roll = 1; }
			pic = db.get_last(roll);
		}
		display();
	}

	boolean tryParseInt(String value)  
	{  
	 try  
	 {  
		 Integer.parseInt(value);  
		 return true;  
	  } catch(NumberFormatException nfe)  
	  {  
		  return false;  
	  }  
	}

	public void save() { 
		EditText et_date = (EditText) findViewById(R.id.et_date);		
		EditText et_aperture = (EditText) findViewById(R.id.et_aperture);		
		EditText et_shutter_speed = (EditText) findViewById(R.id.et_shutter_speed);		
		EditText et_notes = (EditText) findViewById(R.id.et_notes);		
		if (pic != -1) {
			// if somethg written in any field except date auto filled, then save
			String date = et_date.getText().toString();
			String aperture = et_aperture.getText().toString();
			String shutter_speed = et_shutter_speed.getText().toString();
			String notes = et_notes.getText().toString();
			Log.i("oldcam", aperture + " " + shutter_speed + " " + String.valueOf(pic));
			if ((aperture.isEmpty()) && (shutter_speed.isEmpty()) && (notes.isEmpty())) { 
				db.del_pic(roll, pic);
			}
			else {
				db.add_pic(roll, pic, date, aperture, shutter_speed, notes);
			} 
		}
	}


	public void display() { 
		EditText et_date = (EditText) findViewById(R.id.et_date);		
		EditText et_aperture = (EditText) findViewById(R.id.et_aperture);		
		EditText et_shutter_speed = (EditText) findViewById(R.id.et_shutter_speed);		
		EditText et_notes = (EditText) findViewById(R.id.et_notes);		
		String data[] = db.get_pic(roll, pic); 
		TextView tv_roll = (TextView) findViewById(R.id.tv_roll);
		TextView tv_pic = (TextView) findViewById(R.id.tv_pic);
		tv_roll.setText("Roll #" + String.valueOf(roll));
		tv_pic.setText("Pic #" + String.valueOf(pic));
		String sdate = data[0];
		et_date.setText(data[0]);
		et_aperture.setText(data[1]);
		Log.i("oldcam", data[2]);
		et_shutter_speed.setText(data[2]);
		et_notes.setText(data[3]);
		Log.i("oldcam", "date = " + sdate);
		if (sdate.isEmpty()) {
			sdate = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(Calendar.getInstance().getTime()); 
			et_date.setText(sdate);	
		}
		db.setParamInt("roll", roll);
		db.setParamInt("pic", pic);
	}

	public void ask_for_roll() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
	
		alert.setTitle("Roll");
		alert.setMessage("Enter the roll number");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input); 
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		final Context context = this;

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				// Do something with value!
				if (tryParseInt(value)) { 
					save();
					int ival = Integer.parseInt(value);
					roll = ival;
					pic = db.get_last(roll);
					display();
				}
				else {
					Toast t = Toast.makeText(context, "Incorrect value",  Toast.LENGTH_SHORT);
					t.show(); 
				} 
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});
		alert.show();
	}

	public void ask_for_pic() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
	
		alert.setTitle("Pic");
		alert.setMessage("Enter the pic number");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		final Context context = this;

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				// Do something with value!
				if (tryParseInt(value)) { 
					save();
					int ival = Integer.parseInt(value);
					pic = ival;
					display();
				}
				else {
					Toast t = Toast.makeText(context, "Incorrect value",  Toast.LENGTH_SHORT);
					t.show(); 
				} 
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});
		alert.show();
	}


}
