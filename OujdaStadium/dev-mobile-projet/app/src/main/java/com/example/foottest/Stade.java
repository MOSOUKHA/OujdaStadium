package com.example.foottest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Stade implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String userId;
    private String nomStade;
    private double latitude;
    private double longitude;
    private String adresse;
    private String description;
    private int capacite;
    private double prixHeure;
    private List<String> creneauxDisponibles; // Ex: ["10:00-11:00", "11:00-12:00"]
    private List<String> joursOuverture; // Ex: ["LUNDI", "MARDI", "MERCREDI"]
    private String heureOuverture; // Ex: "08:00"
    private String heureFermeture; // Ex: "22:00"
    private int dureeCreneau; // Ex: 60 (minutes)
    private List<String> photos;

    public Stade() {
        photos = new ArrayList<>();
    }

    public Stade(String userId, String nomStade, double latitude, double longitude,
                 String adresse, String description, int capacite, double prixHeure) {
        this.userId = userId;
        this.nomStade = nomStade;
        this.latitude = latitude;
        this.longitude = longitude;
        this.adresse = adresse;
        this.description = description;
        this.capacite = capacite;
        this.prixHeure = prixHeure;
        this.photos = new ArrayList<>();
    }

    // Getters et setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getNomStade() { return nomStade; }
    public void setNomStade(String nomStade) { this.nomStade = nomStade; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }
    public double getPrixHeure() { return prixHeure; }
    public void setPrixHeure(double prixHeure) { this.prixHeure = prixHeure; }
    public List<String> getCreneauxDisponibles() { return creneauxDisponibles; }
    public void setCreneauxDisponibles(List<String> creneauxDisponibles) { this.creneauxDisponibles = creneauxDisponibles; }
    public List<String> getJoursOuverture() { return joursOuverture; }
    public void setJoursOuverture(List<String> joursOuverture) { this.joursOuverture = joursOuverture; }
    public String getHeureOuverture() { return heureOuverture; }
    public void setHeureOuverture(String heureOuverture) { this.heureOuverture = heureOuverture; }
    public String getHeureFermeture() { return heureFermeture; }
    public void setHeureFermeture(String heureFermeture) { this.heureFermeture = heureFermeture; }
    public int getDureeCreneau() { return dureeCreneau; }
    public void setDureeCreneau(int dureeCreneau) { this.dureeCreneau = dureeCreneau; }
    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }
    public void addPhoto(String photo) { this.photos.add(photo); }
}