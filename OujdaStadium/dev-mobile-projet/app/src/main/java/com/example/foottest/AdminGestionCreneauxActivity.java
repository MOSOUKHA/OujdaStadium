package com.example.foottest;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminGestionCreneauxActivity extends AppCompatActivity {

    private Stade stade;
    private EditText etHeureOuverture, etHeureFermeture, etDureeCreneau;
    private RecyclerView rvCreneaux;
    private Button btnGenererCreneaux, btnSauvegarder;
    private List<String> listeCreneauxActuels;
    private FireBaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_creneaux);

        firebaseManager = new FireBaseManager();
        String stadeId = getIntent().getStringExtra("stadeId");
        initializeViews();
        loadStade(stadeId);
    }

    private void initializeViews() {
        etHeureOuverture = findViewById(R.id.etHeureOuverture);
        etHeureFermeture = findViewById(R.id.etHeureFermeture);
        etDureeCreneau = findViewById(R.id.etDureeCreneau);
        rvCreneaux = findViewById(R.id.rvCreneaux);
        btnGenererCreneaux = findViewById(R.id.btnGenererCreneaux);
        btnSauvegarder = findViewById(R.id.btnSauvegarder);

        // Configurer le RecyclerView
        rvCreneaux.setLayoutManager(new LinearLayoutManager(this));
        listeCreneauxActuels = new ArrayList<>();

        btnGenererCreneaux.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                genererCreneaux();
            }
        });

        btnSauvegarder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sauvegarderCreneaux();
            }
        });
    }

    private void loadStade(String stadeId) {
        if (stadeId == null || stadeId.isEmpty()) {
            Toast.makeText(this, "ID du stade manquant", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseManager.getAllStades(new FireBaseManager.OnStadesLoadedListener() {
            @Override
            public void onStadesLoaded(List<Stade> stades) {
                for (Stade s : stades) {
                    if (s.getId().equals(stadeId)) {
                        stade = s;
                        runOnUiThread(() -> afficherDonneesStade());
                        break;
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() ->
                        Toast.makeText(AdminGestionCreneauxActivity.this, "Erreur: " + errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void afficherDonneesStade() {
        if (stade != null) {
            // Afficher les données existantes
            if (stade.getHeureOuverture() != null) {
                etHeureOuverture.setText(stade.getHeureOuverture());
            }
            if (stade.getHeureFermeture() != null) {
                etHeureFermeture.setText(stade.getHeureFermeture());
            }
            if (stade.getDureeCreneau() > 0) {
                etDureeCreneau.setText(String.valueOf(stade.getDureeCreneau()));
            }

            // Afficher les créneaux existants
            if (stade.getCreneauxDisponibles() != null) {
                listeCreneauxActuels = stade.getCreneauxDisponibles();
                afficherCreneaux();
            }
        }
    }

    private void genererCreneaux() {
        String heureOuverture = etHeureOuverture.getText().toString().trim();
        String heureFermeture = etHeureFermeture.getText().toString().trim();
        String dureeText = etDureeCreneau.getText().toString().trim();

        if (heureOuverture.isEmpty() || heureFermeture.isEmpty() || dureeText.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int duree = Integer.parseInt(dureeText);
            listeCreneauxActuels = genererCreneauxAutomatiques(heureOuverture, heureFermeture, duree);
            afficherCreneaux();

            Toast.makeText(this, listeCreneauxActuels.size() + " créneaux générés", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Durée invalide", Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> genererCreneauxAutomatiques(String heureOuverture, String heureFermeture, int duree) {
        List<String> creneauxGeneres = new ArrayList<>();

        try {
            int heureOuv = Integer.parseInt(heureOuverture.split(":")[0]);
            int minuteOuv = Integer.parseInt(heureOuverture.split(":")[1]);
            int heureFerm = Integer.parseInt(heureFermeture.split(":")[0]);
            int minuteFerm = Integer.parseInt(heureFermeture.split(":")[1]);

            int currentHeure = heureOuv;
            int currentMinute = minuteOuv;

            while (currentHeure < heureFerm || (currentHeure == heureFerm && currentMinute < minuteFerm)) {
                // Heure de début
                String debut = String.format("%02d:%02d", currentHeure, currentMinute);

                // Calculer l'heure de fin
                int finHeure = currentHeure;
                int finMinute = currentMinute + duree;

                if (finMinute >= 60) {
                    finHeure += finMinute / 60;
                    finMinute = finMinute % 60;
                }

                String fin = String.format("%02d:%02d", finHeure, finMinute);

                // Vérifier si on dépasse l'heure de fermeture
                if (finHeure > heureFerm || (finHeure == heureFerm && finMinute > minuteFerm)) {
                    break;
                }

                creneauxGeneres.add(debut + " - " + fin);

                // Passer au créneau suivant
                currentHeure = finHeure;
                currentMinute = finMinute;
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Créneaux par défaut en cas d'erreur
            creneauxGeneres.add("08:00 - 09:00");
            creneauxGeneres.add("09:00 - 10:00");
            creneauxGeneres.add("10:00 - 11:00");
        }

        return creneauxGeneres;
    }

    private void afficherCreneaux() {
        SlotAdapter adapter = new SlotAdapter(listeCreneauxActuels, null);
        rvCreneaux.setAdapter(adapter);
    }

    private void sauvegarderCreneaux() {
        if (stade == null) {
            Toast.makeText(this, "Stade non chargé", Toast.LENGTH_SHORT).show();
            return;
        }

        if (listeCreneauxActuels.isEmpty()) {
            Toast.makeText(this, "Générez d'abord les créneaux", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("heureOuverture", etHeureOuverture.getText().toString());
        updates.put("heureFermeture", etHeureFermeture.getText().toString());
        updates.put("dureeCreneau", Integer.parseInt(etDureeCreneau.getText().toString()));
        updates.put("creneauxDisponibles", listeCreneauxActuels);

        FirebaseFirestore.getInstance().collection("stades")
                .document(stade.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Créneaux sauvegardés avec succès!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}