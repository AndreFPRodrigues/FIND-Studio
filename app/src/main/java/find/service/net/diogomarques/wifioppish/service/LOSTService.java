package find.service.net.diogomarques.wifioppish.service;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import find.service.R;
import find.service.gcm.DemoActivity;
import find.service.net.diogomarques.wifioppish.AndroidEnvironment;
import find.service.net.diogomarques.wifioppish.AndroidPreferences;
import find.service.net.diogomarques.wifioppish.IEnvironment;
import find.service.net.diogomarques.wifioppish.MessagesGenerator;
import find.service.net.diogomarques.wifioppish.MessagesProvider;
import find.service.net.diogomarques.wifioppish.IEnvironment.State;
import find.service.net.diogomarques.wifioppish.MyPreferenceActivity;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Button;

/**
 * Represents the service that runs on foreground. It uses the LOST-OppNet
 * business logic to create an opportunistic network and exchange messages. To
 * start the service, an {@link Intent} must be created with the action
 * <tt>net.diogomarques.wifioppish.service.LOSTService.START_SERVICE</tt>,
 * followed by a call to {@link Activity#startService(Intent)}. Example:
 * 
 * <pre>
 * {
 * 	&#064;code
 * 	Intent i = new Intent(
 * 			&quot;net.diogomarques.wifioppish.service.LOSTService.START_SERVICE&quot;);
 * 	startService(i);
 * }
 * </pre>
 * 
 * This service also creates a {@link Notification} to ensure the service
 * remains active event the system is low on resources.
 * 
 * @author André Silva <asilva@lasige.di.fc.ul.pt>
 */
public class LOSTService extends Service {

	private final int NOTIFICATION_STICKY = 1;
	public final static String TAG = "LOST Service";
	private NotificationManager notificationManager;
	private static IEnvironment environment;
	private static MessagesGenerator msg_gen;
	public static boolean serviceActive = false;
	public static boolean toStop = false;
	public static boolean synced = false;
	private static boolean isLogging = false;
	private static final int MESSAGE_RATE = 30000;

	private static LocalBroadcastManager buttonBroadcaster;

	@Override
	public void onCreate() {
		super.onCreate();
		buttonBroadcaster = LocalBroadcastManager.getInstance(this);
		Log.i(TAG, "Service created");
	}

	@Override
	public void onDestroy() {

		if (environment != null) {
			Log.i(TAG, "Stopped looped");

			environment.stopStateLoop();
			environment = null;
			serviceActive = false;
		}
		stopSelf();
		Log.i(TAG, "Service destroyed");
		super.onDestroy();

	}

	public static void stop(Context context) {

		if (!toStop && serviceActive) {

			LOSTService.toStop = true;
			Log.d(TAG, "Syncing service");
			// saveLogCat("stop");
			// indicate that service is now stopped connected
			ContentValues cv = new ContentValues();
			cv.put(MessagesProvider.COL_STATUSKEY, "service");
			cv.put(MessagesProvider.COL_STATUSVALUE, "Stopping");
			context.getContentResolver()
					.insert(MessagesProvider.URI_STATUS, cv);

			if (environment != null) {
				msg_gen.stopAutoGeneration();
				serviceActive = false;
				environment.stopStateLoop();
			}
		}
		return;
	}

	public static void terminate(Context context) {
		LOSTService.toStop = false;
		LOSTService.serviceActive = false;
		LOSTService.synced = false;

		Intent svcIntent = new Intent(
				"find.service.net.diogomarques.wifioppish.service.LOSTService.START_SERVICE");

		// context.deleteDatabase("LOSTMessages");
		ContentResolver cr = environment.getAndroidContext()
				.getContentResolver();
		cr.delete(
				Uri.parse("content://find.service.net.diogomarques.wifioppish.MessagesProvider/truncate"),
				"", null);

		ContentValues cv = new ContentValues();
		cv.put(MessagesProvider.COL_STATUSKEY, "service");
		cv.put(MessagesProvider.COL_STATUSVALUE, "Disabled");
		context.getContentResolver().insert(MessagesProvider.URI_STATUS, cv);
		boolean forceExit = environment.getPreferences().isRunningLocally();
		context.stopService(svcIntent);

		// send a broadcast to change the button state
		Intent buttonIntent = new Intent("changeButtonState");
		buttonIntent.putExtra("state", true);
		buttonBroadcaster.sendBroadcast(buttonIntent);

		Log.d(TAG, "Correctly synced and terminated service");
		if (forceExit)
			System.exit(0);

	}

	/**
	 * Starts the business logic to create an opportunistic network
	 */
	private void processStart() {
		// send a broadcast to change the button state
		Intent buttonIntent = new Intent("changeButtonState");
		buttonIntent.putExtra("state", false);
		buttonBroadcaster.sendBroadcast(buttonIntent);

		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {

				environment.startStateLoop(State.Scanning);

				return null;
			}

		}.execute();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "About to start service");

		if (environment == null) {
			Log.i(TAG, "Creating new instance");
			serviceActive = true;
			LOSTService.saveLogCat("service start");

			// populate default preferences that may be missing
			PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

			environment = AndroidEnvironment.createInstance(this);

			msg_gen = MessagesGenerator.sharedInstance();
			msg_gen.initialize(environment);
			msg_gen.startAutoGeneration(MESSAGE_RATE);

			startForeground(NOTIFICATION_STICKY,
					getNotification("The FIND Service is now running"));
			processStart();
		}

		return Service.START_STICKY;
	}

	/**
	 * Creates a notification telling that the LOST Service is running. This
	 * notification is important to ensure the service keeps running and doesn't
	 * killed by Android system when the system is low on resources.
	 * 
	 * @param contentText
	 * 
	 * @return {@link Notification} instance, with default values to tell
	 *         service is running
	 */
	@SuppressWarnings("deprecation")
	private Notification getNotification(String contentText) {
		Log.i(TAG, "Get notification");

		if (notificationManager == null)
			notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		CharSequence contentTitle = "FIND Service";
		notificationManager.cancelAll();
		// Although deprecated, this code ensures compatibility with older
		// Android versions
		Notification note = new Notification(R.drawable.service_logo,
				contentTitle, 0);
		note.flags |= Notification.FLAG_NO_CLEAR;
		note.flags |= Notification.FLAG_FOREGROUND_SERVICE;

		PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(
				this, MyPreferenceActivity.class), 0);

		note.setLatestEventInfo(this, contentTitle, contentText, intent);
		return note;
	}

	public static void saveLogCat(String from) {
		if (!isLogging) {
			isLogging = true;

			Log.d("logcat",
					"-------------------Logging----------------------------"
							+ from);
			String filePath = Environment.getExternalStorageDirectory()
					+ "/logcat_FIND.txt";

			try {

				Runtime.getRuntime().exec(
						new String[] { "logcat", "-f", filePath, "-v", "time",
								"dalvikvm:S *:V" });
				// Runtime.getRuntime().exec(new String[] { "logcat", "-c" });

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static String getCurrentState() {
		return environment == null ? "None" : environment.getCurrentState().toString();
	}

}
