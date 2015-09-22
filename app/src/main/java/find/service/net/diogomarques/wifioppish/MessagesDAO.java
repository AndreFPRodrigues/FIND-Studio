package find.service.net.diogomarques.wifioppish;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;

import find.service.net.diogomarques.wifioppish.networking.Message;
import find.service.net.diogomarques.wifioppish.sensors.SensorGroup;

public class MessagesDAO {

    private Context context;

    public static final String[] TYPES = {"Battery", "Movements", "Screen", "Message"};

    public MessagesDAO(Context context){
        this.context = context;
    }

    public void insert(Message msg){

		ContentValues cv = new ContentValues();

        // unit_info
		cv.put("sender_mac", msg.getNodeId());
		cv.put("google_account", msg.getAccountName());
		cv.put("tempo_geracao", msg.getTimestamp());
		cv.put(MessagesProvider.COL_LAT, msg.getLatitude());
		cv.put(MessagesProvider.COL_LON, msg.getLongitude());
		cv.put("precisao", msg.getLocationAccuracy());
		cv.put("tempo_coordenadas", msg.getLocationTime());
        cv.put(MessagesProvider.COL_SAFE, msg.isSafe() ? 1 : 0);
        cv.put(MessagesProvider.COL_STATUS, msg.getStatus());
        cv.put("tempo_rececao", msg.getStatusTime());
        cv.put("origin", msg.getOrigin());

        Uri uriInfo = context.getContentResolver().insert(
                MessagesProvider.URI_INFO, cv);
        if (uriInfo != null)
            Log.i("storeMessage", "Message persistently stored via "
                    + uriInfo.toString());

        String[] values = {String.valueOf(msg.getBattery()), String.valueOf(msg.getSteps()),
                String.valueOf(msg.getScreenOn()), msg.getMessage()};

        for(int i = 0; i < TYPES.length; i++){
            cv = new ContentValues();
            cv.put("sender_mac", msg.getNodeId());
            cv.put("tempo_geracao", msg.getTimestamp());
            cv.put("tipo", TYPES[i]);
            cv.put("valor", values[i]);

            Uri uriData = context.getContentResolver().insert(
                    MessagesProvider.URI_DATA, cv);
            if (uriData != null)
                Log.i("storeMessage", "Message persistently stored via "
                        + uriData.toString());
        }
    }

    public void updateStatus(Message msg){
        ContentValues cv = new ContentValues();
        cv.put(MessagesProvider.COL_STATUS, msg.getStatus());
        cv.put("tempo_rececao", msg.getStatusTime());

        String[] selectionArgs = {msg.getNodeId(), String.valueOf(msg.getTimestamp())};
        context.getContentResolver().update(MessagesProvider.URI_INFO, cv, "sender_mac = ? and tempo_geracao = ?", selectionArgs);
    }

    public ArrayList<Message> getMessages(){

        ArrayList<Message> messages = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(MessagesProvider.URI_INFO, null, null, null, null);

        if (cursor.moveToFirst()) {

            do {

                String nodeId = cursor.getString(cursor.getColumnIndex("sender_mac"));
                String google = cursor.getString(cursor.getColumnIndex("google_account"));
                long timestamp = cursor.getLong(cursor.getColumnIndex("tempo_geracao"));

                double lat = cursor.getDouble(cursor.getColumnIndex("latitude"));
                double lng = cursor.getDouble(cursor.getColumnIndex("longitude"));
                int accuracy = cursor.getInt(cursor.getColumnIndex("precisao"));

                long locationTimestamp = cursor.getLong(cursor.getColumnIndex("tempo_coordenadas"));

                String origin = cursor.getString(cursor.getColumnIndex("origin"));

                boolean safe = cursor.getInt(cursor.getColumnIndex("safe")) == 1;

                String status = cursor.getString(cursor.getColumnIndex("status"));
                long statusTimestamp = cursor.getLong(cursor.getColumnIndex("tempo_rececao"));

                Message msg = new Message(nodeId, google, timestamp, "", status, statusTimestamp, origin);

                msg.setSafe(safe);

                Location loc = new Location("");
                loc.setLatitude(lat);
                loc.setLongitude(lng);
                loc.setTime(locationTimestamp);
                loc.setAccuracy(accuracy);
                msg.setLocation(loc);

                String[] selectionArgs = {nodeId, String.valueOf(timestamp)};

                Cursor dataCursor = context.getContentResolver().query(MessagesProvider.URI_DATA, null,
                        "sender_mac = ? and tempo_geracao = ?", selectionArgs, null);

                if (dataCursor.moveToFirst()) {

                    do {
                        String type = dataCursor.getString(dataCursor.getColumnIndex("tipo"));

                        if (type.equals(TYPES[0])) {
                            int bat = dataCursor.getInt(dataCursor.getColumnIndex("valor"));
                            msg.setBattery(bat);
                        } else if (type.equals(TYPES[1])) {
                            int steps = dataCursor.getInt(dataCursor.getColumnIndex("valor"));
                            msg.setSteps(steps);
                        } else if (type.equals(TYPES[2])) {
                            int screen = dataCursor.getInt(dataCursor.getColumnIndex("valor"));
                            msg.setScreenOn(screen);
                        } else if (type.equals(TYPES[3])) {
                            String message = dataCursor.getString(dataCursor.getColumnIndex("valor"));
                            msg.setMessage(message);
                        }
                    }while(dataCursor.moveToNext());

                }
                messages.add(msg);
                dataCursor.close();

            } while (cursor.moveToNext());

            cursor.close();
        }
        return messages;
    }
}
