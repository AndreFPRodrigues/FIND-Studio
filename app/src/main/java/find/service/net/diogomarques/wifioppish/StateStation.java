package find.service.net.diogomarques.wifioppish;

import net.diogomarques.utils.CountDownTimer;
import find.service.net.diogomarques.wifioppish.IEnvironment.State;
import find.service.net.diogomarques.wifioppish.INetworkingFacade.OnSendListener;
import find.service.net.diogomarques.wifioppish.networking.Message;
import find.service.net.diogomarques.wifioppish.networking.MessageGroup;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * Android implementation of state {@link IEnvironment.State#Station}
 * 
 * @author Diogo Marques <diogohomemmarques@gmail.com>
 */
public class StateStation extends AState {

	/**
	 * Creates a new Station state
	 * 
	 * @param environment
	 *            Environment running the state machine
	 */
	public StateStation(IEnvironment environment) {
		super(environment);
	}

	OnSendListener listener;

	@Override
	public void start(int timeout, Context c) {
		Log.w("Machine State", "Station");

		context = c;
		environment.deliverMessage("entered station state");
		final INetworkingFacade networking = environment.getNetworkingFacade();

		// prepare messages to be sent to network
		final MessageGroup toSend = new MessageGroup();
		toSend.addAllMessages(environment.fetchMessagesFromQueue());

		// send messages for the network
		int period = environment.getPreferences().getSendPeriod();
		new CountDownTimer(environment.getPreferences().getTCon(), period) {

			@Override
			public void onTick(long arg0) {

				listener = new OnSendListener() {

					@Override
					public void onSendError(String errorMsg) {
						environment.deliverMessage("send error: " + errorMsg
								+ "[" + environment.getCurrentState().name()
								+ "]");
						cancel();
						environment.gotoState(State.Scanning);
					}

					@Override
					public void onMessageSent(Message msg) {
						if (msg.getStatus().equals(MessagesProvider.CREATED)
								|| msg.getStatus()
										.equals(MessagesProvider.SENT)) {

							long statusTime = System.currentTimeMillis();
							msg.setStatus(MessagesProvider.SENT, statusTime);

							environment.updateMessage(msg);

							environment
									.deliverMessage("message successfully sent");
						}
					}

					@Override
					public void onMessageSent(MessageGroup msgs) {
						for (Message m : msgs)
							onMessageSent(m);
					}

					@Override
					public void forceTransition() {
						onFinish();

					}
				};

				Log.w("Station",
						"About to send message group: " + toSend.toString());
				networking.send(toSend, listener);
			}

			@Override
			public void onFinish() {
				environment
						.deliverMessage("t_con finished, exiting station mode");
				environment.gotoState(State.Scanning);
			}
		}.start();
		environment.currentListener(listener);

	}

}
