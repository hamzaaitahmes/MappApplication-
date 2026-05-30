package com.example.mapapplication;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;

public class MapActivity extends AppCompatActivity {

    private MapView mapView;
    private ProgressBar progressBar;
    private RequestQueue requestQueue;

    private String getPositionsUrl = "http://10.0.2.2/map_project/getPosition.php";
    private ArrayList<GeoPoint> positions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuration d'OSMDroid
        Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences("osmdroid_prefs", MODE_PRIVATE)
        );

        setContentView(R.layout.activity_map);

        // Initialisation
        mapView = findViewById(R.id.map);
        progressBar = findViewById(R.id.progressBar);
        requestQueue = Volley.newRequestQueue(this);

        // Configuration de la carte
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        // Centre initial (Paris)
        mapView.getController().setCenter(new GeoPoint(48.8566, 2.3522));

        // Chargement des positions
        loadPositions();
    }

    private void loadPositions() {
        if (progressBar != null) {
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                getPositionsUrl,
                null,
                new com.android.volley.Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (progressBar != null) {
                            progressBar.setVisibility(ProgressBar.GONE);
                        }
                        try {
                            if (response.getBoolean("success")) {
                                JSONArray positionsArray = response.getJSONArray("positions");

                                if (mapView != null) {
                                    mapView.getOverlays().clear();
                                }

                                for (int i = 0; i < positionsArray.length(); i++) {
                                    JSONObject position = positionsArray.getJSONObject(i);
                                    double lat = position.getDouble("latitude");
                                    double lng = position.getDouble("longitude");

                                    addMarker(lat, lng, "Position " + (i + 1));
                                    positions.add(new GeoPoint(lat, lng));
                                }

                                if (positions.size() > 0 && mapView != null) {
                                    mapView.getController().setCenter(positions.get(0));
                                }

                                Toast.makeText(MapActivity.this,
                                        positions.size() + " positions chargées",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MapActivity.this,
                                        "Erreur: " + response.getString("message"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MapActivity.this,
                                    "Erreur de lecture des données",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(com.android.volley.VolleyError error) {
                        if (progressBar != null) {
                            progressBar.setVisibility(ProgressBar.GONE);
                        }
                        Toast.makeText(MapActivity.this,
                                "Erreur réseau: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestQueue.add(request);
    }

    private void addMarker(double latitude, double longitude, String title) {
        if (mapView == null) return;

        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(latitude, longitude));
        marker.setTitle(title);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        mapView.getOverlays().add(marker);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDetach();
        }
    }
}