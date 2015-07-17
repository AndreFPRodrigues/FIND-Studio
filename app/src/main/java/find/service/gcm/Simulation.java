package find.service.gcm;

import find.service.gcm.map.TilesProvider;

import java.io.File;
import java.util.GregorianCalendar;

import find.service.net.diogomarques.wifioppish.MessagesProvider;
import find.service.org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class Simulation {
	String name;
	String location;
	String date;
	String duration;
	String latS;
	String lonS;
	String latE;
	String lonE;

	private final static int MIN_ZOOM = 0;
	private final static int MAX_ZOOM = 17;

	public Simulation(JSONObject jsonObject) {
		name = jsonObject.getString("name");
		location = jsonObject.getString("location");
		date = jsonObject.getString("start_date");
		duration = jsonObject.getString("duration_m");
		latS = jsonObject.getString("latS");
		lonS = jsonObject.getString("lonS");
		latE = jsonObject.getString("latE");
		lonE = jsonObject.getString("lonE");
		Log.d("debugg", "json create " + name);


	}

	public String getName() {
		return name;
	}

	public String getLocation() {
		return location;
	}

	public String getDate() {

		String[] d = date.split(" ");
		String[] da = d[0].split("-");
		String[] time = d[1].split(":");
		return da[1] + "/" + da[2] + " at " + time[0] + ":" + time[1];
	}

	public String getDuration() {
		return duration;
	}

	@Override
	public String toString() {
		return name + ", " + location + " " + getDate() + " for " + duration
				+ "min";
	}

	/**
	 * Start tile provider and download map tiles
	 * 
	 * @param c
	 */
	public void activate(Context c) {
		final double f_latS = Double.parseDouble(latS);
		final double f_lonS = Double.parseDouble(lonS);
		final double f_latE = Double.parseDouble(latE);
		final double f_lonE = Double.parseDouble(lonE);
		final TilesProvider tp = new TilesProvider(DemoActivity.PATH);
		tp.downloadTilesInBound(f_latS, f_lonE, f_latE, f_lonS, MIN_ZOOM,
				MAX_ZOOM, c);
	}

	/**
	 * Download map tiles
	 * 
	 * @param c
	 */
	public static void preDownloadTiles(double f_latS, double f_lonS,
			double f_latE, double f_lonE, Context c) {

		final TilesProvider tp = new TilesProvider(DemoActivity.PATH);
		Log.d("gcm", "downloading " + f_latS + " " + f_lonS + " " + f_latE
				+ " " + f_lonE);

		tp.downloadTilesInBound(f_latS, f_lonE, f_latE, f_lonS, MIN_ZOOM,
				MAX_ZOOM, c);
	}
	
	
	/**
	 * Registers the simulation name in the content provider
	 * @param value
	 */
	public  static void regSimulationContentProvider(String name, String date, String duration, String local, Context context) {
		ContentValues cv = new ContentValues();
		cv.put(MessagesProvider.COL_SIMUKEY, "simulation");
		cv.put(MessagesProvider.COL_SIMUVALUE, name);
		cv.put(MessagesProvider.COL_SIMU_DATE, date);
		cv.put(MessagesProvider.COL_SIMU_DURATION, duration);
		cv.put(MessagesProvider.COL_SIMU_LOCAL, local);

		context.getContentResolver()
				.insert(MessagesProvider.URI_SIMULATION, cv);

	}

}
