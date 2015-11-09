package find.service.gcm;

import java.util.GregorianCalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import org.w3c.dom.ls.LSException;

import find.service.net.diogomarques.wifioppish.sensors.LocationSensor;
import find.service.net.diogomarques.wifioppish.service.LOSTService;

public class ScheduleService extends BroadcastReceiver {
	private final static String TAG = "gcm";
	private static Context c;
	private LocationSensor ls;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		c= context;
		Log.d(TAG, "received alarm");

		String action=intent.getAction();
		if (action.equals("startAlarm")) {
			Log.d(TAG, "received start alarm: starting service");
			handleStartAlarm();
		}else{
			if (action.equals("stopAlarm")) {
				Log.d(TAG, "received stop alarm: stopping service");

				handleStopAlarm();
			}
			
		}
		
		/*Intent openMainActivity= new Intent(context, DemoActivity.class);
        openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        openMainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(openMainActivity);*/
	}
	/**
	 * Handles the stop service alarm
	 */
	private void handleStopAlarm() {
		Simulation.regSimulationContentProvider("", "", "", "", c);
		LOSTService.stop(c);
	}

	/**
	 * Handles the start service alarm
	 */
	private void handleStartAlarm() {
	

		//Intent svcIntent = new Intent(
		//		"find.service.net.diogomarques.wifioppish.service.LOSTService.START_SERVICE");
		Intent svcIntent = new Intent(c, LOSTService.class);
		c.startService(svcIntent);

		final SharedPreferences preferences = c.getApplicationContext()
				.getSharedPreferences("Lost",
						android.content.Context.MODE_PRIVATE);
		boolean checkedLocation = preferences.getBoolean("location", false);
		if (!checkedLocation) {
			Log.d(TAG, "Verifiyng if location is within bounds");
			ls = new LocationSensor(c);
			ls.startSensor();
			isInSimulationLocation(preferences.getFloat("latS", 0),
					preferences.getFloat("lonS", 0),
					preferences.getFloat("latE", 0),
					preferences.getFloat("lonE", 0));
		}
	}
	
	/**
	 * Verify if
	 * 
	 * @param lat
	 *            - top left latitude
	 * @param lon
	 *            - top left longitude
	 * @param lat2
	 *            - bottom right latitude
	 * @param lon2
	 *            - bottom right longitude
	 */
	private void isInSimulationLocation(float lat, float lon, float lat2,
			float lon2) {

		verifyLoc.postDelayed(new MyRunnable(lat, lon, lat2, lon2), 30000);

	}
	// On service start verify if the user is in a affected area
	Handler verifyLoc = new Handler();

	public class MyRunnable implements Runnable {
		private float lat;
		private float lat2;
		private float lon;
		private float lon2;

		public MyRunnable(float lat, float lat2, float lon, float lon2) {
			this.lat = lat;
			this.lat2 = lat2;
			this.lon = lon;
			this.lon2 = lon2;
		}

		public void run() {
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params1) {
					Location location = (Location) ls.getCurrentValue();
					if (location.getLatitude() != 0) {
						if (!LocationFunctions
								.isInLocation(location, lat, lon, lat2, lon2)) {
							stop(c);
							Log.d(TAG, "Not in location");
						}
						Log.d(TAG, "In location");

					} else {
						Log.d(TAG, "Undefined location");
						isInSimulationLocation(lat, lon, lat2, lon2);

					}
					return null;
				}
			}.execute(null, null, null);
		}
	}
	
	/**
	 * Stops the service the user is not in location and deletes all points
	 */
	private void stop(Context c) {
		Log.d(TAG, "Manually stopping service");
		Notifications.generateNotification(c,"FIND Service" ,"Stopping service",null);

		//Intent svcIntent = new Intent(
		//		"find.service.net.diogomarques.wifioppish.service.LOSTService.START_SERVICE");
		Intent svcIntent = new Intent(c, LOSTService.class);
		final SharedPreferences prefs = c.getSharedPreferences(
				DemoActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		c.stopService(svcIntent);
		Simulation.regSimulationContentProvider("","","","",c);
		String regid = prefs.getString(SplashScreen.PROPERTY_REG_ID, "");

		RequestServer.deletePoints(regid);
		c.deleteDatabase("LOSTMessages");

		Intent mStartActivity = new Intent(c, DemoActivity.class);
		int mPendingIntentId = 123456;
		PendingIntent mPendingIntent = PendingIntent.getActivity(c,
				mPendingIntentId, mStartActivity,
				PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager mgr = (AlarmManager) c
				.getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100,
				mPendingIntent);
		System.exit(0);
		return;

	}
	
	private static boolean isLong(String s) {
	    try { 
	        Long.parseLong(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    // only got here if we didn't return false
	    return true;
	}
	
	
	
	
	public static void setStartAlarm(String date, String duration, Context c) {
		date= date.replaceAll("-", "/");
		long timeleft = DateFunctions.timeToDate(date);
		if (timeleft < 0) {
			timeleft=0;
		
		}
		
		//if the alert is a simulation it has a 
		//duration field not null and therefor we set a stop alarm
		if(isLong(duration) && Long.parseLong(duration)!=-1){
			
			setStopAlarm(date, duration, c);
		}
		
			Long time = new GregorianCalendar().getTimeInMillis() + timeleft;
			Log.d(TAG, "setting start alarm to alarm " + timeleft + " date:" + date + " duration:" + duration + " " + time);
			Intent intentAlarm = new Intent("startAlarm");
			PendingIntent startPIntent = PendingIntent.getBroadcast(c, 0,
					intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT); 
			
			// create the object
			AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);

			// set the alarm for particular time
			alarmManager.set(AlarmManager.RTC_WAKEUP, time, startPIntent);
			Toast.makeText(c, "Service will automatically start at " + date,
			Toast.LENGTH_LONG).show();
	}
	
	private static void setStopAlarm(String date,String durationT,  Context c) {
		date= date.replaceAll("-", "/");
		long timeleft = DateFunctions.timeToDate(date);
		long duration= Long.parseLong(durationT) *60*1000;
		timeleft+=duration;
		if (timeleft < 0) {
			timeleft=0;
		
		}
			Long time = new GregorianCalendar().getTimeInMillis() + timeleft;
			Log.d(TAG, "setting stop alarm to alarm " + timeleft + " date:" + date + " duration:" + duration + " " + time);
			Intent intentAlarm = new Intent("stopAlarm");
			PendingIntent startPIntent = PendingIntent.getBroadcast(c, 1231,
					intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);
			// create the object
			AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);

			// set the alarm for particular time
			alarmManager.set(AlarmManager.RTC_WAKEUP, time, startPIntent);
			
			
			/*Toast.makeText(c, "Service will automatically start at " + date,
			Toast.LENGTH_LONG).show();*/
	}
	

	public static void cancelAlarm(Context c) {
		Log.d(TAG, "canceling start alarm");

		Intent intentAlarm = new Intent("startAlarm");
		PendingIntent startPIntent = PendingIntent.getBroadcast(c, 0,
				intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);

		// create the object
		AlarmManager alarmManager = (AlarmManager) c
				.getSystemService(Context.ALARM_SERVICE);

		// stop alarm
		alarmManager.cancel(startPIntent);
		
		Log.d(TAG, "canceling stop alarm");
		Intent intentStopAlarm = new Intent("stopAlarm");
		PendingIntent stopPIntent = PendingIntent.getBroadcast(c, 1231,
				intentStopAlarm, PendingIntent.FLAG_UPDATE_CURRENT);

		// stop alarm
		alarmManager.cancel(stopPIntent);
		

	}

}
