package com.example.foottest;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TerrainCardAdapter extends RecyclerView.Adapter<TerrainCardAdapter.TerrainViewHolder> {

    private List<Stade> stadesList;
    private OnTerrainClickListener listener;

    public interface OnTerrainClickListener {
        void onTerrainClick(Stade stade);
        void onAddToCartClick(Stade stade);
    }

    public TerrainCardAdapter(List<Stade> stadesList, OnTerrainClickListener listener) {
        this.stadesList = stadesList;
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
        Stade stade = stadesList.get(position);

        // Nom du stade
        holder.productName.setText(stade.getNomStade());

        // Prix
        holder.productPrice.setText(String.format("%.0f MAD/h", stade.getPrixHeure()));

        // Image du stade
        if (stade.getPhotos() != null && !stade.getPhotos().isEmpty()) {
            String photoUrl = stade.getPhotos().get(0);
            try {
                Uri imageUri = Uri.parse(photoUrl);
                holder.productImage.setImageURI(imageUri);
            } catch (Exception e) {
                holder.productImage.setImageResource(R.drawable.ic_launcher_background);
            }
        } else {
            holder.productImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // Click sur la card
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTerrainClick(stade);
            }
        });

        // Click sur le bouton "Ajouter"
        holder.addToCartButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddToCartClick(stade);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stadesList.size();
    }

    static class TerrainViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName;
        TextView productPrice;
        ImageView addToCartButton;

        public TerrainViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            addToCartButton = itemView.findViewById(R.id.addToCartButton);
        }
    }
}