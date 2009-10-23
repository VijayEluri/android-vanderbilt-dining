package edu.vanderbilt.vuphone.android.dining;

import java.util.Calendar;

import android.app.Activity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class RestaurantDetails extends Activity {
	
	
	private int restaurant;
	private Calendar rightNow;

	
	private OnClickListener mapListener = new OnClickListener() {
		public void onClick(View v) {
			
			// TODO
			
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Main.DEBUG) Log.i("Dining", "Restaurant details Activity created");
		
		setContentView(R.layout.restaurant_details);
		
		rightNow = Calendar.getInstance();
		restaurant = getIntent().getExtras().getInt("restaurant");

		((TextView) findViewById(R.restaurantDetailsPage.restaurantName)).setText(Main.RESTAURANTS[restaurant]); 
		((TextView) findViewById(R.restaurantDetailsPage.restaurantHours)).setText(WeeklySchedule());
		
		((Button) findViewById(R.restaurantDetailsPage.to_map_button)).setOnClickListener(mapListener);

	}
	
	CharSequence WeeklySchedule() {
		SpannableStringBuilder out = new SpannableStringBuilder();
		
		// TODO This is temporary till the database is implemented
		
		out.append("S " + Main.SUNDAY_START[restaurant] + " - " + Main.SUNDAY_END[restaurant] + '\n' + 
		"M " + Main.MONDAY_START[restaurant] + " - " + Main.MONDAY_END[restaurant] + '\n' +
		"T " + Main.TUESDAY_START[restaurant] + " - " + Main.TUESDAY_END[restaurant] + '\n' + 
		"W " + Main.WEDNESDAY_START[restaurant] + " - " + Main.WEDNESDAY_END[restaurant] + '\n' + 
		"T " + Main.THURSDAY_START[restaurant] + " - " + Main.THURSDAY_END[restaurant] + '\n' + 
		"F " + Main.FRIDAY_START[restaurant] + " - " + Main.FRIDAY_END[restaurant] + '\n' + 
		"M " + Main.SATURDAY_START[restaurant] + " - " + Main.SATURDAY_END[restaurant] + '\n');
		
		int i = 0;
		for (int day = 1;day<=7;day++) {
			
			int next = out.toString().indexOf('\n', i);
			next = (next != -1 ? next : out.length());
			
			if (day==rightNow.get(Calendar.DAY_OF_WEEK))
				out.setSpan(new RelativeSizeSpan((float)1.4), i, next, 0);  // trying to find way to do bold but had to settle for this
			i = next + 1;
		}
		
		return out;
	}
}