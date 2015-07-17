package find.service.net.diogomarques.wifioppish;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpStatus;

import find.service.gcm.Notifications;
import find.service.net.diogomarques.wifioppish.INetworkingFacade.OnAccessPointScanListener;
import find.service.net.diogomarques.wifioppish.networking.Message;
import find.service.net.diogomarques.wifioppish.networking.MessageGroup;
import find.service.net.diogomarques.wifioppish.networking.SoftAPDelegate;
import find.service.net.diogomarques.wifioppish.networking.UDPDelegate;
import find.service.net.diogomarques.wifioppish.networking.WiFiDelegate;
import find.service.net.diogomarques.wifioppish.service.LOSTService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.os.Handler;
import android.util.Log;

/**
 * The Android-specific networking controller facade.
 * 
 * @author Diogo Marques <diogohomemmarques@gmail.com>
 * 
 */
public class AndroidNetworkingFacade implements INetworkingFacade {

	/*
	 * Dependencies.
	 */
	private final Context mContext;
	private final IEnvironment mEnvironment;
	private final SoftAPDelegate mSoftAP;
	private final WiFiDelegate mWiFi;
	private final UDPDelegate mUdp;

	private final String TAG = "LOST Service";

	private long lastInternetConnection;
	private long timeInScan;

	/**
	 * Static factory that creates instances of networking controllers.
	 * 
	 * @param c
	 *            the context
	 * @param env
	 *            the state machine environment
	 * @return a new instance will all dependencies set
	 */
	public static AndroidNetworkingFacade createInstance(Context c,
			IEnvironment env) {
		return new AndroidNetworkingFacade(c, env, new SoftAPDelegate(c),
				new WiFiDelegate(c, env), new UDPDelegate(c, env));

	}

	/**
	 * Convenience method to create an {@link AndroidNetworkingFacade} instance
	 * with some default values
	 * 
	 * @param context
	 *            Android context
	 * @param environment
	 *            LOST-OppNet Environment
	 * @param softAP
	 *            Software AccessPoint controller
	 * @param wiFi
	 *            WiFi controller
	 * @param udp
	 *            UDP network manager to establish connections
	 */
	private AndroidNetworkingFacade(Context context, IEnvironment environment,
			SoftAPDelegate softAP, WiFiDelegate wiFi, UDPDelegate udp) {
		this.mContext = context;
		this.mEnvironment = environment;
		this.mSoftAP = softAP;
		this.mWiFi = wiFi;
		this.mUdp = udp;
		lastInternetConnection = 0;
		timeInScan = 0;
	}

	/**
	 * Gets the current Android context
	 * 
	 * @return Android context
	 */
	protected Context getContext() {
		return mContext;
	}

	@Override
	public void startAcessPoint() {
		mSoftAP.startWifiAP(this);
	}

	@Override
	public void stopAccessPoint() {
		mUdp.releaseBroadcastSocket();
		mSoftAP.stopWifiAP(this);
	}

	/**
	 * Get the WifiConfiguration based on the SSID and password set on the
	 * domain parameters in {@link IDomainPreferences}.
	 * 
	 * @return a WifiConfiguration for a WPA access point with the SSID in
	 *         {@link IDomainPreferences#getWifiSSID()} and the password on
	 *         {@link IDomainPreferences#getWifiPassword()}.
	 */
	public WifiConfiguration getWifiSoftAPConfiguration() {
		WifiConfiguration wc = new WifiConfiguration();
		wc.SSID = mEnvironment.getPreferences().getWifiSSID();
		wc.preSharedKey = mEnvironment.getPreferences().getWifiPassword();
		wc.allowedGroupCiphers.clear();
		wc.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
		wc.allowedPairwiseCiphers.clear();
		wc.allowedProtocols.clear();
		return wc;
	}

	@Override
	public void send(Message msg, OnSendListener listener) {
		mUdp.send(msg, listener);
	}

	@Override
	public void send(MessageGroup msgs, OnSendListener listener) {
		mUdp.send(msgs, listener);
	}

	@Override
	public void receiveFirst(int timeoutMilis, OnReceiveListener listener) {
		mUdp.receiveFirst(timeoutMilis, listener);
	}

	@Override
	public void receive(int timeoutMilis, OnReceiveListener listener) {
		mUdp.receive(timeoutMilis, listener);
	}

