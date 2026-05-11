package com.example.foottest;

import java.util.Date;

public class Utilisateur {
    private String id;
    private String nom;
    private String numero;
    private String email;
    private String motdepasse;
    private String typeUtilisateur;
    private Date dateCreation;

    public Utilisateur() {}

    public Utilisateur(String nom, String numero, String email, String motdepasse, String typeUtilisateur) {
        this.nom = nom;
        this.numero = numero;
        this.email = email;
        this.motdepasse = motdepasse;
        this.typeUtilisateur = typeUtilisateur;
        this.dateCreation = new Date();
    }

    // Getters et setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMotdepasse() { return motdepasse; }
    public void setMotdepasse(String motdepasse) { this.motdepasse = motdepasse; }
    public String getTypeUtilisateur() { return typeUtilisateur; }
    public void setTypeUtilisateur(String typeUtilisateur) { this.typeUtilisateur = typeUtilisateur; }
    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }
}