package com.example.mapapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Button btnMap;
    private TextView tvStatus;
    private LocationManager locationManager;
    private RequestQueue requestQueue;

    private double latitude;
    private double longitude;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final long MIN_TIME_UPDATE = 60000; // 1 minute
    private static final float MIN_DISTANCE_UPDATE = 150; // 150 mètres

    private String insertUrl = "http://10.0.2.2/map_project/createPosition.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation
        btnMap = findViewById(R.id.btnMap);
        tvStatus = findViewById(R.id.tvStatus);
        requestQueue = Volley.newRequestQueue(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Vérification et demande des permissions
        checkAndRequestPermissions();

        // Bouton pour ouvrir la carte
        btnMap.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MapActivity.class));
        });
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Vérifier si le GPS est activé
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            tvStatus.setText("GPS désactivé. Veuillez activer le GPS.");
            Toast.makeText(this, "Veuillez activer le GPS", Toast.LENGTH_LONG).show();
            return;
        }

        tvStatus.setText("Recherche de position GPS...");

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_UPDATE,
                MIN_DISTANCE_UPDATE,
                locationListener
        );

        // Également utiliser le réseau pour une localisation plus rapide
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                MIN_TIME_UPDATE,
                MIN_DISTANCE_UPDATE,
                locationListener
        );
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            double altitude = location.getAltitude();
            float accuracy = location.getAccuracy();

            String msg = String.format(Locale.FRANCE,
                    "Position: %.6f, %.6f\nAltitude: %.1fm, Précision: %.1fm",
                    latitude, longitude, altitude, accuracy);

            tvStatus.setText(msg);

            // Envoi au serveur
            sendPositionToServer(latitude, longitude);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            tvStatus.setText("GPS désactivé");
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            tvStatus.setText("GPS activé, recherche de position...");
        }
    };

    private void sendPositionToServer(final double lat, final double lon) {
        StringRequest request = new StringRequest(Request.Method.POST, insertUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Succès - pas d'action nécessaire
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Erreur silencieuse pour ne pas déranger l'utilisateur
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE);

                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("date", sdf.format(new Date()));

                // Identifiant unique de l'appareil
                String androidId = Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ANDROID_ID
                );
                params.put("imei", androidId);

                return params;
            }
        };

        requestQueue.add(request);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Permission de localisation refusée", Toast.LENGTH_SHORT).show();
                tvStatus.setText("Permission refusée - application limitée");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Arrêter les mises à jour pour économiser la batterie
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }
}