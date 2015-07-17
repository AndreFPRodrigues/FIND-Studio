package find.service.gcm;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class DateFunctions { 
	private final static String TAG="GCM";

	/**
	 * Get a diff between two dates
	 * 
	 * @param date1
	 *            the oldest date
	 * @param date2
	 *            the newest date
	 * @param timeUnit
	 *            the unit in which you want the diff
	 * @return the diff value, in the provided unit
	 */
	public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
		long diffInMillies = date2.getTime() - date1.getTime();
		return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
	}

	public static long timeToDate(String dtStart) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		try {
			Date date = format.parse(dtStart);
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date c_date = new Date();
			dateFormat.format(c_date);
			return DateFunctions.getDateDiff(c_date, date, TimeUnit.MILLISECONDS);

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d(TAG, " 22 "+dtStart );

		}
		return 0;
	}

	
}
