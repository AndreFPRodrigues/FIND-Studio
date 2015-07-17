package find.service.net.diogomarques.wifioppish;

import java.util.Random;

import find.service.R;
import find.service.gcm.DemoActivity;
import find.service.gcm.RequestServer;
import find.service.gcm.SplashScreen;
import find.service.net.diogomarques.wifioppish.IEnvironment.State;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Android-specific domain parameters.
 * 
 * @author Diogo Marques <diogohomemmarques@gmail.com>
 */
public class AndroidPreferences implements IDomainPreferences {

	// FIXME switch before deployment
	public final static boolean DEBUG = false;
	// public static boolean INTERNET_AVAILABLE= true;

	public static boolean apAvailable = true;

	/*
	 * Universal timeout parameter for use in debugging.
	 */
	int debugMinTimeMilis = 1000 * 30;

	// Dependencies
	private Context mContext;

	/**
	 * Constructor.
	 * 
	 * @param ctx
	 *            the current context.
	 */
	public AndroidPreferences(Context ctx) {
		mContext = ctx;
	}

	/**
	 * Gets the Android context
	 * 
	 * @return Android Context
	 */
	protected Context getContext() {
		return mContext;
	}

	@Override
	public int getPort() {
		return 33333;
	}

	@Override
	public int getTBeac() {
		return getRandomTimeFromKey(R.string.key_t_beac);
	}

	@Override
	public int getTPro() {
		return getRandomTimeFromKey(R.string.key_t_pro);
	}

	@Override
	public int getTScan() {
		return getRandomTimeFromKey(R.string.key_t_scan);
	}

	@Override
	public int getTCon() {
		return getRandomTimeFromKey(R.string.key_t_con);
	}

	@Override
	public int getTInt() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		String key = mContext.getString(R.string.key_t_int);
		int minTime = Integer.parseInt(prefs.getString(key, null));
		// TODO was 2* minTime
		int dif = minTime;

		return (int) (new Random().nextDouble() * dif + minTime);
	}

	/**
	 * Returns a random uniform value between a minimum (set in the default
	 * {@link SharedPreferences} with key <i>resId</i>) and a maximum (set at 3
	 * the times the minimum).
	 * <p>
	 * <blockquote>"The respective maximum times are always 3 times the min
	 * times." (p. 39)<footer>Trifunovic et al, 2011, <a
	 * href="http://dl.acm.org/citation.cfm?id=2030664"
	 * >PDF</a></footer></blockquote>
	 * 
	 * @param resId
	 *            the resource identifier for they key in
	 *            {@link SharedPreferences}
	 * @return a randomly distributed value between
	 */
	protected int getRandomTimeFromKey(int resId) {
		if (DEBUG)
			return debugMinTimeMilis; // + (new Random().nextInt(10) *1000);

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		String key = mContext.getString(resId);
		int minTime = Integer.parseInt(prefs.getString(key, null));
		// TODO was 2* minTime
		int dif = minTime;
		return (int) (new Random().nextDouble() * dif + minTime);
	}

	@Override
	public int getScanPeriod() {
		return 30000;
	}

	@Override
	public String getWifiSSID() {
		return "emergencyAP";
	}

	@Override
	public String getWifiPassword() {
		return "emergency";
	}

	@Override
	public State getStartState() {
		return IEnvironment.State.Scanning;
	}

	@Override
	public boolean checkInternetMode() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		boolean internetState = prefs.getBoolean("internet", true);
		// FIXME Before deployinmentwas 2* minTime

		return internetState;
	}

	@Override
	public String getApiEndpoint() {
		SharedPreferences prefs = mContext.getSharedPreferences(
				DemoActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		String address = prefs.getString(RequestServer.SERVER, null);

		return address;
	}

	@Override
	public int getSendPeriod() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		String key = mContext.getString(R.string.key_t_freq);
		int value = Integer.parseInt(prefs.getString(key, "2000"));
		return value;
	}

	@Override
	public int getTWeb() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		String key = mContext.getString(R.string.key_t_web);
		int value = Integer.parseInt(prefs.getString(key, "5000"));
		return value;
	}

	@Override
	public String getNodeId() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		String key = mContext.getString(R.string.key_t_nodeid);
		String nodeid = prefs.getString(key, null);

		// generate if invalid
		if (nodeid == null || nodeid.equals("")) {
			nodeid = NodeIdentification.getMyNodeId(mContext);
			Editor prefEditor = prefs.edit();
			prefEditor.putString(key, nodeid);
			prefEditor.commit();
		}

		return nodeid;
	}

	@Override
	public String getAccountName() {
		SharedPreferences prefs = mContext.getSharedPreferences(
				DemoActivity.class.getSimpleName(), Context.MODE_PRIVATE);

		String key = SplashScreen.PROPERTY_ACCOUNT;
		String accountName = prefs.getString(key, "Unknown");
		return accountName;
	}

	// check if should run only locally 
	//without connecting to the internet
	@Override
	public boolean isRunningLocally() {
		SharedPreferences prefs = mContext.getSharedPreferences(
				DemoActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		String mode = prefs.getString(RequestServer.MODE, "LOCAL");
		Log.d("gcm", "Mode: " +mode);

		if(mode.equals("LOCAL"))
			return true;
		else{
			return false;
		}
	}
}
