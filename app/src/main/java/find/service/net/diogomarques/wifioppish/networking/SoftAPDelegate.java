package find.service.net.diogomarques.wifioppish.networking;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import find.service.net.diogomarques.wifioppish.AndroidNetworkingFacade;
import find.service.net.diogomarques.wifioppish.AndroidPreferences;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

public class SoftAPDelegate {

	private static final String TAG = SoftAPDelegate.class.getSimpleName();

	/**
	 * The original softAP configuration, saved to restore after execution
	 */
	private WifiConfiguration fOriginalApConfiguration;

	/**
	 * The original wi-fi state, saved to restore after execution
	 */
	private boolean fOriginalIsWifiEnabled; // true if enabled

	/* Dependencies */
	private final Context mContext;

	public SoftAPDelegate(Context context) {
		mContext = context;
	}

	private void saveApConfiguration() {
		WifiManager manager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		fOriginalIsWifiEnabled = manager.isWifiEnabled();
		try {
			Method mGetWifiApConfiguration = manager.getClass().getMethod(
					"getWifiApConfiguration");
			// Save this configuration for later
			fOriginalApConfiguration = (WifiConfiguration) mGetWifiApConfiguration
					.invoke(manager, (Object[]) null);

		} catch (SecurityException e) {
			e.printStackTrace();
			AndroidPreferences.apAvailable = false;
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			AndroidPreferences.apAvailable = false;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			AndroidPreferences.apAvailable = false;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			AndroidPreferences.apAvailable = false;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			AndroidPreferences.apAvailable = false;
		}
	}

	/*
	 * public boolean getWifiApState(WifiManager mWifiManager) { Method method;
	 * try { method =
	 * mWifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
	 * method.setAccessible(true); //in the case of visibility change in future
	 * APIs return (Boolean) method.invoke(mWifiManager); } catch
	 * (NoSuchMethodException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); } catch (IllegalArgumentException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); } catch
	 * (IllegalAccessException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); } catch (InvocationTargetException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); } return false;
	 * 
	 * 
	 * }
	 */

	private void setSoftAPEnabled(WifiConfiguration cfg, boolean enable) {
		WifiManager manager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);

		// Log.d(TAG, "APPP 1: " + getWifiApState(manager) + " " + enable);

		if (enable == true)
			manager.setWifiEnabled(false); // Stop wi-fi station mode
		try {
			// Log.d(TAG, "APPP 2: " + getWifiApState(manager)+ " " + enable);

			Method mSetWifiApEnabled = manager.getClass().getMethod(
					"setWifiApEnabled", WifiConfiguration.class, boolean.class);
			boolean verify = (Boolean) mSetWifiApEnabled.invoke(manager, cfg,
					enable);
			Log.d(TAG, "Appp:" + verify);

		} catch (SecurityException e) {
			AndroidPreferences.apAvailable = false;
		} catch (NoSuchMethodException e) {
			Log.e(TAG, e.getMessage(), e);
			AndroidPreferences.apAvailable = false;
		} catch (IllegalArgumentException e) {
			Log.e(TAG, e.getMessage(), e);
			AndroidPreferences.apAvailable = false;
		} catch (IllegalAccessException e) {
			AndroidPreferences.apAvailable = false;
		} catch (InvocationTargetException e) {
			Log.e(TAG, e.getMessage(), e);
			AndroidPreferences.apAvailable = false;
		}
		// Log.d(TAG, "APPP 3: " + getWifiApState(manager) + " " + enable);

	}

	private void restoreWifiState(
			AndroidNetworkingFacade androidNetworkingFacade) {
		WifiManager manager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		try {
			// Disable emergency access point
			setSoftAPEnabled(
					androidNetworkingFacade.getWifiSoftAPConfiguration(), false);
			setSoftAPEnabled(fOriginalApConfiguration, false); // hack
			Method mSetWifiApConfiguration = manager.getClass().getMethod(
					"setWifiApConfiguration", WifiConfiguration.class);
			mSetWifiApConfiguration.invoke(manager, fOriginalApConfiguration);

			if (fOriginalIsWifiEnabled)
				manager.setWifiEnabled(true);
		} catch (SecurityException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (IllegalAccessException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (InvocationTargetException e) {
			Log.e(TAG, e.getMessage(), e);
		} catch (NoSuchMethodException e) {
			Log.e(TAG, e.getMessage(), e);
		}

		// Re-enable wi-fi if it was originally enabled
		if (fOriginalIsWifiEnabled)
			manager.setWifiEnabled(true);

	}

	public void startWifiAP(AndroidNetworkingFacade androidNetworkingFacade) {
		saveApConfiguration();
		setSoftAPEnabled(androidNetworkingFacade.getWifiSoftAPConfiguration(),
				true);
	}

	public void stopWifiAP(AndroidNetworkingFacade androidNetworkingFacade) {
		restoreWifiState(androidNetworkingFacade);
	}

}
