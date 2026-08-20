package com.cashflow.app.service;

import java.util.HashMap;
import java.util.Map;

import com.cashflow.app.model.Nutzer;

public class AuthService {

    private Map<String, Nutzer> nutzerMap = new HashMap<>();

    // 🔐 Registrierung eines neuen Nutzers
    public boolean registrieren(String email, String passwort, String vorname, String nachname) {
        if (nutzerMap.containsKey(email)) {
            return false; // E-Mail bereits vergeben
        }
        int neueId = nutzerMap.size() + 1; // einfache ID-Generierung
        Nutzer nutzer = new Nutzer(neueId, vorname, nachname, email, passwort);
        nutzerMap.put(email, nutzer);
        return true;
    }

    // 🔐 Login
    public Nutzer login(String email, String passwort) {
        Nutzer nutzer = nutzerMap.get(email);
        if (nutzer != null && nutzer.getPasswort().equals(passwort)) {
            return nutzer;
        }
        return null; // Fehlerhafte Anmeldedaten
    }

    // 🔐 Nutzerkonto löschen
    public boolean kontoLoeschen(String email, String passwort) {
        Nutzer nutzer = nutzerMap.get(email);
        if (nutzer != null && nutzer.getPasswort().equals(passwort)) {
            nutzerMap.remove(email);
            return true;
        }
        return false; // Falsche Zugangsdaten
    }

    // 🔐 Alle Nutzer anzeigen (nur zu Testzwecken)
    public void zeigeAlleNutzer() {
        System.out.println("👥 Aktuelle Nutzer:");
        for (Nutzer n : nutzerMap.values()) {
            System.out.println("- " + n.getVorname() + " " + n.getNachname() + " (" + n.getEmail() + ")");
        }
    }
}
