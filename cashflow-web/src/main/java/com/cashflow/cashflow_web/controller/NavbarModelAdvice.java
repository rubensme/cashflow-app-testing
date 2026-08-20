// src/main/java/com/cashflow/cashflow_web/controller/NavbarModelAdvice.java
package com.cashflow.cashflow_web.controller;

import com.cashflow.app.dao.HaushaltsgruppeDAO;
import com.cashflow.app.model.Haushaltsgruppe;
import com.cashflow.app.model.Nutzer;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@ControllerAdvice(annotations = Controller.class)
public class NavbarModelAdvice {

    private final HaushaltsgruppeDAO gruppeDAO = new HaushaltsgruppeDAO();

    @ModelAttribute
    public void addNavbarGroups(Model model, HttpSession session) {
        Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (n == null) return;

        try {
            List<Haushaltsgruppe> owned = gruppeDAO.findOwnedBy(n.getNutzerId());
            List<Haushaltsgruppe> member = gruppeDAO.findMemberOfButNotOwner(n.getNutzerId());
            model.addAttribute("ownedGroups", owned);
            model.addAttribute("memberGroups", member);
        } catch (SQLException e) {
            // Em caso de erro, evita quebrar a view
            model.addAttribute("ownedGroups", Collections.emptyList());
            model.addAttribute("memberGroups", Collections.emptyList());
        }
    }
}
