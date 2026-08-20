package com.cashflow.cashflow_web.controller;

import com.cashflow.app.dao.NutzerDAO;
import com.cashflow.app.model.Nutzer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.sql.SQLException;

@Controller
public class NutzerController {

    private final NutzerDAO nutzerDAO = new NutzerDAO();

    // Exibe a página combinada de login + cadastro
    @GetMapping("/nutzer")
    public String zeigeFormular(Model model, HttpSession session) {
        Nutzer eingeloggterNutzer = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (eingeloggterNutzer != null) {
            model.addAttribute("loggedInMessage", "Du bist eingeloggt als " +
                    eingeloggterNutzer.getVorname() + " " + eingeloggterNutzer.getNachname());
        }
        model.addAttribute("nutzer", new Nutzer());
        return "nutzer-login-und-form";
    }

    // 🔹 Processar cadastro
    @PostMapping("/nutzer")
    public String registriereNutzer(@ModelAttribute Nutzer nutzer, Model model) {
        if (nutzer.getVorname() == null || nutzer.getVorname().trim().isEmpty() ||
            nutzer.getNachname() == null || nutzer.getNachname().trim().isEmpty() ||
            nutzer.getEmail() == null || nutzer.getEmail().trim().isEmpty() ||
            nutzer.getPasswort() == null || nutzer.getPasswort().trim().isEmpty()) {

            model.addAttribute("errorMessage", "Bitte füllen Sie alle Felder aus.");
            return "nutzer-login-und-form";
        }

        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        if (!nutzer.getEmail().matches(emailRegex)) {
            model.addAttribute("errorMessage", "Bitte geben Sie eine gültige E-Mail-Adresse ein.");
            return "nutzer-login-und-form";
        }

        try {
            Nutzer vorhandenerNutzer = nutzerDAO.findByEmail(nutzer.getEmail());
            if (vorhandenerNutzer != null) {
                model.addAttribute("errorMessage", "Diese E-Mail wird bereits verwendet.");
                return "nutzer-login-und-form";
            }

            nutzerDAO.save(nutzer);
            model.addAttribute("successMessage", "Registrierung erfolgreich. Bitte logge dich jetzt ein.");
            model.addAttribute("nutzer", new Nutzer());
            return "nutzer-login-und-form";

        } catch (SQLException e) {
            model.addAttribute("errorMessage", "Fehler beim Zugriff auf die Datenbank.");
            return "nutzer-login-und-form";
        }
    }

    // 🔹 Processar login → redireciona para /home
    @PostMapping("/nutzer/login")
    public String loginNutzer(@RequestParam String email,
                              @RequestParam String passwort,
                              Model model,
                              HttpSession session) {
        try {
            Nutzer nutzer = nutzerDAO.findByEmail(email);
            if (nutzer != null && nutzer.getPasswort().equals(passwort)) {
                session.setAttribute("eingeloggterNutzer", nutzer);
                return "redirect:/home"; // ✅ vai direto para o painel
            } else {
                model.addAttribute("errorMessage", "E-Mail oder Passwort ist ungültig.");
                model.addAttribute("nutzer", new Nutzer());
                return "nutzer-login-und-form";
            }
        } catch (SQLException e) {
            model.addAttribute("errorMessage", "Fehler beim Zugriff auf die Datenbank.");
            model.addAttribute("nutzer", new Nutzer());
            return "nutzer-login-und-form";
        }
    }

    // 🔹 Logout (mantive para voltar à página de login/registro)
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/nutzer";
    }
}
