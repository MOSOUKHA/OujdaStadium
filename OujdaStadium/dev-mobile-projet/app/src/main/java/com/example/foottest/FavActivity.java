package com.example.foottest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class FavActivity extends AppCompatActivity {
    private static final String TAG = "FavActivity";

    private RecyclerView reservationsRecyclerView;
    private LinearLayout navHome, navFavorite, navReview, navProfile;
    private ImageView menuIcon;
    private PopupWindow popupWindow;

    private FavoritesManager favoritesManager;
    private FireBaseManager firebaseManager;
    private List<Stade> favoriteStades = new ArrayList<>();
    private FavoriteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favorite_activity);

        Log.d(TAG, "Création de FavActivity");

        favoritesManager = FavoritesManager.getInstance();
        firebaseManager = new FireBaseManager();

        initializeViews();
        setupListeners();
        loadFavorites();
    }

    private void initializeViews() {
        menuIcon = findViewById(R.id.menuIcon);
        reservationsRecyclerView = findViewById(R.id.reservationsRecyclerView);

        // Bottom Navigation
        navHome = findViewById(R.id.nav_home);
        navFavorite = findViewById(R.id.nav_favorite);
        navReview = findViewById(R.id.nav_review);
        navProfile = findViewById(R.id.nav_profile);

        // Configuration RecyclerView
        reservationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavoriteAdapter(favoriteStades, new FavoriteAdapter.OnFavoriteActionListener() {
            @Override
            public void onStadeClick(Stade stade) {
                // Ouvrir les détails
                Intent intent = new Intent(FavActivity.this, DetailTerrainActivity.class);
                intent.putExtra("stadeId", stade.getId());
                startActivity(intent);
            }

            @Override
            public void onRemoveFavorite(Stade stade) {
                // Retirer des favoris
                favoritesManager.removeFavorite(stade.getId());
                favoriteStades.remove(stade);
                adapter.notifyDataSetChanged();

                Toast.makeText(FavActivity.this,
                        "Retiré des favoris: " + stade.getNomStade(),
                        Toast.LENGTH_SHORT).show();

                // Si plus de favoris, afficher message
                if (favoriteStades.isEmpty()) {
                    Toast.makeText(FavActivity.this,
                            "Aucun favori", Toast.LENGTH_SHORT).show();
                }
            }
        });
        reservationsRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        // Menu
        menuIcon.setOnClickListener(v -> showPopupMenu(v));

        // Bottom Navigation
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(FavActivity.this, AccueilActivity.class);
            startActivity(intent);
            finish();
        });

        navFavorite.setOnClickListener(v -> {
            // Déjà sur la page favoris
            Toast.makeText(this, "Favoris", Toast.LENGTH_SHORT).show();
        });

        navReview.setOnClickListener(v -> {
            Intent intent = new Intent(FavActivity.this, MyReviewActivity.class);
            startActivity(intent);
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(FavActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    private void loadFavorites() {
        Log.d(TAG, "Chargement des favoris");

        List<String> favoriteIds = favoritesManager.getFavoriteIds();

        if (favoriteIds.isEmpty()) {
            Toast.makeText(this, "Aucun favori pour le moment", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Nombre de favoris: " + favoriteIds.size());

        // Charger tous les stades et filtrer
        firebaseManager.getAllStades(new FireBaseManager.OnStadesLoadedListener() {
            @Override
            public void onStadesLoaded(List<Stade> stades) {
                runOnUiThread(() -> {
                    favoriteStades.clear();

                    // Filtrer seulement les stades favoris
                    for (Stade stade : stades) {
                        if (favoritesManager.isFavorite(stade.getId())) {
                            favoriteStades.add(stade);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    Log.d(TAG, "Favoris chargés: " + favoriteStades.size());
                    Toast.makeText(FavActivity.this,
                            favoriteStades.size() + " terrain(x) favori(s)",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Erreur chargement: " + errorMessage);
                    Toast.makeText(FavActivity.this,
                            "Erreur: " + errorMessage,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showPopupMenu(View anchorView) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_menu, null);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true;

        popupWindow = new PopupWindow(popupView, width, height, focusable);
        popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.popup_background));
        popupWindow.setElevation(20);

        LinearLayout menuParametres = popupView.findViewById(R.id.menuParametres);
        LinearLayout menuAbout = popupView.findViewById(R.id.menuAbout);
        LinearLayout menuLogout = popupView.findViewById(R.id.menuLogout);

        menuParametres.setOnClickListener(v -> {
            popupWindow.dismiss();

                Intent intent = new Intent(FavActivity.this, ProfileActivity.class);
                startActivity(intent);


        });

        menuAbout.setOnClickListener(v -> {
            popupWindow.dismiss();
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("À propos")
                    .setMessage("FootTest v1.0.0\nGestion des favoris locale")
                    .setPositiveButton("OK", null)
                    .show();
        });

        menuLogout.setOnClickListener(v -> {
            popupWindow.dismiss();
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Déconnexion")
                    .setMessage("Voulez-vous vraiment vous déconnecter?\n\n⚠️ Vos favoris seront perdus!")
                    .setPositiveButton("Oui", (dialog, which) -> {
                        SessionManager sessionManager = new SessionManager(this);
                        sessionManager.logout();

                        // Vider les favoris
                        favoritesManager.clearAll();

                        Intent intent = new Intent(FavActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Non", null)
                    .show();
        });

        popupWindow.showAsDropDown(anchorView, 0, 0, Gravity.END);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les favoris au retour sur la page
        loadFavorites();
    }

    @Override
    protected void onDestroy() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
        super.onDestroy();
    }
}

// ==================== ADAPTER POUR LES FAVORIS ====================

class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {
    private static final String TAG = "FavoriteAdapter";
    private List<Stade> stades;
    private OnFavoriteActionListener listener;

    public interface OnFavoriteActionListener {
        void onStadeClick(Stade stade);
        void onRemoveFavorite(Stade stade);
    }

    public FavoriteAdapter(List<Stade> stades, OnFavoriteActionListener listener) {
        this.stades = stades;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Stade stade = stades.get(position);

        holder.tvStadeName.setText(stade.getNomStade());
        holder.tvStadePrice.setText(stade.getPrixHeure() + " MAD/h");
        holder.tvStadeAddress.setText(stade.getAdresse());
        holder.tvStadeCapacity.setText("Capacité: " + stade.getCapacite() + " joueurs");

        // Charger l'image
        if (stade.getPhotos() != null && !stade.getPhotos().isEmpty()) {
            String photoUrl = stade.getPhotos().get(0);
            Glide.with(holder.itemView.getContext())
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(holder.ivStadeImage);
        } else {
            holder.ivStadeImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // Clic sur la carte
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStadeClick(stade);
            }
        });

        // Clic sur le bouton supprimer
        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveFavorite(stade);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stades.size();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStadeImage;
        TextView tvStadeName;
        TextView tvStadePrice;
        TextView tvStadeAddress;
        TextView tvStadeCapacity;
        ImageView btnRemove;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStadeImage = itemView.findViewById(R.id.ivStadeImage);
            tvStadeName = itemView.findViewById(R.id.tvStadeName);
            tvStadePrice = itemView.findViewById(R.id.tvStadePrice);
            tvStadeAddress = itemView.findViewById(R.id.tvStadeAddress);
            tvStadeCapacity = itemView.findViewById(R.id.tvStadeCapacity);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}