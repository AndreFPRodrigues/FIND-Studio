package find.service.net.diogomarques.wifioppish;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;

import find.service.R;
import find.service.net.diogomarques.wifioppish.INetworkingFacade.IListener;
import find.service.net.diogomarques.wifioppish.networking.Message;
import find.service.net.diogomarques.wifioppish.sensors.BatterySensor;
import find.service.net.diogomarques.wifioppish.sensors.LocationSensor;
import find.service.net.diogomarques.wifioppish.sensors.PedometerSensor;
import find.service.net.diogomarques.wifioppish.sensors.ScreenOnSensor;
import find.service.net.diogomarques.wifioppish.sensors.SensorGroup;
import find.service.net.diogomarques.wifioppish.sensors.SensorGroup.SensorGroupKey;
import find.service.net.diogomarques.wifioppish.service.LOSTService;
import find.service.net.diogomarques.wifioppish.structs.ConcurrentForwardingQueue;

/**
 * An Android-specific implementation of the state machine environment. To
 * create an instance, use {@link #createInstance(Context, Handler)}.
 * <p>
 * Since the state machine will typically run outside the UI thread, an
 * {@link Handler} is used to forward status messages obtained through
 * {@link #deliverMessage(String)};
 * 
 * @author Diogo Marques <diogohomemmarques@gmail.com>
 * 
 */
public class AndroidEnvironment implements IEnvironment {

	private final String TAG = "LOST Service";

	/* Dependencies */
	private INetworkingFacade mNetworkingFacade;
	private IDomainPreferences mPreferences;
	private StateBeaconing mBeaconing;
	private StateProviding mProviding;
	private StateScanning mScanning;
	private StateStation mStation;
	private StateInternetCheck mInternet;
	private StateInternetConn mConnected;

	private State mCurrentState;

	// last state, used to determine the next state after the internet state
	private State lastState;

	private State nextState;
	private Semaphore semNextState;
	private Context context;

	//private ConcurrentForwardingQueue mQueue;

	private String myNodeID;
	private String myAccountName;
	private boolean victimSafe;

	// stats
	private int totalReceived;
	private int totalSent;

	/* private MessageDumper dumper; */

	// duplicates prevention
	private List<Integer> duplicates;

	private SensorGroup sensorGroup;

	private CustomMessagesObserver cmo;

	// singleton
	private static AndroidEnvironment instance;

	private static IListener currentListener;

	private MessagesDAO messagesDAO;

	/**
	 * Convenience constructor for {@link #createInstance(Context, Handler)}.
	 */
	private AndroidEnvironment() {
	}

	/**
	 * Static factory that creates instances of state machine environments.
	 * 
	 * @param c
	 *            the context
	 * @param h
	 *            handler to send messages to the UI
	 * @return a new instance with all dependencies set
	 */
	public static IEnvironment createInstance(Context c) {
		if (instance != null)
			return instance;

		AndroidEnvironment environment = new AndroidEnvironment();

		// states
		StateBeaconing beaconing = new StateBeaconing(environment);
		StateProviding providing = new StateProviding(environment);
		StateScanning scanning = new StateScanning(environment);
		StateStation station = new StateStation(environment);
		StateInternetCheck internet = new StateInternetCheck(environment);
		StateInternetConn sconnect = new StateInternetConn(environment);

		INetworkingFacade networkingFacade = AndroidNetworkingFacade
				.createInstance(c, environment);
		IDomainPreferences preferences = new AndroidPreferences(c);

		// networking
		environment.mNetworkingFacade = networkingFacade;
		environment.mPreferences = preferences;
		environment.mBeaconing = beaconing;
		environment.mProviding = providing;
		environment.mScanning = scanning;
		environment.mStation = station;
		environment.mInternet = internet;
		environment.mConnected = sconnect;

		// allow one state transition (to the first one)
		environment.semNextState = new Semaphore(1);
		// allowing the gathering of shared preferences
		environment.context = c;

		// get/generate the node ID and spread the word
		environment.myNodeID = environment.mPreferences.getNodeId();

		environment.messagesDAO = new MessagesDAO(c);

		ContentValues contentvalues = new ContentValues();
		contentvalues.put(MessagesProvider.COL_STATUSKEY, "nodeid");
		contentvalues.put(MessagesProvider.COL_STATUSVALUE,
				environment.myNodeID);
		c.getContentResolver().insert(MessagesProvider.URI_STATUS,
				contentvalues);
		Log.i("NodeID", "My node id is: " + environment.myNodeID);

		// get/generate the accountName and spread the word
		environment.myAccountName = environment.mPreferences.getAccountName();
		contentvalues = new ContentValues();
		contentvalues.put(MessagesProvider.COL_STATUSKEY, "account");
		contentvalues.put(MessagesProvider.COL_STATUSVALUE,
				environment.myAccountName);
		c.getContentResolver().insert(MessagesProvider.URI_STATUS,
				contentvalues);
		Log.i("AccountName", "My account name is: " + environment.myAccountName);

		// start forwarding sending queue
		//environment.mQueue = new ConcurrentForwardingQueue();
		// stats
		environment.totalReceived = environment.totalSent = 0;
		// duplicate hashes
		environment.duplicates = new ArrayList<Integer>();

		// sensors
		environment.sensorGroup = new SensorGroup();
		environment.sensorGroup.addSensor(SensorGroupKey.Location,
				new LocationSensor(c), true);
		environment.sensorGroup.addSensor(SensorGroupKey.Battery,
				new BatterySensor(c), true);
		environment.sensorGroup.addSensor(SensorGroupKey.ScreenOn,
				new ScreenOnSensor(c), true);
		environment.sensorGroup.addSensor(SensorGroupKey.MicroMovements,
				new PedometerSensor(c), true);

		// custom client Messages
		environment.cmo = new CustomMessagesObserver(null, environment);
		environment
				.getAndroidContext()
				.getContentResolver()
				.registerContentObserver(MessagesProvider.URI_CUSTOM, true,
						environment.cmo);

		// singleton setup
		instance = environment;

		return environment;
	}

