package me.gooey.wlanscanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class DBUtil {
    static final boolean POPULATE_DUMMY_DATA = false;

    private static final String DATABASE_NAME = "wlan.db";
    private static final int DATABASE_VERSION = 3;
    private static final String T = "DBUtilClass";
    private static final String TBL_DEVICE = "wlan";
    private DBHelper db_helper;

    public DBUtil(Context context) {
        db_helper = new DBHelper(context);
    }

    private static class DBHelper extends SQLiteOpenHelper {
        // private Context ctx;

        private static final String DB_WIFI_TABLE_CREATE = "CREATE TABLE "
                + TBL_DEVICE
                + " (mac TEXT, ssid TEXT, lat REAL, lng REAL, cap TEXT, freq TEXT, power INTEGER, "
                + " PRIMARY KEY (mac, ssid, lat, lng))";
        
        public DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            // ctx = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DB_WIFI_TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(T, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS "+TBL_DEVICE);
            onCreate(db);
        }

    }

    /*
     * return total count of all profile data records
     */
    public List<WLANStation> getAllWlanRecords() {
        SQLiteDatabase db = db_helper.getReadableDatabase();
        ArrayList<WLANStation> wlanlist = new ArrayList<WLANStation>(){};
        try {
            Cursor c = db.query(TBL_DEVICE, 
                    new String[] { "mac", "ssid", "lat", "lng", "cap", "freq", "power" },
                    null, null, null, null, null, "power DESC");

            Log.d(T, String.format("Query result has %d rows.", c.getCount()));
            
            if (c.moveToFirst()) {
                wlanlist.clear();
                for (int i=0; i<c.getCount(); i++) {
                    WLANStation ws = new WLANStation();
                    ws.mac = c.getString(0);
                    ws.ssid = c.getString(1);
                    ws.lat = c.getDouble(2);
                    ws.lng = c.getDouble(3);
                    ws.cap = c.getString(4);
                    ws.freq = c.getInt(5);
                    ws.power = c.getInt(6);
                    wlanlist.add(ws);
                }
            }
            c.close();
        } catch (Exception e) {
            Log.e(T, "getAllWLANRecords: Error while querying DB.", e);
        } finally {
            db.close();
        }
        return wlanlist;
    }
    
    /*
     * insert wlan data into sqlite
     */
    public int updateWlanTable(List<WLANStation> wlanlist) {
        int count = 0;
        SQLiteDatabase db = db_helper.getWritableDatabase();
        try {
        for (WLANStation ws : wlanlist) {
            String sql = "INSERT OR REPLACE INTO "+TBL_DEVICE+
                    " ( mac, ssid, lat, lng, cap, freq, power ) " +
                    " VALUES ( \""+ws.mac+"\", " +
                            "\""+ws.ssid+"\", " +
                            +ws.lat+", " +
                            +ws.lng+", " +
                            "\""+ws.cap+"\", " +
                            +ws.freq+", " +
                            +ws.power+" )";
            // Log.d(T, "SQL: "+sql);
            db.execSQL(sql);
        }
        } catch (Exception e) {
            Log.e(T, "getAllWLANRecords: Error while querying DB.", e);
        } finally {
            db.close();
        }
        return count;
    }
}
