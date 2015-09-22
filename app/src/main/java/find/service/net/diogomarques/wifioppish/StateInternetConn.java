package find.service.net.diogomarques.wifioppish;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.diogomarques.utils.CountDownTimer;
import find.service.gcm.RequestServer;
import find.service.net.diogomarques.wifioppish.IEnvironment.State;
import find.service.net.diogomarques.wifioppish.networking.Message;
import find.service.net.diogomarques.wifioppish.networking.MessageFormatter;
import find.service.net.diogomarques.wifioppish.service.LOSTService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * Android implementation of state {@link IEnvironment.State#InternetConn}
 * 
 * @author André Rodrigues
 * @author André Silva <asilva@lasige.di.fc.ul.pt>
 */
public class StateInternetConn extends AState {

	private final int HTTP_OK = 200;
	private final String METHOD = "victims";

	/**
	 * Creates a new InternetConnected state
	 * 
	 * @param environment
	 *            Environment running the state machine
	 */
	public StateInternetConn(IEnvironment env) {
		super(env);
	}

	@Override
	public void start(int timeout, Context c) {

		Log.w("Machine State", "Internet Connected");

		context = c;
		environment.deliverMessage("entered Internet connected state");
		environment.currentListener(null);

		long startTime = new Date().getTime();
		if (LOSTService.toStop) {
			RequestServer.uploadLogFile(NodeIdentification.getMyNodeId(c));
		}
		try {
			String endpoint;
			endpoint = new StringBuilder()
					.append(environment.getPreferences().getApiEndpoint()).append("index.php/rest/")
					.append(METHOD).toString();
			if (LOSTService.toStop) {
				endpoint += "/legacy";
			}
			Log.d("Webservice", "Endpoint: " + endpoint);

			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(endpoint);

			// get messages from send queue and create auto-message
			List<Message> messages = environment.fetchMessagesFromQueue();
			// messages.add(environment.createTextMessage(""));
			JSONArray jsonArray = new JSONArray();

			for (Message m : messages) {
				JSONObject json = MessageFormatter.messageToJsonObject(m);
				jsonArray.put(json);
			}

			String contents = jsonArray.toString();

			Log.d("Webservice", "About to send: " + contents);

			// send request to webservice
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("data", contents));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			String body = EntityUtils.toString(entity, "UTF-8");

			Log.d("Webservice", "Response: "
					+ response.getStatusLine().getStatusCode());
			Log.d("Webservice", "Response body: " + body);

			// if messages were successfully inserted, clear the queue
			if (response.getStatusLine().getStatusCode() == HTTP_OK) {
				// message sent feedback
				ContentResolver cr = context.getContentResolver();

				for (Message m : messages) {
					// TODO get operation code (805) from shared/centralized
					// handler
					environment.deliverCustomMessage(m, 805);

					long statusTime = System.currentTimeMillis();
					m.setStatus(MessagesProvider.REC_CC, statusTime);
					environment.updateMessage(m);

					/*
					// update content provider to tell messages were sucessfully
					// sent to webservice
					ContentValues cv = new ContentValues();
					cv.put(MessagesProvider.COL_STATUS, MessagesProvider.REC_CC);
					cv.put(MessagesProvider.COL_STATUS_TIME, statusTime);
					Uri sentUri = Uri.parse(MessagesProvider.PROVIDER_URL
							+ MessagesProvider.METHOD_SENT + "/"
							+ m.getNodeId() + m.getTimestamp());
					context.getContentResolver()
							.update(sentUri, cv, null, null);*/
				}

				// environment.clearQueue();
			}

		} catch (IOException e) {
			Log.e("Webservice",
					"Cannot connect to webservice: " + e.getMessage(), e);
		}

		// wait until time limit reached before changing state
		while (new Date().getTime() < startTime + timeout)
			;
		environment.deliverMessage("t_i_con timeout");

		if (LOSTService.toStop) {
			LOSTService.synced = true;
			environment.gotoState(State.Stopped);
		} else {
			environment.gotoState(State.Scanning);
		}
	}

}
