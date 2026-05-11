package com.example.foottest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class DetailTerrainActivity extends AppCompatActivity {

    private static final String TAG = "DetailTerrain";
    private FireBaseManager firebaseManager;
    private String stadeId;
    private Stade stade;
    private String selectedCreneau = null;

    private TextView tvName, tvLocation, tvPrice, tvReviews, tvNoPhotos, tvDescription, tvCapacity;
    private LinearLayout imagesContainer, capacityLayout;
    private RecyclerView rvSlots;
    private Button btnSeeAllReviews, btnBook, btnOpenMap;
    private SlotAdapter slotAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_terrain);

        Log.d(TAG, "========== DÉMARRAGE ACTIVITY ==========");

        firebaseManager = new FireBaseManager();

        stadeId = getIntent().getStringExtra("stadeId");
        Log.d(TAG, "StadeId reçu: " + stadeId);

        if (stadeId == null || stadeId.isEmpty()) {
            Toast.makeText(this, "Stade introuvable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadStadeDetails();
    }

    private void initializeViews() {
        Log.d(TAG, "Initialisation des vues...");

        tvName = findViewById(R.id.tvName);
        tvLocation = findViewById(R.id.tvLocation);
        tvPrice = findViewById(R.id.tvPrice);
        tvReviews = findViewById(R.id.tvReviews);
        imagesContainer = findViewById(R.id.imagesContainer);
        tvNoPhotos = findViewById(R.id.tvNoPhotos);
        rvSlots = findViewById(R.id.rvSlots);

        tvDescription = findViewById(R.id.tvDescription);
        tvCapacity = findViewById(R.id.tvCapacity);
        capacityLayout = findViewById(R.id.capacityLayout);

        btnSeeAllReviews = findViewById(R.id.btnSeeAllReviews);
        btnBook = findViewById(R.id.btnBook);
        btnOpenMap = findViewById(R.id.btnOpenMap);

        btnBook.setEnabled(false);
        btnBook.setAlpha(0.5f);
        btnBook.setText("Sélectionnez un créneau");

        Log.d(TAG, "✓ Vues initialisées");
    }

    private void loadStadeDetails() {
        Log.d(TAG, "Chargement des détails du stade...");

        firebaseManager.getAllStades(new FireBaseManager.OnStadesLoadedListener() {
            @Override
            public void onStadesLoaded(List<Stade> stades) {
                Log.d(TAG, "Nombre de stades chargés: " + stades.size());

                Stade stadeTrouve = null;
                for (Stade s : stades) {
                    if (s.getId().equals(stadeId)) {
                        stadeTrouve = s;
                        Log.d(TAG, "✓ Stade trouvé: " + s.getNomStade());
                        break;
                    }
                }

                if (stadeTrouve == null) {
                    Log.e(TAG, "✗ Stade non trouvé avec l'ID: " + stadeId);
                    runOnUiThread(() -> {
                        Toast.makeText(DetailTerrainActivity.this,
                                "Détails introuvables", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                final Stade finalStadeTrouve = stadeTrouve;
                stade = finalStadeTrouve;

                runOnUiThread(() -> {
                    afficherDetailsStade(finalStadeTrouve);
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "✗ Erreur chargement: " + errorMessage);
                runOnUiThread(() -> {
                    Toast.makeText(DetailTerrainActivity.this,
                            "Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void afficherDetailsStade(Stade stade) {
        Log.d(TAG, "========== AFFICHAGE DÉTAILS ==========");
        Log.d(TAG, "Nom: " + stade.getNomStade());
        Log.d(TAG, "Adresse: " + stade.getAdresse());
        Log.d(TAG, "Prix: " + stade.getPrixHeure());

        tvName.setText(stade.getNomStade());
        tvLocation.setText(stade.getAdresse());
        tvPrice.setText(stade.getPrixHeure() + " MAD / heure");
        tvReviews.setText("4.5/5 (24 avis)");

        if (stade.getDescription() != null && !stade.getDescription().isEmpty()) {
            tvDescription.setText(stade.getDescription());
            tvDescription.setVisibility(TextView.VISIBLE);
        } else {
            tvDescription.setVisibility(TextView.GONE);
        }

        if (stade.getCapacite() > 0) {
            String capaciteText = "Capacité: " + stade.getCapacite() + " personnes";
            tvCapacity.setText(capaciteText);
            capacityLayout.setVisibility(View.VISIBLE);
        } else {
            capacityLayout.setVisibility(View.GONE);
        }

        chargerPhotos(stade.getPhotos());
        setupCreneaux(stade);
        configurerBoutons(stade);

        Log.d(TAG, "✓ Détails affichés");
    }

    private void setupCreneaux(Stade stade) {
        Log.d(TAG, "========== CONFIGURATION CRÉNEAUX ==========");

        // ✅ SOLUTION: Désactiver le recyclage et forcer la hauteur complète
        LinearLayoutManager layoutManager = new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false; // Désactiver le scroll interne
            }
        };
        rvSlots.setLayoutManager(layoutManager);
        rvSlots.setNestedScrollingEnabled(false);
        rvSlots.setItemAnimator(null);

        List<String> creneaux = null;

        // [... reste du code identique ...]

        // Vérifier les créneaux dans la DB
        if (stade.getCreneauxDisponibles() != null) {
            Log.d(TAG, "CreneauxDisponibles existe: " + stade.getCreneauxDisponibles().size() + " items");

            if (!stade.getCreneauxDisponibles().isEmpty()) {
                creneaux = new ArrayList<>(stade.getCreneauxDisponibles());
                Log.d(TAG, "✓ Utilisation créneaux DB");

                for (int i = 0; i < creneaux.size(); i++) {
                    Log.d(TAG, "  [" + i + "] " + creneaux.get(i));
                }
            } else {
                Log.d(TAG, "⚠ CreneauxDisponibles est vide");
            }
        } else {
            Log.d(TAG, "⚠ CreneauxDisponibles est null");
        }

        // Générer selon heures
        if (creneaux == null || creneaux.isEmpty()) {
            Log.d(TAG, "HeureOuverture: " + stade.getHeureOuverture());
            Log.d(TAG, "HeureFermeture: " + stade.getHeureFermeture());
            Log.d(TAG, "DureeCreneau: " + stade.getDureeCreneau());

            if (stade.getHeureOuverture() != null && stade.getHeureFermeture() != null &&
                    !stade.getHeureOuverture().isEmpty() && !stade.getHeureFermeture().isEmpty()) {

                creneaux = genererCreneauxAutomatiques(stade);
                Log.d(TAG, "✓ Génération automatique: " + creneaux.size() + " créneaux");

                for (int i = 0; i < creneaux.size(); i++) {
                    Log.d(TAG, "  [" + i + "] " + creneaux.get(i));
                }
            } else {
                Log.d(TAG, "⚠ Heures d'ouverture/fermeture manquantes");
            }
        }

        // Par défaut
        if (creneaux == null || creneaux.isEmpty()) {
            Log.d(TAG, "✓ Utilisation créneaux par défaut");
            creneaux = new ArrayList<>();
            for (int h = 8; h < 22; h++) {
                creneaux.add(String.format("%02d:00 - %02d:00", h, h + 1));
            }

            for (int i = 0; i < creneaux.size(); i++) {
                Log.d(TAG, "  [" + i + "] " + creneaux.get(i));
            }
        }

        Log.d(TAG, "TOTAL CRÉNEAUX À AFFICHER: " + creneaux.size());
        Log.d(TAG, "==========================================");

        final List<String> finalCreneaux = creneaux;

        slotAdapter = new SlotAdapter(finalCreneaux, new SlotAdapter.OnSlotClickListener() {
            @Override
            public void onSlotClick(String slot, int position) {
                Log.d(TAG, "Créneau cliqué: " + slot + " (position: " + position + ")");
                selectedCreneau = slot;
                btnBook.setEnabled(true);
                btnBook.setAlpha(1.0f);
                btnBook.setText("Réserver maintenant");
                slotAdapter.setSelectedPosition(position);
                Toast.makeText(DetailTerrainActivity.this,
                        "Créneau sélectionné: " + slot, Toast.LENGTH_SHORT).show();
            }
        });

        rvSlots.setAdapter(slotAdapter);

        Log.d(TAG, "✓ Adapter configuré avec " + finalCreneaux.size() + " items");

        // ✅ CRUCIAL: Forcer le RecyclerView à mesurer tous ses enfants
        rvSlots.post(() -> {
            slotAdapter.notifyDataSetChanged();
            rvSlots.requestLayout();
            Log.d(TAG, "✓ Layout forcé");
        });
    }

    private List<String> genererCreneauxAutomatiques(Stade stade) {
        List<String> creneauxGeneres = new ArrayList<>();

        Log.d(TAG, "--- Début génération automatique ---");

        try {
            String[] ouvertureTab = stade.getHeureOuverture().split(":");
            String[] fermetureTab = stade.getHeureFermeture().split(":");

            int heureOuv = Integer.parseInt(ouvertureTab[0]);
            int minuteOuv = Integer.parseInt(ouvertureTab[1]);
            int heureFerm = Integer.parseInt(fermetureTab[0]);
            int minuteFerm = Integer.parseInt(fermetureTab[1]);

            int duree = stade.getDureeCreneau() > 0 ? stade.getDureeCreneau() : 60;

            Log.d(TAG, "Ouverture: " + heureOuv + ":" + minuteOuv);
            Log.d(TAG, "Fermeture: " + heureFerm + ":" + minuteFerm);
            Log.d(TAG, "Durée: " + duree + " minutes");

            int currentHeure = heureOuv;
            int currentMinute = minuteOuv;
            int compteur = 0;

            while (compteur < 50) {
                compteur++;

                String debut = String.format("%02d:%02d", currentHeure, currentMinute);

                int finHeure = currentHeure;
                int finMinute = currentMinute + duree;

                if (finMinute >= 60) {
                    finHeure += finMinute / 60;
                    finMinute = finMinute % 60;
                }

                if (finHeure > heureFerm || (finHeure == heureFerm && finMinute > minuteFerm)) {
                    Log.d(TAG, "Arrêt: " + finHeure + ":" + finMinute + " > fermeture");
                    break;
                }

                String fin = String.format("%02d:%02d", finHeure, finMinute);
                creneauxGeneres.add(debut + " - " + fin);
                Log.d(TAG, "Ajouté: " + debut + " - " + fin);

                currentHeure = finHeure;
                currentMinute = finMinute;
            }

            Log.d(TAG, "Génération terminée: " + creneauxGeneres.size() + " créneaux");

        } catch (Exception e) {
            Log.e(TAG, "✗ Erreur génération: " + e.getMessage());
            e.printStackTrace();

            for (int h = 8; h < 22; h++) {
                creneauxGeneres.add(String.format("%02d:00 - %02d:00", h, h + 1));
            }
        }

        Log.d(TAG, "--- Fin génération automatique ---");
        return creneauxGeneres;
    }

    private void chargerPhotos(List<String> photos) {
        Log.d(TAG, "========== CHARGEMENT PHOTOS ==========");
        imagesContainer.removeAllViews();

        if (photos == null) {
            Log.d(TAG, "❌ Liste de photos est null");
            tvNoPhotos.setVisibility(TextView.VISIBLE);
            tvNoPhotos.setText("📸 Aucune photo disponible");
            return;
        }

        Log.d(TAG, "Nombre de photos: " + photos.size());

        if (photos.isEmpty()) {
            Log.d(TAG, "❌ Liste de photos est vide");
            tvNoPhotos.setVisibility(TextView.VISIBLE);
            tvNoPhotos.setText("📸 Aucune photo disponible");
            return;
        }

        // Masquer le message "pas de photos"
        tvNoPhotos.setVisibility(TextView.GONE);

        // Charger chaque photo
        for (int i = 0; i < photos.size(); i++) {
            String photoUrl = photos.get(i);
            Log.d(TAG, "Photo [" + i + "]: " + photoUrl);

            // Vérifier que l'URL n'est pas vide
            if (photoUrl == null || photoUrl.trim().isEmpty()) {
                Log.d(TAG, "❌ URL vide pour photo " + i);
                continue;
            }

            // Créer l'ImageView
            ImageView imageView = new ImageView(this);

            // Définir les dimensions (400x400 pixels)
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(400, 400);
            params.setMargins(8, 8, 8, 8);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // Ajouter un fond temporaire pour voir si l'ImageView est créée
            imageView.setBackgroundColor(0xFFEEEEEE);

            final int position = i;
            imageView.setOnClickListener(v -> {
                showImageDialog(photos, position);
            });

            // Charger l'image avec Glide
            try {
                Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_launcher_background) // Image pendant le chargement
                        .error(R.drawable.ic_launcher_background) // Image en cas d'erreur
                        .centerCrop()
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                Log.e(TAG, "❌ Échec chargement image " + position + ": " + (e != null ? e.getMessage() : "Erreur inconnue"));
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                Log.d(TAG, "✅ Image " + position + " chargée avec succès");
                                return false;
                            }
                        })
                        .into(imageView);

                Log.d(TAG, "✅ ImageView ajoutée pour photo " + i);
            } catch (Exception e) {
                Log.e(TAG, "❌ Erreur lors du chargement de l'image " + i + ": " + e.getMessage());
                e.printStackTrace();
            }

            // Ajouter l'ImageView au conteneur
            imagesContainer.addView(imageView);
        }

        Log.d(TAG, "✅ Total ImageViews ajoutées: " + imagesContainer.getChildCount());
        Log.d(TAG, "==========================================");
    }

    private void showImageDialog(List<String> photoUrls, int position) {
        Toast.makeText(this, "Image " + (position + 1) + " - Fonctionnalité gallery à venir",
                Toast.LENGTH_SHORT).show();
    }

    private void configurerBoutons(Stade stade) {
        SessionManager sessionManager = new SessionManager(this);

        btnBook.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                Toast.makeText(this, "Veuillez vous connecter pour réserver.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedCreneau != null) {
                ouvrirReservation(stade, selectedCreneau);
            } else {
                Toast.makeText(this, "Veuillez sélectionner un créneau",
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnOpenMap.setOnClickListener(v -> {
            Toast.makeText(this, "Ouverture de la carte pour " + stade.getNomStade(),
                    Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("latitude", stade.getLatitude());
             intent.putExtra("longitude", stade.getLongitude());
            intent.putExtra("nomStade", stade.getNomStade());
            startActivity(intent);
        });

        btnSeeAllReviews.setOnClickListener(v -> {
            Toast.makeText(this, "Voir tous les avis pour " + stade.getNomStade(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void ouvrirReservation(Stade stade, String slot) {
        Intent intent = new Intent(this, ReservationActivity.class);
        intent.putExtra("stade", stade);
        intent.putExtra("creneau", slot);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity détruite");
    }
}