	@Override
	public State getCurrentState() {
		return mCurrentState;
	}

	@Override
	public INetworkingFacade getNetworkingFacade() {
		return mNetworkingFacade;
	}

	@Override
	public IDomainPreferences getPreferences() {
		return mPreferences;
	}

	@Override
	public void deliverMessage(String msg) {
		Log.d(LOSTService.TAG, "delivered message:" + msg);
	}

	@Override
	public void gotoState(State state) {

		if ((nextState != State.Stopped && !LOSTService.toStop)
				|| state == State.InternetConn) {
			nextState = state;
			semNextState.release();

		} else {
			nextState = State.Stopped;
			semNextState.release();
		}
	}

	@Override
	public void startStateLoop(State first) {
		nextState = first;

		// indicate that service is connected
		ContentValues contentvalues = new ContentValues();
		contentvalues.put(MessagesProvider.COL_STATUSKEY, "service");
		contentvalues.put(MessagesProvider.COL_STATUSVALUE, "Enabled");
		context.getContentResolver().insert(MessagesProvider.URI_STATUS,
				contentvalues);

		while (true) {

			try {
				semNextState.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (mCurrentState != null && mCurrentState != State.InternetCheck
					&& mCurrentState != State.InternetConn)
				lastState = mCurrentState;

			mCurrentState = nextState;
			AState next = null;
			int timeout = 0;

			// send broadcast of state change...
			Intent intent = new Intent();
			intent.setAction("stateChange");
			intent.putExtra("State", nextState.toString());
			context.sendBroadcast(intent);

			// ... and also register state update to persistent storage
			ContentValues cv = new ContentValues();
			cv.put(MessagesProvider.COL_STATUSKEY, "state");
			cv.put(MessagesProvider.COL_STATUSVALUE, nextState.toString());
			context.getContentResolver()
					.insert(MessagesProvider.URI_STATUS, cv);

			switch (nextState) {
			case Beaconing:
				next = mBeaconing;
				timeout = mPreferences.getTBeac();
				break;
			case Providing:
				next = mProviding;
				timeout = mPreferences.getTPro();
				break;
			case Scanning:
				next = mScanning;
				timeout = mPreferences.getTScan();
				break;
			case Station:
				next = mStation;
				timeout = mPreferences.getTCon();
				break;
			case InternetCheck:
				next = mInternet;
				timeout = mPreferences.getTInt();
				break;
			case InternetConn:
				next = mConnected;
				timeout = mPreferences.getTWeb();
				break;
			case Stopped:
				if (LOSTService.synced || mPreferences.isRunningLocally()) {
					LOSTService.terminate(context);
					return;
				} else {
					next = mInternet;
					timeout = mPreferences.getTInt();
				}
			}
			synchronized (next) {
				next.start(timeout, context);
			}

		}

	}

	@Override
	public void pushMessageToQueue(Message m) {
		boolean needsUpdate = false;

		// update status
		if (m.getStatus().equals(MessagesProvider.SENT)
				|| m.getStatus().equals(MessagesProvider.REC_VIC)
				|| (m.getStatus().equals(MessagesProvider.CREATED) && !m
						.getNodeId().equals(getMyNodeId()))) {

			// FIXME: change this when there is an option to choose if a user is
			// a victim or rescuer
			m.setStatus(MessagesProvider.REC_VIC, System.currentTimeMillis());
			needsUpdate = true;
		}

		// duplicate check
		Integer msgHashcode = Integer.valueOf(m.hashCode());
		if (!duplicates.contains(msgHashcode)) {
			//mQueue.add(m);
			Log.w("SendingQueue", "New message to queue: " + m
					+ " (received by " + myNodeID + ")");
			//Log.w("SendingQueue", "Added message to queue, " + mQueue.size()
			//		+ " messages remaining");
			duplicates.add(msgHashcode);

			// also store Message in Persistent Storage
			storeMessage(m);
		} else if (needsUpdate) {

			// already exists but needs to update
			updateMessage(m);
		}
	}

	@Override
	public List<Message> fetchMessagesFromQueue() {

		return messagesDAO.getMessages();

		/*
		ArrayList<Message> messages = new ArrayList<Message>();

		for (Message m : mQueue)
			messages.add(m);

		return messages;*/
	}

	/*
	@Override
	public boolean hasMessages() {
		return !mQueue.isEmpty();
	}

	@Override
	public void clearQueue() {
		mQueue.clear();
		Log.w("SendingQueue", "Queue cleared");
	}

	@Override
	public boolean removeFromQueue(Message msg) {
		boolean removed = mQueue.remove(msg);
		if (removed)
			Log.w("SendingQueue",
					"Removed message from queue, " + mQueue.size()
							+ " messages remaining");

		return removed;
	}
	*/

	@Override
	public void updateStats(int sent, int received) {
		totalSent += sent;
		totalReceived += received;

		Bundle b = new Bundle();
		b.putIntArray("stats", new int[] { totalSent, totalReceived });

	}

	@Override
	public String getMyNodeId() {
		return myNodeID;
	}

	@Override
	public String getMyAccountName() {
		return myAccountName;
	}

	@Override
	public void storeReceivedMessage(Message m) {
		/*
		 * try { dumper.addMessage(m); } catch (IOException e) {
		 * Log.e("AndroidEnvironment", "Cannot store message into Dumper: " +
		 * e.getMessage()); }
		 */
	}

	@Override
	public Message createTextMessage(String text) {

		String nodeID = getMyNodeId();
		String accountName = getMyAccountName();
		long timestamp = System.currentTimeMillis();

		Message newMsg = new Message(nodeID, accountName, timestamp, text,
				MessagesProvider.CREATED, timestamp, MessagesProvider.VICTIM);

		// set other attributes
		newMsg.setSafe(victimSafe);

		if (sensorGroup != null) {
			Location location = (Location) sensorGroup
					.getSensorCurrentValue(SensorGroupKey.Location);
			if (location != null)
				newMsg.setLocation(location);

			Integer battery = (Integer) sensorGroup
					.getSensorCurrentValue(SensorGroupKey.Battery);
			if (battery != null)
				newMsg.setBattery(battery);

			Integer steps = (Integer) sensorGroup
					.getSensorCurrentValue(SensorGroupKey.MicroMovements);
			if (steps != null)
				newMsg.setSteps(steps);

			Integer screen = (Integer) sensorGroup
					.getSensorCurrentValue(SensorGroupKey.ScreenOn);
			if (screen != null)
				newMsg.setScreenOn(screen);
		}

		return newMsg;
	}

	@Override
	public void deliverCustomMessage(Object object, int code) {
		// mHandler.sendMessage(Message.obtain(mHandler, code, object));
	}

	@Override
	public void markVictimAsSafe(boolean safe) {
		victimSafe = safe;
	}

	@Override
	public State getLastState() {
		return lastState;
	}

	@Override
	public boolean internetState() {
		return mPreferences.checkInternetMode();
	}

	@Override
	public SensorGroup getSensorGroup() {
		if (sensorGroup == null)
			sensorGroup = new SensorGroup();

		return sensorGroup;
	}

	/**
	 * Stores a {@link Message} in a persistent form
	 * 
	 * @param msg
	 *            Message to be stored persistently
	 */
	private void storeMessage(Message msg) {

		messagesDAO.insert(msg);
		/*
		ContentValues cv = new ContentValues();
		cv.put(MessagesProvider.COL_ID,
				String.format("%s%d", msg.getNodeId(), msg.getTimestamp()));
		cv.put(MessagesProvider.COL_NODE, msg.getNodeId());
		cv.put(MessagesProvider.COL_GOOGLE, msg.getAccountName());
		cv.put(MessagesProvider.COL_TIME, msg.getTimestamp());

		cv.put(MessagesProvider.COL_LAT, msg.getLatitude());
		cv.put(MessagesProvider.COL_LON, msg.getLongitude());
		cv.put(MessagesProvider.COL_ACC, msg.getLocationAccuracy());
		cv.put(MessagesProvider.COL_LOC_TIME, msg.getLocationTime());

		cv.put(MessagesProvider.COL_BATTERY, msg.getBattery());
		cv.put(MessagesProvider.COL_STEPS, msg.getSteps());
		cv.put(MessagesProvider.COL_SCREEN, msg.getScreenOn());
		cv.put(MessagesProvider.COL_SAFE, msg.isSafe() ? 1 : 0);

		cv.put(MessagesProvider.COL_MSG, msg.getMessage());
		cv.put(MessagesProvider.COL_ADDED, System.currentTimeMillis());
		cv.put(MessagesProvider.COL_STATUS, msg.getStatus());
		cv.put(MessagesProvider.COL_STATUS_TIME, msg.getStatusTime());
		cv.put(MessagesProvider.COL_ORIGIN, msg.getOrigin());

		cv.put(MessagesProvider.COL_TARGET, msg.getTarget());
		cv.put(MessagesProvider.COL_TAR_LAT, msg.getTargetLatitude());
		cv.put(MessagesProvider.COL_TAR_LON, msg.getTargetLongitude());
		cv.put(MessagesProvider.COL_TAR_RAD, msg.getTargetRadius());

		// check Message author and save accordingly
		if (!msg.getNodeId().equals(getMyNodeId())) {
			Uri uriRec = context.getContentResolver().insert(
					MessagesProvider.URI_RECEIVED, cv);
			if (uriRec != null)
				Log.i("storeMessage", "Message persistently stored via "
						+ uriRec.toString());
		}
		Uri uri = context.getContentResolver().insert(
				MessagesProvider.URI_SENT, cv);

		if (uri != null)
			Log.i("storeMessage",
					"Message persistently stored via " + uri.toString());
					*/
	}

	@Override
	public void updateMessage(Message msg) {

		messagesDAO.updateStatus(msg);
		/*
		ContentValues cv = new ContentValues();
		cv.put(MessagesProvider.COL_STATUS, msg.getStatus());
		cv.put(MessagesProvider.COL_STATUS_TIME, msg.getStatusTime());

		if (!msg.getNodeId().equals(getMyNodeId())) {
			Uri receivedUri = Uri.parse(MessagesProvider.PROVIDER_URL
					+ MessagesProvider.METHOD_RECEIVED + "/" + msg.getNodeId()
					+ msg.getTimestamp());
			context.getContentResolver().update(receivedUri, cv, null, null);
		}
		Uri sentUri = Uri.parse(MessagesProvider.PROVIDER_URL
				+ MessagesProvider.METHOD_SENT + "/" + msg.getNodeId()
				+ msg.getTimestamp());
		context.getContentResolver().update(sentUri, cv, null, null);
		*/
	}

	@Override
	public void stopStateLoop() {

		// disable sensors and remote hotspot feature
		if (currentListener != null) {
			currentListener.forceTransition();
		}
		sensorGroup.removeAllSensors(true);
		mNetworkingFacade.stopAccessPoint();
		instance=null;
	}

	/**
	 * Gets the Android {@link Context} associated with this instance
	 * 
	 * @return Android Context
	 */
	@Override
	public Context getAndroidContext() {
		return context;
	}

	/**
	 * Class to allow observing custom text {@link Message Messages} from
	 * clients
	 * 
	 * @author André Silva <asilva@lasige.di.fc.ul.pt>
	 */
	private static class CustomMessagesObserver extends ContentObserver {

		private AndroidEnvironment environment;

		/**
		 * Creates a new CustomMessagesObserver
		 * 
		 * @param h
		 *            Handler (optional)
		 * @param env
		 *            Environment to add new Messages to sending queue
		 */
		public CustomMessagesObserver(Handler h, AndroidEnvironment env) {
			super(h);
			environment = env;
		}

		// This method is called by old Android versions
		@Override
		public void onChange(boolean selfChange) {
			this.onChange(selfChange, null);
		}

		// Newer Android versions call this method
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			ContentResolver cr = environment.getAndroidContext()
					.getContentResolver();
			Cursor c = cr
					.query(MessagesProvider.URI_CUSTOM, null, "", null, "");

			if (c.moveToFirst()) {
				// get Messages and put them into sending queue
				Log.i("CustomMessagesObserver", "Found " + c.getCount()
						+ " custom Messages");
				do {
					String custom = c.getString(c
							.getColumnIndex("customMessage"));
					Message m = environment.createTextMessage(custom);
					environment.pushMessageToQueue(m);
				} while (c.moveToNext());

				// delete custom Messages
				cr.delete(MessagesProvider.URI_CUSTOM, "", null);
			}
		}
	}

	@Override
	public void currentListener(IListener listener) {
		currentListener = listener;

	}

	@Override
	public void onServiceStopped() {

	}
}
