/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package find.service.gcm;

import com.google.android.gms.maps.model.LatLng;
import find.service.net.diogomarques.wifioppish.NodeIdentification;
import find.service.net.diogomarques.wifioppish.sensors.LocationSensor;
import find.service.net.diogomarques.wifioppish.service.LOSTService;
import android.app.Activity;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * This {@code WakefulBroadcastReceiver} takes care of creating and managing a
 * partial wake lock for your app. It passes off the work of processing the GCM
 * message to an {@code IntentService}, while ensuring that the device does not
 * go back to sleep in the transition. The {@code IntentService} calls
 * {@code GcmBroadcastReceiver.completeWakefulIntent()} when it is ready to
 * release the wake lock.
 */

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
	private final String TAG = "GCM_Receiver";
	private final int STOP = 3;
	private final int CHANGE_MODE = 4;

	private final int RADIUS_DOWNLOAD = 1;
	private Context c;

	private long locationTimeout;
	private long locationTimer;
	private int number_attempts = 5;
	private int attempts = 1;
	private Location currentLoc = null;

	String type;
	String name;
	String date;
	double latS;
	double lonS;
	double latE;
	double lonE;
	Intent intent;

	private LocationSensor ls;

	@Override
	public void onReceive(Context context, Intent intent) {
		c = context;
		Log.d(TAG, "received notification: " + intent.getAction());
		setResultCode(Activity.RESULT_OK);
		this.intent = intent;

		// check if its alarm to start the service and enables it
		/*
		 * if (intent.getAction().equals("startAlarm")) { Log.d(TAG,
		 * "Handling alarm"); handleAlarm(); return; }
		 */

		// get data from push notification
		type = intent.getExtras().getString("type");
		String mode= intent.getExtras().getString("mode");

		Log.d(TAG, "Type:" + type);

		// if received stop notification start the stopping service
		try {
			int tp = Integer.parseInt(type);

			if (tp == STOP) {
				Log.d(TAG, "Stopping service");
				Notifications.generateNotification(c, "FIND Service",
						"Terminating the service", null);
				ScheduleService.cancelAlarm(c);
				Simulation.regSimulationContentProvider("", "", "", "", c);
				LOSTService.stop(c);
				return;
			}
			if (tp == CHANGE_MODE) {
				SharedPreferences prefs = c.getSharedPreferences(DemoActivity.class.getSimpleName(),
						Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString(RequestServer.MODE, mode);
				editor.commit();
				return;
			}
		} catch (NumberFormatException e) {
			Log.d(TAG, "Converted type is not a number");
		}

		Log.d(TAG, "Checking received new notification");
		// received new simulation notification, getting parameters
		name = intent.getExtras().getString("name");
		date = intent.getExtras().getString("date");
		latS = Double.parseDouble(intent.getExtras().getString("latS"));
		lonS = Double.parseDouble(intent.getExtras().getString("lonS"));
		latE = Double.parseDouble(intent.getExtras().getString("latE"));
		lonE = Double.parseDouble(intent.getExtras().getString("lonE"));

		Log.d(TAG, "date: " + date);

		// set timer for retriving location
		long timeleft = DateFunctions.timeToDate(date);
		locationTimer = timeleft / 2;
		locationTimeout = locationTimer / number_attempts;

		Log.d(TAG, "timeleft: " + timeleft);

		// retrieving last best location

		Location l = LocationFunctions.getBestLocation(context);
		if (l == null || LocationFunctions.oldLocation(l)) {
			Log.d(TAG, "old or null location");

			// if old location then try to get new location for half the time
			// left until the starting date
			ls = new LocationSensor(c);
			ls.startSensor();
			getLocation();

		} else {

			Notifications.generateNotification(c, "Alert", "Location Found!",
					null);

			// prompt pop up window
			currentLoc = l;
			startPopUp(currentLoc);
		}

	}

	/**
	 * Prompt timed pop-up asking if the user wishes to associate himself with
	 * if it timeouts it automatically associates the user
	 * 
	 * @param currentLoc
	 */
	private void startPopUp(Location currentLoc) {

		final SharedPreferences preferences = c.getApplicationContext()
				.getSharedPreferences("Lost",
						android.content.Context.MODE_PRIVATE);

		// if location is defined check if its inside the area of the alert
		boolean isInside;
		LatLng center;
		if (currentLoc != null) {
			double latitude = currentLoc.getLatitude();
			double longitude = currentLoc.getLongitude();
			if (latitude != 0) {

				Log.d(TAG, "got location:" + latitude + " " + longitude);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putBoolean("location", true);
				editor.commit();

				center = new LatLng(latitude, longitude);
				isInside = LocationFunctions.isInLocation(currentLoc, latS,
						lonS, latE, lonE);
				if (!isInside) {
					Log.d(TAG, "Stopping: not inside bounds");

					Notifications.generateNotification(c, "Alert",
							"Not inside bounds!", null);
					return;
				}

				final SharedPreferences prefs = c.getSharedPreferences(
						DemoActivity.class.getSimpleName(),
						Context.MODE_PRIVATE);
				String account = prefs.getString(SplashScreen.PROPERTY_ACCOUNT,
						"");

				RequestServer.sendCoordinates(
						NodeIdentification.getMyNodeId(c), center,
						LocationFunctions.getBatteryLevel(c), account,
						currentLoc.getAccuracy(), currentLoc.getTime());
				LatLng start = LocationFunctions.adjustCoordinates(center,
						RADIUS_DOWNLOAD, 135);
				intent.putExtra("latS", start.latitude);
				intent.putExtra("lonS", start.longitude);
				LatLng end = LocationFunctions.adjustCoordinates(center,
						RADIUS_DOWNLOAD, 315);
				intent.putExtra("latE", end.latitude);
				intent.putExtra("lonE", end.longitude);
			}
		} else {
			Log.d(TAG, "Location undefined: calculating center");

			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("location", false);
			editor.putFloat("latS", (float) latS);
			editor.putFloat("lonS", (float) lonS);
			editor.putFloat("latE", (float) latE);
			editor.putFloat("lonE", (float) lonE);
			editor.commit();

			center = LocationFunctions.findCenter(latS, lonS, latE, lonE);
			// get top left coordinate
			LatLng start = LocationFunctions.adjustCoordinates(center,
					RADIUS_DOWNLOAD, 135);
			intent.putExtra("latS", start.latitude);
			intent.putExtra("lonS", start.longitude);
			// get bottom right coordinate
			LatLng end = LocationFunctions.adjustCoordinates(center,
					RADIUS_DOWNLOAD, 315);
			intent.putExtra("latE", end.latitude);
			intent.putExtra("lonE", end.longitude);
		}

		// timed popup dialog
		Log.d(TAG, "Starting pop up");
		intent.setClass(c, AssociationActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		c.startActivity(intent);

	}

	/**
	 * Try to gather location from gps start popup activity if it gets it
	 */
	Handler handler = new Handler();
	final Runnable runnable = new Runnable() {
		@Override
		public void run() {
			Log.d(TAG, "Trying to get  location");
			Location location = (Location) ls.getCurrentValue();
			if (location.getLatitude() != 0) {
				ls.stopSensor();

				Notifications.generateNotification(c, "Alert",
						"Location Found!", null);

				startPopUp(location);
			} else {

				getLocation();
			}

		}
	};

	/**
	 * Try to get gps location "number_attemps" times, if it fails prompt the
	 * pop up
	 */
	private void getLocation() {
		if (attempts < number_attempts) {
			handler.postDelayed(runnable, locationTimeout);
			attempts++;
		} else {

			Notifications.generateNotification(c, "Alert",
					"Undefined location!", null);

			ls.stopSensor();
			startPopUp(null);
		}
	}

}
