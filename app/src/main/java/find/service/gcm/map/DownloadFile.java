package find.service.gcm.map;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Dialog;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadFile {
	private final static String LT = "RESCUE";

	private static int downloadedSize = 0;
	private static int totalSize = 0;
	private static String dwnload_file_path = "http://accessible-serv.lasige.di.fc.ul.pt/~lost/world.sqlitedb";

	public static void downloadTileDB() {
		new Thread(new Runnable() {
			public void run() {
				downloadFile();
			}
		}).start();
	}

	static void downloadFile() {

		try {
			URL url = new URL(dwnload_file_path);
			HttpURLConnection urlConnection = (HttpURLConnection) url
					.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoOutput(true);
			Looper.prepare();
			// connect
			urlConnection.connect();

			// set the path where we want to save the file
			File SDCardRoot = new File(Environment
					.getExternalStorageDirectory().toString() + "/mapapp/");
			if (!SDCardRoot.exists()) {
				SDCardRoot.mkdir();
			}
			// create a new file, to save the downloaded file
			File file = new File(SDCardRoot, "world.sqlitedb");

			FileOutputStream fileOutput = new FileOutputStream(file);

			// Stream used for reading the data from the internet
			InputStream inputStream = urlConnection.getInputStream();

			// this is the total size of the file which we are downloading
			totalSize = urlConnection.getContentLength();

			// create a buffer...
			byte[] buffer = new byte[1024];
			int bufferLength = 0;

			while ((bufferLength = inputStream.read(buffer)) > 0) {
				fileOutput.write(buffer, 0, bufferLength);
				downloadedSize += bufferLength;

			}
			// close the output stream when complete //
			fileOutput.close();
			Log.d(LT, "Downloaded tile db");

			return;

		} catch (final MalformedURLException e) {
			e.printStackTrace();

		} catch (final IOException e) {
			e.printStackTrace();

		} catch (final Exception e) {
		}
	}

}