	@Override
	public void scanForAP(int timeoutMilis,
			final OnAccessPointScanListener listener) {
		timeoutMilis = (int) (timeoutMilis - timeInScan);
		long current = System.currentTimeMillis() - lastInternetConnection;
		if (!mEnvironment.getPreferences().isRunningLocally()
				&& current > mEnvironment.getPreferences().getTInt()) {
			if (scanForInternet(listener)) {
				return;
			}
		}
		mWiFi.scanForAP(timeoutMilis, listener, this);

	}

	/**
	 * Check for internet on AP Scanning Ticks Returns true if state changed to
	 * Connected
	 */
	@Override
	public boolean scanForInternet(OnAccessPointScanListener listener) {
		long current = System.currentTimeMillis() - lastInternetConnection;
		if (current > mEnvironment.getPreferences().getTInt()
				&& isNetworkAvailable()) {
			if (ping()) {
				Log.d("Machine State", " Connected internet From Scan");
				lastInternetConnection = System.currentTimeMillis();
				listener.onInternetConnection();
				return true;
			} else {
				Log.d(TAG, " No ping internet");
			}
		}

		return false;

	}

	/**
	 * Check for internet state (used in the sync process)
	 */
	@Override
	public void scanForInternet(int timeout, OnScanInternet listener) {
		final WifiManager manager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		manager.setWifiEnabled(true);

		long current = System.currentTimeMillis() - lastInternetConnection;
		if ((current > mEnvironment.getPreferences().getTInt() || LOSTService.toStop)
				&& isNetworkAvailable()) {
			if (ping()) {
				Log.d("Machine State", " Connected internet");
				lastInternetConnection = System.currentTimeMillis();
				listener.onInternetConnection();
				return;
			} else {
				Log.d(TAG, " No ping internet");
			}
		}
		// Log.d("Machine State", " No ping internet");

		internetTicking(timeout, mEnvironment.getPreferences().getScanPeriod(),
				0, listener);

	}

	/**
	 * Ticks checking for internet
	 * 
	 * @param timeoutMilis
	 * @param delay
	 * @param totaltime
	 * @param listener
	 */
	private void internetTicking(final int timeoutMilis, final int delay,
			final long totaltime, final OnScanInternet listener) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (totaltime >= timeoutMilis) {
					Log.w("", "Internet timeout");
					listener.onScanTimeout();
				} else {
					Log.w("", "Internet tick " + delay + " " + totaltime);

					long totalTime = delay + totaltime;
					if (isNetworkAvailable()) {
						if (ping()) {
							Log.d(TAG, " Ping successfull");
							listener.onInternetConnection();
							return;
						}
						Log.d(TAG, " Network but no internet");

					} else
						Log.d(TAG, " No internet");
					if ((totaltime % delay) % 2 == 0) {
						final WifiManager manager = (WifiManager) mContext
								.getSystemService(Context.WIFI_SERVICE);
						manager.setWifiEnabled(false);
						manager.setWifiEnabled(true);

					}
					internetTicking(timeoutMilis, delay, totalTime, listener);

				}
			}
		}, delay);
	}

	/**
	 * Checks if an Internet connection is available
	 * 
	 * @return True if connection is available; false otherwise
	 */
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null
				&& activeNetworkInfo.isConnectedOrConnecting();
	}

	/**
	 * Does a network ping to a given hostname
	 * 
	 * @param url
	 *            Hostname to ping
	 * @return Ping command output
	 */
	private boolean ping() {
		HttpURLConnection urlc = null;
		try {
			URL url = new URL("http://www.google.com");
			urlc = (HttpURLConnection) url.openConnection();
			urlc.setConnectTimeout(20000);
			urlc.connect();

			if (urlc.getResponseCode() == HttpStatus.SC_OK) {
				return true;
			}

		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (urlc != null)
				urlc.disconnect();
		}
		return false;
	}

	/**
	 * Sets the Scanning state current time for timeout purposes
	 */
	@Override
	public void setTimeInScan(long timeInScan) {
		this.timeInScan = timeInScan;

	}

	/**
	 * Get time spent in scanning mode
	 */
	@Override
	public long getTimeInScan() {
		return timeInScan;
	}

}
