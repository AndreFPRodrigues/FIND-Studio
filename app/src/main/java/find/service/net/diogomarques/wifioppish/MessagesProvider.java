package find.service.net.diogomarques.wifioppish;

import find.service.net.diogomarques.wifioppish.networking.Message;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

/**
 * Content Provider to access {@link Message Messages} received and sent to
 * opportunistic network.
 * 
 * @author Andr� Silva <asilva@lasige.di.fc.ul.pt>
 */
public class MessagesProvider extends ContentProvider {

	private static final String TAG = "Messages Provider";

	// content provider
	public static final String PROVIDER = "find.service.net.diogomarques.wifioppish.MessagesProvider";
	public static final String PROVIDER_URL = "content://" + PROVIDER + "/";

	// methods available
	public static final String METHOD_RECEIVED = "received";
	public static final Uri URI_RECEIVED = Uri.parse(PROVIDER_URL
			+ METHOD_RECEIVED);
	public static final int URI_RECEIVED_CODE = 1;
	public static final Uri URI_RECEIVED_ID = Uri.parse(PROVIDER_URL
			+ METHOD_RECEIVED + "/*");
	public static final int URI_RECEIVED_ID_CODE = 5;

	public static final String METHOD_SENT = "sent";
	public static final Uri URI_SENT = Uri.parse(PROVIDER_URL + METHOD_SENT);
	public static final int URI_SENT_CODE = 2;
	public static final Uri URI_SENT_ID = Uri.parse(PROVIDER_URL + METHOD_SENT
			+ "/*");
	public static final int URI_SENT_ID_CODE = 8;

	public static final String METHOD_INFO = "info";
	public static final Uri URI_INFO = Uri.parse(PROVIDER_URL + METHOD_INFO);
	public static final int URI_INFO_CODE = 13;
	public static final Uri URI_INFO_ID = Uri.parse(PROVIDER_URL + METHOD_INFO
			+ "/*");
	public static final int URI_INFO_ID_CODE = 14;

	public static final String METHOD_DATA = "data";
	public static final Uri URI_DATA = Uri.parse(PROVIDER_URL + METHOD_DATA);
	public static final int URI_DATA_CODE = 15;
	public static final Uri URI_DATA_ID = Uri.parse(PROVIDER_URL + METHOD_DATA
			+ "/*");
	public static final int URI_DATA_ID_CODE = 16;

	public static final String METHOD_CUSTOM = "customsend";
	public static final Uri URI_CUSTOM = Uri
			.parse(PROVIDER_URL + METHOD_CUSTOM);
	public static final int URI_CUSTOM_CODE = 3;
	public static final Uri URI_CUSTOM_ID = Uri.parse(PROVIDER_URL
			+ METHOD_CUSTOM + "/#");
	public static final int URI_CUSTOM_ID_CODE = 4;

	public static final String METHOD_STATUS = "status";
	public static final Uri URI_STATUS = Uri
			.parse(PROVIDER_URL + METHOD_STATUS);
	public static final int URI_STATUS_CODE = 6;
	public static final Uri URI_STATUS_CUSTOM = Uri.parse(PROVIDER_URL
			+ METHOD_STATUS + "/*");
	public static final int URI_STATUS_CUSTOM_CODE = 7;

	public static final String METHOD_SIMULATION = "simulation";
	public static final Uri URI_SIMULATION = Uri.parse(PROVIDER_URL
			+ METHOD_SIMULATION);
	public static final int URI_SIMULATION_CODE = 9;
	public static final Uri URI_SIMULATION_CUSTOM = Uri.parse(PROVIDER_URL
			+ METHOD_SIMULATION + "/*");
	public static final int URI_SIMULATION_CUSTOM_CODE = 10;


	public static final String METHOD_TRUNCATE = "truncate";
	public static final Uri URI_TRUNCATE= Uri
			.parse(PROVIDER_URL + METHOD_TRUNCATE);
	public static final int URI_TRUNCATE_CODE = 11;




	// database fields - identification
	public static final String COL_ID = "_id";
	public static final String COL_NODE = "sender_mac";
	public static final String COL_GOOGLE = "google_account";
	public static final String COL_TIME = "tempo_geracao";

	// database fields - sensor data - location
	public static final String COL_LAT = "latitude";
	public static final String COL_LON = "longitude";
	public static final String COL_ACC = "precisao";
	public static final String COL_LOC_TIME = "tempo_coordenadas";

