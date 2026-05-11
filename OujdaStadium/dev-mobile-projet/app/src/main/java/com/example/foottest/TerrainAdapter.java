package com.example.foottest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TerrainAdapter extends RecyclerView.Adapter<TerrainAdapter.TerrainViewHolder> {

    private List<Stade> stades;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Stade stade);
    }

    public TerrainAdapter(List<Stade> stades, OnItemClickListener listener) {
        this.stades = stades;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TerrainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_terrain, parent, false);
        return new TerrainViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TerrainViewHolder holder, int position) {
        Stade stade = stades.get(position);
        holder.tvName.setText(stade.getNomStade());
        holder.tvLocation.setText(stade.getAdresse());
        holder.tvPrice.setText("Prix: " + stade.getPrixHeure() + " MAD/h");
        holder.tvRating.setText("★ 4.8"); // Note temporaire - à remplacer plus tard
        // Ajouter image si disponible
        // Charger l'image si disponible
        if (stade.getPhotos() != null && !stade.getPhotos().isEmpty()) {
            // Ici vous pouvez charger avec Glide ou Picasso plus tard
            // Glide.with(holder.imgThumb.getContext()).load(stade.getPhotos().get(0)).into(holder.imgThumb);
            holder.imgThumb.setImageResource(R.drawable.ic_launcher_background); // Placeholder
        } else {
            holder.imgThumb.setImageResource(R.drawable.ic_launcher_background); // Image par défaut
        }
        holder.itemView.setOnClickListener(v -> listener.onItemClick(stade));
    }

    @Override
    public int getItemCount() {
        return stades.size();
    }

    static class TerrainViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumb;
        TextView tvName, tvLocation,tvPrice, tvRating;

        TerrainViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgThumb);
            tvName = itemView.findViewById(R.id.tvName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvRating = itemView.findViewById(R.id.tvRating);
        }
    }
}