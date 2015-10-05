package find.service.gcm;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class RequestServer implements Runnable{
	
	public static final String SERVER = "server";
	public static final String MODE = "mode";

	public static String endpoint = "http://accessible-serv.lasige.di.fc.ul.pt/~lost/LostMap/";
	private static String postCoordinates = "index.php/rest/victims";
	private static String postLogFile = endpoint + "log/upload.php";

	private String macAddress;
	private boolean delete;

	private static String TAG = "gcm";

	/**
	 * Check if there is wifi connection
	 * 
	 * @return
	 */
	public static boolean netCheckin(Context c) {
		try {
			ConnectivityManager connManager = (ConnectivityManager) c
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mWifi = connManager
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

			if (mWifi != null && mWifi.isConnected()) {
				return true;

			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}
	}

	// Register the gcm user
	public static String post(String endpoint, Map<String, String> params)
			throws IOException {

		StringBuilder sb = new StringBuilder();

		URL url;
		try {

			url = new URL(endpoint);

		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("invalid url: " + endpoint);
		}

		StringBuilder bodyBuilder = new StringBuilder();
		Iterator<Entry<String, String>> iterator = params.entrySet().iterator();

		// constructs the POST body using the parameters
		while (iterator.hasNext()) {
			Entry<String, String> param = iterator.next();
			bodyBuilder.append(param.getKey()).append('=')
					.append(param.getValue());
			if (iterator.hasNext()) {
				bodyBuilder.append('&');
			}
		}

		String body = bodyBuilder.toString();

		byte[] bytes = body.getBytes();

		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setFixedLengthStreamingMode(bytes.length);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded;charset=UTF-8");
			// post the request
			OutputStream out = conn.getOutputStream();
			out.write(bytes);
			out.flush();
			// Get the server response
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String line = null;
			// Read Server Response
			while ((line = reader.readLine()) != null) {
				// Append server response in string
				sb.append(line + "\n");
			}
			reader.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (conn != null) {
				conn.disconnect();
				return sb.toString();
			}
		}
		return null;
	}

	// Register this account with the server.
	public static void register(final String locale, final String mac,
			final String regId, final String email, final SharedPreferences prefs) {

		new AsyncTask<Void, Void, String>() {
			
			@Override
			protected String doInBackground(Void... params1) {

				// Get location based on IP
				ArrayList<String> builder = new ArrayList<String>();
				HttpClient client = new DefaultHttpClient();
				HttpGet httpGet;
				httpGet = new HttpGet("http://ip-api.com/line");
				String serverRegister = endpoint + "gcm/register.php";
				String mode ="";
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
							builder.add(line);
						}
						String location = builder.get(1);
						builder.clear();
						httpGet = new HttpGet(endpoint
								+ "index.php/rest/server/location/" + location);

						response = client.execute(httpGet);
						statusLine = response.getStatusLine();
						statusCode = statusLine.getStatusCode();
						if (statusCode == 200) {
							entity = response.getEntity();
							content = entity.getContent();
							reader = new BufferedReader(new InputStreamReader(
									content));
							while ((line = reader.readLine()) != null) {
								builder.add(line);
							}
						}
						if (builder.size() > 0) {
							JSONArray serverArray = new JSONArray(
									builder.get(0));
							JSONObject serverUrl = serverArray.getJSONObject(0);
							endpoint = serverUrl.getString("url") + "LostMap/";
							mode= serverUrl.getString("mode");
							SharedPreferences.Editor editor = prefs.edit();
							editor.putString(SERVER, endpoint);
							editor.putString(MODE, mode);
							editor.commit();
						}
						
						
						
												
						serverRegister = endpoint + "gcm/register.php";
						Map<String, String> params = new HashMap<String, String>();
						params.put("regId", regId);
						params.put("mac", mac);
						params.put("email", email);
						params.put("mode", mode); // Post registration values

						// Post registration values
													// toweb server
						try {
							RequestServer.post(serverRegister, params);

						} catch (IOException e) { // TODO Auto-generated catch
													// block
							e.printStackTrace();
						}

					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				return serverRegister;
			}
			
		}.execute(null, null, null);

	}
	

	public static void sendCoordinates(final String macAddress,
			final LatLng local, final float batery, final String account,
			final float accuracy, final long locationTimestamp) {

		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				JSONArray jsonArray = new JSONArray();

				JSONObject json = new JSONObject();
				try {
					json.put("nodeid", macAddress);
					json.put("account", account);
					json.put("timestamp", System.currentTimeMillis());
					json.put("msg", "");
					json.put("latitude", local.latitude);
					json.put("longitude", local.longitude);
					json.put("accuracy", accuracy);
					json.put("locationTimestamp", locationTimestamp);
					json.put("battery", batery);
					json.put("steps", 0);
					json.put("screen", 0);
					json.put("safe", 0);
					jsonArray.put(json);
					String contents = jsonArray.toString();
					HttpClient httpclient = new DefaultHttpClient();
					HttpPost httppost = new HttpPost(endpoint + postCoordinates);
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
							2);
					nameValuePairs
							.add(new BasicNameValuePair("data", contents));
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
					HttpResponse response = httpclient.execute(httppost);
					HttpEntity entity = response.getEntity();
				} catch (JSONException e) {
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}
		}.execute(null, null, null);

	}

	protected static void deletePoints(final String regid) {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				StringBuilder builder = new StringBuilder();
				HttpClient client = new DefaultHttpClient();
				HttpGet httpGet;

				httpGet = new HttpGet(endpoint
						+ "index.php/rest/simulations/deletePoints/" + regid);

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

	public void uploadLogFile(final String address, final boolean newThread,
							  final boolean delete) {

		macAddress = address;
		this.delete = delete;

		if (newThread) {
			new Thread(this).start();
		}
		else {
			run();
		}

	}

	@Override
	public void run() {
		boolean tryAgain = false;

		do {
			String nodeid = macAddress.replace(":", "");

			String fileName = "logcat_FIND.txt";
			String filePath = Environment.getExternalStorageDirectory()
					+ "/";

			HttpURLConnection conn = null;
			DataOutputStream dos = null;
			String lineEnd = "\r\n";
			String twoHyphens = "--";
			String boundary = "*****";
			int bytesRead, bytesAvailable, bufferSize;
			byte[] buffer;
			int maxBufferSize = 1024 * 1024;
			File sourceFile = new File(filePath + fileName);

			if (!sourceFile.isFile()) {

				Log.e("uploadFile", "Source File not exist" + fileName);

				return;

			} else {
				try {

					// open a URL connection to the Servlet
					FileInputStream fileInputStream = new FileInputStream(
							sourceFile);
					URL url = new URL(postLogFile);

					// Open a HTTP connection to the URL
					conn = (HttpURLConnection) url.openConnection();
					// conn.setDoInput(true); // Allow Inputs
					conn.setConnectTimeout(30000);
					conn.setReadTimeout(30000);
					conn.setDoOutput(true); // Allow Outputs
					conn.setUseCaches(false); // Don't use a Cached Copy
					conn.setChunkedStreamingMode(maxBufferSize);
					conn.setRequestMethod("POST");
					conn.setRequestProperty("Accept-Encoding", "");
					conn.setRequestProperty("Connection", "Keep-Alive");
					conn.setRequestProperty("ENCTYPE",
							"multipart/form-data");
					conn.setRequestProperty("Content-Type",
							"multipart/form-data;boundary=" + boundary);
					conn.setRequestProperty("uploaded_file", nodeid + "_"
							+ System.currentTimeMillis() + ".txt");

					dos = new DataOutputStream(conn.getOutputStream());

					dos.writeBytes(twoHyphens + boundary + lineEnd);
					dos.writeBytes("Content-Disposition: form-data; name='uploaded_file';filename='"
							+ nodeid
							+ "_"
							+ System.currentTimeMillis()
							+ ".txt" + "'" + lineEnd);

					dos.writeBytes(lineEnd);

					// create a buffer of maximum size
					bytesAvailable = fileInputStream.available();

					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					buffer = new byte[bufferSize];

					// read file and write it into form...
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);

					while (bytesRead > 0) {
						dos.write(buffer, 0, bufferSize);
						bytesAvailable = fileInputStream.available();
						bufferSize = Math
								.min(bytesAvailable, maxBufferSize);
						bytesRead = fileInputStream.read(buffer, 0,
								bufferSize);
						dos.flush();

					}

					// send multipart form data necesssary after file
					// data...
					dos.writeBytes(lineEnd);
					dos.writeBytes(twoHyphens + boundary + twoHyphens
							+ lineEnd);

					// Responses from the server (code and message)
					int serverResponseCode = conn.getResponseCode();
					String serverResponseMessage = conn
							.getResponseMessage();

					Log.i("uploadFile", "HTTP Response is : "
							+ serverResponseMessage + ": "
							+ serverResponseCode);

					if (serverResponseCode == 200) {

						Log.i("uploadFile", "Log successfully uploaded");

						if (delete)
							sourceFile.delete();

						tryAgain = false;
					}

					// close the streams //
					fileInputStream.close();
					dos.flush();
					dos.close();

				} catch (MalformedURLException ex) {

					tryAgain = false;

					ex.printStackTrace();

					Log.e("Upload file to server",
							"error: " + ex.getMessage(), ex);
				} catch (Exception e) {
					tryAgain = !tryAgain;

					Log.e("Upload file Exception", e.getMessage(), e);
					e.printStackTrace();

				}
			}

		} while (tryAgain);
	}
}
