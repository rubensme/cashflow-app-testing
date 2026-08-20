package com.cashflow.cashflow_web.controller;

import com.cashflow.app.dao.HaushaltsgruppeDAO;
import com.cashflow.app.model.Haushaltsgruppe;
import com.cashflow.app.model.Nutzer;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@ControllerAdvice
public class NavbarGroupsAdvice {

    private final HaushaltsgruppeDAO gruppeDAO = new HaushaltsgruppeDAO();

    /**
     * Popula a navbar com:
     *  - ownedGroups  : grupos onde o usuário é owner
     *  - memberGroups : grupos onde o usuário é membro ativo e NÃO é owner
     *
     * É executado para toda request que renderiza uma view.
     */
    @ModelAttribute
    public void addNavbarGroups(Model model, HttpSession session) {
        Object obj = session == null ? null : session.getAttribute("eingeloggterNutzer");
        if (!(obj instanceof Nutzer)) {
            model.addAttribute("ownedGroups", Collections.emptyList());
            model.addAttribute("memberGroups", Collections.emptyList());
            return;
        }

        Nutzer user = (Nutzer) obj;
        int nutzerId = user.getNutzerId();

        List<Haushaltsgruppe> owned;
        List<Haushaltsgruppe> member;
        try {
            owned  = gruppeDAO.findOwnedBy(nutzerId);
        } catch (SQLException e) {
            owned = Collections.emptyList();
        }
        try {
            member = gruppeDAO.findMemberOfButNotOwner(nutzerId);
        } catch (SQLException e) {
            member = Collections.emptyList();
        }

        // Ordenação leve (opcional): por nome asc, e id desc como desempate
        Comparator<Haushaltsgruppe> cmp =
                Comparator.comparing(Haushaltsgruppe::getName, Comparator.nullsLast(String::compareToIgnoreCase))
                          .thenComparing(Haushaltsgruppe::getGruppeId, Comparator.reverseOrder());

        owned.sort(cmp);
        member.sort(cmp);

        model.addAttribute("ownedGroups", owned);
        model.addAttribute("memberGroups", member);
    }
}
