package com.example.foottest;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvUserPhone, tvUserType;
    private ImageView imgProfilePicture;
    private Button btnEditProfile, btnChangePassword;
    private Button btnDeconnexion, btnSupprimerCompte;

    // Bottom Navigation Container
    private LinearLayout bottomNavigation;

    // Joueur Navigation Views
    private LinearLayout bottomNavJoueur;
    private LinearLayout navHomeJoueur, navFavoriteJoueur, navReview, navProfileJoueur;
    private ImageView imgNavHomeJoueur, imgNavFavoriteJoueur, imgNavSearchJoueur, imgNavProfileJoueur;
    private TextView tvNavHomeJoueur, tvNavFavoriteJoueur, tvNavSearchJoueur, tvNavProfileJoueur;

    // Admin Navigation Views
    private BottomNavigationView bottomNavAdmin;

    private SessionManager sessionManager;
    private FireBaseManager firebaseManager;
    private Utilisateur currentUser;
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        firebaseManager = new FireBaseManager();

        // Vérifier si l'utilisateur est connecté
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        initializeViews();
        loadUserInfo();
        setupListeners();
    }

    private void initializeViews() {
        // Informations utilisateur
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvUserType = findViewById(R.id.tvUserType);

        // Boutons d'action
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnDeconnexion = findViewById(R.id.btnDeconnexion);
        btnSupprimerCompte = findViewById(R.id.btnSupprimerCompte);

        // Bottom Navigation Container
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Joueur Navigation
        bottomNavJoueur = findViewById(R.id.bottomNavJoueur);
        navHomeJoueur = findViewById(R.id.nav_home_joueur);
        navFavoriteJoueur = findViewById(R.id.nav_favorite_joueur);
        navReview = findViewById(R.id.nav_review);
        navProfileJoueur = findViewById(R.id.nav_profile_joueur);

        imgNavHomeJoueur = findViewById(R.id.imgNavHomeJoueur);
        imgNavFavoriteJoueur = findViewById(R.id.imgNavFavoriteJoueur);
        imgNavSearchJoueur = findViewById(R.id.imgNavSearchJoueur);
        imgNavProfileJoueur = findViewById(R.id.imgNavProfileJoueur);

        tvNavHomeJoueur = findViewById(R.id.tvNavHomeJoueur);
        tvNavFavoriteJoueur = findViewById(R.id.tvNavFavoriteJoueur);
        tvNavSearchJoueur = findViewById(R.id.tvNavSearchJoueur);
        tvNavProfileJoueur = findViewById(R.id.tvNavProfileJoueur);

        // Admin Navigation
        bottomNavAdmin = findViewById(R.id.bottomNavAdmin);
    }

    private void setupBottomNavigation(String userType) {
        // Make bottom navigation visible
        bottomNavigation.setVisibility(View.VISIBLE);

        if ("admin".equals(userType)) {
            // Show admin bottom navigation
            bottomNavAdmin.setVisibility(View.VISIBLE);
            bottomNavJoueur.setVisibility(View.GONE);

            // Setup admin bottom navigation
            setupAdminBottomNavigation();
        } else {
            // Show joueur bottom navigation
            bottomNavAdmin.setVisibility(View.GONE);
            bottomNavJoueur.setVisibility(View.VISIBLE);

            // Setup joueur bottom navigation
            setupJoueurBottomNavigation();
        }
    }

    private void setupJoueurBottomNavigation() {
        // Mettre en surbrillance l'onglet Profil
        highlightNavItem(navProfileJoueur, imgNavProfileJoueur, tvNavProfileJoueur, true);

        navHomeJoueur.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, AccueilActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        navFavoriteJoueur.setOnClickListener(v -> {
            Toast.makeText(this, "Favoris", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, FavActivity.class); // ✅ Changé
            startActivity(intent);
        });


        navReview.setOnClickListener(v -> {
            Toast.makeText(this, "Mes Réservations", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, MyReviewActivity.class);
            startActivity(intent);
            finish(); // Ferme ProfileActivity si vous voulez
        });

        navProfileJoueur.setOnClickListener(v -> {
            // Déjà sur la page profil
            Toast.makeText(this, "Vous êtes sur votre profil", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupAdminBottomNavigation() {
        // Highlight the profile item
        bottomNavAdmin.setSelectedItemId(R.id.nav_profile);

        bottomNavAdmin.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_stades) {
                // Navigate to admin stades
                Intent intent = new Intent(ProfileActivity.this, GestionStadesActivity.class);
                intent.putExtra("email", currentUser.getEmail());
                intent.putExtra("userId", currentUser.getId());
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_reservations) {
                // Navigate to admin reservations
                Intent intent = new Intent(ProfileActivity.this, MesReservationsActivity.class);
                intent.putExtra("email", currentUser.getEmail());
                intent.putExtra("userId", currentUser.getId());
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                // Already on profile
                return true;
            }
            return false;
        });
    }

    private void highlightNavItem(LinearLayout navItem, ImageView icon, TextView text, boolean isActive) {
        if (isActive) {
            icon.setColorFilter(getResources().getColor(android.R.color.holo_green_dark));
            text.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            icon.setColorFilter(getResources().getColor(android.R.color.darker_gray));
            text.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private void loadUserInfo() {
        String userId = sessionManager.getUserId();
        if (userId != null) {
            firebaseManager.getUtilisateur(userId, new FireBaseManager.OnUserLoadListener() {
                @Override
                public void onUserLoaded(Utilisateur utilisateur) {
                    runOnUiThread(() -> {
                        currentUser = utilisateur;
                        displayUserInfo(utilisateur);
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this,
                                "Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                        Log.e("ProfileActivity", "Erreur chargement utilisateur: " + errorMessage);
                    });
                }
            });
        }
    }

    private void displayUserInfo(Utilisateur utilisateur) {
        tvUserName.setText(utilisateur.getNom());
        tvUserEmail.setText(utilisateur.getEmail());
        tvUserPhone.setText(utilisateur.getNumero());

        // Store user type
        userType = utilisateur.getTypeUtilisateur();

        // Afficher le type d'utilisateur
        String typeText = userType.equals("admin") ? "Administrateur" : "Joueur";
        tvUserType.setText(typeText);

        // Setup appropriate bottom navigation
        setupBottomNavigation(userType);

        // TODO: Charger la photo de profil si disponible
        imgProfilePicture.setImageResource(R.drawable.ic_profile);
    }

    private void setupListeners() {
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnDeconnexion.setOnClickListener(v -> showLogoutDialog());
        btnSupprimerCompte.setOnClickListener(v -> showDeleteAccountDialog());
    }

    // ==================== MODIFIER LE PROFIL ====================

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifier le profil");

        // Créer le layout du dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Champ Nom
        final EditText inputNom = new EditText(this);
        inputNom.setHint("Nouveau nom");
        inputNom.setText(currentUser.getNom());
        layout.addView(inputNom);

        // Champ Téléphone
        final EditText inputPhone = new EditText(this);
        inputPhone.setHint("Nouveau numéro");
        inputPhone.setText(currentUser.getNumero());
        inputPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        layout.addView(inputPhone);

        builder.setView(layout);

        builder.setPositiveButton("Enregistrer", (dialog, which) -> {
            String newNom = inputNom.getText().toString().trim();
            String newPhone = inputPhone.getText().toString().trim();

            if (validateProfileUpdate(newNom, newPhone)) {
                updateProfile(newNom, newPhone);
            }
        });

        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private boolean validateProfileUpdate(String nom, String phone) {
        if (nom.isEmpty()) {
            Toast.makeText(this, "Le nom est obligatoire", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (phone.isEmpty()) {
            Toast.makeText(this, "Le téléphone est obligatoire", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (phone.length() != 10) {
            Toast.makeText(this, "Le numéro doit contenir 10 chiffres", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
    private void updateProfile(String newNom, String newPhone) {
        currentUser.setNom(newNom);
        currentUser.setNumero(newPhone);

        firebaseManager.updateUtilisateur(currentUser, new FireBaseManager.OnUserOperationListener() {
            @Override
            public void onSuccess(Utilisateur utilisateur) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this,
                            "✅ Profil mis à jour!", Toast.LENGTH_SHORT).show();
                    displayUserInfo(utilisateur);
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this,
                            "❌ Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ==================== CHANGER LE MOT DE PASSE ====================

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Changer le mot de passe");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Ancien mot de passe
        final EditText inputOldPassword = new EditText(this);
        inputOldPassword.setHint("Ancien mot de passe");
        inputOldPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputOldPassword);

        // Nouveau mot de passe
        final EditText inputNewPassword = new EditText(this);
        inputNewPassword.setHint("Nouveau mot de passe");
        inputNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputNewPassword);

        // Confirmer nouveau mot de passe
        final EditText inputConfirmPassword = new EditText(this);
        inputConfirmPassword.setHint("Confirmer le nouveau mot de passe");
        inputConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputConfirmPassword);

        builder.setView(layout);

        builder.setPositiveButton("Changer", (dialog, which) -> {
            String oldPassword = inputOldPassword.getText().toString().trim();
            String newPassword = inputNewPassword.getText().toString().trim();
            String confirmPassword = inputConfirmPassword.getText().toString().trim();

            if (validatePasswordChange(oldPassword, newPassword, confirmPassword)) {
                changePassword(oldPassword, newPassword);
            }
        });

        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private boolean validatePasswordChange(String oldPassword, String newPassword, String confirmPassword) {
        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Tous les champs sont obligatoires", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!oldPassword.equals(currentUser.getMotdepasse())) {
            Toast.makeText(this, "Ancien mot de passe incorrect", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!newPassword.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            Toast.makeText(this, "Utilisez au moins un caractère spécial", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!newPassword.matches(".*\\d.*")) {
            Toast.makeText(this, "Utilisez au moins un chiffre", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void changePassword(String oldPassword, String newPassword) {
        currentUser.setMotdepasse(newPassword);

        firebaseManager.updateUtilisateur(currentUser, new FireBaseManager.OnUserOperationListener() {
            @Override
            public void onSuccess(Utilisateur utilisateur) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this,
                            "✅ Mot de passe changé avec succès!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this,
                            "❌ Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ==================== DÉCONNEXION ====================

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Voulez-vous vraiment vous déconnecter?")
                .setIcon(R.drawable.ic_info)
                .setPositiveButton("Oui", (dialog, which) -> {
                    logout();
                })
                .setNegativeButton("Non", null)
                .show();
    }

    private void logout() {
        sessionManager.logout();
        Toast.makeText(this, "✅ Déconnexion réussie", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }

    // ==================== SUPPRIMER LE COMPTE ====================

    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚠️ Supprimer le compte");
        builder.setMessage("ATTENTION: Cette action est irréversible!\n\n" +
                "Toutes vos données seront définitivement supprimées:\n" +
                "• Vos informations personnelles\n" +
                "• Vos réservations\n" +
                "• Vos favoris\n\n" +
                "Voulez-vous vraiment continuer?");
        builder.setIcon(android.R.drawable.ic_dialog_alert);

        builder.setPositiveButton("Oui, supprimer", (dialog, which) -> {
            showPasswordConfirmationDialog();
        });

        builder.setNegativeButton("Annuler", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Mettre le bouton positif en rouge
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(getResources().getColor(android.R.color.holo_red_dark));
    }

    private void showPasswordConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmation");
        builder.setMessage("Entrez votre mot de passe pour confirmer:");

        final EditText inputPassword = new EditText(this);
        inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        inputPassword.setHint("Mot de passe");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.addView(inputPassword);

        builder.setView(layout);

        builder.setPositiveButton("Confirmer", (dialog, which) -> {
            String password = inputPassword.getText().toString().trim();

            if (password.equals(currentUser.getMotdepasse())) {
                deleteAccount();
            } else {
                Toast.makeText(this, "Mot de passe incorrect", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void deleteAccount() {
        String userId = sessionManager.getUserId();

        firebaseManager.supprimerUtilisateur(userId, new FireBaseManager.OnDeleteListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    sessionManager.logout();
                    Toast.makeText(ProfileActivity.this,
                            "Compte supprimé avec succès", Toast.LENGTH_LONG).show();
                    redirectToLogin();
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this,
                            "Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Return to appropriate home based on user type
        if ("admin".equals(userType)) {
            Intent intent = new Intent(ProfileActivity.this, GestionStadesActivity.class);
            intent.putExtra("email", currentUser.getEmail());
            intent.putExtra("userId", currentUser.getId());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(ProfileActivity.this, AccueilActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
        super.onBackPressed();
    }
}