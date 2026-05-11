package com.example.foottest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ItemizedIconOverlay;

import java.util.ArrayList;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private MapView mapView;
    private GeoPoint selectedLocation;
    private GeoPoint currentUserLocation;
    private Button btnConfirmerLocalisation, btnRetour;
    private EditText editTextSearch;
    private ListView suggestionsListView;
    private PlaceAutoCompleteAdapter autocompleteAdapter;
    private LocationManager locationManager;

    private LinearLayout locationStatusLayout;
    private TextView locationStatusText;
    private ProgressBar locationProgress;

    private ProgressBar searchProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuration OsmDroid
        Configuration.getInstance().load(this, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this));

        setContentView(R.layout.activity_map);

        initializeComponents();
        setupAutocomplete();
        setupMap();
        setupClickListeners();

        // Demander la permission de localisation au démarrage
        requestLocationPermission();
    }

    private void initializeComponents() {
        btnConfirmerLocalisation = findViewById(R.id.btnConfirmerLocalisation);
        btnRetour = findViewById(R.id.btnRetour);
        editTextSearch = findViewById(R.id.editTextSearch);
        suggestionsListView = findViewById(R.id.suggestionsListView);
        mapView = findViewById(R.id.mapView);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        locationStatusLayout = findViewById(R.id.locationStatusLayout);
        locationStatusText = findViewById(R.id.locationStatusText);
        locationProgress = findViewById(R.id.locationProgress);

        // Progress bar pour la recherche
        searchProgress = new ProgressBar(this);
        searchProgress.setIndeterminate(true);

        // Localisation par défaut : Oujda
        selectedLocation = new GeoPoint(34.6810, -1.9078);
        currentUserLocation = new GeoPoint(34.6810, -1.9078);

        // Adapter pour l'autocomplétion
        autocompleteAdapter = new PlaceAutoCompleteAdapter(this, 0);
        suggestionsListView.setAdapter(autocompleteAdapter);

        Log.d("MAP_ACTIVITY", "Composants initialisés");
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("PERMISSION", "Demande de permission de localisation");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            Log.d("PERMISSION", "Permission déjà accordée");
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            locationStatusLayout.setVisibility(View.VISIBLE);
            locationStatusText.setText("Localisation en cours...");
            Log.d("LOCATION", "Démarrage des mises à jour de localisation");

            // Essayer GPS d'abord
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        10000,
                        10,
                        this
                );
                Log.d("LOCATION", "GPS activé");
            }

            // Essayer le réseau aussi
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        10000,
                        10,
                        this
                );
                Log.d("LOCATION", "Réseau activé");
            }

            // Obtenir la dernière position connue
            Location lastKnownLocation = null;
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (lastKnownLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (lastKnownLocation != null) {
                Log.d("LOCATION", "Dernière position connue trouvée");
                onLocationChanged(lastKnownLocation);
            } else {
                Log.d("LOCATION", "Aucune position connue");
                locationStatusLayout.postDelayed(() -> {
                    locationStatusLayout.setVisibility(View.GONE);
                    Toast.makeText(this, "Utilisation de la position par défaut (Oujda)", Toast.LENGTH_SHORT).show();
                }, 5000);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        currentUserLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

        // Mettre à jour l'adapter avec la position actuelle
        autocompleteAdapter.setCurrentLocation(location.getLatitude(), location.getLongitude());

        locationStatusLayout.setVisibility(View.GONE);

        String message = "Position actuelle détectée: " + location.getLatitude() + ", " + location.getLongitude();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d("LOCATION", message);

        // Si l'utilisateur a déjà tapé du texte, relancer la recherche
        String currentText = editTextSearch.getText().toString().trim();
        if (!currentText.isEmpty()) {
            Log.d("SEARCH", "Relance recherche avec: " + currentText);
            autocompleteAdapter.getFilter().filter(currentText);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("PERMISSION", "Permission accordée");
                startLocationUpdates();
            } else {
                Log.d("PERMISSION", "Permission refusée");
                Toast.makeText(this, "Permission de localisation refusée", Toast.LENGTH_LONG).show();
                locationStatusLayout.setVisibility(View.GONE);
            }
        }
    }

    private void setupAutocomplete() {
        // Écouter les changements de texte
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 1) {
                    Log.d("SEARCH", "Lancement recherche: " + s.toString());
                    autocompleteAdapter.getFilter().filter(s);
                    suggestionsListView.setVisibility(View.VISIBLE);
                } else {
                    suggestionsListView.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Gérer le clic sur une suggestion
        suggestionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PlaceAutoCompleteAdapter.PlaceSuggestion suggestion = autocompleteAdapter.getItem(position);

                if (suggestion != null) {
                    editTextSearch.setText(suggestion.displayName);
                    suggestionsListView.setVisibility(View.GONE);

                    GeoPoint geoPoint = new GeoPoint(suggestion.latitude, suggestion.longitude);
                    updateMapWithLocation(geoPoint, suggestion.displayName);

                    String distanceText = suggestion.distanceInMeters > 0 ?
                            String.format(" (à %.1f km)", suggestion.distanceInMeters / 1000) : "";
                    String message = "Lieu sélectionné: " + suggestion.displayName + distanceText;
                    Toast.makeText(MapActivity.this, message, Toast.LENGTH_LONG).show();
                    Log.d("SELECTION", message);
                }
            }
        });

        editTextSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    suggestionsListView.postDelayed(() -> {
                        suggestionsListView.setVisibility(View.GONE);
                    }, 200);
                } else if (editTextSearch.getText().length() > 1) {
                    suggestionsListView.setVisibility(View.VISIBLE);
                }
            }
        });

        Log.d("AUTOCOMPLETE", "Autocomplete configuré");
    }

    private void updateMapWithLocation(GeoPoint geoPoint, String name) {
        mapView.getOverlays().clear();

        Marker marker = new Marker(mapView);
        marker.setPosition(geoPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(name);
        mapView.getOverlays().add(marker);

        mapView.getController().animateTo(geoPoint);
        mapView.getController().setZoom(16.0);

        selectedLocation = geoPoint;
        mapView.invalidate();

        Log.d("MAP", "Carte mise à jour: " + name + " (" + geoPoint.getLatitude() + ", " + geoPoint.getLongitude() + ")");
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        mapView.getController().setCenter(selectedLocation);
        mapView.getController().setZoom(12.0);

        Marker startMarker = new Marker(mapView);
        startMarker.setPosition(selectedLocation);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle("Oujda, Maroc");
        mapView.getOverlays().add(startMarker);

        // Gestion des clics sur la carte - CORRIGÉ
        mapView.getOverlays().add(new ItemizedIconOverlay<OverlayItem>(
                this,
                new ArrayList<OverlayItem>(),
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem item) {
                        return false;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) {
                        return false;
                    }
                }) {
            @Override
            public boolean onSingleTapConfirmed(android.view.MotionEvent event, MapView mapView) {
                GeoPoint tappedPoint = (GeoPoint) mapView.getProjection().fromPixels(
                        (int) event.getX(), (int) event.getY());

                selectedLocation = tappedPoint;
                MapActivity.this.mapView.getOverlays().clear();

                Marker marker = new Marker(MapActivity.this.mapView);
                marker.setPosition(tappedPoint);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setTitle("Emplacement sélectionné");
                MapActivity.this.mapView.getOverlays().add(marker);

                editTextSearch.setText(tappedPoint.getLatitude() + ", " + tappedPoint.getLongitude());
                Toast.makeText(MapActivity.this, "Emplacement sélectionné sur la carte", Toast.LENGTH_SHORT).show();

                MapActivity.this.mapView.invalidate();
                return true;
            }
        });

        Log.d("MAP", "Carte configurée");
    }

    private void setupClickListeners() {
        btnRetour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("NAVIGATION", "Retour cliqué");
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        btnConfirmerLocalisation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("CONFIRMATION", "Confirmation localisation cliquée");
                confirmLocation();
            }
        });

        editTextSearch.setOnEditorActionListener((v, actionId, event) -> {
            String locationName = editTextSearch.getText().toString().trim();
            if (!locationName.isEmpty()) {
                suggestionsListView.setVisibility(View.GONE);
                Log.d("SEARCH", "Recherche validée: " + locationName);
            }
            return true;
        });

        Log.d("CLICK_LISTENERS", "Listeners configurés");
    }

    private void confirmLocation() {
        if (selectedLocation != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("latitude", selectedLocation.getLatitude());
            resultIntent.putExtra("longitude", selectedLocation.getLongitude());
            resultIntent.putExtra("adresse", editTextSearch.getText().toString());
            setResult(RESULT_OK, resultIntent);
            finish();

            String message = "Localisation confirmée: " + editTextSearch.getText().toString();
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.d("CONFIRMATION", message);
        } else {
            Toast.makeText(this, "Veuillez sélectionner un emplacement", Toast.LENGTH_SHORT).show();
            Log.d("CONFIRMATION", "Aucun emplacement sélectionné");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        Log.d("LIFECYCLE", "MapActivity onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        Log.d("LIFECYCLE", "MapActivity onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        Log.d("LIFECYCLE", "MapActivity onDestroy");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("LOCATION", "Status changed: " + provider + " - " + status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("LOCATION", "Provider enabled: " + provider);
        Toast.makeText(this, "GPS activé: " + provider, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("LOCATION", "Provider disabled: " + provider);
        Toast.makeText(this, "GPS désactivé: " + provider, Toast.LENGTH_SHORT).show();
    }
}