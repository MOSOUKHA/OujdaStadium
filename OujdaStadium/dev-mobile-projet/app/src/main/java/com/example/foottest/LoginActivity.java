package com.example.foottest;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private EditText nom, numero, gmail, motdepasse, etDescriptionLieu, etNomStade, etCapacite, etPrix;
    private Button confirmer, retour, importer, btnAjouterStade;
    private LinearLayout imagesContainer;

    private LinearLayout localisationLayout, stadesLayout;
    private Button btnChoisirLocalisation;
    private TextView tvLocalisationSelectionnee;
    private double selectedLatitude = 0;
    private double selectedLongitude = 0;
    private String selectedAddress = "";

    private Spinner TypeUser;
    private List<Uri> imageUris = new ArrayList<>();
    private List<Stade> stades = new ArrayList<>();
    // ✅ AJOUTER: Stocker les photos pour chaque stade
    private List<List<Uri>> stadesPhotos = new ArrayList<>();

    private Stade currentStade = new Stade();

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int MAP_REQUEST_CODE = 1001;
    private static final int REQUEST_VIEW_IMAGE = 200;

    private FireBaseManager firebaseManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialiser le compteur de stades
        mettreAJourCompteurStades();

        // Initialiser FirebaseManager
        firebaseManager = new FireBaseManager();

        // Initialiser les vues
        localisationLayout = findViewById(R.id.localisationLayout);
        stadesLayout = findViewById(R.id.stadesLayout);
        btnChoisirLocalisation = findViewById(R.id.btnChoisirLocalisation);
        tvLocalisationSelectionnee = findViewById(R.id.tvLocalisationSelectionnee);
        etDescriptionLieu = findViewById(R.id.etDescriptionLieu);
        etNomStade = findViewById(R.id.etNomStade);
        etCapacite = findViewById(R.id.etCapacite);
        etPrix = findViewById(R.id.etPrix);
        TypeUser = findViewById(R.id.TypeUser);
        imagesContainer = findViewById(R.id.imagesContainer);
        nom = findViewById(R.id.nom);
        numero = findViewById(R.id.numero);
        gmail = findViewById(R.id.gmail);
        motdepasse = findViewById(R.id.motdepasse);
        confirmer = findViewById(R.id.confirmer);
        importer = findViewById(R.id.importer);
        btnAjouterStade = findViewById(R.id.btnAjouterStade);
        firebaseManager.diagnosticFirebase(this);
        // Gérer l'importation d'image
        importer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Pour Android 10+, READ_EXTERNAL_STORAGE n'est plus nécessaire pour ACTION_GET_CONTENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ouvrirExplorateurFichiers();
                } else {
                    if (ContextCompat.checkSelfPermission(LoginActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(LoginActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                PERMISSION_REQUEST_CODE);
                    } else {
                        ouvrirExplorateurFichiers();
                    }
                }
            }
        });

        // Gérer le clic sur le bouton de sélection de localisation
        btnChoisirLocalisation.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MapActivity.class);
            startActivityForResult(intent, MAP_REQUEST_CODE);
        });

        // Gérer l'ajout d'un stade
        btnAjouterStade.setOnClickListener(v -> {
            if (validerStade()) {
                ajouterStadeToList();
                reinitialiserFormulaireStade();
                Toast.makeText(this, "Stade ajouté avec succès!", Toast.LENGTH_SHORT).show();
            }
        });

        // Configuration du spinner
        String[] types = {"joueur", "admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>
                (this,
                        android.R.layout.simple_spinner_item,
                        types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        TypeUser.setAdapter(adapter);

        // Gérer les choix de l'utilisateur
        TypeUser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (selected.equals("admin")) {
                    localisationLayout.setVisibility(View.VISIBLE);
                    stadesLayout.setVisibility(View.VISIBLE);
                    imagesContainer.setVisibility(View.VISIBLE);
                    importer.setVisibility(View.VISIBLE);
                    btnAjouterStade.setVisibility(View.VISIBLE);
                } else {
                    localisationLayout.setVisibility(View.GONE);
                    stadesLayout.setVisibility(View.GONE);
                    imagesContainer.setVisibility(View.GONE);
                    importer.setVisibility(View.GONE);
                    btnAjouterStade.setVisibility(View.GONE);
                    stades.clear();
                    imageUris.clear();
                    mettreAJourCompteurStades();
                }
                Toast.makeText(LoginActivity.this, "Tu es un : " + selected, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        confirmer.setOnClickListener(v -> {
            String phone = numero.getText().toString().trim();
            String nomm = nom.getText().toString().trim();
            String gmaill = gmail.getText().toString().trim();
            String motdepassee = motdepasse.getText().toString().trim();
            String type = TypeUser.getSelectedItem().toString();

            if (validationcomplete(nomm, phone, gmaill, motdepassee, type)) {
                // Désactiver le bouton pendant le traitement
                confirmer.setEnabled(false);
                confirmer.setText("Création en cours...");

                // Créer l'objet utilisateur
                Utilisateur utilisateur = new Utilisateur(nomm, phone, gmaill, motdepassee, type);

                // Ajouter l'utilisateur via FirebaseManager
                firebaseManager.ajouterUtilisateur(utilisateur, new FireBaseManager.OnUserOperationListener() {
                    @Override
                    public void onSuccess(Utilisateur utilisateur) {
                        Log.d("USER_CREATED", "✅ Utilisateur créé avec ID: " + utilisateur.getId());

                        // Si c'est un admin avec des stades, les uploader
                        if (type.equals("admin") && !stades.isEmpty()) {
                            uploadStadesAvecPhotos(utilisateur.getId());
                        } else {
                            // Si pas de stades, rediriger directement
                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this,
                                        "✅ Compte créé avec succès!", Toast.LENGTH_LONG).show();

                                new android.os.Handler().postDelayed(() -> {
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }, 1500);
                            });
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        runOnUiThread(() -> {
                            confirmer.setEnabled(true);
                            confirmer.setText("S'inscrire");

                            if (errorMessage.contains("email")) {
                                gmail.setError("Cet email est déjà utilisé");
                            } else if (errorMessage.contains("numéro")) {
                                numero.setError("Ce numéro est déjà utilisé");
                            }
                            Toast.makeText(LoginActivity.this,
                                    "❌ Erreur: " + errorMessage, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });

    }

    private void uploadStadesAvecPhotos(String userId) {
        final int[] stadesUploades = {0};
        final int totalStades = stades.size();

        Log.d("UPLOAD", "========================================");
        Log.d("UPLOAD", "Début upload de " + totalStades + " stades");
        Log.d("UPLOAD", "UserId: " + userId);

        if (totalStades == 0) {
            Toast.makeText(this, "Aucun stade à uploader", Toast.LENGTH_SHORT).show();
            return;
        }

        // Uploader chaque stade avec ses photos
        for (int i = 0; i < stades.size(); i++) {
            final int index = i;
            Stade stade = stades.get(i);
            stade.setUserId(userId);

            // ✅ Récupérer les photos POUR CE STADE SPÉCIFIQUE
            List<Uri> photoUris = stadesPhotos.get(i);

            Log.d("UPLOAD", "--- Stade " + (i+1) + "/" + totalStades + " ---");
            Log.d("UPLOAD", "Nom: " + stade.getNomStade());
            Log.d("UPLOAD", "Photos: " + photoUris.size());

            for (int j = 0; j < photoUris.size(); j++) {
                Log.d("UPLOAD", "  Photo " + j + ": " + photoUris.get(j).toString());
            }

            // ✅ IMPORTANT: Passer le contexte (this) comme troisième paramètre
            firebaseManager.uploadPhotosAndAddStade(stade, photoUris,
                    new FireBaseManager.OnStadeOperationListener() {
                        @Override
                        public void onSuccess(Stade stadeUploaded) {
                            stadesUploades[0]++;

                            Log.d("UPLOAD_SUCCESS", "========================================");
                            Log.d("UPLOAD_SUCCESS", "✅ Stade uploadé: " + stadeUploaded.getNomStade());
                            Log.d("UPLOAD_SUCCESS", "Progression: " + stadesUploades[0] + "/" + totalStades);

                            // ✅ VÉRIFIER les URLs Firebase (doivent commencer par https://)
                            if (stadeUploaded.getPhotos() != null) {
                                Log.d("UPLOAD_SUCCESS", "URLs des photos:");
                                for (int k = 0; k < stadeUploaded.getPhotos().size(); k++) {
                                    String url = stadeUploaded.getPhotos().get(k);
                                    Log.d("UPLOAD_SUCCESS", "  [" + k + "] " + url);

                                    if (!url.startsWith("https://")) {
                                        Log.e("UPLOAD_ERROR", "❌ URL invalide (pas HTTPS): " + url);
                                    } else {
                                        Log.d("UPLOAD_SUCCESS", "  ✅ URL valide");
                                    }
                                }
                            } else {
                                Log.e("UPLOAD_ERROR", "❌ Aucune photo après upload!");
                            }

                            // Quand tous les stades sont uploadés
                            if (stadesUploades[0] == totalStades) {
                                Log.d("UPLOAD_COMPLETE", "========================================");
                                Log.d("UPLOAD_COMPLETE", "✅ TOUS LES STADES UPLOADÉS!");
                                Log.d("UPLOAD_COMPLETE", "========================================");

                                runOnUiThread(() -> {
                                    confirmer.setEnabled(true);
                                    confirmer.setText("S'inscrire");

                                    Toast.makeText(LoginActivity.this,
                                            "✅ Compte et " + totalStades + " stade(s) créé(s) avec succès!",
                                            Toast.LENGTH_LONG).show();

                                    new android.os.Handler().postDelayed(() -> {
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }, 2000);
                                });
                            }
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Log.e("UPLOAD_ERROR", "========================================");
                            Log.e("UPLOAD_ERROR", "❌ Échec upload stade " + (index+1));
                            Log.e("UPLOAD_ERROR", "Erreur: " + errorMessage);
                            Log.e("UPLOAD_ERROR", "========================================");

                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this,
                                        "❌ Erreur upload stade: " + errorMessage,
                                        Toast.LENGTH_LONG).show();

                                // Réactiver le bouton en cas d'erreur
                                confirmer.setEnabled(true);
                                confirmer.setText("S'inscrire");
                            });
                        }
                    },LoginActivity.this); // ✅ Passer le contexte ici
        }
    }

    // Méthode pour ouvrir l'explorateur de fichiers
    private void ouvrirExplorateurFichiers() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Sélectionner des photos"), PICK_IMAGE_REQUEST);
        } catch (android.content.ActivityNotFoundException ex) {
            // Fallback vers la galerie standard
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
        }
    }

    // Gérer le résultat de la demande de permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ouvrirExplorateurFichiers();
            } else {
                Toast.makeText(this, "Permission refusée pour accéder aux fichiers", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Gérer le résultat de la sélection d'image et de la carte
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            try {
                if (data.getClipData() != null) {
                    // Images multiples sélectionnées
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        imageUris.add(imageUri);
                    }
                } else if (data.getData() != null) {
                    // Une seule image sélectionnée
                    Uri imageUri = data.getData();
                    imageUris.add(imageUri);
                }

                // Afficher toutes les images
                afficherImagesSelectionnees();

                Toast.makeText(this, imageUris.size() + " image(s) sélectionnée(s)", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Erreur lors du chargement des images", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Sélection annulée", Toast.LENGTH_SHORT).show();
        }

        // Gestion de la carte
        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedLatitude = data.getDoubleExtra("latitude", 0);
            selectedLongitude = data.getDoubleExtra("longitude", 0);
            selectedAddress = data.getStringExtra("adresse");

            if (selectedAddress != null && !selectedAddress.isEmpty()) {
                tvLocalisationSelectionnee.setText(selectedAddress);
                Toast.makeText(this, "Localisation sélectionnée: " + selectedAddress, Toast.LENGTH_LONG).show();
            } else {
                tvLocalisationSelectionnee.setText("Localisation sélectionnée");
                Toast.makeText(this, "Localisation sélectionnée avec succès", Toast.LENGTH_SHORT).show();
            }
        }

        // Gestion du retour de l'activité de visualisation d'image
        if (requestCode == REQUEST_VIEW_IMAGE && resultCode == RESULT_OK) {
            // Rien à faire, l'utilisateur a simplement visualisé l'image
        }
    }

    // Méthode pour afficher toutes les images sélectionnées
    private void afficherImagesSelectionnees() {
        // Vider le conteneur d'images
        imagesContainer.removeAllViews();

        if (!imageUris.isEmpty()) {
            imagesContainer.setVisibility(View.VISIBLE);

            for (int i = 0; i < imageUris.size(); i++) {
                Uri imageUri = imageUris.get(i);

                // Créer une nouvelle ImageView pour chaque image
                ImageView imageView = new ImageView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        150, // largeur
                        150  // hauteur
                );
                params.setMargins(0, 0, 16, 0); // marge à droite
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setBackgroundResource(R.drawable.image_border);

                // Charger l'image dans l'ImageView
                imageView.setImageURI(imageUri);

                // Stocker l'index de l'image dans le tag
                final int imageIndex = i;

                // Ajouter un écouteur de clic pour agrandir l'image
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ouvrirImageAgrandie(imageUri);
                    }
                });

                // Ajouter l'ImageView au conteneur
                imagesContainer.addView(imageView);
            }
        } else {
            imagesContainer.setVisibility(View.GONE);
        }
    }

    // Méthode pour ouvrir une image en grand
    private void ouvrirImageAgrandie(Uri imageUri) {
        Intent intent = new Intent(LoginActivity.this, ImageViewActivity.class);
        intent.putExtra("image_uri", imageUri.toString());
        startActivityForResult(intent, REQUEST_VIEW_IMAGE);
    }

    private boolean validationcomplete(String nomm, String phone, String gmaill, String motdepassee, String type) {
        if (nomm.isEmpty()) {
            nom.setError("Le nom est obligatoire");
            return false;
        }

        if (phone.isEmpty()) {
            numero.setError("Le téléphone est obligatoire");
            return false;
        }

        if (gmaill.isEmpty()) {
            gmail.setError("L'email est obligatoire");
            return false;
        }

        if (motdepassee.isEmpty()) {
            motdepasse.setError("Le mot de passe est obligatoire");
            return false;
        }

        if (!gmaill.contains("@") || !gmaill.contains(".")) {
            gmail.setError("L'email doit contenir @ et .");
            return false;
        }

        if (phone.length() != 10) {
            numero.setError("Le numéro doit contenir exactement 10 chiffres");
            return false;
        }

        if (!phone.matches("\\d+")) {
            numero.setError("Le numéro doit contenir uniquement des chiffres");
            return false;
        }

        if (motdepassee.length() < 6) {
            motdepasse.setError("Le mot de passe doit contenir au moins 6 caractères");
            return false;
        }

        if (!motdepassee.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            motdepasse.setError("Utilisez au moins un caractère spécial");
            return false;
        }

        if (!motdepassee.matches(".*\\d.*")) {
            motdepasse.setError("Utilisez au moins un chiffre");
            return false;
        }

        // Vérification supplémentaire pour les administrateurs
        if (type.equals("admin")) {
            if (selectedLatitude == 0 || selectedLongitude == 0) {
                Toast.makeText(this, "Veuillez sélectionner la localisation du stade sur la carte", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (stades.isEmpty()) {
                Toast.makeText(this, "Veuillez ajouter au moins un stade", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }

    private void mettreAJourCompteurStades() {
        TextView tvStadesAjoutes = findViewById(R.id.tvStadesAjoutes);
        if (tvStadesAjoutes != null) {
            tvStadesAjoutes.setText("Stades ajoutés: " + stades.size());
        }
    }

    private boolean validerStade() {
        String nomStade = etNomStade.getText().toString().trim();
        String description = etDescriptionLieu.getText().toString().trim();
        String capaciteStr = etCapacite.getText().toString().trim();
        String prixStr = etPrix.getText().toString().trim();

        if (nomStade.isEmpty()) {
            etNomStade.setError("Le nom du stade est obligatoire");
            return false;
        }

        if (description.isEmpty()) {
            etDescriptionLieu.setError("La description est obligatoire");
            return false;
        }

        if (capaciteStr.isEmpty()) {
            etCapacite.setError("La capacité est obligatoire");
            return false;
        }

        if (prixStr.isEmpty()) {
            etPrix.setError("Le prix est obligatoire");
            return false;
        }

        try {
            int capacite = Integer.parseInt(capaciteStr);
            if (capacite <= 0) {
                etCapacite.setError("La capacité doit être positive");
                return false;
            }
        } catch (NumberFormatException e) {
            etCapacite.setError("Capacité invalide");
            return false;
        }

        try {
            double prix = Double.parseDouble(prixStr);
            if (prix <= 0) {
                etPrix.setError("Le prix doit être positif");
                return false;
            }
        } catch (NumberFormatException e) {
            etPrix.setError("Prix invalide");
            return false;
        }

        if (imageUris.isEmpty()) {
            Toast.makeText(this, "Veuillez ajouter au moins une photo", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void ajouterStadeToList() {
        Stade stade = new Stade();
        stade.setNomStade(etNomStade.getText().toString().trim());
        stade.setDescription(etDescriptionLieu.getText().toString().trim());
        stade.setCapacite(Integer.parseInt(etCapacite.getText().toString().trim()));
        stade.setPrixHeure(Double.parseDouble(etPrix.getText().toString().trim()));
        stade.setLatitude(selectedLatitude);
        stade.setLongitude(selectedLongitude);
        stade.setAdresse(selectedAddress);

        // ✅ Ajouter le stade
        stades.add(stade);

        // ✅ Copier les photos pour CE stade (important: faire une copie)
        List<Uri> photosDeceStade = new ArrayList<>(imageUris);
        stadesPhotos.add(photosDeceStade);

        Log.d("ADD_STADE", "Stade ajouté: " + stade.getNomStade() +
                " avec " + photosDeceStade.size() + " photos");

        // ✅ Maintenant on peut vider pour le prochain stade
        imageUris.clear();

        mettreAJourCompteurStades();
    }

    private void reinitialiserFormulaireStade() {
        etNomStade.setText("");
        etDescriptionLieu.setText("");
        etCapacite.setText("");
        etPrix.setText("");
        imagesContainer.removeAllViews();
        imagesContainer.setVisibility(View.GONE);
    }

}