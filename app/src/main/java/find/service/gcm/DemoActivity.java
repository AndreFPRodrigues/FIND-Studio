package find.service.gcm;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import find.service.R;
import find.service.gcm.map.DownloadFile;
import find.service.net.diogomarques.wifioppish.NodeIdentification;
import find.service.net.diogomarques.wifioppish.service.LOSTService;
import find.service.org.json.JSONArray;
import find.service.org.json.JSONObject;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Main UI for the demo app.
 */
public class DemoActivity extends Activity {

	public static final String EXTRA_MESSAGE = "message";
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	private final int DISASSOCIATE_THRESHOLD = 1000 * 60 * 3;

	/**
	 * Substitute you own sender ID here. This is the project number you got
	 * from the API Console, as described in "Getting Started."
	 */
	String SENDER_ID = "253078140647";

	/**
	 * Tag used on log messages.
	 */
	static final String TAG = "gcm";

	private GoogleCloudMessaging gcm;
	private AtomicInteger msgId = new AtomicInteger();
	private Context context;

	private String regid;
	private Simulation[] activeSimulations;
	private int indexSimu;
	private String address;
	private String registeredSimulation;
	private String location;
	private String date;
	private String duration;

	private Handler ui;
	private Button associate;
	private boolean state_associated;
	private Button serviceActivate;
	private TextView test;
	// private CheckBox storage;
	private RadioGroup associationStatus;

	int associationState;
	int allowStorage;

	private final int MANUAL = 0;
	private final int AUTO = 1;
	private final int POP_UP = 2;

	final static String PATH = Environment.getExternalStorageDirectory()
			+ "/mapapp/world.sqlitedb";;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LOSTService.saveLogCat("create");

		Log.d(TAG, "started demoactivity");
		
		context = getApplicationContext();
		setContentView(R.layout.service_main);
		associate = (Button) findViewById(R.id.associate);
		associationStatus = (RadioGroup) findViewById(R.id.radioGroup1);
		state_associated = false;
		test = (TextView) findViewById(R.id.with);
		serviceActivate = (Button) findViewById(R.id.sservice);
		indexSimu = -1;
		final SharedPreferences prefs = getSharedPreferences(
				DemoActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		regid = prefs.getString(SplashScreen.PROPERTY_REG_ID, "");
		
		//onStartUp();
		
		final SharedPreferences preferences = getApplicationContext()
				.getSharedPreferences("Lost",
						android.content.Context.MODE_PRIVATE);

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		onStartUp();
	}
	
	private void onStartUp() {
		//check the dps permissions
		if (!((LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE))
				.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			showGPSDisabledAlertToUser();
		} else {
			// gps is enabled
		}

		// Check if the service is stopping and blocks interface
		if (LOSTService.toStop) {
			test.setText(R.string.syncService);
			test.setVisibility(View.VISIBLE);
			associate.setVisibility(View.GONE);
			associationStatus.setEnabled(false);
			((RadioButton) findViewById(R.id.manual)).setEnabled(false);
			((RadioButton) findViewById(R.id.pop)).setEnabled(false);
			serviceActivate.setText("Stopping Service");
			serviceActivate.setEnabled(false);
			return;
		}

		// Check service state and changes the button text
		if (LOSTService.serviceActive) {
			onServiceRunning();
			return;
		}

		// checks if there is internet connection
		if (!RequestServer.netCheckin(context)) {

			// Blocks all the association and service preferences, only allows
			// the user to start/stop the service
			test.setText("FIND Service requires internet connection to alter preferences "
					+ "please connect via WIFI and restart the application");
			Toast.makeText(getApplicationContext(),
					"FIND Service Preferences requires internet connection",
					Toast.LENGTH_LONG).show();
			Toast.makeText(getApplicationContext(),
					"Connect via WIFI and restart the application",
					Toast.LENGTH_LONG).show();
			associate.setEnabled(false);
			associationStatus.setEnabled(false);
			((RadioButton) findViewById(R.id.manual)).setEnabled(false);
			((RadioButton) findViewById(R.id.pop)).setEnabled(false);

		} else {

			// Checks if the BD responsible for the tiles exits, if not download
			// the file from the server
			// set the tile provider and database
			File bd = new File(Environment.getExternalStorageDirectory()
					.toString() + "/mapapp/world.sqlitedb");

			if (!bd.exists()) {
				DownloadFile.downloadTileDB();
			}

			final SharedPreferences preferences = getApplicationContext()
					.getSharedPreferences("Lost",
							android.content.Context.MODE_PRIVATE);
			int idRadioButton = preferences.getInt("associationState", 2);
			ui = new Handler();

			WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			WifiInfo info = manager.getConnectionInfo();

			// gets mac_address (user identification)
			address = info.getMacAddress();
			address = NodeIdentification.getNodeId(address);


			// registers user for simulation if intent equals
			// "registerParticipant"
			/*
			 * Intent intent = getIntent(); String action = intent.getAction();
			 * if (action != null && action.equals("registerParticipant")) {
			 * RequestServer.registerForSimulation(
			 * intent.getStringExtra("name"), regid, address); Log.d("debugg",
			 * "Register for simulation 0"); }
			 */

			// populates the active simulations window
			getActiveSimulations();

		}
	}

	
	private boolean checkAssociationLocal() {

		String URL = "content://find.service.net.diogomarques.wifioppish.MessagesProvider/simulation";
		Uri uri = Uri.parse(URL);
		Cursor c = getContentResolver().query(uri, null, "", null, "");

		if (c.moveToFirst()) {
			do {
				if (c.getString(c.getColumnIndex("simukey")).equals(
						"simulation")) {
					registeredSimulation = c.getString(c
							.getColumnIndex("simuvalue"));
					date = c.getString(c.getColumnIndex("simudate"));
					duration = c.getString(c.getColumnIndex("simuduration"));
					location = c.getString(c.getColumnIndex("simulocal"));

				}
			} while (c.moveToNext());
		}
		c.close();

		if (registeredSimulation != null && registeredSimulation.length() > 0) {
			ui.post(new Runnable() {
				public void run() {
					Log.d("gcm", "Associado a " + registeredSimulation);
					test.setText(registeredSimulation + ", " + location
							+ " at " + date + " for " + duration + "min");
					test.setVisibility(View.VISIBLE);
					associate.setText(R.string.disassociate);
					associate.setEnabled(true);
					state_associated = true;
				}
			});
			return true;
		} else {
			if (activeSimulations.length > 0)
				ui.post(new Runnable() {
					public void run() {
						associate.setEnabled(true);
						associate.setText(R.string.associate);
					}
				});
			return false;
		}
		// Log.d(TAG, "simu value:" + registeredSimulation + " " + date + " " +
		// duration
		// + " " + location);
	}

