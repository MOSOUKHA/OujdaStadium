package com.example.foottest;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.widget.ImageView;
import androidx.appcompat.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class GestionStadesActivity extends AppCompatActivity {

    private FireBaseManager firebaseManager;
    private LinearLayout stadesContainer;
    private Button btnAjouterStade;
    private EditText searchBar; // Search bar reference
    private BottomNavigationView bottomNavigationView;
    private String userEmail;
    private String userId;
    private ImageView menuIcon;

    private List<Stade> allStades = new ArrayList<>(); // All loaded stades
    private List<Stade> filteredStades = new ArrayList<>(); // Filtered stades for search
    private static final int MAP_REQUEST_CODE = 1002;

    // Variables to store location data between activities
    private double tempSelectedLatitude = 0;
    private double tempSelectedLongitude = 0;
    private String tempSelectedAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_stades_with_nav);

        firebaseManager = new FireBaseManager();

        // Récupérer l'email et l'ID de l'utilisateur
        userEmail = getIntent().getStringExtra("email");
        userId = getIntent().getStringExtra("userId");

        // Initialiser les vues du layout original
        stadesContainer = findViewById(R.id.stadesContainer);
        btnAjouterStade = findViewById(R.id.btnAjouterStade);
        searchBar = findViewById(R.id.searchBar); // Make sure this exists in your XML


        // Setup search functionality
        setupSearchBar();

        chargerStades();
        // Initialize menu icon
        menuIcon = findViewById(R.id.menuIcon);

