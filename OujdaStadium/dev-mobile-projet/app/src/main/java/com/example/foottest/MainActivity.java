package com.example.foottest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Button connecter, voir;
    private TextView inscrire;
    private EditText etEmail, etPassword;
    private FireBaseManager firebaseManager;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("MainActivity", "onCreate démarré");

        // Initialisation
        firebaseManager = new FireBaseManager();
        sessionManager = new SessionManager(this);

        // Vérifier si l'utilisateur est déjà connecté
        if (sessionManager.isLoggedIn()) {
            Log.d("MainActivity", "Utilisateur déjà connecté, redirection...");
            // Pour une redirection automatique, vous devriez vérifier le userType depuis Firebase
            // Pour l'instant, redirigez vers AccueilActivity
            redirectToAccueil();
            return;
        }

        connecter = findViewById(R.id.connecter);
        inscrire = findViewById(R.id.inscrire);
        voir = findViewById(R.id.voir);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        inscrire.setOnClickListener(v -> {
            Log.d("MainActivity", "Inscription cliquée");
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        connecter.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("MainActivity", "Tentative de connexion: " + email);
            Toast.makeText(MainActivity.this, "Connexion en cours...", Toast.LENGTH_SHORT).show();

            firebaseManager.verifierConnexion(email, password, new FireBaseManager.OnLoginListener() {
                @Override
                public void onSuccess(String userId, int userType) {
                    runOnUiThread(() -> {
                        Log.d("MainActivity", "Connexion réussie - userId: " + userId + ", userType: " + userType);
                        Toast.makeText(MainActivity.this, "Connexion réussie!", Toast.LENGTH_SHORT).show();

                        // Sauvegarder dans la session
                        sessionManager.setLogin(userId);

                        // Redirection selon le type d'utilisateur
                        if (userType == 1) {
                            // Admin - aller à GestionStadesActivity
                            Log.d("MainActivity", "Redirection vers GestionStadesActivity (Admin)");
                            Intent intent = new Intent(MainActivity.this, GestionStadesActivity.class);
                            intent.putExtra("email", email);
                            intent.putExtra("userId", userId);
                            intent.putExtra("userType", userType);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } else {
                            // Utilisateur normal (joueur) - aller à AccueilActivity
                            Log.d("MainActivity", "Redirection vers AccueilActivity (Joueur)");
                            Intent intent = new Intent(MainActivity.this, AccueilActivity.class);
                            intent.putExtra("email", email);
                            intent.putExtra("userId", userId);
                            intent.putExtra("userType", userType);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                        finish();
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    runOnUiThread(() -> {
                        Log.e("MainActivity", "Erreur de connexion: " + errorMessage);
                        Toast.makeText(MainActivity.this, "Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        voir.setOnClickListener(v -> {
            Log.d("MainActivity", "Voir terrains cliqué");
            Intent intent = new Intent(MainActivity.this, TerrainsActivity.class);
            startActivity(intent);
        });
    }

    private void redirectToAccueil() {
        Intent intent = new Intent(MainActivity.this, AccueilActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Vérifiez à nouveau au cas où l'utilisateur s'est connecté depuis LoginActivity
        if (sessionManager.isLoggedIn()) {
            Log.d("MainActivity", "onResume - Utilisateur connecté, redirection");
            redirectToAccueil();
        }
    }
}