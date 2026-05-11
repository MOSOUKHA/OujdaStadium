package com.example.foottest;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaceAutoCompleteAdapter extends ArrayAdapter<PlaceAutoCompleteAdapter.PlaceSuggestion> implements Filterable {

    private List<PlaceSuggestion> suggestions;
    private Context context;
    private Location currentLocation;
    private ExecutorService executorService;

    public static class PlaceSuggestion {
        public String displayName;
        public String fullAddress;
        public double latitude;
        public double longitude;
        public float distanceInMeters;

        public PlaceSuggestion(String displayName, String fullAddress, double latitude, double longitude) {
            this.displayName = displayName;
            this.fullAddress = fullAddress;
            this.latitude = latitude;
            this.longitude = longitude;
            this.distanceInMeters = 0;
        }
    }

    public PlaceAutoCompleteAdapter(@NonNull Context context, int resource) {
        super(context, resource);
        this.context = context;
        this.suggestions = new ArrayList<>();
        this.currentLocation = null;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void setCurrentLocation(double latitude, double longitude) {
        this.currentLocation = new Location("");
        this.currentLocation.setLatitude(latitude);
        this.currentLocation.setLongitude(longitude);
    }

    @Override
    public int getCount() {
        return suggestions.size();
    }

    @Nullable
    @Override
    public PlaceSuggestion getItem(int position) {
        return suggestions.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_autocomplete, parent, false);
        }

        TextView primaryText = convertView.findViewById(R.id.primaryText);
        TextView secondaryText = convertView.findViewById(R.id.secondaryText);
        TextView distanceText = convertView.findViewById(R.id.distanceText);

        PlaceSuggestion suggestion = suggestions.get(position);

        primaryText.setText(suggestion.displayName);
        secondaryText.setText(suggestion.fullAddress);

        // Afficher la distance
        if (suggestion.distanceInMeters > 0) {
            if (suggestion.distanceInMeters < 1000) {
                distanceText.setText(String.format(Locale.getDefault(), "%.0f m", suggestion.distanceInMeters));
            } else {
                distanceText.setText(String.format(Locale.getDefault(), "%.1f km", suggestion.distanceInMeters / 1000));
            }
            distanceText.setVisibility(View.VISIBLE);
        } else {
            distanceText.setVisibility(View.GONE);
        }

        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<PlaceSuggestion> filteredSuggestions = new ArrayList<>();

                if (constraint != null && constraint.length() > 1) {
                    try {
                        String searchText = constraint.toString().trim();
                        Log.d("API_SEARCH", "Recherche: " + searchText);

                        // Recherche avec OpenStreetMap
                        List<PlaceSuggestion> apiResults = searchWithOpenStreetMap(searchText);

                        if (apiResults != null && !apiResults.isEmpty()) {
                            filteredSuggestions.addAll(apiResults);
                            Log.d("API_SEARCH", "Résultats trouvés: " + apiResults.size());
                        } else {
                            Log.d("API_SEARCH", "Aucun résultat trouvé");
                        }

                        // Calculer et trier par distance
                        if (currentLocation != null && !filteredSuggestions.isEmpty()) {
                            for (PlaceSuggestion suggestion : filteredSuggestions) {
                                Location placeLocation = new Location("");
                                placeLocation.setLatitude(suggestion.latitude);
                                placeLocation.setLongitude(suggestion.longitude);
                                suggestion.distanceInMeters = currentLocation.distanceTo(placeLocation);
                            }

                            Collections.sort(filteredSuggestions, new Comparator<PlaceSuggestion>() {
                                @Override
                                public int compare(PlaceSuggestion p1, PlaceSuggestion p2) {
                                    return Float.compare(p1.distanceInMeters, p2.distanceInMeters);
                                }
                            });
                        }

                    } catch (Exception e) {
                        Log.e("API_SEARCH", "Erreur lors de la recherche", e);
                        e.printStackTrace();
                    }
                }

                results.values = filteredSuggestions;
                results.count = filteredSuggestions.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                suggestions.clear();
                if (results.values != null) {
                    suggestions.addAll((List<PlaceSuggestion>) results.values);
                }
                if (results.count > 0) {
                    notifyDataSetChanged();
                    Log.d("API_SEARCH", "Suggestions affichées: " + results.count);
                } else {
                    notifyDataSetInvalidated();
                    Log.d("API_SEARCH", "Aucune suggestion à afficher");
                }
            }
        };
    }

    private List<PlaceSuggestion> searchWithOpenStreetMap(String query) {
        List<PlaceSuggestion> results = new ArrayList<>();
        HttpURLConnection connection = null;

        try {
            // Construire l'URL de recherche AVEC USER-AGENT CORRECT
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String urlString = "https://nominatim.openstreetmap.org/search?" +
                    "format=json&" +
                    "q=" + encodedQuery + "&" +
                    "limit=10&" +
                    "addressdetails=1&" +
                    "countrycodes=ma&" +  // Limiter au Maroc
                    "bounded=1";

            Log.d("API_URL", "URL: " + urlString);

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // USER-AGENT SPÉCIFIQUE POUR ÉVITER LE 403
            connection.setRequestProperty("User-Agent", "OujdaFootApp/1.0");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9");

            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            int responseCode = connection.getResponseCode();
            Log.d("API_RESPONSE", "Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d("API_RESPONSE", "Réponse reçue, longueur: " + response.length());

                // Parser la réponse JSON
                JSONArray jsonArray = new JSONArray(response.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject place = jsonArray.getJSONObject(i);

                    String displayName = place.getString("display_name");
                    double lat = place.getDouble("lat");
                    double lon = place.getDouble("lon");

                    // Extraire un nom court pour l'affichage
                    String shortName = extractShortName(displayName, place);

                    PlaceSuggestion suggestion = new PlaceSuggestion(shortName, displayName, lat, lon);
                    results.add(suggestion);

                    Log.d("API_RESULT", "Lieu trouvé: " + shortName + " (" + lat + ", " + lon + ")");
                }
            } else {
                Log.e("API_ERROR", "Erreur HTTP: " + responseCode);

                // En cas d'erreur 403, utiliser des données de test
                if (responseCode == 403) {
                    Log.d("API_FALLBACK", "Utilisation des données de test");
                    results = getTestData(query);
                }
            }

        } catch (Exception e) {
            Log.e("API_ERROR", "Erreur réseau: " + e.getMessage());

            // En cas d'erreur, utiliser des données de test
            results = getTestData(query);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return results;
    }

    // Données de test pour Oujda
    private List<PlaceSuggestion> getTestData(String query) {
        List<PlaceSuggestion> testData = new ArrayList<>();

        // Lieux populaires à Oujda
        String[] oujdaPlaces = {
                "Dhar Lamhala", "Hay Al Qods", "Medina Oujda", "Hay Nassim",
                "Complexe Sportif", "Stade d'Honneur", "Université Mohammed Premier",
                "Place du 16 Août", "Hay Essalam", "Centre Ville Oujda"
        };

        // Coordonnées approximatives pour Oujda
        double baseLat = 34.6860;
        double baseLon = -1.9000;

        for (String placeName : oujdaPlaces) {
            if (placeName.toLowerCase().contains(query.toLowerCase())) {
                // Générer des coordonnées légèrement différentes pour chaque lieu
                double lat = baseLat + (Math.random() * 0.02 - 0.01);
                double lon = baseLon + (Math.random() * 0.02 - 0.01);

                PlaceSuggestion suggestion = new PlaceSuggestion(
                        placeName,
                        placeName + ", Oujda, Maroc",
                        lat,
                        lon
                );
                testData.add(suggestion);

                Log.d("TEST_DATA", "Lieu test ajouté: " + placeName);
            }
        }

        return testData;
    }

    private String extractShortName(String fullDisplayName, JSONObject place) {
        try {
            // Essayer d'extraire le nom spécifique
            if (place.has("name") && !place.isNull("name")) {
                String name = place.getString("name");
                if (!name.isEmpty() && !name.equals("null") && !name.equals(fullDisplayName)) {
                    return name;
                }
            }

            // Essayer d'extraire le quartier
            if (place.has("address")) {
                JSONObject address = place.getJSONObject("address");
                if (address.has("suburb") && !address.isNull("suburb")) {
                    return address.getString("suburb");
                }
                if (address.has("neighbourhood") && !address.isNull("neighbourhood")) {
                    return address.getString("neighbourhood");
                }
                if (address.has("quarter") && !address.isNull("quarter")) {
                    return address.getString("quarter");
                }
                if (address.has("road") && !address.isNull("road")) {
                    return address.getString("road");
                }
            }

            // Utiliser la première partie du display_name
            String[] parts = fullDisplayName.split(",");
            if (parts.length > 0) {
                String firstPart = parts[0].trim();
                // Éviter les noms trop génériques
                if (!firstPart.equalsIgnoreCase("Oujda") &&
                        !firstPart.equalsIgnoreCase("Maroc") &&
                        !firstPart.equalsIgnoreCase("Morocco") &&
                        !firstPart.equalsIgnoreCase("ⴰⵄⵓⵊⴷⴰ")) {
                    return firstPart;
                }
            }

            // Fallback intelligent
            for (String part : fullDisplayName.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() &&
                        !trimmed.equalsIgnoreCase("Oujda") &&
                        !trimmed.equalsIgnoreCase("Maroc") &&
                        !trimmed.equalsIgnoreCase("Morocco") &&
                        !trimmed.equalsIgnoreCase("ⴰⵄⵓⵊⴷⴰ") &&
                        trimmed.length() > 3) {
                    return trimmed;
                }
            }

        } catch (Exception e) {
            Log.e("EXTRACT_NAME", "Erreur extraction nom", e);
        }

        // Fallback final
        return fullDisplayName.length() > 40 ?
                fullDisplayName.substring(0, 40) + "..." : fullDisplayName;
    }

    @Override
    protected void finalize() throws Throwable {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        super.finalize();
    }
}