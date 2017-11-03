package app.de.schmitta.fullpersoncontact;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Handler handler;
    private double latitude = -17.6900;
    private double longitude = -96.9937;
    private String time = "0";
    Marker lastPosition;
    static final String API_URL = "http://api.open-notify.org/iss-now.json";

    Polyline polyline;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        handler = new Handler();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151.0);
        lastPosition = mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        new RetrieveFeedTask().execute();
        PolylineOptions rectOptions = new PolylineOptions();
        // Get back the mutable Polyline
         polyline = mMap.addPolyline(rectOptions);
    }

    public void setNewMarker(double latitude, double longitude, String timestamp){
        long dv = Long.valueOf(timestamp)*1000;// its need to be in millisecond
        Date df = new java.util.Date(dv);
        String Datestring = new SimpleDateFormat("hh:mm:ss dd.MM.yyyy ", Locale.GERMANY).format(df);
        LatLng newPosition = new LatLng(longitude, latitude);
        List<LatLng> pointlist = polyline.getPoints();
        pointlist.add(newPosition);
        polyline.setPoints(pointlist);
        lastPosition.setPosition(newPosition);
        lastPosition.setTitle("ISS um " + Datestring);
        //mMap.addMarker(new MarkerOptions().position(newPosition).title("ISS um " + Datestring));
        mMap.animateCamera(CameraUpdateFactory.newLatLng(newPosition));
        new RetrieveFeedTask().execute();
    }

    class  RetrieveFeedTask extends AsyncTask<Void, Void, String> {

        private Exception exception;

        protected String doInBackground(Void... urls) {
            // Do some validation here

            try {
                //https://api.fullcontact.com/v2/person.json?email=bart@fullcontact.com&apiKey=d0c78f3260bf406
                URL url = new URL(API_URL);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                }
                finally{
                    urlConnection.disconnect();
                }
            }
            catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }

        }

        protected void onPostExecute(String response) {
            if(response == null) {
                response = "THERE WAS AN ERROR";
            }

            Log.i("INFO", response);

            try {
                JSONObject object = (JSONObject) new JSONTokener(response).nextValue();
                JSONObject iss_position = object.getJSONObject("iss_position");
                latitude = iss_position.getDouble("longitude");
                longitude = iss_position.getDouble("latitude");
                time = object.getString("timestamp");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setNewMarker(latitude, longitude, time);
                }
            }, 10*1000);
        }
    }
}
