package find.service.net.diogomarques.wifioppish;

import find.service.net.diogomarques.wifioppish.IEnvironment.State;
import find.service.net.diogomarques.wifioppish.networking.Message;
import android.content.Context;
import android.util.Log;

/**
 * Android implementation of state {@link IEnvironment.State#Scanning}
 * 
 * @author Diogo Marques <diogohomemmarques@gmail.com>
 */
public class StateScanning extends AState {

	/**
	 * Creates a new Scanning state
	 * 
	 * @param environment
	 *            Environment running the state machine
	 */
	public StateScanning(IEnvironment environment) {
		super(environment);
	}

	@Override
	public void start(int timeout, Context c) {

		Log.w("Machine State", "Scanning:" + timeout);

		final INetworkingFacade networking = environment.getNetworkingFacade();

		// add auto-message to be accumulated
		//Message autoMessage = environment.createTextMessage("");
		//environment.pushMessageToQueue(autoMessage);

		environment.deliverMessage("entered scanning state");
		INetworkingFacade.OnAccessPointScanListener listener = new INetworkingFacade.OnAccessPointScanListener() {

			@Override
			public void onScanTimeout() {
				environment.deliverMessage("t_scan timeout");
				Log.w("Machine State", "Scan Timeout");
					environment.gotoState(State.Beaconing);
			}

			@Override
			public void onAPConnection(String bSSID) {
				// calculate remote node ID
				String mac = bSSID;

				if (mac != null) {
					String remoteId = mac; //NodeIdentification.getNodeId(mac);
					environment.deliverMessage("connected to AP! (node ID is "
							+ remoteId + " )");
				} else {
					environment.deliverMessage("connected to AP!");
				}
				//reset time in scanning mode
				environment.getNetworkingFacade().setTimeInScan(0);
				
				environment.gotoState(State.Station);
			}

			@Override
			public void forceTransition() {
				onScanTimeout();
			}

			@Override
			public void onInternetConnection() {
				environment
				.deliverMessage("connected to the internet!");
				environment.gotoState(State.InternetConn);		
			}
		};
		environment.currentListener(listener);
		networking.scanForAP(timeout, listener);
	}
}
