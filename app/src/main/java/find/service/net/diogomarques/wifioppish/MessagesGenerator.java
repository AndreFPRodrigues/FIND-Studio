package find.service.net.diogomarques.wifioppish;

import find.service.net.diogomarques.wifioppish.networking.Message;
import android.os.Environment;
import android.os.Handler;

public class MessagesGenerator {

	private static MessagesGenerator mSharedInstance = null;
	private IEnvironment environment;
	private boolean autoGenaration;

	protected MessagesGenerator() {

	}

	public static MessagesGenerator sharedInstance() {
		if (mSharedInstance == null)
			mSharedInstance = new MessagesGenerator();
		return mSharedInstance;
	}

	public void initialize(IEnvironment environment) {
		this.environment = environment;

	}

	public void startAutoGeneration(int rate) {
		autoGenaration = true;
		getMessage(rate);
	}

	public void stopAutoGeneration() {
		autoGenaration = false;
	}

	private void getMessage(final int rate) {

		Handler h = new Handler();
		h.postDelayed(new Runnable() {

			@Override
			public void run() {
				// add auto-message to be accumulated
				if (autoGenaration) {
					Message autoMessage = environment.createTextMessage("");
					environment.pushMessageToQueue(autoMessage);
					getMessage(rate);
				}
			}
		}, rate);

	}

}