	// database fields - sensors data - other
	public static final String COL_BATTERY = "battery";
	public static final String COL_SCREEN = "screen";
	public static final String COL_STEPS = "steps";
	public static final String COL_SAFE = "safe";

	// database fields - info
	public static final String COL_MSG = "message";
	public static final String COL_ADDED = "added";
	public static final String COL_STATUS = "status";
	public static final String COL_STATUS_TIME = "status_timestamp";
	public static final String COL_ORIGIN = "origin";
	public static final String COL_STATUSKEY = "statuskey";
	public static final String COL_STATUSVALUE = "statusvalue";

	// database fields - info - target messaging
	public static final String COL_TARGET = "target";
	public static final String COL_TAR_LAT = "tartet_latitude";
	public static final String COL_TAR_LON = "target_longitude";
	public static final String COL_TAR_RAD = "target_radius";

	// database fields - simulation
	public static final String COL_SIMUKEY = "simukey";
	public static final String COL_SIMUVALUE = "simuvalue";
	public static final String COL_SIMU_DATE = "simudate";
	public static final String COL_SIMU_DURATION = "simuduration";
	public static final String COL_SIMU_LOCAL = "simulocal";

	// constants - message status
	public static final String CREATED = "created";
	public static final String SENT = "sent";
	public static final String REC_VIC = "receivedVictim";
	public static final String REC_RES = "receivedRescuer";
	public static final String REC_CC = "receivedControlCentre";

	// contants - message origin & target message
	public static final String VICTIM = "victim";
	public static final String RESCUER = "rescuer";
	public static final String CONTROL_CENTRE = "controlCentre";

