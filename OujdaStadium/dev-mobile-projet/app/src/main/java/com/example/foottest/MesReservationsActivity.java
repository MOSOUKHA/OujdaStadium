package com.example.foottest;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
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

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MesReservationsActivity extends AppCompatActivity {

    private FireBaseManager firebaseManager;
    private LinearLayout reservationsContainer;
    private ImageView menuIcon;
    private EditText searchBar;
    private String userId;
    private String userEmail;
    private List<Reservation> allReservations = new ArrayList<>();
    private List<Reservation> filteredReservations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mes_reservations_with_nav);

        firebaseManager = new FireBaseManager();

        userId = getIntent().getStringExtra("userId");
        userEmail = getIntent().getStringExtra("email");
        // Initialize menu icon
        menuIcon = findViewById(R.id.menuIcon);

        // Set click listener for menu icon
        menuIcon.setOnClickListener(v -> {
            showTopMenu(v);
        });
        reservationsContainer = findViewById(R.id.reservationsContainer);

        searchBar = findViewById(R.id.searchBar);


        setupSearchBar();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_reservations) {
                return true;
            } else if (id == R.id.nav_stades) {
                Intent intent = new Intent(MesReservationsActivity.this, GestionStadesActivity.class);
                intent.putExtra("email", userEmail);
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {  // Changed from nav_compte to nav_profile
                // Navigate to ProfileActivity
                Intent intent = new Intent(MesReservationsActivity.this, ProfileActivity.class);
                intent.putExtra("email", userEmail);
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_reservations);

        chargerReservations();

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
                    Toast.makeText(MesReservationsActivity.this, "Paramètres", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.menu_about) {
                    Toast.makeText(MesReservationsActivity.this, "À propos", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.menu_logout) {
                    // Handle logout
                    SessionManager sessionManager = new SessionManager(MesReservationsActivity.this);
                    sessionManager.logout();

                    Intent intent = new Intent(MesReservationsActivity.this, MainActivity.class);
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
                filterReservations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    private void filterReservations(String searchText) {
        filteredReservations.clear();

        if (searchText.isEmpty()) {
            filteredReservations.addAll(allReservations);
        } else {
            String searchLowerCase = searchText.toLowerCase();
            for (Reservation reservation : allReservations) {
                if (reservation.getStadeName().toLowerCase().contains(searchLowerCase)) {
                    filteredReservations.add(reservation);
                }
            }
        }

        displayFilteredReservations();
    }

    private void displayFilteredReservations() {
        reservationsContainer.removeAllViews();

        if (filteredReservations.isEmpty()) {
            if (searchBar.getText().toString().isEmpty()) {
                afficherMessage("Aucune réservation pour vos stades");
            } else {
                afficherMessage("Aucune réservation trouvée pour: " + searchBar.getText().toString());
            }
        } else {
            for (Reservation reservation : filteredReservations) {
                ajouterVueReservation(reservation);
            }
        }
    }

    private void chargerReservations() {
        reservationsContainer.removeAllViews();

        if (userId == null || userId.isEmpty()) {
            afficherMessage("Erreur: Utilisateur non identifié");
            return;
        }

        firebaseManager.getReservationsByOwnerId(userId, new FireBaseManager.OnReservationsLoadedListener() {
            @Override
            public void onReservationsLoaded(List<Reservation> reservations) {
                runOnUiThread(() -> {
                    allReservations.clear();
                    allReservations.addAll(reservations);

                    // Apply current search filter if any
                    String currentSearch = searchBar.getText().toString();
                    if (currentSearch.isEmpty()) {
                        filteredReservations.clear();
                        filteredReservations.addAll(allReservations);
                        displayFilteredReservations();
                    } else {
                        filterReservations(currentSearch);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MesReservationsActivity.this,
                            "Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                    afficherMessage("Erreur de chargement: " + errorMessage);
                });
            }
        });
    }

    private void ajouterVueReservation(Reservation reservation) {
        LinearLayout reservationLayout = new LinearLayout(this);
        reservationLayout.setOrientation(LinearLayout.VERTICAL);
        reservationLayout.setPadding(16, 16, 16, 16);
        reservationLayout.setBackgroundResource(R.drawable.edittext_background);

        reservationLayout.setClickable(true);
        reservationLayout.setFocusable(true);
        reservationLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReservationDialog(reservation);
            }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        reservationLayout.setLayoutParams(params);

        TextView tvStadeName = new TextView(this);
        tvStadeName.setText(reservation.getStadeName());
        tvStadeName.setTextSize(18);
        tvStadeName.setTypeface(null, android.graphics.Typeface.BOLD);
        reservationLayout.addView(tvStadeName);

        TextView tvDateCreneau = new TextView(this);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String dateStr = reservation.getDateReservation() != null ?
                sdf.format(reservation.getDateReservation()) : "Date non définie";

        tvDateCreneau.setText(dateStr + " - " + reservation.getCreneau());
        tvDateCreneau.setTextSize(14);
        tvDateCreneau.setPadding(0, 4, 0, 4);
        reservationLayout.addView(tvDateCreneau);

        TextView tvDetails = new TextView(this);
        tvDetails.setText("Joueurs: " + reservation.getNombreJoueurs() +
                " | Prix: " + reservation.getPrixTotal() + " DH");
        tvDetails.setTextSize(14);
        tvDetails.setPadding(0, 4, 0, 4);
        reservationLayout.addView(tvDetails);

        TextView tvStatut = new TextView(this);
        tvStatut.setText("Statut: " + reservation.getStatut());
        tvStatut.setTextSize(14);
        tvStatut.setTypeface(null, android.graphics.Typeface.BOLD);

        switch (reservation.getStatut()) {
            case "confirmée":
                tvStatut.setTextColor(0xFF4CAF50);
                break;
            case "annulée":
                tvStatut.setTextColor(0xFFF44336);
                break;
            case "terminée":
                tvStatut.setTextColor(0xFF2196F3);
                break;
            default:
                tvStatut.setTextColor(0xFF000000);
        }

        reservationLayout.addView(tvStatut);

        if (reservation.getNotes() != null && !reservation.getNotes().isEmpty()) {
            TextView tvNotes = new TextView(this);
            tvNotes.setText("Notes: " + reservation.getNotes());
            tvNotes.setTextSize(12);
            tvNotes.setTextColor(0xFF666666);
            tvNotes.setPadding(0, 8, 0, 0);
            reservationLayout.addView(tvNotes);
        }

        TextView tvDateCreation = new TextView(this);
        String dateCreationStr = reservation.getDateCreation() != null ?
                sdf.format(reservation.getDateCreation()) : "";
        tvDateCreation.setText("Réservé le: " + dateCreationStr);
        tvDateCreation.setTextSize(11);
        tvDateCreation.setTextColor(0xFF999999);
        tvDateCreation.setPadding(0, 8, 0, 0);
        reservationLayout.addView(tvDateCreation);

        reservationsContainer.addView(reservationLayout);
    }


    private void showReservationDialog(Reservation reservation) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_reservation_details);

        // Get references to views
        TextView tvStade = dialog.findViewById(R.id.tvStade);
        TextView tvDateTime = dialog.findViewById(R.id.tvDateTime);
        TextView tvPlayers = dialog.findViewById(R.id.tvPlayers);
        TextView tvPrice = dialog.findViewById(R.id.tvPrice);
        TextView tvStatus = dialog.findViewById(R.id.tvStatus);
        TextView tvNotes = dialog.findViewById(R.id.tvNotes);
        TextView tvCreationDate = dialog.findViewById(R.id.tvCreationDate);
        LinearLayout layoutNotes = dialog.findViewById(R.id.layoutNotes);
        Button btnRefuser = dialog.findViewById(R.id.btnRefuser);
        Button btnConfirmer = dialog.findViewById(R.id.btnConfirmer);
        Button btnOk = dialog.findViewById(R.id.btnOk);

        // Format dates
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String dateReservationStr = reservation.getDateReservation() != null ?
                sdf.format(reservation.getDateReservation()) : "Non définie";
        String dateCreationStr = reservation.getDateCreation() != null ?
                sdf.format(reservation.getDateCreation()) : "Non définie";

        // Set data
        tvStade.setText("Stade: " + reservation.getStadeName());
        tvDateTime.setText("Date: " + dateReservationStr + "\nCréneau: " + reservation.getCreneau());
        tvPlayers.setText("Joueurs: " + reservation.getNombreJoueurs());
        tvPrice.setText("Prix: " + reservation.getPrixTotal() + " DH");
        tvStatus.setText("Statut: " + reservation.getStatut());
        tvCreationDate.setText("Créée le: " + dateCreationStr);

        // Set status color
        switch (reservation.getStatut().toLowerCase()) {
            case "confirmée":
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "annulée":
            case "refusée":
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                break;
            case "terminée":
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                break;
            case "en attente":
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                break;
            default:
                tvStatus.setTextColor(getResources().getColor(android.R.color.black));
        }

        // Handle notes
        if (reservation.getNotes() != null && !reservation.getNotes().isEmpty()) {
            layoutNotes.setVisibility(View.VISIBLE);
            tvNotes.setText("Notes: " + reservation.getNotes());
        } else {
            layoutNotes.setVisibility(View.GONE);
        }

        // Handle buttons based on status
        if ("en attente".equals(reservation.getStatut())) {
            btnRefuser.setVisibility(View.VISIBLE);
            btnConfirmer.setVisibility(View.VISIBLE);
            btnOk.setVisibility(View.GONE);
        } else {
            btnRefuser.setVisibility(View.GONE);
            btnConfirmer.setVisibility(View.GONE);
            btnOk.setVisibility(View.VISIBLE);
        }

        // Set button listeners
        btnRefuser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateReservationStatus(reservation, "refusée");
                dialog.dismiss();
            }
        });

        btnConfirmer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateReservationStatus(reservation, "confirmée");
                dialog.dismiss();
            }
        });

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        // Set dialog properties
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private void updateReservationStatus(Reservation reservation, String newStatus) {
        firebaseManager.updateReservationStatus(reservation.getId(), newStatus, new FireBaseManager.OnStatusUpdateListener() {
            @Override
            public void onStatusUpdated() {
                runOnUiThread(() -> {
                    Toast.makeText(MesReservationsActivity.this,
                            "Statut mis à jour: " + newStatus, Toast.LENGTH_SHORT).show();
                    chargerReservations();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MesReservationsActivity.this,
                            "Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void afficherMessage(String message) {
        TextView tvMessage = new TextView(this);
        tvMessage.setText(message);
        tvMessage.setTextSize(16);
        tvMessage.setPadding(16, 16, 16, 16);
        tvMessage.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        reservationsContainer.addView(tvMessage);
    }

    @Override
    protected void onResume() {
        super.onResume();
        chargerReservations();
    }
}