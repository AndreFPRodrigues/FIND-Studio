package find.service.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import find.service.R;

public class Notifications {
	public static  void generateNotification(Context context, String title, String message, Intent intent) {
		/*int icon = R.drawable.service_logo;
		if(intent==null){
			Log.d("gcm", "intent e null");
			intent= new Intent(context, DemoActivity.class);
		}
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(context.NOTIFICATION_SERVICE);
		notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
	    Notification notifyDetails = new Notification( icon,title,System.currentTimeMillis());
	    PendingIntent myIntent = PendingIntent.getActivity(context, 0,intent, 0);
	    notifyDetails.setLatestEventInfo(context, title, message, myIntent);
	    notifyDetails.flags |= Notification.FLAG_AUTO_CANCEL;
		// Play default notification sound
	    notifyDetails.defaults |= Notification.DEFAULT_SOUND;
		// Vibrate if vibrate is enabled
	    notifyDetails.defaults |= Notification.DEFAULT_VIBRATE;
	    notificationManager.notify(0, notifyDetails);  */
	}
}
