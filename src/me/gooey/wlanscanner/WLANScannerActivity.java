package me.gooey.wlanscanner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.fusiontables.ftclient.ClientLogin;
import com.google.fusiontables.ftclient.FtClient;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class WLANScannerActivity extends Activity {
    private static String TAG = "WLANScanner";
    
    List<ScanResult> results;
    WifiManager wm;
    TableLayout tv;
    TimerTask scanTask;
    Timer t = new Timer();
    DBUtil db;
    BroadcastReceiver wlanscanReceiver;
    Location lastLocation = null;
    LocationManager lm;
    LocationListener ll;
    private FtClient ftclient;
    // google account email
    private String username = "xxx@gmail.com";
    // google account password
    private String password = "secret";
    // fusion table id
    private String tableid = "xxx-xxx";
    private boolean ft_ready = false;
    private DateFormat df = new DateFormat();
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        db = new DBUtil(getApplicationContext());
        new StartFTClient().execute("");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // start the LocationManager
        lm = (LocationManager)
                getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        ll = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "Location received: "+location.toString());
                lastLocation = location;
            }

            @Override
            public void onProviderDisabled(String provider) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void onProviderEnabled(String provider) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void onStatusChanged(String provider, int status,
                    Bundle extras) {
                // TODO Auto-generated method stub
                
            }
          };


        
        wm = (WifiManager) getApplicationContext().getSystemService(
                Context.WIFI_SERVICE );
        wm.setWifiEnabled(true);
        Log.d(TAG, "Starting wlan scan: " + wm.startScan());
        
        
        wlanscanReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context c, Intent intent) 
            {
               tv = (TableLayout)findViewById(R.id.table_wlan);       
               results = wm.getScanResults();
               tv.removeAllViews();
               ArrayList<WLANStation> wlanlist = new ArrayList<WLANStation>(){};
               for (ScanResult sr : results) {
                   Log.d(TAG,  
                         "scanned found SSID: "+ sr.SSID + " MAC: "+ sr.BSSID);
                   WLANStation ws = new WLANStation();
                   ws.mac = sr.BSSID;
                   ws.ssid = sr.SSID;
                   ws.cap = sr.capabilities;
                   ws.freq = sr.frequency;
                   ws.power = sr.level;
                   if (lastLocation != null) {
                       ws.lat = lastLocation.getLatitude();
                       ws.lng = lastLocation.getLongitude();
                   }
                   wlanlist.add(ws);
                   
                   // populate Fusion Table
                   // Generate INSERT statement
                   if (ft_ready && lastLocation != null) {
                       StringBuilder insert = new StringBuilder();
                       insert.append("INSERT INTO ");
                       insert.append(tableid);
                       insert.append(" (mac, ssid, lat, lng, cap, freq, power, timestamp) VALUES ");
                       insert.append("( '");
                       insert.append(ws.mac);
                       insert.append("', '");
                       insert.append(ws.ssid);
                       insert.append("', ");
                       insert.append(ws.lat);
                       insert.append(", ");
                       insert.append(ws.lng);
                       insert.append(", '");
                       insert.append(ws.cap);
                       insert.append("', '");
                       insert.append(ws.freq);
                       insert.append("', ");
                       insert.append(ws.power);
                       insert.append(", '");
                       insert.append(df.format("yyyy-MM-dd kk:mm:ss zz", new Date()));
                       insert.append("')");
                       // Save the data to Fusion Tables
                       new StartFTClient().execute(insert.toString());
                   }
                   
                   TableRow tbr = new TableRow(getApplicationContext());
                   TextView ssidview = new TextView(getApplication());
                   TextView macview = new TextView(getApplication());
                   // TextView capview = new TextView(getApplication());
                   // TextView freqview = new TextView(getApplication());
                   TextView lvlview = new TextView(getApplication());
                   ssidview.setText(sr.SSID );
                   macview.setText(sr.BSSID);
                   // capview.setText(sr.capabilities);
                   // freqview.setText("{"+sr.frequency+"}");
                   lvlview.setText(""+sr.level+"dB");
                   
                   tbr.addView(ssidview);
                   tbr.addView(macview);
                   // tbr.addView(capview);
                   // tbr.addView(freqview);
                   tbr.addView(lvlview);
                   tv.addView(tbr);
               }
               Toast.makeText(getApplicationContext(), 
                       "UPDATE: Found "+results.size()+" APs!", 
                       Toast.LENGTH_SHORT).show();
               
               // populate DB
               /*
               if (wlanlist.size() > 0 ) {
                   db.updateWlanTable(wlanlist);
               }
               */

            }
        };
        registerReceiver(wlanscanReceiver, 
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));  
    
        scanTask = new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "timer triggered start scanning wlan");
                wm.startScan();
            }
        };
        
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        t.schedule(scanTask, 15000, 15000);
        
        // Register the listener with the Location Manager to receive location updates
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, ll);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, ll);

    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        scanTask.cancel();
        lm.removeUpdates(ll);
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        unregisterReceiver(wlanscanReceiver);
    }
    
    private class StartFTClient extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... query) {
            // Initialize FTClient
            if (query[0].length() == 0) {
                String token = ClientLogin.authorize(username, password);
                ftclient = new FtClient(token);
            } else {
                String result = ftclient.query(query[0]);
                Log.d(TAG, "FT Query Result: "+ result);
            }
            
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ft_ready = true;
            super.onPostExecute(result);
        }
        
    }
    
}
