package com.example.foottest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;


public class ReservationsAdapter extends RecyclerView.Adapter<ReservationsAdapter.ViewHolder> {

    private List<Reservation> reservations;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public ReservationsAdapter(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reservation reservation = reservations.get(position);

        holder.tvStadeName.setText(reservation.getStadeName());
        holder.tvCreneau.setText(reservation.getCreneau());
        holder.tvJoueurs.setText(reservation.getNombreJoueurs() + " joueurs");
        holder.tvPrix.setText(reservation.getPrixTotal() + " MAD");

        if (reservation.getDateReservation() != null) {
            holder.tvDate.setText(dateFormat.format(reservation.getDateReservation()));
        }

        // Gestion de la couleur du statut
        switch (reservation.getStatut()) {
            case "confirmée":
                holder.tvStatut.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_green_dark));
                break;
            case "en attente":
                holder.tvStatut.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_orange_dark));
                break;
            case "refusée":
                holder.tvStatut.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_red_dark));
                break;
            default:
                holder.tvStatut.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), android.R.color.darker_gray));
        }
    }

    @Override
    public int getItemCount() {
        return reservations.size();
    }

    public void updateData(List<Reservation> newReservations) {
        this.reservations = newReservations;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStadeName, tvDate, tvCreneau, tvJoueurs, tvPrix, tvStatut;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStadeName = itemView.findViewById(R.id.tvStadeName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCreneau = itemView.findViewById(R.id.tvCreneau);
            tvJoueurs = itemView.findViewById(R.id.tvJoueurs);
            tvPrix = itemView.findViewById(R.id.tvPrix);
            tvStatut = itemView.findViewById(R.id.tvStatut);
        }
    }
}