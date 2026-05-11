package com.example.foottest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gestionnaire simple des favoris (en mémoire)
 * Les favoris sont perdus quand on ferme l'app
 */
public class FavoritesManager {
    private static FavoritesManager instance;
    private Set<String> favoriteStadeIds; // Stocke les IDs des stades favoris

    private FavoritesManager() {
        favoriteStadeIds = new HashSet<>();
    }

    // Singleton
    public static FavoritesManager getInstance() {
        if (instance == null) {
            instance = new FavoritesManager();
        }
        return instance;
    }

    // Ajouter aux favoris
    public void addFavorite(String stadeId) {
        favoriteStadeIds.add(stadeId);
    }

    // Retirer des favoris
    public void removeFavorite(String stadeId) {
        favoriteStadeIds.remove(stadeId);
    }

    // Vérifier si un stade est favori
    public boolean isFavorite(String stadeId) {
        return favoriteStadeIds.contains(stadeId);
    }

    // Basculer (ajouter ou retirer)
    public boolean toggleFavorite(String stadeId) {
        if (isFavorite(stadeId)) {
            removeFavorite(stadeId);
            return false; // Retiré
        } else {
            addFavorite(stadeId);
            return true; // Ajouté
        }
    }

    // Récupérer tous les IDs favoris
    public List<String> getFavoriteIds() {
        return new ArrayList<>(favoriteStadeIds);
    }

    // Obtenir le nombre de favoris
    public int getFavoritesCount() {
        return favoriteStadeIds.size();
    }

    // Vider tous les favoris
    public void clearAll() {
        favoriteStadeIds.clear();
    }
}
