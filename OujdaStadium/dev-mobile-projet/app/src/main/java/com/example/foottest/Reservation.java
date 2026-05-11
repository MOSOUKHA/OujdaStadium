// Reservation.java
package com.example.foottest;

import java.util.Date;

public class Reservation {
    private String id;
    private String userId;
    private String stadeId;
    private String stadeName;
    private String creneau;
    private Date dateReservation; // Date du match
    private Date dateCreation;
    private String statut;
    private double prixTotal;
    private int nombreJoueurs;
    private String notes;

    public Reservation() {}

    public Reservation(String userId, String stadeId, String stadeName, String creneau,
                       Date dateReservation, double prixTotal, int nombreJoueurs) {
        this.userId = userId;
        this.stadeId = stadeId;
        this.stadeName = stadeName;
        this.creneau = creneau;
        this.dateReservation = dateReservation;
        this.prixTotal = prixTotal;
        this.nombreJoueurs = nombreJoueurs;
        this.statut = "en attente";
        this.dateCreation = new Date();
    }

    // Getters et setters...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStadeId() { return stadeId; }
    public void setStadeId(String stadeId) { this.stadeId = stadeId; }
    public String getStadeName() { return stadeName; }
    public void setStadeName(String stadeName) { this.stadeName = stadeName; }
    public String getCreneau() { return creneau; }
    public void setCreneau(String creneau) { this.creneau = creneau; }
    public Date getDateReservation() { return dateReservation; }
    public void setDateReservation(Date dateReservation) { this.dateReservation = dateReservation; }
    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public double getPrixTotal() { return prixTotal; }
    public void setPrixTotal(double prixTotal) { this.prixTotal = prixTotal; }
    public int getNombreJoueurs() { return nombreJoueurs; }
    public void setNombreJoueurs(int nombreJoueurs) { this.nombreJoueurs = nombreJoueurs; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}