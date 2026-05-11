package com.example.foottest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class AccueilActivity extends AppCompatActivity {
    private PopupWindow popupWindow;
    private FavoritesManager favoritesManager;
    private RecyclerView productsRecyclerView;
    private EditText searchInput;
    private ImageView menuIcon;
    private LinearLayout navHome, navFavorite, navReview, navProfile;

    private FireBaseManager firebaseManager;
    private SessionManager sessionManager;
    private List<Stade> stadesList = new ArrayList<>();
    private List<Stade> stadesFiltered = new ArrayList<>();
    private TerrainProductAdapter adapter;

    // Pour les catégories - ce sont des TextView dans le XML
    private TextView tvCategoryAll, tvCategoryOne, tvCategoryTwo, tvCategoryDoor;
    private String currentCategory = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accuill);

        Log.d("AccueilActivity", "onCreate démarré");

        // Initialiser les managers
        firebaseManager = new FireBaseManager();
        sessionManager = new SessionManager(this);

        // Vérifier si l'utilisateur est connecté
        if (!sessionManager.isLoggedIn()) {
            Log.d("AccueilActivity", "Utilisateur non connecté, redirection vers login");
            redirectToLogin();
            return;
        }

        Log.d("AccueilActivity", "Utilisateur connecté, userId: " + sessionManager.getUserId());

        // Récupérer les extras si nécessaire
        Intent intent = getIntent();
        if (intent != null) {
            String email = intent.getStringExtra("email");
            String userId = intent.getStringExtra("userId");
            int userType = intent.getIntExtra("userType", 0);

            Log.d("AccueilActivity", "Extras reçus - email: " + email + ", userId: " + userId + ", userType: " + userType);
        }

        initializeViews();
        setupListeners();
        loadStades();
    }

    private void initializeViews() {
        Log.d("AccueilActivity", "Initialisation des vues");
        favoritesManager = FavoritesManager.getInstance();
        // Header
        menuIcon = findViewById(R.id.menuIcon);


        // Search
        searchInput = findViewById(R.id.searchInput);

        // Categories - Ce sont des TextView dans votre XML
        tvCategoryAll = findViewById(R.id.categoryAll);
        tvCategoryOne = findViewById(R.id.categoryOne);
        tvCategoryTwo = findViewById(R.id.categoryTwo);
        tvCategoryDoor = findViewById(R.id.categoryDoor);

        // RecyclerView
        productsRecyclerView = findViewById(R.id.productsRecyclerView);
        productsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Bottom Navigation
        navHome = findViewById(R.id.nav_home);
        navFavorite = findViewById(R.id.nav_favorite);
        navReview = findViewById(R.id.nav_review);
        navProfile = findViewById(R.id.nav_profile);

        Log.d("AccueilActivity", "Vues initialisées avec succès");
    }

    private void setupListeners() {
        Log.d("AccueilActivity", "Configuration des listeners");

        // Menu Icon
        menuIcon.setOnClickListener(v -> {
            showPopupMenu(v);
        });

        // Search Input
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStades(s.toString());
            };

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Categories - Gestion des clics sur les TextView
        if (tvCategoryAll != null) {
            tvCategoryAll.setOnClickListener(v -> {
                Toast.makeText(this, "Tous les terrains", Toast.LENGTH_SHORT).show();
                filterByCategory("all");
            });
        }

        if (tvCategoryOne != null) {
            tvCategoryOne.setOnClickListener(v -> {
                Toast.makeText(this, "Prix les plus élevés", Toast.LENGTH_SHORT).show();
                filterByCategory("most_price");
            });
        }

        if (tvCategoryTwo != null) {
            tvCategoryTwo.setOnClickListener(v -> {
                Toast.makeText(this, "Meilleures notes", Toast.LENGTH_SHORT).show();
                filterByCategory("most_rating");
            });
        }

        if (tvCategoryDoor != null) {
            tvCategoryDoor.setOnClickListener(v -> {
                Toast.makeText(this, "Récemment ajoutés", Toast.LENGTH_SHORT).show();
                filterByCategory("recently_added");
            });
        }

        // Bottom Navigation
        navHome.setOnClickListener(v -> {
            // Déjà sur la page d'accueil
            Toast.makeText(this, "Accueil", Toast.LENGTH_SHORT).show();
        });

        navFavorite.setOnClickListener(v -> {
            Toast.makeText(this, "Favoris", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AccueilActivity.this, FavActivity.class); // ✅ Changé
            startActivity(intent);
        });

        navReview.setOnClickListener(v -> {
            Toast.makeText(this, "Mes Réservations", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AccueilActivity.this, MyReviewActivity.class);
            startActivity(intent);
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(AccueilActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }
    // ==================== MENU POPUP ====================

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
        Intent intent = new Intent(AccueilActivity.this, ProfileActivity.class);
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
                    Intent intent = new Intent(AccueilActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Non", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        // Fermer le popup si ouvert
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
        super.onDestroy();
    }

    private void loadStades() {
        Log.d("AccueilActivity", "Chargement des stades depuis Firebase");

        firebaseManager.getAllStades(new FireBaseManager.OnStadesLoadedListener() {
            @Override
            public void onStadesLoaded(List<Stade> stades) {
                runOnUiThread(() -> {
                    Log.d("AccueilActivity", "Stades chargés: " + stades.size() + " éléments");

                    stadesList.clear();
                    stadesList.addAll(stades);
                    stadesFiltered.clear();
                    stadesFiltered.addAll(stades);

                    if (stadesList.isEmpty()) {
                        Toast.makeText(AccueilActivity.this,
                                "Aucun terrain disponible", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AccueilActivity.this,
                                stadesList.size() + " terrains disponibles", Toast.LENGTH_SHORT).show();
                    }

                    setupRecyclerView();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Log.e("AccueilActivity", "Erreur chargement stades: " + errorMessage);
                    Toast.makeText(AccueilActivity.this,
                            "Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupRecyclerView() {
        Log.d("AccueilActivity", "Configuration du RecyclerView");

        adapter = new TerrainProductAdapter(stadesFiltered, new TerrainProductAdapter.OnTerrainClickListener() {
            @Override
            public void onTerrainClick(Stade stade) {
                Log.d("AccueilActivity", "Clic sur terrain: " + stade.getNomStade());
                // Naviguer vers les détails du terrain
                Intent intent = new Intent(AccueilActivity.this, DetailTerrainActivity.class);
                intent.putExtra("stadeId", stade.getId());
                startActivity(intent);
            }

            @Override
            public void onAddToCartClick(Stade stade) {
                Log.d("AccueilActivity", "Clic favori: " + stade.getNomStade());

                // ✅ Basculer le favori
                boolean isNowFavorite = favoritesManager.toggleFavorite(stade.getId());

                if (isNowFavorite) {
                    Toast.makeText(AccueilActivity.this,
                            "✅ Ajouté aux favoris: " + stade.getNomStade(),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AccueilActivity.this,
                            "❌ Retiré des favoris: " + stade.getNomStade(),
                            Toast.LENGTH_SHORT).show();
                }

                // Mettre à jour l'icône
                adapter.notifyDataSetChanged();
            }
        });
        productsRecyclerView.setAdapter(adapter);

        Log.d("AccueilActivity", "RecyclerView configuré avec " + stadesFiltered.size() + " éléments");
    }



    // ==================== FILTRES ====================

    private void filterStades(String query) {
        stadesFiltered.clear();

        if (query.isEmpty()) {
            stadesFiltered.addAll(stadesList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Stade stade : stadesList) {
                if (stade.getNomStade().toLowerCase().contains(lowerQuery) ||
                        stade.getAdresse().toLowerCase().contains(lowerQuery) ||
                        stade.getDescription().toLowerCase().contains(lowerQuery)) {
                    stadesFiltered.add(stade);
                }
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        Log.d("AccueilActivity", "Filtrage terminé: " + stadesFiltered.size() + " résultats pour '" + query + "'");
    }

    private void filterByCategory(String category) {
        Log.d("AccueilActivity", "Filtrage par catégorie: " + category);

        currentCategory = category;
        stadesFiltered.clear();

        switch (category) {
            case "all":
                // Afficher tous les stades
                stadesFiltered.addAll(stadesList);
                updateCategoryUI("all");
                break;

            case "most_price":
                // Trier par prix décroissant (plus cher en premier)
                List<Stade> sortedByPrice = new ArrayList<>(stadesList);
                sortedByPrice.sort((s1, s2) -> Double.compare(s2.getPrixHeure(), s1.getPrixHeure()));
                stadesFiltered.addAll(sortedByPrice);
                updateCategoryUI("most_price");
                break;

            case "most_rating":
                // Trier par note (pour l'instant simulé avec capacité)
                // TODO: Remplacer par la vraie note quand disponible
                List<Stade> sortedByRating = new ArrayList<>(stadesList);
                sortedByRating.sort((s1, s2) -> {
                    // Simulation: utiliser la capacité comme "rating"
                    // Plus tard, utilisez: Double.compare(s2.getNoteMoyenne(), s1.getNoteMoyenne())
                    return Integer.compare(s2.getCapacite(), s1.getCapacite());
                });
                stadesFiltered.addAll(sortedByRating);
                updateCategoryUI("most_rating");
                break;

            case "recently_added":
                // Prendre les 4 derniers stades ajoutés
                int count = Math.min(4, stadesList.size());
                if (stadesList.size() > 0) {
                    for (int i = stadesList.size() - 1; i >= Math.max(0, stadesList.size() - 4); i--) {
                        stadesFiltered.add(stadesList.get(i));
                    }
                }
                updateCategoryUI("recently_added");
                break;

            default:
                stadesFiltered.addAll(stadesList);
                updateCategoryUI("all");
                break;
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        Log.d("AccueilActivity", "Résultats: " + stadesFiltered.size() + " stades");
    }

    // ==================== UI CATÉGORIES ====================

    private void updateCategoryUI(String activeCategory) {
        // Couleurs
        int colorActive = 0xFF333333;      // Noir pour actif
        int colorInactive = 0xFFF5F5F5;    // Gris clair pour inactif
        int textColorActive = 0xFFFFFFFF;  // Blanc pour texte actif
        int textColorInactive = 0xFF666666; // Gris pour texte inactif

        // Réinitialiser toutes les catégories
        resetCategoryStyle(tvCategoryAll, colorInactive, textColorInactive);
        resetCategoryStyle(tvCategoryOne, colorInactive, textColorInactive);
        resetCategoryStyle(tvCategoryTwo, colorInactive, textColorInactive);
        resetCategoryStyle(tvCategoryDoor, colorInactive, textColorInactive);

        // Activer la catégorie sélectionnée
        switch (activeCategory) {
            case "all":
                setCategoryActive(tvCategoryAll, colorActive, textColorActive);
                break;
            case "most_price":
                setCategoryActive(tvCategoryOne, colorActive, textColorActive);
                break;
            case "most_rating":
                setCategoryActive(tvCategoryTwo, colorActive, textColorActive);
                break;
            case "recently_added":
                setCategoryActive(tvCategoryDoor, colorActive, textColorActive);
                break;
        }
    }

    private void setCategoryActive(TextView textView, int bgColor, int textColor) {
        if (textView != null) {
            textView.setTextColor(textColor);
            ViewGroup parent = (ViewGroup) textView.getParent();
            if (parent instanceof androidx.cardview.widget.CardView) {
                ((androidx.cardview.widget.CardView) parent).setCardBackgroundColor(bgColor);
            }
        }
    }

    private void resetCategoryStyle(TextView textView, int bgColor, int textColor) {
        if (textView != null) {
            textView.setTextColor(textColor);
            ViewGroup parent = (ViewGroup) textView.getParent();
            if (parent instanceof androidx.cardview.widget.CardView) {
                ((androidx.cardview.widget.CardView) parent).setCardBackgroundColor(bgColor);
            }
        }
    }


    private void redirectToLogin() {
        Log.d("AccueilActivity", "Redirection vers login");
        Intent intent = new Intent(AccueilActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Demander confirmation avant de quitter
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Quitter")
                .setMessage("Voulez-vous vraiment quitter l'application?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    super.onBackPressed();
                })
                .setNegativeButton("Non", null)
                .show();
    }
}

// Adapter qui utilise le layout item_product.xml avec les données terrain
// Remplacez la classe TerrainProductAdapter dans AccueilActivity.java

class TerrainProductAdapter extends RecyclerView.Adapter<TerrainProductAdapter.TerrainViewHolder> {

    private List<Stade> stades;
    private OnTerrainClickListener listener;
    private static final String TAG = "TerrainAdapter";

    public interface OnTerrainClickListener {
        void onTerrainClick(Stade stade);
        void onAddToCartClick(Stade stade);
    }

    public TerrainProductAdapter(List<Stade> stades, OnTerrainClickListener listener) {
        this.stades = stades;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TerrainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new TerrainViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TerrainViewHolder holder, int position) {
        Stade stade = stades.get(position);

        // Mapper les données terrain sur le layout produit
        holder.productName.setText(stade.getNomStade());
        holder.productPrice.setText(stade.getPrixHeure() + " MAD/h");

        // ✅ Charger l'image du terrain avec Glide
        if (stade.getPhotos() != null && !stade.getPhotos().isEmpty()) {
            String premierPhotoUrl = stade.getPhotos().get(0);

            Log.d(TAG, "Chargement image pour: " + stade.getNomStade());
            Log.d(TAG, "URL: " + premierPhotoUrl);

            if (premierPhotoUrl != null && !premierPhotoUrl.trim().isEmpty()) {
                try {
                    Glide.with(holder.itemView.getContext())
                            .load(premierPhotoUrl)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_background)
                            .centerCrop()
                            .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                                @Override
                                public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e,
                                                            Object model,
                                                            com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                            boolean isFirstResource) {
                                    Log.e(TAG, "❌ Erreur chargement image pour " + stade.getNomStade() + ": " +
                                            (e != null ? e.getMessage() : "Erreur inconnue"));
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                                               Object model,
                                                               com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                               com.bumptech.glide.load.DataSource dataSource,
                                                               boolean isFirstResource) {
                                    Log.d(TAG, "✅ Image chargée avec succès pour " + stade.getNomStade());
                                    return false;
                                }
                            })
                            .into(holder.productImage);
                } catch (Exception e) {
                    Log.e(TAG, "❌ Exception lors du chargement: " + e.getMessage());
                    holder.productImage.setImageResource(R.drawable.ic_launcher_background);
                }
            } else {
                Log.d(TAG, "⚠️ URL vide pour " + stade.getNomStade());
                holder.productImage.setImageResource(R.drawable.ic_launcher_background);
            }
        } else {
            Log.d(TAG, "⚠️ Pas de photos pour " + stade.getNomStade());
            holder.productImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // Gestion du clic sur l'icône "Add to cart" (étoile/favori)
        holder.addToCartButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddToCartClick(stade);
                // Changer l'icône pour indiquer qu'il est dans les favoris
                holder.addToCartButton.setImageResource(R.drawable.ic_favorite_filled);
            }
        });

        // Gestion du clic sur la carte entière
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTerrainClick(stade);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stades.size();
    }

    public void updateData(List<Stade> newStades) {
        this.stades = newStades;
        notifyDataSetChanged();
        Log.d(TAG, "Données mises à jour: " + newStades.size() + " stades");
    }

    static class TerrainViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName;
        TextView productPrice;
        ImageView addToCartButton;

        public TerrainViewHolder(@NonNull View itemView) {
            super(itemView);
            // Utilisez les IDs de item_product.xml
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            addToCartButton = itemView.findViewById(R.id.addToCartButton);
        }
    }
}