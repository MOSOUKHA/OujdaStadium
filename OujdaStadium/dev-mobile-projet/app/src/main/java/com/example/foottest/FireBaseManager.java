package com.example.foottest;

import android.net.Uri;
import android.util.Log;
import android.content.Context;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.UploadTask;

import java.util.*;

public class FireBaseManager {
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private static final String COLLECTION_UTILISATEURS = "utilisateurs";
    private static final String COLLECTION_STADES = "stades";
    private static final String COLLECTION_RESERVATIONS = "reservations";
    private static final String COLLECTION_AVIS = "avis";
    private static final String TAG = "FireBaseManager";

    public FireBaseManager() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }
    // ========== UTILISATEURS ==========
    //Ajouter un utilisateur
    public void ajouterUtilisateur(Utilisateur utilisateur, OnUserOperationListener listener) {
        verifierEmailExiste(utilisateur.getEmail(), existe -> {
            if (existe) {
                listener.onFailure("Cet email est déjà utilisé");
                return;
            }
            verifierNumeroExiste(utilisateur.getNumero(), numeroExiste -> {
                if (numeroExiste) {
                    listener.onFailure("Ce numéro est déjà utilisé");
                    return;
                }

                // Créer un Map avec les données sans l'ID
                Map<String, Object> userData = new HashMap<>();
                userData.put("nom", utilisateur.getNom());
                userData.put("numero", utilisateur.getNumero());
                userData.put("email", utilisateur.getEmail());
                userData.put("motdepasse", utilisateur.getMotdepasse());
                userData.put("typeUtilisateur", utilisateur.getTypeUtilisateur());
                userData.put("dateCreation", FieldValue.serverTimestamp());

                db.collection(COLLECTION_UTILISATEURS)
                        .add(userData) // Utiliser le Map au lieu de l'objet Utilisateur
                        .addOnSuccessListener(documentReference -> {
                            String generatedId = documentReference.getId();
                            utilisateur.setId(generatedId);
                            Log.d(TAG, "Utilisateur ajouté avec ID: " + generatedId);
                            listener.onSuccess(utilisateur);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Erreur ajout utilisateur: " + e.getMessage());
                            listener.onFailure(e.getMessage());
                        });
            });
        });
    }
    // Connexion d’un utilisateur
    public void verifierConnexion(String email, String motdepasse, OnLoginListener listener) {
        db.collection(COLLECTION_UTILISATEURS)
                .whereEqualTo("email", email)
                .whereEqualTo("motdepasse", motdepasse)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);

                        // Créer l'utilisateur à partir des données du document
                        Utilisateur utilisateur = new Utilisateur(
                                document.getString("nom"),
                                document.getString("numero"),
                                document.getString("email"),
                                document.getString("motdepasse"),
                                document.getString("typeUtilisateur")
                        );
                        utilisateur.setId(document.getId());

                        // CORRECTION : Retourner l'ID comme String et le type
                        String userId = utilisateur.getId();
                        int type = utilisateur.getTypeUtilisateur().equals("admin") ? 1 : -1;

                        listener.onSuccess(userId, type);
                    } else {
                        listener.onFailure("Email ou mot de passe incorrect");
                    }
                });
    }

    // ========== STADES ==========
    // Ajouter un stade :
    public void ajouterStade(Stade stade, OnStadeOperationListener listener) {
        // Créer un Map avec les données sans l'ID
        Map<String, Object> stadeData = new HashMap<>();
        stadeData.put("nomStade", stade.getNomStade());
        stadeData.put("description", stade.getDescription());
        stadeData.put("capacite", stade.getCapacite());
        stadeData.put("prixHeure", stade.getPrixHeure());
        stadeData.put("latitude", stade.getLatitude());
        stadeData.put("longitude", stade.getLongitude());
        stadeData.put("adresse", stade.getAdresse());
        stadeData.put("userId", stade.getUserId());
        stadeData.put("photos", stade.getPhotos());
        stadeData.put("dateCreation", FieldValue.serverTimestamp());

        db.collection(COLLECTION_STADES)
                .add(stadeData) // Utiliser le Map au lieu de l'objet Stade
                .addOnSuccessListener(documentReference -> {
                    String generatedId = documentReference.getId();
                    stade.setId(generatedId);
                    Log.d(TAG, "Stade ajouté avec ID: " + generatedId);
                    listener.onSuccess(stade);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur ajout stade: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    // NOUVELLE MÉTHODE : Upload des photos vers Firebase Storage
    // Uploader des photos vers Firebase Storage
// ✅ REMPLACE la méthode uploadPhotosAndAddStade dans FireBaseManager.java
// Note : Ajoute "Context context" comme dernier paramètre

    // REMPLACE uploadPhotosAndAddStade dans FireBaseManager.java

// REMPLACE uploadPhotosAndAddStade dans FireBaseManager.java
// Cette version convertit l'image en byte[] avant l'upload

// REMPLACE uploadPhotosAndAddStade dans FireBaseManager.java

    public void uploadPhotosAndAddStade(Stade stade, List<Uri> photoUris,
                                        OnStadeOperationListener listener,
                                        Context context) {
        if (photoUris == null || photoUris.isEmpty()) {
            Log.d(TAG, "⚠️ Aucune photo à uploader");
            ajouterStade(stade, listener);
            return;
        }

        Log.d(TAG, "🔄 Upload de " + photoUris.size() + " photos via ImgBB");

        // Utiliser ImgBB au lieu de Firebase Storage
        ImgBBUploader uploader = new ImgBBUploader(context);

        uploader.uploadImages(photoUris, new ImgBBUploader.OnUploadListener() {
            @Override
            public void onSuccess(List<String> imageUrls) {
                Log.d(TAG, "✅ " + imageUrls.size() + " photos uploadées sur ImgBB");

                for (int i = 0; i < imageUrls.size(); i++) {
                    Log.d(TAG, "   [" + i + "] " + imageUrls.get(i));
                }

                // Sauvegarder le stade avec les URLs ImgBB
                stade.setPhotos(imageUrls);
                ajouterStade(stade, listener);

                // Nettoyer
                uploader.shutdown();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "❌ Erreur upload ImgBB: " + errorMessage);
                listener.onFailure(errorMessage);
                uploader.shutdown();
            }
        });
    }

    // Méthode helper
    private void checkUploadFinished(Stade stade, List<String> uploadedUrls,
                                     int uploadCount, int successCount,
                                     int totalPhotos, OnStadeOperationListener listener) {
        if (uploadCount == totalPhotos) {
            Log.d(TAG, "🏁 Terminé: " + successCount + "/" + totalPhotos + " réussies");

            if (uploadedUrls.isEmpty()) {
                Log.e(TAG, "❌ Aucune photo uploadée avec succès");
                listener.onFailure("Aucune photo n'a pu être uploadée");
                return;
            }

            Log.d(TAG, "✅ Sauvegarde du stade avec " + uploadedUrls.size() + " photos");
            stade.setPhotos(uploadedUrls);
            ajouterStade(stade, listener);
        }
    }

    // Méthode helper
    private void checkIfFinished(Stade stade, List<String> uploadedUrls,
                                 int uploadCount, int successCount,
                                 int totalPhotos, OnStadeOperationListener listener) {
        if (uploadCount == totalPhotos) {
            Log.d(TAG, "🏁 Upload terminé: " + successCount + "/" + totalPhotos + " réussies");

            if (uploadedUrls.isEmpty()) {
                Log.e(TAG, "❌ Aucune photo uploadée avec succès");
                listener.onFailure("Aucune photo n'a pu être uploadée");
                return;
            }

            Log.d(TAG, "✅ Sauvegarde du stade avec " + uploadedUrls.size() + " photos");
            stade.setPhotos(uploadedUrls);
            ajouterStade(stade, listener);
        }
    }

    // Méthode helper pour finaliser l'upload
    private void finishUpload(Stade stade, List<String> uploadedUrls,
                              int successCount, int totalPhotos,
                              OnStadeOperationListener listener) {
        Log.d(TAG, "✅ Toutes les photos traitées : " + successCount + "/" + totalPhotos + " réussies");

        if (uploadedUrls.isEmpty()) {
            listener.onFailure("Aucune photo n'a pu être uploadée");
            return;
        }

        stade.setPhotos(uploadedUrls);
        ajouterStade(stade, listener);
    }    // Récupérer les stades d’un utilisateur
    public void getStadesByUserId(String userId, OnStadesLoadedListener listener) {
        db.collection(COLLECTION_STADES)
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Stade> stades = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Stade stade = documentToStade(document);
                            stades.add(stade);
                        }
                        listener.onStadesLoaded(stades);
                    } else {
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }
    // Récupérer tous les stades
    public void getAllStades(OnStadesLoadedListener listener) {
        db.collection(COLLECTION_STADES)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Stade> stades = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Stade stade = documentToStade(document);
                            stades.add(stade);
                        }
                        listener.onStadesLoaded(stades);
                    } else {
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }

    // ========== MÉTHODES PRIVÉES ==========
    // // Vérifier email / numéro déjà existants +
    private void verifierEmailExiste(String email, OnCheckListener listener) {
        db.collection(COLLECTION_UTILISATEURS)
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    listener.onResult(task.isSuccessful() && !task.getResult().isEmpty());
                });
    }
    // ========== RÉSERVATIONS POUR ADMIN ==========
    public void getReservationsByOwnerId(String ownerId, OnReservationsLoadedListener listener) {
        // D'abord, récupérer tous les stades de cet admin
        db.collection(COLLECTION_STADES)
                .whereEqualTo("userId", ownerId)
                .get()
                .addOnCompleteListener(stadeTask -> {
                    if (stadeTask.isSuccessful() && !stadeTask.getResult().isEmpty()) {
                        List<String> stadeIds = new ArrayList<>();

                        // Collecter tous les IDs de stades
                        for (QueryDocumentSnapshot stadeDoc : stadeTask.getResult()) {
                            stadeIds.add(stadeDoc.getId());
                        }

                        if (stadeIds.isEmpty()) {
                            listener.onReservationsLoaded(new ArrayList<>());
                            return;
                        }

                        // Maintenant, récupérer toutes les réservations pour ces stades
                        db.collection(COLLECTION_RESERVATIONS)
                                .whereIn("stadeId", stadeIds)
                                .orderBy("dateReservation", Query.Direction.ASCENDING)
                                .get()
                                .addOnCompleteListener(reservationTask -> {
                                    if (reservationTask.isSuccessful()) {
                                        List<Reservation> reservations = new ArrayList<>();
                                        for (QueryDocumentSnapshot document : reservationTask.getResult()) {
                                            reservations.add(documentToReservation(document));
                                        }
                                        listener.onReservationsLoaded(reservations);
                                    } else {
                                        listener.onFailure(reservationTask.getException().getMessage());
                                    }
                                });
                    } else {
                        // Aucun stade trouvé pour cet admin
                        listener.onReservationsLoaded(new ArrayList<>());
                    }
                });
    }

    private void verifierNumeroExiste(String numero, OnCheckListener listener) {
        db.collection(COLLECTION_UTILISATEURS)
                .whereEqualTo("numero", numero)
                .get()
                .addOnCompleteListener(task -> {
                    listener.onResult(task.isSuccessful() && !task.getResult().isEmpty());
                });
    }

    // Méthode utilitaire pour convertir un document en objet Stade
    private Stade documentToStade(QueryDocumentSnapshot document) {
        Stade stade = new Stade();
        stade.setId(document.getId());
        stade.setNomStade(document.getString("nomStade"));
        stade.setDescription(document.getString("description"));

        // Capacité
        if (document.getLong("capacite") != null) {
            stade.setCapacite(document.getLong("capacite").intValue());
        }

        // Prix
        if (document.getDouble("prixHeure") != null) {
            stade.setPrixHeure(document.getDouble("prixHeure"));
        }

        // Coordonnées
        if (document.getDouble("latitude") != null) {
            stade.setLatitude(document.getDouble("latitude"));
        }
        if (document.getDouble("longitude") != null) {
            stade.setLongitude(document.getDouble("longitude"));
        }

        stade.setAdresse(document.getString("adresse"));
        stade.setUserId(document.getString("userId"));

        // Créneaux
        if (document.contains("creneauxDisponibles")) {
            List<String> creneaux = (List<String>) document.get("creneauxDisponibles");
            if (creneaux != null) {
                stade.setCreneauxDisponibles(creneaux);
                Log.d(TAG, "Créneaux chargés: " + creneaux.size());
            }
        }

        // Jours d'ouverture
        if (document.contains("joursOuverture")) {
            List<String> jours = (List<String>) document.get("joursOuverture");
            if (jours != null) {
                stade.setJoursOuverture(jours);
            }
        }

        // Heures
        if (document.contains("heureOuverture")) {
            stade.setHeureOuverture(document.getString("heureOuverture"));
        }
        if (document.contains("heureFermeture")) {
            stade.setHeureFermeture(document.getString("heureFermeture"));
        }

        // Durée créneau
        if (document.contains("dureeCreneau") && document.getLong("dureeCreneau") != null) {
            stade.setDureeCreneau(document.getLong("dureeCreneau").intValue());
        }

        // ✅ PHOTOS - Gestion améliorée
        if (document.contains("photos")) {
            try {
                List<String> photos = (List<String>) document.get("photos");
                if (photos != null && !photos.isEmpty()) {
                    // Filtrer les URLs vides
                    List<String> photosValides = new ArrayList<>();
                    for (String photo : photos) {
                        if (photo != null && !photo.trim().isEmpty()) {
                            photosValides.add(photo);
                            Log.d(TAG, "Photo URL: " + photo);
                        }
                    }
                    stade.setPhotos(photosValides);
                    Log.d(TAG, "✅ Photos chargées pour stade " + stade.getNomStade() + ": " + photosValides.size());
                } else {
                    Log.d(TAG, "⚠️ Aucune photo pour stade " + stade.getNomStade());
                    stade.setPhotos(new ArrayList<>());
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Erreur chargement photos: " + e.getMessage());
                stade.setPhotos(new ArrayList<>());
            }
        } else {
            Log.d(TAG, "⚠️ Champ 'photos' absent pour stade " + stade.getNomStade());
            stade.setPhotos(new ArrayList<>());
        }

        return stade;
    }

    public void creerReservation(Reservation reservation, OnReservationOperationListener listener) {
        // Vérifier si le créneau est disponible
          verifierCreneauDisponible(reservation.getStadeId(), reservation.getCreneau(),
                reservation.getDateReservation(), disponible -> {
                    if (!disponible) {
                        listener.onFailure("Ce créneau n'est plus disponible");
                        return;
                    }

                    Map<String, Object> reservationData = new HashMap<>();
                    reservationData.put("userId", reservation.getUserId());
                    reservationData.put("stadeId", reservation.getStadeId());
                    reservationData.put("stadeName", reservation.getStadeName());
                    reservationData.put("creneau", reservation.getCreneau());
                    reservationData.put("dateReservation", reservation.getDateReservation());
                    reservationData.put("dateCreation", FieldValue.serverTimestamp());
                    reservationData.put("statut", reservation.getStatut());
                    reservationData.put("prixTotal", reservation.getPrixTotal());
                    reservationData.put("nombreJoueurs", reservation.getNombreJoueurs());
                    reservationData.put("notes", reservation.getNotes());

                    db.collection(COLLECTION_RESERVATIONS)
                            .add(reservationData)
                            .addOnSuccessListener(documentReference -> {
                                String generatedId = documentReference.getId();
                                reservation.setId(generatedId);
                                Log.d(TAG, "Réservation créée avec ID: " + generatedId);

                                // Programmer une notification de rappel
                                programmerRappelReservation(reservation);

                                listener.onSuccess(reservation);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Erreur création réservation: " + e.getMessage());
                                listener.onFailure(e.getMessage());
                            });
                });
    }

    public void verifierCreneauDisponible(String stadeId, String creneau, Date date, OnCheckListener listener) {
        // Convertir la date en format string pour la requête
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = sdf.format(date);

        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("stadeId", stadeId)
                .whereEqualTo("creneau", creneau)
                .whereEqualTo("dateReservation", dateStr)
                .whereEqualTo("statut", "confirmée")
                .get()
                .addOnCompleteListener(task -> {
                    boolean disponible = !task.isSuccessful() || task.getResult().isEmpty();
                    listener.onResult(disponible);
                });
    }

    public void getReservationsUtilisateur(String userId, OnReservationsLoadedListener listener) {
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("userId", userId)
                .orderBy("dateReservation", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Reservation> reservations = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            reservations.add(documentToReservation(document));
                        }
                        listener.onReservationsLoaded(reservations);
                    } else {
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }

    public void annulerReservation(String reservationId, OnOperationListener listener) {
        db.collection(COLLECTION_RESERVATIONS)
                .document(reservationId)
                .update("statut", "annulée")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Réservation annulée: " + reservationId);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur annulation: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    private Reservation documentToReservation(QueryDocumentSnapshot document) {
        Reservation reservation = new Reservation();
        reservation.setId(document.getId());
        reservation.setUserId(document.getString("userId"));
        reservation.setStadeId(document.getString("stadeId"));
        reservation.setStadeName(document.getString("stadeName"));
        reservation.setCreneau(document.getString("creneau"));

        // Gestion sécurisée des dates
        if (document.getDate("dateReservation") != null) {
            reservation.setDateReservation(document.getDate("dateReservation"));
        }
        if (document.getDate("dateCreation") != null) {
            reservation.setDateCreation(document.getDate("dateCreation"));
        }

        reservation.setStatut(document.getString("statut"));

        // Gestion sécurisée des nombres
        if (document.getDouble("prixTotal") != null) {
            reservation.setPrixTotal(document.getDouble("prixTotal"));
        }
        if (document.getLong("nombreJoueurs") != null) {
            reservation.setNombreJoueurs(document.getLong("nombreJoueurs").intValue());
        }

        reservation.setNotes(document.getString("notes"));
        return reservation;
    }

    private void programmerRappelReservation(Reservation reservation) {
        // Implémentation basique - à compléter avec le service de notifications
        Log.d(TAG, "Rappel programmé pour réservation: " + reservation.getId());
    }


    // ========== AVIS ==========

    public void ajouterAvis(Avis avis, OnAvisOperationListener listener) {
        Map<String, Object> avisData = new HashMap<>();
        avisData.put("userId", avis.getUserId());
        avisData.put("userNom", avis.getUserNom());
        avisData.put("stadeId", avis.getStadeId());
        avisData.put("stadeName", avis.getStadeName());
        avisData.put("reservationId", avis.getReservationId());
        avisData.put("note", avis.getNote());
        avisData.put("commentaire", avis.getCommentaire());
        avisData.put("dateCreation", FieldValue.serverTimestamp());
        avisData.put("verifie", avis.isVerifie());

        db.collection(COLLECTION_AVIS)
                .add(avisData)
                .addOnSuccessListener(documentReference -> {
                    String generatedId = documentReference.getId();
                    avis.setId(generatedId);

                    // Mettre à jour la note moyenne du stade
                    mettreAJourNoteMoyenneStade(avis.getStadeId());

                    listener.onSuccess(avis);
                })
                .addOnFailureListener(e -> {
                    listener.onFailure(e.getMessage());
                });
    }

    public void getAvisParStade(String stadeId, OnAvisLoadedListener listener) {
        db.collection(COLLECTION_AVIS)
                .whereEqualTo("stadeId", stadeId)
                .whereEqualTo("verifie", true)
                .orderBy("dateCreation", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Avis> avisList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            avisList.add(documentToAvis(document));
                        }
                        listener.onAvisLoaded(avisList);
                    } else {
                        listener.onFailure(task.getException().getMessage());
                    }
                });
    }

    private void mettreAJourNoteMoyenneStade(String stadeId) {
        db.collection(COLLECTION_AVIS)
                .whereEqualTo("stadeId", stadeId)
                .whereEqualTo("verifie", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        double sommeNotes = 0;
                        int nombreAvis = task.getResult().size();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            sommeNotes += document.getLong("note");
                        }

                        double noteMoyenne = sommeNotes / nombreAvis;

                        // Mettre à jour le stade
                        db.collection(COLLECTION_STADES)
                                .document(stadeId)
                                .update("noteMoyenne", noteMoyenne, "nombreAvis", nombreAvis);
                    }
                });
    }

    private Avis documentToAvis(QueryDocumentSnapshot document) {
        Avis avis = new Avis();
        avis.setId(document.getId());
        avis.setUserId(document.getString("userId"));
        avis.setUserNom(document.getString("userNom"));
        avis.setStadeId(document.getString("stadeId"));
        avis.setStadeName(document.getString("stadeName"));
        avis.setReservationId(document.getString("reservationId"));

        if (document.getLong("note") != null) {
            avis.setNote(document.getLong("note").intValue());
        }

        avis.setCommentaire(document.getString("commentaire"));

        if (document.getDate("dateCreation") != null) {
            avis.setDateCreation(document.getDate("dateCreation"));
        }

        if (document.getBoolean("verifie") != null) {
            avis.setVerifie(document.getBoolean("verifie"));
        }

        return avis;
    }
    public void getUtilisateur(String userId, OnUserLoadListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onFailure("ID utilisateur manquant");
            return;
        }

        db.collection(COLLECTION_UTILISATEURS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Utilisateur utilisateur = new Utilisateur(
                                documentSnapshot.getString("nom"),
                                documentSnapshot.getString("numero"),
                                documentSnapshot.getString("email"),
                                documentSnapshot.getString("motdepasse"),
                                documentSnapshot.getString("typeUtilisateur")
                        );
                        utilisateur.setId(documentSnapshot.getId());

                        Log.d(TAG, "Utilisateur chargé: " + utilisateur.getNom());
                        listener.onUserLoaded(utilisateur);
                    } else {
                        listener.onFailure("Utilisateur non trouvé");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur chargement utilisateur: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    // Mettre à jour un utilisateur
    public void updateUtilisateur(Utilisateur utilisateur, OnUserOperationListener listener) {
        if (utilisateur.getId() == null || utilisateur.getId().isEmpty()) {
            listener.onFailure("ID utilisateur manquant");
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("nom", utilisateur.getNom());
        userData.put("numero", utilisateur.getNumero());
        userData.put("email", utilisateur.getEmail());
        userData.put("motdepasse", utilisateur.getMotdepasse());
        userData.put("typeUtilisateur", utilisateur.getTypeUtilisateur());

        db.collection(COLLECTION_UTILISATEURS)
                .document(utilisateur.getId())
                .update(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Utilisateur mis à jour: " + utilisateur.getId());
                    listener.onSuccess(utilisateur);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur mise à jour utilisateur: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    // Supprimer un utilisateur et toutes ses données
    public void supprimerUtilisateur(String userId, OnDeleteListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onFailure("ID utilisateur manquant");
            return;
        }

        // Étape 1: Supprimer toutes les réservations de l'utilisateur
        db.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(reservationsSnapshot -> {
                    // Créer un batch pour supprimer toutes les réservations
                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot doc : reservationsSnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    // Exécuter le batch de suppression des réservations
                    batch.commit()
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d(TAG, "Réservations supprimées: " + reservationsSnapshot.size());

                                // Étape 2: Supprimer tous les avis de l'utilisateur
                                db.collection(COLLECTION_AVIS)
                                        .whereEqualTo("userId", userId)
                                        .get()
                                        .addOnSuccessListener(avisSnapshot -> {
                                            WriteBatch avisBatch = db.batch();

                                            for (DocumentSnapshot doc : avisSnapshot.getDocuments()) {
                                                avisBatch.delete(doc.getReference());
                                            }

                                            avisBatch.commit()
                                                    .addOnSuccessListener(aVoid2 -> {
                                                        Log.d(TAG, "Avis supprimés: " + avisSnapshot.size());

                                                        // Étape 3: Si c'est un admin, supprimer ses stades
                                                        db.collection(COLLECTION_STADES)
                                                                .whereEqualTo("userId", userId)
                                                                .get()
                                                                .addOnSuccessListener(stadesSnapshot -> {
                                                                    WriteBatch stadesBatch = db.batch();

                                                                    for (DocumentSnapshot doc : stadesSnapshot.getDocuments()) {
                                                                        stadesBatch.delete(doc.getReference());
                                                                    }

                                                                    stadesBatch.commit()
                                                                            .addOnSuccessListener(aVoid3 -> {
                                                                                Log.d(TAG, "Stades supprimés: " + stadesSnapshot.size());

                                                                                // Étape 4: Supprimer l'utilisateur
                                                                                db.collection(COLLECTION_UTILISATEURS)
                                                                                        .document(userId)
                                                                                        .delete()
                                                                                        .addOnSuccessListener(aVoid4 -> {
                                                                                            Log.d(TAG, "Utilisateur supprimé: " + userId);
                                                                                            listener.onSuccess();
                                                                                        })
                                                                                        .addOnFailureListener(e -> {
                                                                                            Log.e(TAG, "Erreur suppression utilisateur: " + e.getMessage());
                                                                                            listener.onFailure(e.getMessage());
                                                                                        });
                                                                            })
                                                                            .addOnFailureListener(e -> {
                                                                                listener.onFailure("Erreur suppression stades: " + e.getMessage());
                                                                            });
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    listener.onFailure("Erreur récupération stades: " + e.getMessage());
                                                                });
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        listener.onFailure("Erreur suppression avis: " + e.getMessage());
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            listener.onFailure("Erreur récupération avis: " + e.getMessage());
                                        });
                            })
                            .addOnFailureListener(e -> {
                                listener.onFailure("Erreur suppression réservations: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    listener.onFailure("Erreur récupération réservations: " + e.getMessage());
                });
    }

    // Update a stade
    public void updateStade(String stadeId, Stade stade, OnStadeOperationListener listener) {
        Map<String, Object> stadeData = new HashMap<>();
        stadeData.put("nomStade", stade.getNomStade());
        stadeData.put("description", stade.getDescription());
        stadeData.put("capacite", stade.getCapacite());
        stadeData.put("prixHeure", stade.getPrixHeure());
        stadeData.put("latitude", stade.getLatitude());
        stadeData.put("longitude", stade.getLongitude());
        stadeData.put("adresse", stade.getAdresse());
        stadeData.put("userId", stade.getUserId());
        stadeData.put("photos", stade.getPhotos());
        stadeData.put("dateModification", FieldValue.serverTimestamp());

        db.collection(COLLECTION_STADES)
                .document(stadeId)
                .update(stadeData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Stade mis à jour avec ID: " + stadeId);
                    stade.setId(stadeId);
                    listener.onSuccess(stade);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur mise à jour stade: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    // Delete a stade
    public void deleteStade(String stadeId, OnOperationListener listener) {
        db.collection(COLLECTION_STADES)
                .document(stadeId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Stade supprimé avec ID: " + stadeId);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur suppression stade: " + e.getMessage());
                    listener.onFailure(e.getMessage());
                });
    }

    // Update stade with photos (if you want to allow photo updates)
    public void updateStadeWithPhotos(String stadeId, Stade stade, List<Uri> photoUris, OnStadeOperationListener listener) {
        if (photoUris == null || photoUris.isEmpty()) {
            // Pas de nouvelles photos, mettre à jour directement
            updateStade(stadeId, stade, listener);
            return;
        }

        List<String> uploadedUrls = new ArrayList<>();
        int[] uploadCount = {0};

        for (Uri photoUri : photoUris) {
            String fileName = "stades/" + stade.getUserId() + "/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference photoRef = storage.getReference().child(fileName);

            photoRef.putFile(photoUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            uploadedUrls.add(uri.toString());
                            uploadCount[0]++;

                            Log.d(TAG, "Photo uploadée: " + uploadCount[0] + "/" + photoUris.size());

                            // Si toutes les photos sont uploadées
                            if (uploadCount[0] == photoUris.size()) {
                                // Combine existing photos with new ones
                                List<String> allPhotos = stade.getPhotos();
                                if (allPhotos == null) {
                                    allPhotos = new ArrayList<>();
                                }
                                allPhotos.addAll(uploadedUrls);
                                stade.setPhotos(allPhotos);

                                updateStade(stadeId, stade, listener);
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erreur upload photo: " + e.getMessage());
                        listener.onFailure("Erreur lors de l'upload des photos: " + e.getMessage());
                    });
        }
    }
    public void updateReservationStatus(String reservationId, String newStatus, OnStatusUpdateListener listener) {
        if (reservationId == null || reservationId.isEmpty()) {
            listener.onFailure("ID de réservation invalide");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("statut", newStatus);

        db.collection("reservations")
                .document(reservationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    listener.onStatusUpdated();
                })
                .addOnFailureListener(e -> {
                    listener.onFailure(e.getMessage());
                });
    }

    // ========== INTERFACES ==========
    public interface OnUserOperationListener {
        void onSuccess(Utilisateur utilisateur);
        void onFailure(String errorMessage);
    }

    public interface OnLoginListener {
        void onSuccess(String userId, int userType);
        void onFailure(String errorMessage);
    }

    public interface OnStadeOperationListener {
        void onSuccess(Stade stade);
        void onFailure(String errorMessage);
    }

    public interface OnStadesLoadedListener {
        void onStadesLoaded(List<Stade> stades);
        void onFailure(String errorMessage);
    }

    public interface OnCheckListener {
        void onResult(boolean existe);
    }
    // AJOUTER CES INTERFACES À LA FIN, AVEC LES AUTRE

    public interface OnReservationOperationListener {
        void onSuccess(Reservation reservation);
        void onFailure(String errorMessage);
    }

    public interface OnReservationsLoadedListener {
        void onReservationsLoaded(List<Reservation> reservations);
        void onFailure(String errorMessage);
    }

    public interface OnOperationListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface OnAvisOperationListener {
        void onSuccess(Avis avis);
        void onFailure(String errorMessage);
    }

    public interface OnAvisLoadedListener {
        void onAvisLoaded(List<Avis> avis);
        void onFailure(String errorMessage);
    }
    public interface OnStatusUpdateListener {
        void onStatusUpdated();
        void onFailure(String errorMessage);
    }
    public interface OnUserLoadListener {
        void onUserLoaded(Utilisateur utilisateur);
        void onFailure(String errorMessage);
    }

    public interface OnDeleteListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }
    public void diagnosticFirebase(Context context) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "🔍 DIAGNOSTIC FIREBASE");
        Log.d(TAG, "========================================");

        try {
            // Test 1: Vérifier FirebaseStorage
            FirebaseStorage storageInstance = FirebaseStorage.getInstance();
            Log.d(TAG, "✅ FirebaseStorage instance OK");

            // Test 2: Vérifier le bucket
            String bucket = storageInstance.getReference().getBucket();
            Log.d(TAG, "📁 Bucket: " + bucket);

            if (bucket == null || bucket.isEmpty()) {
                Log.e(TAG, "❌ PROBLÈME: Bucket est null ou vide!");
                Log.e(TAG, "   Solution: Vérifie google-services.json");
                return;
            }

            // Test 3: Vérifier l'URL de stockage
            String storageUrl = storageInstance.getReference().toString();
            Log.d(TAG, "🔗 Storage URL: " + storageUrl);

            // Test 4: Essayer de créer une référence
            String testPath = "test_diagnostic_" + System.currentTimeMillis() + ".txt";
            StorageReference testRef = storageInstance.getReference().child(testPath);
            Log.d(TAG, "📝 Référence test créée: " + testRef.getPath());
            Log.d(TAG, "🌐 URL complète: " + testRef.toString());

            // Test 5: Upload de test
            byte[] testData = "Test Firebase Storage".getBytes();
            Log.d(TAG, "⬆️ Tentative d'upload de test...");

            testRef.putBytes(testData)
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "========================================");
                        Log.d(TAG, "🎉 SUCCÈS! Firebase Storage fonctionne!");
                        Log.d(TAG, "========================================");

                        testRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            Log.d(TAG, "✅ URL téléchargeable: " + uri.toString());
                            Log.d(TAG, "   Le problème vient donc de la lecture du fichier local");

                            // Nettoyer le fichier de test
                            testRef.delete();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "========================================");
                        Log.e(TAG, "❌ ÉCHEC! Firebase Storage ne fonctionne pas!");
                        Log.e(TAG, "========================================");
                        Log.e(TAG, "Erreur: " + e.getMessage());
                        Log.e(TAG, "Type: " + e.getClass().getName());

                        if (e.getMessage() != null) {
                            if (e.getMessage().contains("permission")) {
                                Log.e(TAG, "💡 Problème de permissions Storage");
                                Log.e(TAG, "   → Va dans Firebase Console > Storage > Rules");
                                Log.e(TAG, "   → Change en: allow write: if true;");
                            } else if (e.getMessage().contains("not found") || e.getMessage().contains("does not exist")) {
                                Log.e(TAG, "💡 Storage pas activé ou bucket invalide");
                                Log.e(TAG, "   → Va dans Firebase Console > Storage");
                                Log.e(TAG, "   → Clique sur 'Get Started' si nécessaire");
                            } else if (e.getMessage().contains("network")) {
                                Log.e(TAG, "💡 Problème de connexion internet");
                            }
                        }

                        e.printStackTrace();
                    });

        } catch (Exception e) {
            Log.e(TAG, "========================================");
            Log.e(TAG, "❌ EXCEPTION lors du diagnostic!");
            Log.e(TAG, "========================================");
            Log.e(TAG, "Erreur: " + e.getMessage());
            e.printStackTrace();

            if (e.getMessage() != null && e.getMessage().contains("FirebaseApp")) {
                Log.e(TAG, "💡 Firebase pas initialisé correctement");
                Log.e(TAG, "   → Vérifie que google-services.json est présent");
                Log.e(TAG, "   → Vérifie build.gradle (app)");
            }
        }
    }
}