package com.example.foottest;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.SlotViewHolder> {

    private static final String TAG = "SlotAdapter";
    private List<String> slots;
    private OnSlotClickListener listener;
    private int selectedPosition = -1;

    public interface OnSlotClickListener {
        void onSlotClick(String slot, int position);
    }

    // CONSTRUCTEUR POUR L'ADMIN
    public SlotAdapter(List<String> slots, OnSlotClickListener listener) {
        this.slots = new ArrayList<>(slots); // Copie pour éviter les modifications externes
        this.listener = listener;
        setHasStableIds(true); // IDs stables pour éviter les problèmes de recyclage
        Log.d(TAG, "Adapter créé avec " + slots.size() + " créneaux");
    }

    // CONSTRUCTEUR POUR LES JOUEURS
    public SlotAdapter(Stade stade, OnSlotClickListener listener) {
        this.listener = listener;
        this.slots = new ArrayList<>();
        setHasStableIds(true);

        if (stade.getCreneauxDisponibles() != null && !stade.getCreneauxDisponibles().isEmpty()) {
            this.slots = new ArrayList<>(stade.getCreneauxDisponibles());
        } else {
            this.slots = genererCreneauxAutomatiques(stade);
        }

        Log.d(TAG, "Adapter créé pour stade avec " + slots.size() + " créneaux");
    }

    public void setSelectedPosition(int position) {
        Log.d(TAG, "setSelectedPosition: " + position + " (ancien: " + selectedPosition + ")");
        selectedPosition = position;
        notifyDataSetChanged(); // Plus stable que notifyItemChanged
    }

    public void clearSelection() {
        Log.d(TAG, "clearSelection");
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return position; // ID stable basé sur la position
    }

    @Override
    public int getItemViewType(int position) {
        return 0; // Un seul type de vue
    }

    private List<String> genererCreneauxAutomatiques(Stade stade) {
        List<String> creneauxGeneres = new ArrayList<>();

        if (stade.getHeureOuverture() != null && stade.getHeureFermeture() != null) {
            int duree = stade.getDureeCreneau() > 0 ? stade.getDureeCreneau() : 60;

            try {
                String[] ouvertureTab = stade.getHeureOuverture().split(":");
                String[] fermetureTab = stade.getHeureFermeture().split(":");

                int heureOuverture = Integer.parseInt(ouvertureTab[0]);
                int minuteOuverture = Integer.parseInt(ouvertureTab[1]);
                int heureFermeture = Integer.parseInt(fermetureTab[0]);
                int minuteFermeture = Integer.parseInt(fermetureTab[1]);

                Log.d(TAG, "Génération créneaux: " + heureOuverture + ":" + minuteOuverture +
                        " → " + heureFermeture + ":" + minuteFermeture + " (durée: " + duree + "min)");

                int currentHeure = heureOuverture;
                int currentMinute = minuteOuverture;
                int compteur = 0; // Pour éviter les boucles infinies

                // CORRECTION: Continuer tant qu'on peut créer un créneau complet
                while (compteur < 50) { // Limite de sécurité
                    compteur++;

                    String debut = String.format("%02d:%02d", currentHeure, currentMinute);

                    // Calculer l'heure de fin
                    int finHeure = currentHeure;
                    int finMinute = currentMinute + duree;

                    if (finMinute >= 60) {
                        finHeure += finMinute / 60;
                        finMinute = finMinute % 60;
                    }

                    // CORRECTION: Vérifier si le créneau de FIN dépasse la fermeture
                    if (finHeure > heureFermeture ||
                            (finHeure == heureFermeture && finMinute > minuteFermeture)) {
                        Log.d(TAG, "Arrêt: " + finHeure + ":" + finMinute + " dépasse la fermeture");
                        break;
                    }

                    String fin = String.format("%02d:%02d", finHeure, finMinute);
                    creneauxGeneres.add(debut + " - " + fin);
                    Log.d(TAG, "Créneau ajouté: " + debut + " - " + fin);

                    // Passer au créneau suivant
                    currentHeure = finHeure;
                    currentMinute = finMinute;
                }

                Log.d(TAG, "Total créneaux générés: " + creneauxGeneres.size());

            } catch (Exception e) {
                Log.e(TAG, "Erreur génération créneaux: " + e.getMessage());
                e.printStackTrace();
                creneauxGeneres = getDefaultCreneaux();
            }
        } else {
            Log.d(TAG, "Pas d'heures définies, utilisation des créneaux par défaut");
            creneauxGeneres = getDefaultCreneaux();
        }

        return creneauxGeneres;
    }

    private List<String> getDefaultCreneaux() {
        List<String> defaults = new ArrayList<>();
        // Générer des créneaux de 8h à 22h par défaut
        for (int hour = 8; hour < 22; hour++) {
            String debut = String.format("%02d:00", hour);
            String fin = String.format("%02d:00", hour + 1);
            defaults.add(debut + " - " + fin);
        }
        Log.d(TAG, "Créneaux par défaut: " + defaults.size());
        return defaults;
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_slot, parent, false);
        return new SlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        String slot = slots.get(position);

        // IMPORTANT: Toujours définir le texte en premier
        holder.tvSlot.setText(slot);

        // Logs de débogage
        Log.d(TAG, "onBind pos=" + position + " slot=" + slot + " selected=" + selectedPosition);

        // Définir les couleurs selon la sélection
        if (position == selectedPosition) {
            // Sélectionné - VERT
            holder.tvSlot.setBackgroundResource(R.drawable.slot_bg_selected);
            holder.tvSlot.setTextColor(0xFFFFFFFF); // Blanc
            Log.d(TAG, "  → VERT (sélectionné)");
        } else {
            // Normal - GRIS
            holder.tvSlot.setBackgroundResource(R.drawable.slot_bg);
            holder.tvSlot.setTextColor(0xFF000000); // Noir
        }

        // CRUCIAL: Capturer la position dans une variable finale
        final int currentPosition = holder.getAdapterPosition();
        final String currentSlot = slot;

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && currentPosition != RecyclerView.NO_POSITION) {
                Log.d(TAG, "Click: pos=" + currentPosition + " slot=" + currentSlot);
                listener.onSlotClick(currentSlot, currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        int count = slots != null ? slots.size() : 0;
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    static class SlotViewHolder extends RecyclerView.ViewHolder {
        TextView tvSlot;

        SlotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSlot = itemView.findViewById(R.id.tvSlot);

            // S'assurer que la vue a les bonnes dimensions
            if (tvSlot != null) {
                tvSlot.setMinHeight(48 * 3); // 48dp en pixels (approximatif)
            }
        }
    }
}