	private DBHelper dbHelper;
	static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PROVIDER, METHOD_INFO, URI_INFO_CODE);
		uriMatcher.addURI(PROVIDER, METHOD_DATA, URI_DATA_CODE);

		uriMatcher.addURI(PROVIDER, METHOD_RECEIVED, URI_RECEIVED_CODE);
		uriMatcher.addURI(PROVIDER, METHOD_SENT, URI_SENT_CODE);
		uriMatcher.addURI(PROVIDER, METHOD_CUSTOM, URI_CUSTOM_CODE);

		uriMatcher.addURI(PROVIDER, METHOD_CUSTOM + "/#", URI_CUSTOM_ID_CODE);
		uriMatcher.addURI(PROVIDER, METHOD_RECEIVED + "/*",
				URI_RECEIVED_ID_CODE);
		uriMatcher.addURI(PROVIDER, METHOD_STATUS, URI_STATUS_CODE);
		uriMatcher.addURI(PROVIDER, METHOD_STATUS + "/*",
				URI_STATUS_CUSTOM_CODE);
		uriMatcher.addURI(PROVIDER, METHOD_SENT + "/*", URI_SENT_ID_CODE);
		uriMatcher.addURI(PROVIDER, METHOD_SIMULATION, URI_SIMULATION_CODE);
		uriMatcher.addURI(PROVIDER, METHOD_SIMULATION + "/*",
				URI_SIMULATION_CUSTOM_CODE);
		uriMatcher.addURI(PROVIDER, METHOD_TRUNCATE, URI_TRUNCATE_CODE);

	}

	// database declarations
	private SQLiteDatabase database;
	static final String DATABASE_NAME = "LOSTMessages";
	static final String TABLE_UNIT_INFO = "unit_info";
	static final String TABLE_UNIT_DATA = "unit_data";
	/*static final String TABLE_OUTGOING = "outgoing";
	static final String TABLE_INCOMING = "incoming";*/
	static final String TABLE_TOSEND = "tosend";
	static final String TABLE_STATUS = "status";
	static final String TABLE_SIMULATION = "simulation";

	static final int DATABASE_VERSION = 2;

	private static final String CREATE_TABLE_UNIT_INFO = "CREATE TABLE unit_info (\n" +
			"  sender_mac varchar(100) NOT NULL DEFAULT '',\n" +
			"  google_account TEXT, \n" +
			"  tempo_geracao datetime NOT NULL,\n" +
			"  tempo_rececao datetime DEFAULT NULL,\n" +
			"  tempo_coordenadas datetime DEFAULT NULL,\n" +
			"  latitude float DEFAULT NULL,\n" +
			"  longitude float DEFAULT NULL,\n" +
			"  precisao int(11) DEFAULT NULL,\n" +
			"  prioridade varchar(10) DEFAULT NULL,\n" +
			"  safe tinyint(4) DEFAULT NULL,\n" +
			"  origin TEXT, \n" +
			"  status TEXT DEFAULT NULL, \n" +
			"  PRIMARY KEY (sender_mac,tempo_geracao)\n" +
			")";

	private static final String CREATE_TABLE_UNIT_DATA = "CREATE TABLE unit_data (\n" +
			"  sender_mac varchar(100) NOT NULL DEFAULT '',\n" +
			"  tempo_geracao datetime NOT NULL,\n" +
			"  valor varchar(200) DEFAULT NULL,\n" +
			"  tipo varchar(30) NOT NULL DEFAULT '',\n" +
			"  PRIMARY KEY (sender_mac, tempo_geracao, tipo),\n" +
			"  CONSTRAINT fk_testing FOREIGN KEY (tempo_geracao) REFERENCES unit_info (tempo_geracao) \n" +
			")";

	/*private static final String TABLE = " (" + COL_ID + " TEXT PRIMARY KEY, "
			+ " " + COL_NODE + " TEXT," + " " + COL_GOOGLE + " TEXT," + " "
			+ COL_TIME + " DOUBLE," + " " + COL_LAT + " DOUBLE," + " "
			+ COL_LON + " DOUBLE," + " " + COL_ACC + " INTEGER," + " "
			+ COL_LOC_TIME + " DOUBLE," + " " + COL_BATTERY + " INTEGER," + " "
			+ COL_STEPS + " INTEGER," + " " + COL_SCREEN + " INTEGER," + " "
			+ COL_SAFE + " INTEGER," + " " + COL_MSG + " TEXT," + " "
			+ COL_ADDED + " DOUBLE," + " " + COL_STATUS + " TEXT," + " "
			+ COL_STATUS_TIME + " DOUBLE, " + " " + COL_ORIGIN + " TEXT," + " "
			+ COL_TARGET + " TEXT," + " " + COL_TAR_LAT + " DOUBLE," + " "
			+ COL_TAR_LON + " DOUBLE," + " " + COL_TAR_RAD + " INTEGER" + ");";
	static final String CREATE_TABLE_OUTGOING = " CREATE TABLE "
			+ TABLE_OUTGOING + TABLE;
	static final String CREATE_TABLE_INCOMING = " CREATE TABLE "
			+ TABLE_INCOMING + TABLE;*/
	static final String CREATE_TABLE_TOSEND = " CREATE TABLE " + TABLE_TOSEND
			+ " (customMessage TEXT PRIMARY KEY);";
	static final String CREATE_TABLE_STATUS = " CREATE TABLE " + TABLE_STATUS
			+ " (" + COL_STATUSKEY + " TEXT," + " " + COL_STATUSVALUE
			+ " TEXT)";
	static final String CREATE_TABLE_SIMULATION = " CREATE TABLE "
			+ TABLE_SIMULATION + " (" + COL_SIMUKEY + " TEXT," + " "
			+ COL_SIMUVALUE + " TEXT, " + COL_SIMU_DATE + " TEXT, "
			+ COL_SIMU_DURATION + " TEXT, " + COL_SIMU_LOCAL + " TEXT" + ")";

	// class that creates and manages the provider's database
	public static class DBHelper extends SQLiteOpenHelper {

		public DBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TABLE_UNIT_INFO);
			db.execSQL(CREATE_TABLE_UNIT_DATA);
			/*db.execSQL(CREATE_TABLE_INCOMING);
			db.execSQL(CREATE_TABLE_OUTGOING);*/
			db.execSQL(CREATE_TABLE_TOSEND);
			db.execSQL(CREATE_TABLE_STATUS);
			db.execSQL(CREATE_TABLE_SIMULATION);

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ". Old data will be destroyed.");

			db.execSQL("DROP TABLE IF EXISTS " + TABLE_UNIT_INFO);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_UNIT_DATA);
			/*db.execSQL("DROP TABLE IF EXISTS " + TABLE_INCOMING);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_OUTGOING);*/
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_TOSEND);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATUS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_SIMULATION);

			onCreate(db);
		}

		public ArrayList<Cursor> getData(String Query){
			//get writable database
			SQLiteDatabase sqlDB = getWritableDatabase();
			String[] columns = new String[] { "mesage" };
			//an array list of cursor to save two cursors one has results from the query
			//other cursor stores error message if any errors are triggered
			ArrayList<Cursor> alc = new ArrayList<Cursor>(2);
			MatrixCursor Cursor2= new MatrixCursor(columns);
			alc.add(null);
			alc.add(null);


			try{
				String maxQuery = Query ;
				//execute the query results will be save in Cursor c
				Cursor c = sqlDB.rawQuery(maxQuery, null);


				//add value to cursor2
				Cursor2.addRow(new Object[] { "Success" });

				alc.set(1,Cursor2);
				if (null != c && c.getCount() > 0) {


					alc.set(0,c);
					c.moveToFirst();

					return alc ;
				}
				return alc;
			} catch(SQLException sqlEx){
				Log.d("printing exception", sqlEx.getMessage());
				//if any exceptions are triggered save the error message to cursor an return the arraylist
				Cursor2.addRow(new Object[] { ""+sqlEx.getMessage() });
				alc.set(1,Cursor2);
				return alc;
			} catch(Exception ex){

				Log.d("printing exception", ex.getMessage());

				//if any exceptions are triggered save the error message to cursor an return the arraylist
				Cursor2.addRow(new Object[] { ""+ex.getMessage() });
				alc.set(1,Cursor2);
				return alc;
			}


		}

	}

	@Override
	public boolean onCreate() {
		Context context = getContext();
		dbHelper = new DBHelper(context);
		// permissions to be writable
		database = dbHelper.getWritableDatabase();

		return database != null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long row = -1;
		boolean received = false;
		boolean status = false;
		boolean simulation = false;

		try {
			switch (uriMatcher.match(uri)) {
			case URI_INFO_CODE:
				row = database.insertOrThrow(TABLE_UNIT_INFO, "", values);
				break;

			case URI_DATA_CODE:
				row = database.insertOrThrow(TABLE_UNIT_DATA, "", values);
				break;

			case URI_CUSTOM_CODE:
				row = database.insertOrThrow(TABLE_TOSEND, "", values);
				break;

			/*case URI_RECEIVED_CODE:
				row = database.insertOrThrow(TABLE_INCOMING, "", values);
				received = true;
				break;

			case URI_SENT_CODE:
				row = database.insertOrThrow(TABLE_OUTGOING, "", values);
				break;*/

			case URI_STATUS_CODE:
				try {
					row = database.insert(TABLE_STATUS, "", values);
					status = true;
				} catch (SQLException e) {
					// key already exists, fallback to update
					update(uri,
							values,
							COL_STATUSKEY + "=\""
									+ values.getAsString(COL_STATUSKEY) + "\"",
							null);
				}
				break;
			case URI_SIMULATION_CODE:
				try {
					row = database.insert(TABLE_SIMULATION, "", values);
					simulation = true;
				} catch (SQLException e) {
					// key already exists, fallback to update
					update(uri,
							values,
							COL_SIMUKEY + "=\""
									+ values.getAsString(COL_SIMUKEY) + "\"",
							null);
				}
				break;
			case URI_TRUNCATE_CODE:

				break;
			}
		} catch (SQLException e) {
			Log.w(TAG, "Tried to insert duplicate data, records not changed", e);
			row = -1;
		}

		if (row > 0) {
			Uri newUri;

			if (received) {
				String id = String.format("%s%s", values.getAsString(COL_NODE),
						values.getAsString(COL_TIME));
				newUri = Uri.withAppendedPath(uri, id);
			} else if (status) {
				newUri = Uri.withAppendedPath(URI_STATUS,
						values.getAsString(COL_STATUSKEY));
			} else {
				if (simulation) {
					newUri = Uri.withAppendedPath(URI_SIMULATION,
							values.getAsString(COL_SIMUKEY));
				} else

					newUri = ContentUris.withAppendedId(uri, row);
			}

			Log.d(TAG, "Generating notification for " + newUri);
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		Log.d(TAG, "No notification for " + uri);
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		// select correct table based on URI
		switch (uriMatcher.match(uri)) {
		/*case URI_RECEIVED_CODE:
			queryBuilder.setTables(TABLE_INCOMING);
			break;

		case URI_RECEIVED_ID_CODE:
			queryBuilder.setTables(TABLE_INCOMING);
			String receivedId = uri.getLastPathSegment();
			queryBuilder.appendWhere(COL_ID + "=\"" + receivedId + "\"");
			break;

		case URI_SENT_CODE:
			queryBuilder.setTables(TABLE_OUTGOING);
			break;

		case URI_SENT_ID_CODE:
			queryBuilder.setTables(TABLE_OUTGOING);
			String sentId = uri.getLastPathSegment();
			queryBuilder.appendWhere("_id = " + sentId);
			break;*/

		case URI_INFO_CODE:
			queryBuilder.setTables(TABLE_UNIT_INFO);
			break;

		case URI_DATA_CODE:
			queryBuilder.setTables(TABLE_UNIT_DATA);
			break;

		case URI_CUSTOM_CODE:
			queryBuilder.setTables(TABLE_TOSEND);
			break;

		case URI_CUSTOM_ID_CODE:
			queryBuilder.setTables(TABLE_TOSEND);
			String toSendId = uri.getLastPathSegment();
			queryBuilder.appendWhere("rowid = " + toSendId);
			break;

		case URI_STATUS_CODE:
			queryBuilder.setTables(TABLE_STATUS);
			break;

		case URI_STATUS_CUSTOM_CODE:
			queryBuilder.setTables(TABLE_STATUS);
			String key = uri.getLastPathSegment();
			queryBuilder.appendWhere(COL_STATUSKEY + "=\"" + key + "\"");
			break;

		case URI_SIMULATION_CODE:
			queryBuilder.setTables(TABLE_SIMULATION);
			break;

		case URI_SIMULATION_CUSTOM_CODE:
			queryBuilder.setTables(TABLE_SIMULATION);
			String key2 = uri.getLastPathSegment();
			queryBuilder.appendWhere(COL_SIMUKEY + "=\"" + key2 + "\"");
			break;
		case URI_TRUNCATE_CODE:

			break;

		default:
			Log.w(TAG, "Unknown URI to query:" + uri);
			return null;
		}

		Cursor cursor = queryBuilder.query(database, projection, selection,
				selectionArgs, null, null, sortOrder);

		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int rows = -1;

		switch (uriMatcher.match(uri)) {

		case URI_INFO_CODE:
			rows = database.update(TABLE_UNIT_INFO, values, selection,
					selectionArgs);
			if (rows > 0) {
				Log.i(TAG, "Generating notification for " + uri);
				getContext().getContentResolver().notifyChange(uri, null);
			}
			break;
		/*case URI_SENT_ID_CODE:
			String id = uri.getLastPathSegment();
			selection = "_id = \"" + id + "\"";
			rows = database.update(TABLE_OUTGOING, values, selection,
					selectionArgs);
			if (rows > 0) {
				Log.i(TAG, "Generating notification for " + uri);
				getContext().getContentResolver().notifyChange(uri, null);
			}
			break;*/

		case URI_STATUS_CODE:
			rows = database.update(TABLE_STATUS, values, selection,
					selectionArgs);
			if (rows > 0) {
				Uri newUri = Uri.withAppendedPath(URI_STATUS,
						values.getAsString(COL_STATUSKEY));
				getContext().getContentResolver().notifyChange(newUri, null);
			}
			break;
		case URI_SIMULATION_CODE:
			rows = database.update(TABLE_SIMULATION, values, selection,
					selectionArgs);
			if (rows > 0) {
				Uri newUri = Uri.withAppendedPath(URI_SIMULATION,
						values.getAsString(COL_SIMUKEY));
				getContext().getContentResolver().notifyChange(newUri, null);
			}
			break;
		}

		return rows;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = 0;

		switch (uriMatcher.match(uri)) {
		case URI_CUSTOM_CODE:
			count = database.delete(TABLE_TOSEND, selection, selectionArgs);
			break;

		case URI_CUSTOM_ID_CODE:
			String id = uri.getLastPathSegment();
			count = database.delete(TABLE_TOSEND,
					"rowid = "
							+ id
							+ (!TextUtils.isEmpty(selection) ? " AND ("
							+ selection + ')' : ""), selectionArgs);
		case URI_TRUNCATE_CODE:
			//database.execSQL("delete from "+ TABLE_OUTGOING);
			//database.execSQL("delete from "+ TABLE_INCOMING);
			database.execSQL("delete from "+ TABLE_UNIT_INFO);
			database.execSQL("delete from "+ TABLE_UNIT_DATA);
			database.execSQL("delete from "+ TABLE_TOSEND);


			break;
		}

		if (count > 0)
			getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}
}
