package find.service.gcm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class LocationFunctions {
	private final static String TAG = "GCM";
	private final static int LAST_UPDATE_THRESHOLD = 1000 * 60 * 120;
	private final static int ACCURACY = 4000;

	public static LatLng findCenter(double f_latS, double f_lonS,
			double f_latE, double f_lonE) {
		double diffLat = Math.abs(f_latS - f_latE) / 2;
		double diffLon = Math.abs(f_lonE - f_lonS) / 2;

		// tp.downloadTilesInBound(f_latS, f_lonE, f_latE, f_lonS , MIN_ZOOM,
		// MAX_ZOOM, c);

		return new LatLng(f_latS + diffLat, f_lonE + diffLon);
	}

	// Get coordidates at a certain radius and degrees
	public static LatLng adjustCoordinates(LatLng center, int radius,
			int degrees) {
		double lat = (center.latitude * Math.PI) / 180;

		double lon = (center.longitude * Math.PI) / 180;

		double d = (float) (((float) radius) / 6378.1);

		double brng = degrees * Math.PI / 180;
		// rad
		double destLat = Math.asin(Math.sin(lat) * Math.cos(d) + Math.cos(lat)
				* Math.sin(d) * Math.cos(brng));
		double destLng = ((lon + Math.atan2(
				Math.sin(brng) * Math.sin(d) * Math.cos(lat), Math.cos(d)
						- Math.sin(lat) * Math.sin(destLat))) * 180)
				/ Math.PI;
		destLat = (destLat * 180) / Math.PI;

		// Log.d(TAG, "lat:" + lat + "->" + destLat + " lon:" + lon + "->"
		// + destLng);
		return new LatLng(destLat, destLng);
	}

	// Primitive location checker
	//TODO real location compare
	public static boolean isInLocation(Location loc, double f_latS,
			double f_lonS, double f_latE, double f_lonE) {

		Log.d(TAG, "values " + f_latS + " " + f_lonS + " " + f_latE + " "
				+ f_lonE);
		double lat = loc.getLatitude();
		double lon = loc.getLongitude();
		if (lat < f_latS && lat > f_latE && lon > f_lonE && lon < f_lonS) {
			return true;
		}
		return false;
	}

	public static boolean oldLocation(Location l) {
		long old = System.currentTimeMillis() - LAST_UPDATE_THRESHOLD;
		return (l.getTime() < old);
	}

	/**
	 * get the last known location from a specific provider (network/gps)
	 */
	public static Location getLocationByProvider(String provider, Context c) {
		Location location = null;

		LocationManager locationManager = (LocationManager) c
				.getApplicationContext().getSystemService(
						Context.LOCATION_SERVICE);
		try {
			if (locationManager.isProviderEnabled(provider)) {
				location = locationManager.getLastKnownLocation(provider);
			}
		} catch (IllegalArgumentException e) {
			Log.d(TAG, "Cannot acces Provider " + provider);
		}
		return location;
	}

	/**
	 * try to get the 'best' location selected from all providers
	 */
	public static Location getBestLocation(Context c) {
		Location gpslocation = LocationFunctions.getLocationByProvider(
				LocationManager.GPS_PROVIDER, c);
		Location networkLocation = LocationFunctions.getLocationByProvider(
				LocationManager.NETWORK_PROVIDER, c);
		// if we have only one location available, the choice is easy
		if (gpslocation == null) {
			Log.d(TAG, "No GPS Location available.");
			if (networkLocation != null
					&& networkLocation.getAccuracy() < ACCURACY) {
				Log.d(TAG, "Available accurate network location");
				return networkLocation;

			} else {
				Log.d(TAG, "No Network Location available");
				return null;
			}
		}
		if (networkLocation == null) {
			Log.d(TAG, "No Network Location available");
			return gpslocation;
		}
		// a locationupdate is considered 'old' if its older than the configured
		// update interval. this means, we didn't get a
		// update from this provider since the last check
		boolean gpsIsOld = LocationFunctions.oldLocation(gpslocation);
		boolean networkIsOld = LocationFunctions.oldLocation(networkLocation);
		// gps is current and available, gps is better than network
		if (!gpsIsOld) {
			Log.d(TAG, "Returning current GPS Location");
			return gpslocation;
		}
		// gps is old, we can't trust it. use network location
		if (!networkIsOld) {
			Log.d(TAG, "GPS is old, Network is current, returning network");
			return networkLocation;
		}
		// both are old return the newer of those two
		if (gpslocation.getTime() > networkLocation.getTime()) {
			Log.d(TAG, "Both are old, returning gps(newer)");
			return gpslocation;
		} else {
			Log.d(TAG, "Both are old, returning network(newer)");
			return networkLocation;
		}
	}

	public static float getBatteryLevel(Context c) {
		Intent batteryIntent = c.getApplicationContext().registerReceiver(null,
				new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		// Error checking that probably isn't needed but I added just in case.
		if (level == -1 || scale == -1) {
			return 50.0f;
		}

		return ((float) level / (float) scale) * 100.0f;
	}



}
