// Avis.java
package com.example.foottest;

import java.util.Date;

public class Avis {
    private String id;
    private String userId;
    private String userNom;
    private String stadeId;
    private String stadeName;
    private String reservationId;
    private int note;
    private String commentaire;
    private Date dateCreation;
    private boolean verifie;

    public Avis() {}

    public Avis(String userId, String userNom, String stadeId, String stadeName,
                String reservationId, int note, String commentaire) {
        this.userId = userId;
        this.userNom = userNom;
        this.stadeId = stadeId;
        this.stadeName = stadeName;
        this.reservationId = reservationId;
        this.note = note;
        this.commentaire = commentaire;
        this.dateCreation = new Date();
        this.verifie = false;
    }

    // Getters et setters...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserNom() { return userNom; }
    public void setUserNom(String userNom) { this.userNom = userNom; }
    public String getStadeId() { return stadeId; }
    public void setStadeId(String stadeId) { this.stadeId = stadeId; }
    public String getStadeName() { return stadeName; }
    public void setStadeName(String stadeName) { this.stadeName = stadeName; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }
    public boolean isVerifie() { return verifie; }
    public void setVerifie(boolean verifie) { this.verifie = verifie; }
}