// Set click listener for menu icon
        menuIcon.setOnClickListener(v -> {
            showTopMenu(v);
        });

        // Initialiser la bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_stades) {
                    // On est déjà sur cette page
                    return true;
                } else if (id == R.id.nav_reservations) {
                    // Aller aux réservations
                    Intent intent = new Intent(GestionStadesActivity.this, MesReservationsActivity.class);
                    intent.putExtra("email", userEmail);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_profile) {
                    // Navigate to ProfileActivity
                    Intent intent = new Intent(GestionStadesActivity.this, ProfileActivity.class);
                    intent.putExtra("email", userEmail);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            }
        });

        // Sélectionner l'onglet "Stades" par défaut
        bottomNavigationView.setSelectedItemId(R.id.nav_stades);

        // CHANGEMENT ICI: Utiliser le même dialogue pour ajouter un stade
        btnAjouterStade.setOnClickListener(v -> {
            showAddStadeDialog();
        });
    }
    private void showTopMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.top_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.menu_settings) {
                    Toast.makeText(GestionStadesActivity.this, "Paramètres", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.menu_about) {
                    Toast.makeText(GestionStadesActivity.this, "À propos", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.menu_logout) {
                    // Handle logout
                    SessionManager sessionManager = new SessionManager(GestionStadesActivity.this);
                    sessionManager.logout();

                    Intent intent = new Intent(GestionStadesActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            }
        });

        popup.show();
    }

    private void setupSearchBar() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStades(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    private void filterStades(String searchText) {
        filteredStades.clear();

        if (searchText.isEmpty()) {
            filteredStades.addAll(allStades);
        } else {
            String searchLowerCase = searchText.toLowerCase();
            for (Stade stade : allStades) {
                if (stade.getNomStade().toLowerCase().contains(searchLowerCase)) {
                    filteredStades.add(stade);
                }
            }
        }

        displayFilteredStades();
    }

    private void displayFilteredStades() {
        stadesContainer.removeAllViews();

        if (filteredStades.isEmpty()) {
            if (searchBar.getText().toString().isEmpty()) {
                TextView tvAucunStade = new TextView(GestionStadesActivity.this);
                tvAucunStade.setText("Aucun stade ajouté");
                tvAucunStade.setTextSize(16);
                tvAucunStade.setPadding(16, 16, 16, 16);
                stadesContainer.addView(tvAucunStade);
            } else {
                TextView tvNoResults = new TextView(GestionStadesActivity.this);
                tvNoResults.setText("Aucun stade trouvé pour: \"" + searchBar.getText().toString() + "\"");
                tvNoResults.setTextSize(16);
                tvNoResults.setPadding(16, 16, 16, 16);
                tvNoResults.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                stadesContainer.addView(tvNoResults);
            }
        } else {
            for (int i = 0; i < filteredStades.size(); i++) {
                ajouterVueStade(filteredStades.get(i), i);
            }
        }
    }

    private void chargerStades() {
        stadesContainer.removeAllViews();
        allStades.clear();
        filteredStades.clear();

        if (userId == null || userId.isEmpty()) {
            TextView tvErreur = new TextView(this);
            tvErreur.setText("Erreur: Utilisateur non identifié");
            tvErreur.setTextSize(16);
            tvErreur.setPadding(16, 16, 16, 16);
            stadesContainer.addView(tvErreur);
            return;
        }

        firebaseManager.getStadesByUserId(userId, new FireBaseManager.OnStadesLoadedListener() {
            @Override
            public void onStadesLoaded(List<Stade> stades) {
                runOnUiThread(() -> {
                    allStades.clear();
                    allStades.addAll(stades);

                    // Apply current search filter if any
                    String currentSearch = searchBar.getText().toString();
                    if (currentSearch.isEmpty()) {
                        filteredStades.clear();
                        filteredStades.addAll(allStades);
                        displayFilteredStades();
                    } else {
                        filterStades(currentSearch);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(GestionStadesActivity.this, "Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();

                    TextView tvErreur = new TextView(GestionStadesActivity.this);
                    tvErreur.setText("Erreur de chargement: " + errorMessage);
                    tvErreur.setTextSize(16);
                    tvErreur.setPadding(16, 16, 16, 16);
                    stadesContainer.addView(tvErreur);
                });
            }
        });
    }

    private void ajouterVueStade(Stade stade, int position) {
        LinearLayout stadeLayout = new LinearLayout(this);
        stadeLayout.setOrientation(LinearLayout.VERTICAL);
        stadeLayout.setPadding(16, 16, 16, 16);
        stadeLayout.setBackgroundResource(R.drawable.edittext_background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        stadeLayout.setLayoutParams(params);

        // Make the layout clickable
        stadeLayout.setClickable(true);
        stadeLayout.setOnClickListener(v -> showStadeOptions(stade, getOriginalPosition(stade)));

        // Nom du stade
        TextView tvNom = new TextView(this);
        tvNom.setText(stade.getNomStade());
        tvNom.setTextSize(18);
        tvNom.setTypeface(null, Typeface.BOLD);
        stadeLayout.addView(tvNom);

        // Description
        TextView tvDescription = new TextView(this);
        tvDescription.setText(stade.getDescription());
        tvDescription.setTextSize(14);
        tvDescription.setPadding(0, 8, 0, 8);
        stadeLayout.addView(tvDescription);

        // Capacité et prix
        TextView tvDetails = new TextView(this);
        tvDetails.setText("Capacité: " + stade.getCapacite() + " personnes | Prix: " + stade.getPrixHeure() + " DH/heure");
        tvDetails.setTextSize(14);
        tvDetails.setPadding(0, 0, 0, 8);
        stadeLayout.addView(tvDetails);

        // Adresse
        TextView tvAdresse = new TextView(this);
        tvAdresse.setText("Adresse: " + stade.getAdresse());
        tvAdresse.setTextSize(12);
        tvAdresse.setTextColor(0xFF666666);
        stadeLayout.addView(tvAdresse);

        // Nombre de photos
        List<String> photos = stade.getPhotos();
        TextView tvPhotos = new TextView(this);
        tvPhotos.setText("Photos: " + (photos != null ? photos.size() : 0) + " image(s)");
        tvPhotos.setTextSize(12);
        tvPhotos.setTextColor(0xFF666666);
        stadeLayout.addView(tvPhotos);

        // Add click indicator
        TextView tvClickIndicator = new TextView(this);
        tvClickIndicator.setText("👆 Cliquez pour modifier/supprimer");
        tvClickIndicator.setTextSize(10);
        tvClickIndicator.setTextColor(0xFF4CAF50);
        tvClickIndicator.setPadding(0, 8, 0, 0);
        stadeLayout.addView(tvClickIndicator);

        stadesContainer.addView(stadeLayout);
    }

    // Helper method to get original position in allStades list
    private int getOriginalPosition(Stade stade) {
        for (int i = 0; i < allStades.size(); i++) {
            if (allStades.get(i).getId().equals(stade.getId())) {
                return i;
            }
        }
        return -1;
    }

    private void showStadeOptions(Stade stade, int originalPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Options pour: " + stade.getNomStade());
        builder.setItems(new CharSequence[]{"Modifier", "Supprimer"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        // Modifier
                        showModifyStadeDialog(stade, originalPosition);
                        break;
                    case 1:
                        // Supprimer
                        showDeleteConfirmation(stade, originalPosition);
                        break;
                }
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void showDeleteConfirmation(Stade stade, int originalPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmer la suppression");
        builder.setMessage("Êtes-vous sûr de vouloir supprimer le stade \"" + stade.getNomStade() + "\" ?");
        builder.setPositiveButton("Supprimer", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteStade(stade.getId(), originalPosition);
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void deleteStade(String stadeId, int originalPosition) {
        firebaseManager.deleteStade(stadeId, new FireBaseManager.OnOperationListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(GestionStadesActivity.this, "Stade supprimé avec succès!", Toast.LENGTH_SHORT).show();
                    // Remove from both lists
                    if (originalPosition < allStades.size()) {
                        allStades.remove(originalPosition);
                    }
                    // Refresh with current search filter
                    filterStades(searchBar.getText().toString());
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(GestionStadesActivity.this, "Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // NOUVELLE MÉTHODE: Pour ajouter un nouveau stade
    private void showAddStadeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ajouter un nouveau stade");

        // Create dialog view
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_modify_stade, null);
        builder.setView(dialogView);

        // Initialize dialog fields with empty values
        EditText etNomStade = dialogView.findViewById(R.id.etNomStade);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        EditText etCapacite = dialogView.findViewById(R.id.etCapacite);
        EditText etPrix = dialogView.findViewById(R.id.etPrix);
        EditText etAdresse = dialogView.findViewById(R.id.etAdresse);
        Button btnChoisirLocalisation = dialogView.findViewById(R.id.btnChoisirLocalisation);
        TextView tvLocalisation = dialogView.findViewById(R.id.tvLocalisation);

        // Clear all fields for new stade
        etNomStade.setText("");
        etDescription.setText("");
        etCapacite.setText("");
        etPrix.setText("");
        etAdresse.setText("");
        tvLocalisation.setText("Aucune localisation sélectionnée");

        // Reset location data
        tempSelectedLatitude = 0;
        tempSelectedLongitude = 0;
        tempSelectedAddress = "";

        // Map button click listener
        btnChoisirLocalisation.setOnClickListener(v -> {
            Intent intent = new Intent(GestionStadesActivity.this, MapActivity.class);
            // Pass current location if available
            if (tempSelectedLatitude != 0 && tempSelectedLongitude != 0) {
                intent.putExtra("latitude", tempSelectedLatitude);
                intent.putExtra("longitude", tempSelectedLongitude);
                intent.putExtra("adresse", tempSelectedAddress);
            }
            startActivityForResult(intent, MAP_REQUEST_CODE);
        });

        AlertDialog dialog = builder.create();

        builder.setPositiveButton("Ajouter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Validate inputs
                String nomStade = etNomStade.getText().toString().trim();
                String description = etDescription.getText().toString().trim();
                String capaciteStr = etCapacite.getText().toString().trim();
                String prixStr = etPrix.getText().toString().trim();
                String adresse = etAdresse.getText().toString().trim();

                if (nomStade.isEmpty()) {
                    Toast.makeText(GestionStadesActivity.this, "Le nom du stade est requis", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (description.isEmpty()) {
                    Toast.makeText(GestionStadesActivity.this, "La description est requise", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (capaciteStr.isEmpty()) {
                    Toast.makeText(GestionStadesActivity.this, "La capacité est requise", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (prixStr.isEmpty()) {
                    Toast.makeText(GestionStadesActivity.this, "Le prix est requis", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if location is selected
                if (tempSelectedLatitude == 0 && tempSelectedLongitude == 0 && adresse.isEmpty()) {
                    Toast.makeText(GestionStadesActivity.this, "Veuillez sélectionner une localisation sur la carte", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int capacite = Integer.parseInt(capaciteStr);
                    if (capacite <= 0) {
                        Toast.makeText(GestionStadesActivity.this, "La capacité doit être positive", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(GestionStadesActivity.this, "Capacité invalide", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double prix = Double.parseDouble(prixStr);
                    if (prix <= 0) {
                        Toast.makeText(GestionStadesActivity.this, "Le prix doit être positif", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(GestionStadesActivity.this, "Prix invalide", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create new Stade object
                Stade newStade = new Stade();
                newStade.setNomStade(nomStade);
                newStade.setDescription(description);
                newStade.setCapacite(Integer.parseInt(capaciteStr));
                newStade.setPrixHeure(Double.parseDouble(prixStr));
                newStade.setLatitude(tempSelectedLatitude);
                newStade.setLongitude(tempSelectedLongitude);

                // Use address from map if available, otherwise use the text field
                if (!tempSelectedAddress.isEmpty()) {
                    newStade.setAdresse(tempSelectedAddress);
                } else {
                    newStade.setAdresse(adresse);
                }

                newStade.setUserId(userId); // Set the user ID

                // Add to Firebase - IMPORTANT: Use the uploadPhotosAndAddStade method
                addStadeToFirebase(newStade);
            }
        });

        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void showModifyStadeDialog(Stade stade, int originalPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifier: " + stade.getNomStade());

        // Create dialog view
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_modify_stade, null);
        builder.setView(dialogView);

        // Initialize dialog fields with current values
        EditText etNomStade = dialogView.findViewById(R.id.etNomStade);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        EditText etCapacite = dialogView.findViewById(R.id.etCapacite);
        EditText etPrix = dialogView.findViewById(R.id.etPrix);
        EditText etAdresse = dialogView.findViewById(R.id.etAdresse);
        Button btnChoisirLocalisation = dialogView.findViewById(R.id.btnChoisirLocalisation);
        TextView tvLocalisation = dialogView.findViewById(R.id.tvLocalisation);

        // Set current values
        etNomStade.setText(stade.getNomStade());
        etDescription.setText(stade.getDescription());
        etCapacite.setText(String.valueOf(stade.getCapacite()));
        etPrix.setText(String.valueOf(stade.getPrixHeure()));
        etAdresse.setText(stade.getAdresse());
        tvLocalisation.setText(stade.getAdresse());

        // Store location data for this specific dialog
        tempSelectedLatitude = stade.getLatitude();
        tempSelectedLongitude = stade.getLongitude();
        tempSelectedAddress = stade.getAdresse();

        // Map button click listener
        btnChoisirLocalisation.setOnClickListener(v -> {
            Intent intent = new Intent(GestionStadesActivity.this, MapActivity.class);
            intent.putExtra("latitude", tempSelectedLatitude);
            intent.putExtra("longitude", tempSelectedLongitude);
            intent.putExtra("adresse", tempSelectedAddress);
            startActivityForResult(intent, MAP_REQUEST_CODE);
        });

        AlertDialog dialog = builder.create();

        builder.setPositiveButton("Enregistrer", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Validate inputs
                String nomStade = etNomStade.getText().toString().trim();
                String description = etDescription.getText().toString().trim();
                String capaciteStr = etCapacite.getText().toString().trim();
                String prixStr = etPrix.getText().toString().trim();
                String adresse = etAdresse.getText().toString().trim();

                if (nomStade.isEmpty()) {
                    Toast.makeText(GestionStadesActivity.this, "Le nom du stade est requis", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (description.isEmpty()) {
                    Toast.makeText(GestionStadesActivity.this, "La description est requise", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (capaciteStr.isEmpty()) {
                    Toast.makeText(GestionStadesActivity.this, "La capacité est requise", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (prixStr.isEmpty()) {
                    Toast.makeText(GestionStadesActivity.this, "Le prix est requis", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int capacite = Integer.parseInt(capaciteStr);
                    if (capacite <= 0) {
                        Toast.makeText(GestionStadesActivity.this, "La capacité doit être positive", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(GestionStadesActivity.this, "Capacité invalide", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double prix = Double.parseDouble(prixStr);
                    if (prix <= 0) {
                        Toast.makeText(GestionStadesActivity.this, "Le prix doit être positif", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(GestionStadesActivity.this, "Prix invalide", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update stade with new values
                stade.setNomStade(nomStade);
                stade.setDescription(description);
                stade.setCapacite(Integer.parseInt(capaciteStr));
                stade.setPrixHeure(Double.parseDouble(prixStr));
                stade.setLatitude(tempSelectedLatitude);
                stade.setLongitude(tempSelectedLongitude);

                // Use the address from map if available, otherwise use the text field
                if (!tempSelectedAddress.isEmpty()) {
                    stade.setAdresse(tempSelectedAddress);
                } else {
                    stade.setAdresse(adresse);
                }

                // Update in Firebase
                updateStadeInFirebase(stade);
            }
        });

        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void addStadeToFirebase(Stade stade) {
        // Create an empty list for photos (you can modify this to add photos later)
        List<Uri> photoUris = new ArrayList<>();

        // Use the uploadPhotosAndAddStade method (same as in LoginActivity)
        firebaseManager.uploadPhotosAndAddStade(stade, photoUris, new FireBaseManager.OnStadeOperationListener() {
            @Override
            public void onSuccess(Stade addedStade) {
                runOnUiThread(() -> {
                    Toast.makeText(GestionStadesActivity.this, "Stade ajouté avec succès!", Toast.LENGTH_SHORT).show();
                    // Refresh the list with current search filter
                    chargerStades();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(GestionStadesActivity.this, "Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        },GestionStadesActivity.this);
    }

    private void updateStadeInFirebase(Stade stade) {
        firebaseManager.updateStade(stade.getId(), stade, new FireBaseManager.OnStadeOperationListener() {
            @Override
            public void onSuccess(Stade updatedStade) {
                runOnUiThread(() -> {
                    Toast.makeText(GestionStadesActivity.this, "Stade mis à jour avec succès!", Toast.LENGTH_SHORT).show();
                    // Refresh the list with current search filter
                    chargerStades();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(GestionStadesActivity.this, "Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            tempSelectedLatitude = data.getDoubleExtra("latitude", 0);
            tempSelectedLongitude = data.getDoubleExtra("longitude", 0);
            tempSelectedAddress = data.getStringExtra("adresse");

            if (tempSelectedAddress != null && !tempSelectedAddress.isEmpty()) {
                Toast.makeText(this, "Localisation mise à jour: " + tempSelectedAddress, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Localisation mise à jour avec succès", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les stades quand on revient sur cette activité
        chargerStades();
    }
}