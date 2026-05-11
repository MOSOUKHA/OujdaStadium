package com.example.foottest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TerrainsActivity extends AppCompatActivity {

    private RecyclerView rvTerrains;
    private FireBaseManager firebaseManager;
    private List<Stade> stades;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terrains);
        // Vérifier si on vient d'une réservation réussie
        if (getIntent() != null && getIntent().hasExtra("reservation_success")) {
            boolean reservationSuccess = getIntent().getBooleanExtra("reservation_success", false);
            if (reservationSuccess) {
                String reservationId = getIntent().getStringExtra("reservation_id");
                Toast.makeText(this, "✅ Réservation #" + reservationId + " confirmée!",
                        Toast.LENGTH_LONG).show();
            }
        }

        rvTerrains = findViewById(R.id.rvTerrains);
        rvTerrains.setLayoutManager(new LinearLayoutManager(this));

        firebaseManager = new FireBaseManager();

        // Récupérer tous les stades depuis Firebase
        firebaseManager.getAllStades(new FireBaseManager.OnStadesLoadedListener() {
            @Override
            public void onStadesLoaded(List<Stade> stadesList) {
                runOnUiThread(() -> {
                    stades = stadesList;

                    if (stades.isEmpty()) {
                        Toast.makeText(TerrainsActivity.this, "Aucun terrain disponible", Toast.LENGTH_SHORT).show();
                    }

                    // Adapter pour RecyclerView
                    TerrainAdapter adapter = new TerrainAdapter(stades, new TerrainAdapter.OnItemClickListener() {
                        @Override
                        public void onItemClick(Stade stade) {
                            // Ouvrir l'activité de détail
                            Intent intent = new Intent(TerrainsActivity.this, DetailTerrainActivity.class);
                            intent.putExtra("stadeId", stade.getId());
                            startActivity(intent);
                        }
                    });
                    rvTerrains.setAdapter(adapter);
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(TerrainsActivity.this, "Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}