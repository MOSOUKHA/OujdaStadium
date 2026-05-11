package com.example.foottest;

import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.widget.Toast;

public class MyReviewActivity extends AppCompatActivity {
    private PopupWindow popupWindow;
    private ImageView menuIcon;
    private LinearLayout navHome, navFavorite, navReview, navProfile;
    private RecyclerView reservationsRecyclerView;
    private TextView tvConfirme, tvEnAttente, tvRefuse;

    private FireBaseManager firebaseManager;
    private SessionManager sessionManager;
    private List<Reservation> reservationsList = new ArrayList<>();
    private ReservationsAdapter adapter;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_review);

        Log.d("MyReviewActivity", "onCreate démarré");

        firebaseManager = new FireBaseManager();
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            Log.d("MyReviewActivity", "Utilisateur non connecté");
            Toast.makeText(this, "Veuillez vous connecter", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupListeners();
        loadReservations();
    }

    private void initializeViews() {
        Log.d("MyReviewActivity", "Initialisation des vues");

        // Header
        menuIcon = findViewById(R.id.menuIcon);

        // Filter buttons
        tvConfirme = findViewById(R.id.tvConfirme);
        tvEnAttente = findViewById(R.id.tvEnAttente);
        tvRefuse = findViewById(R.id.tvRefuse);

        // RecyclerView
        reservationsRecyclerView = findViewById(R.id.reservationsRecyclerView);
        reservationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Bottom Navigation
        navHome = findViewById(R.id.nav_home);
        navFavorite = findViewById(R.id.nav_favorite);
        navReview = findViewById(R.id.nav_review);
        navProfile = findViewById(R.id.nav_profile);

        Log.d("MyReviewActivity", "Vues initialisées");
    }

    private void setupListeners() {
        menuIcon.setOnClickListener(v -> showPopupMenu(v));

        // Filter listeners
        tvConfirme.setOnClickListener(v -> {
            Log.d("MyReviewActivity", "Filtre: confirmée");
            filterReservations("confirmée");
            updateFilterUI("confirmée");
        });

        tvEnAttente.setOnClickListener(v -> {
            Log.d("MyReviewActivity", "Filtre: en attente");
            filterReservations("en attente");
            updateFilterUI("en attente");
        });

        tvRefuse.setOnClickListener(v -> {
            Log.d("MyReviewActivity", "Filtre: refusée");
            filterReservations("refusée");
            updateFilterUI("refusée");
        });

        // Bottom Navigation
        navHome.setOnClickListener(v -> {
            finish(); // Retour à l'accueil
        });

        navFavorite.setOnClickListener(v -> {
            Toast.makeText(this, "Favoris", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MyReviewActivity.this, FavActivity.class); // ✅ Changé
            startActivity(intent);
        });

        navReview.setOnClickListener(v -> {
            // Déjà sur cette page
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MyReviewActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    private void loadReservations() {
        String userId = sessionManager.getUserId();
        Log.d("MyReviewActivity", "Chargement réservations pour userId: " + userId);

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Erreur: utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseManager.getReservationsUtilisateur(userId, new FireBaseManager.OnReservationsLoadedListener() {
            @Override
            public void onReservationsLoaded(List<Reservation> reservations) {
                Log.d("MyReviewActivity", "Réservations chargées: " + reservations.size() + " éléments");

                runOnUiThread(() -> {
                    reservationsList.clear();
                    reservationsList.addAll(reservations);

                    // Afficher un message si vide
                    if (reservations.isEmpty()) {
                        Toast.makeText(MyReviewActivity.this,
                                "Aucune réservation trouvée", Toast.LENGTH_SHORT).show();
                    } else {
                        // DEBUG: Afficher les réservations dans les logs
                        for (Reservation r : reservations) {
                            Log.d("MyReviewActivity", "Réservation: " +
                                    r.getStadeName() + " - " + r.getStatut());
                        }
                    }

                    if (adapter == null) {
                        adapter = new ReservationsAdapter(reservationsList);
                        reservationsRecyclerView.setAdapter(adapter);
                    } else {
                        adapter.updateData(reservationsList);
                    }

                    // Appliquer le filtre initial
                    filterReservations("all");
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e("MyReviewActivity", "Erreur Firebase: " + errorMessage);
                runOnUiThread(() -> {
                    Toast.makeText(MyReviewActivity.this,
                            "Erreur de chargement: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void filterReservations(String status) {
        currentFilter = status;
        List<Reservation> filteredList = new ArrayList<>();

        if (status.equals("all")) {
            filteredList.addAll(reservationsList);
        } else {
            for (Reservation reservation : reservationsList) {
                if (reservation.getStatut() != null &&
                        reservation.getStatut().equals(status)) {
                    filteredList.add(reservation);
                }
            }
        }

        Log.d("MyReviewActivity", "Filtre '" + status + "': " + filteredList.size() + " résultats");

        if (adapter != null) {
            adapter.updateData(filteredList);
        }
    }

    private void updateFilterUI(String selectedFilter) {
        // Réinitialiser tous les styles
        resetFilterButtons();

        // Appliquer le style au bouton sélectionné
        switch (selectedFilter) {
            case "confirmée":
                tvConfirme.setBackgroundResource(R.drawable.badge_background);
                tvConfirme.setTextColor(getResources().getColor(android.R.color.white));
                break;
            case "en attente":
                tvEnAttente.setBackgroundResource(R.drawable.badge_background);
                tvEnAttente.setTextColor(getResources().getColor(android.R.color.white));
                break;
            case "refusée":
                tvRefuse.setBackgroundResource(R.drawable.badge_background);
                tvRefuse.setTextColor(getResources().getColor(android.R.color.white));
                break;
        }
    }

    private void resetFilterButtons() {
        tvConfirme.setBackgroundResource(R.drawable.filter_unselected_bg);
        tvConfirme.setTextColor(getResources().getColor(android.R.color.black));

        tvEnAttente.setBackgroundResource(R.drawable.filter_unselected_bg);
        tvEnAttente.setTextColor(getResources().getColor(android.R.color.black));

        tvRefuse.setBackgroundResource(R.drawable.filter_unselected_bg);
        tvRefuse.setTextColor(getResources().getColor(android.R.color.black));
    }

    // Méthode pour montrer le menu popup (identique à AcceuilActivity)
    private void showPopupMenu(View anchorView) {
        // Inflater le layout du menu
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_menu, null);

        // Créer le popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // Permet de fermer en cliquant à l'extérieur

        popupWindow = new PopupWindow(popupView, width, height, focusable);

        // Définir l'arrière-plan
        popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.popup_background));
        popupWindow.setElevation(20);

        // Trouver les éléments du menu
        LinearLayout menuParametres = popupView.findViewById(R.id.menuParametres);
        LinearLayout menuAbout = popupView.findViewById(R.id.menuAbout);
        LinearLayout menuLogout = popupView.findViewById(R.id.menuLogout);

        // Ajouter les listeners
        menuParametres.setOnClickListener(v -> {
            popupWindow.dismiss();
            openParametres();
        });

        menuAbout.setOnClickListener(v -> {
            popupWindow.dismiss();
            showAboutDialog();
        });

        menuLogout.setOnClickListener(v -> {
            popupWindow.dismiss();
            logoutUser();
        });

        // Afficher le popup
        popupWindow.showAsDropDown(anchorView, 0, 0, Gravity.END);
    }

        private void openParametres() {
            Intent intent = new Intent(MyReviewActivity.this, ProfileActivity.class);
            startActivity(intent);


    }
    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("À propos de FootTest")
                .setMessage("Application de réservation de terrains de football\n\n" +
                        "Version: 1.0.0\n" +
                        "Développé par: Votre équipe\n\n" +
                        "Cette application vous permet de trouver et réserver\n" +
                        "des terrains de football dans votre région.")
                .setPositiveButton("OK", null)
                .setIcon(R.drawable.ic_info) // Assurez-vous d'avoir cette icône
                .show();
    }
    private void logoutUser() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Voulez-vous vraiment vous déconnecter?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    // Déconnexion
                    sessionManager.logout();
                    Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();

                    // Rediriger vers MainActivity
                    Intent intent = new Intent(MyReviewActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Non", null)
                .show();
    }

}