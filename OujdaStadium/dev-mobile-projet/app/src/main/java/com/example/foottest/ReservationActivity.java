// ReservationActivity.java
package com.example.foottest;

import android.content.Intent;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ReservationActivity extends AppCompatActivity {

    private Stade stade;
    private String selectedCreneau;
    private EditText etDate, etJoueurs, etNotes;
    private Button btnReserver, btnSelectDate;
    private TextView tvStadeName, tvCreneau, tvPrixTotal;
    private FireBaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation);

        firebaseManager = new FireBaseManager();

        // Récupérer les données
        stade = (Stade) getIntent().getSerializableExtra("stade");
        selectedCreneau = getIntent().getStringExtra("creneau");
        if (stade == null || selectedCreneau == null) {
            Toast.makeText(this, "Données manquantes", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        initializeViews();
        setupListeners();
        updatePrixTotal();
    }

    private void initializeViews() {
        etDate = findViewById(R.id.etDate);
        etJoueurs = findViewById(R.id.etJoueurs);
        etNotes = findViewById(R.id.etNotes);
        btnReserver = findViewById(R.id.btnReserver);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        tvStadeName = findViewById(R.id.tvStadeName);
        tvCreneau = findViewById(R.id.tvCreneau);
        tvPrixTotal = findViewById(R.id.tvPrixTotal);

        tvStadeName.setText(stade.getNomStade());
        tvCreneau.setText(selectedCreneau);
        etJoueurs.setText("10"); // Valeur par défaut
    }

    private void setupListeners() {
        btnSelectDate.setOnClickListener(v -> showDatePicker());

        etJoueurs.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { updatePrixTotal(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        btnReserver.setOnClickListener(v -> {
            if (validerFormulaire()) {
                creerReservation();
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    // Formater la date
                    String dateStr = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year);
                    etDate.setText(dateStr);

                    // Vérifier la disponibilité
                    verifierDisponibilite(selectedDate.getTime());
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        // Empêcher la sélection de dates passées
        datePicker.getDatePicker().setMinDate(calendar.getTimeInMillis());
        calendar.add(Calendar.MONTH, 3); // 3 mois max à l'avance
        datePicker.getDatePicker().setMaxDate(calendar.getTimeInMillis());

        datePicker.show();
    }

    private void verifierDisponibilite(Date date) {
        firebaseManager.verifierCreneauDisponible(stade.getId(), selectedCreneau, date,
                disponible -> {
                    runOnUiThread(() -> {
                        if (!disponible) {
                            etDate.setError("Ce créneau n'est pas disponible à cette date");
                            btnReserver.setEnabled(false);
                        } else {
                            etDate.setError(null);
                            btnReserver.setEnabled(true);
                        }
                        updatePrixTotal();
                    });
                });
    }

    private void updatePrixTotal() {
        try {
            int joueurs = Integer.parseInt(etJoueurs.getText().toString());
            double prixTotal = stade.getPrixHeure() * (joueurs / 10.0); // Prix basé sur 10 joueurs
            tvPrixTotal.setText(String.format("%.2f MAD", prixTotal));
        } catch (NumberFormatException e) {
            tvPrixTotal.setText("0.00 MAD");
        }
    }

    private boolean validerFormulaire() {
        if (etDate.getText().toString().isEmpty()) {
            etDate.setError("Veuillez sélectionner une date");
            return false;
        }

        if (etJoueurs.getText().toString().isEmpty()) {
            etJoueurs.setError("Veuillez saisir le nombre de joueurs");
            return false;
        }

        try {
            int joueurs = Integer.parseInt(etJoueurs.getText().toString());
            if (joueurs < 2 || joueurs > stade.getCapacite()) {
                etJoueurs.setError("Nombre de joueurs invalide (2-" + stade.getCapacite() + ")");
                return false;
            }
        } catch (NumberFormatException e) {
            etJoueurs.setError("Nombre invalide");
            return false;
        }

        return true;
    }

    private void creerReservation() {
        btnReserver.setEnabled(false);

        try {
            // Récupérer les données
            int joueurs = Integer.parseInt(etJoueurs.getText().toString());
            double prixTotal = stade.getPrixHeure() * (joueurs / 10.0);

            // Convertir la date
            String dateStr = etDate.getText().toString();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date dateReservation = sdf.parse(dateStr);

            // Créer la réservation
            String userId = "user123"; // À remplacer par l'ID réel de l'utilisateur
            Reservation reservation = new Reservation(
                    userId, stade.getId(), stade.getNomStade(),
                    selectedCreneau, dateReservation, prixTotal, joueurs
            );
            reservation.setNotes(etNotes.getText().toString());

            firebaseManager.creerReservation(reservation, new FireBaseManager.OnReservationOperationListener() {
                @Override
                public void onSuccess(Reservation reservation) {
                    runOnUiThread(() -> {
                        Toast.makeText(ReservationActivity.this,
                                "Réservation confirmée!", Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(ReservationActivity.this, DetailTerrainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    runOnUiThread(() -> {
                        btnReserver.setEnabled(true);
                        Toast.makeText(ReservationActivity.this,
                                "Erreur: " + errorMessage, Toast.LENGTH_LONG).show();
                    });
                }
            });

        } catch (Exception e) {
            btnReserver.setEnabled(true);
            Toast.makeText(this, "Erreur lors de la création", Toast.LENGTH_SHORT).show();
        }
    }
}