	/**
	 * Check if there the user is associated with a simulation
	 */
	private void checkAssociation() {
		if (!checkAssociationLocal()) {
			if (activeSimulations.length == 0) {
				ui.post(new Runnable() {
					public void run() {
						test.setText(R.string.noSimulations);
						state_associated = false;
					}
				});
			}
		}

	}

	/**
	 * Populate the list of active simulations
	 */
	private void getActiveSimulations() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				StringBuilder builder = new StringBuilder();
				HttpClient client = new DefaultHttpClient();
				HttpGet httpGet;

				httpGet = new HttpGet(
						"http://accessible-serv.lasige.di.fc.ul.pt/~lost/index.php/rest/simulations");

				try {
					HttpResponse response = client.execute(httpGet);
					StatusLine statusLine = response.getStatusLine();
					int statusCode = statusLine.getStatusCode();
					if (statusCode == 200) {
						HttpEntity entity = response.getEntity();
						InputStream content = entity.getContent();
						BufferedReader reader = new BufferedReader(
								new InputStreamReader(content));
						String line;
						while ((line = reader.readLine()) != null) {
							builder.append(line);
						}

						String simulations = builder.toString();
						Log.d(TAG, "active simulations json: " + simulations);

						JSONArray jsonArray = new JSONArray(simulations);

						activeSimulations = new Simulation[jsonArray.length()];
						for (int i = 0; i < jsonArray.length(); i++) {
							JSONObject jsonObject = jsonArray.getJSONObject(i);
							activeSimulations[i] = new Simulation(jsonObject);
						}

						checkAssociation();
					} else {
						Log.e(TAG, "Failed to download file");
					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				return null;
			}
		}.execute(null, null, null);

	}

	/**
	 * Starts/Stops the service Handles the onclick of the Start/Stop Button
	 * 
	 * @param view
	 */
	public void activateService(final View view) {
		if (LOSTService.serviceActive) {
			stop();
		} else {
			Intent svcIntent = new Intent(
					"find.service.net.diogomarques.wifioppish.service.LOSTService.START_SERVICE");
			context.startService(svcIntent);
			onServiceRunning();

		}
	}

	/**
	 * Initiates the stopping mechanism
	 */
	private void stop() {
		serviceActivate.setText("Stopping service");
		test.setText("Waiting for internet connection to sync files");
		associate.setEnabled(false);
		state_associated = false;
		associationStatus.setEnabled(false);
		((RadioButton) findViewById(R.id.manual)).setEnabled(false);
		((RadioButton) findViewById(R.id.pop)).setEnabled(false);
		serviceActivate.setEnabled(false);

		// if the starting date of the current association is in less than 3
		// minutes and we stop the service we unregister
		// todo verify date

		if (date == null||date==""
				|| DateFunctions.timeToDate(date.replace("-", "/")) < DISASSOCIATE_THRESHOLD) {
			Simulation.regSimulationContentProvider("", "", "", "", context);
		}
		LOSTService.stop(context);

	}

	

	/**
	 * Dissassociate from the current simulation/alert
	 */
	/*public void disassociate() {
		Simulation.regSimulationContentProvider("", "", "", "", context);

		ui.post(new Runnable() {
			public void run() {
				// Log.d("gcm", registeredSimulation);
				test.setVisibility(View.GONE);
				associate.setText(R.string.associate);
				ScheduleService.cancelAlarm(context);
			}
		});
		state_associated = false;

	}*/


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    //MenuInflater inflater = getMenuInflater();
	    //inflater.inflate(R.menu.downloadmenu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.down_message:
	        	//RequestServer.downloadAPK(this,"message.apk");
	            return true;
	        case R.id.down_rescue:
	        	//RequestServer.downloadAPK(this,"rescuer.apk");

	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void showGPSDisabledAlertToUser() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder
				.setMessage(
						"GPS is disabled in your device. Would you like to enable it?")
				.setCancelable(false)
				.setPositiveButton("Goto Settings Page To Enable GPS",

				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Intent callGPSSettingIntent = new Intent(
								android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						startActivity(callGPSSettingIntent);
					}
				});
		// alertDialogBuilder.setNegativeButton("Cancel",
		// new DialogInterface.OnClickListener() {
		// public void onClick(DialogInterface dialog, int id) {
		// dialog.cancel();
		// }
		// });
		AlertDialog alert = alertDialogBuilder.create();
		alert.show();
	}

	private void onServiceRunning() {
		serviceActivate.setText("Stop Service");
		test.setText("Service running");
		test.setVisibility(View.VISIBLE);
		associate.setEnabled(false);
		associationStatus.setEnabled(false);
		((RadioButton) findViewById(R.id.manual)).setEnabled(false);
		((RadioButton) findViewById(R.id.pop)).setEnabled(false);
	}

}