package find.service.gcm;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import find.service.R;
import find.service.gcm.map.DownloadFile;
import find.service.net.diogomarques.wifioppish.NodeIdentification;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class SplashScreen extends Activity {

	public static final String EXTRA_MESSAGE = "message";
	public static final String PROPERTY_REG_ID = "registration_id";
	public static final String PROPERTY_ACCOUNT = "google_account";
	private static final String PROPERTY_APP_VERSION = "appVersion";

	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	private static final int REQUEST_CODE_EMAIL = 1;

	/**
	 * Tag used on log messages.
	 */
	static final String TAG = "gcm";

	private GoogleCloudMessaging gcm;
	private String regid;
	private String address;
	private Context context;
	private TextView connectionDetails;
	private TextView registrationDetails;
	private String account;
	/**
	 * Substitute you own sender ID here. This is the project number you got
	 * from the API Console, as described in "Getting Started."
	 */
	String SENDER_ID = "253078140647";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		context = getApplicationContext();
		setContentView(R.layout.activity_splash);

		connectionDetails = (TextView) findViewById(R.id.connectionDetails);
		registrationDetails = (TextView) findViewById(R.id.registerDetails);

		WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = manager.getConnectionInfo();

		// gets mac_address (user identification)
		address = info.getMacAddress();
		address = NodeIdentification.getNodeId(address);

		// check if registered
		if (checkIfRegistered()) {
			final SharedPreferences prefs = getGcmPreferences(context);
			RequestServer.endpoint=prefs.getString(RequestServer.SERVER, "");
			
			// if registered send again to server and go to demoAcitivity
			Log.d(TAG, regid);
			Log.d(TAG, "endpoint:" + RequestServer.endpoint);

			
			// RequestServer.register(address, regid, account);
			RequestServer.uploadLogFile(address);
			

			Intent i = new Intent(SplashScreen.this, DemoActivity.class);
			startActivity(i);

			// close this activity
			finish();

		} else
			try {
				Intent intent = AccountPicker.newChooseAccountIntent(null,
						null,
						new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE },
						false, null, null, null, null);
				startActivityForResult(intent, REQUEST_CODE_EMAIL);
			} catch (ActivityNotFoundException e) {
				account = "noID";
				iterateHandler();
			}

	}

	private void iterateHandler() {
		new Handler().postDelayed(new Runnable() {

			/*
			 * Showing splash screen with a timer. This will be useful when you
			 * want to show case your app logo / company
			 */

			@Override
			public void run() {

				// checks if there is internet connection
				if (RequestServer.netCheckin(context)) {

					connectionDetails.setText("Network Found!");

					// Checks if the BD responsible for the tiles exits, if not
					// download the file from the server
					// set the tile provider and database
					File bd = new File(Environment
							.getExternalStorageDirectory().toString()
							+ "/mapapp/world.sqlitedb");

					if (!bd.exists()) {
						DownloadFile.downloadTileDB();
					}

					WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
					WifiInfo info = manager.getConnectionInfo();

					// gets mac_address (user identification)
					address = info.getMacAddress();
					address = NodeIdentification.getNodeId(address);

					// has network, register and go to demoActivity

					registerInBackground();

				} else {
					connectionDetails.setText("No Network Found!");
					iterateHandler();
				}
			}
		}, 1000);

	}

	private boolean checkIfRegistered() {
		// Check device for Play Services APK. If check succeeds, proceed
		// with
		// GCM registration and get active simulations.
		if (checkPlayServices()) {
			gcm = GoogleCloudMessaging.getInstance(context);
			regid = getRegistrationId(context);
			Log.d(TAG, regid);

			if (regid.isEmpty())
				return false;

			else
				return true;

		} else {
			Log.i(TAG, "No valid Google Play Services APK found.");
			return false;
		}

	}

	private void sendToActivity() {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				Intent i = new Intent(SplashScreen.this, DemoActivity.class);
				startActivity(i);

				// close this activity
				finish();
			}
		}, 1000);
	}

	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and the app versionCode in the application's
	 * shared preferences.
	 */
	private void registerInBackground() {
		new AsyncTask<Void, Void, String>() {

			private boolean success;

			@Override
			protected String doInBackground(Void... params) {
				String msg = "";
				Log.d(TAG, "Registering in the background");

				try {
					if (gcm == null) {
						gcm = GoogleCloudMessaging.getInstance(context);
					}
					if (regid.isEmpty()) {
						Log.d(TAG, "REGID is empty");

						regid = gcm.register(SENDER_ID);
					}

					msg = "Device registered, registration ID=" + regid
							+ " add:" + address + " ac:" + account;
					Log.d(TAG, msg);
					
					String locale = context.getResources().getConfiguration().locale.getCountry();
					
					SharedPreferences prefs = getGcmPreferences(context);
					RequestServer.register(locale, address, regid, account,prefs);
					
				
					
					// Persist the regID and account - no need to register again.
					storeRegistrationDetails(context, regid, account);

					success = true;
				} catch (IOException ex) {
					msg = "Error :" + ex.getMessage();
					// If there is an error, don't just keep trying to register.
					// Require the user to click a button again, or perform
					// exponential back-off.
					registerInBackground();
				}
				return msg;
			}

			@Override
			protected void onPostExecute(String result) {
				if (success) {
					sendToActivity();
				}
			}

		}.execute(null, null, null);
	}

	/**
	 * Stores the registration ID, the google account name and the app
	 * versionCode in the application's {@code SharedPreferences}.
	 * 
	 * @param context
	 *            application's context.
	 * @param regId
	 *            registration ID
	 * @param account
	 *            google account name
	 */
	private void storeRegistrationDetails(Context context, String regId, String account) {
		final SharedPreferences prefs = getGcmPreferences(context);
		int appVersion = getAppVersion(context);
		Log.i(TAG, "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putString(PROPERTY_ACCOUNT, account);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}

	/**
	 * Check the device to make sure it has the Google Play Services APK. If it
	 * doesn't, display a dialog that allows users to download the APK from the
	 * Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i(TAG, "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}

	/**
	 * Gets the current registration ID for application on GCM service, if there
	 * is one.
	 * <p>
	 * If result is empty, the app needs to register.
	 * 
	 * @return registration ID, or empty string if there is no existing
	 *         registration ID.
	 */
	private String getRegistrationId(Context context) {
		final SharedPreferences prefs = getGcmPreferences(context);
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId.isEmpty()) {
			Log.i(TAG, "Registration not found.");
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION,
				Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			Log.i(TAG, "App version changed.");
			return "";
		}
		return registrationId;
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	private SharedPreferences getGcmPreferences(Context context) {
		return getSharedPreferences(DemoActivity.class.getSimpleName(),
				Context.MODE_PRIVATE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_EMAIL && resultCode == RESULT_OK) {
			String accountName = data
					.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			account = accountName;
			Log.d("gcm", account);

		} else {
			account = "noID";

		}
		iterateHandler();	
	}

}