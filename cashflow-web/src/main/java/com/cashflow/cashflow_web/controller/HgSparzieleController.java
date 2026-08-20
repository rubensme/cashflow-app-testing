package com.cashflow.cashflow_web.controller;

import com.cashflow.app.dao.HaushaltsgruppeDAO;
import com.cashflow.app.dao.SparzielDAO;
import com.cashflow.app.model.Haushaltsgruppe;
import com.cashflow.app.model.HaushaltsgruppeMitglied;
import com.cashflow.app.model.Nutzer;
import com.cashflow.app.model.Sparziel;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HgSparzieleController {

    private final HaushaltsgruppeDAO gruppeDAO = new HaushaltsgruppeDAO();
    private final SparzielDAO sparzielDAO = new SparzielDAO();

    /** Página pessoal (usa o mesmo template "sparziele") */
    @GetMapping("/sparziele")
    public String personalSparziele(Model model, HttpSession session) throws SQLException {
        Nutzer u = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (u == null) return "redirect:/nutzer";

        // navbar
        var owned  = gruppeDAO.findOwnedBy(u.getNutzerId());
        var member = gruppeDAO.findMemberOfButNotOwner(u.getNutzerId());
        owned.sort(Comparator.comparingInt(Haushaltsgruppe::getGruppeId).reversed());
        member.sort(Comparator.comparingInt(Haushaltsgruppe::getGruppeId).reversed());
        model.addAttribute("ownedGroups", owned);
        model.addAttribute("memberGroups", member);

        // nome do usuário p/ rótulo
        model.addAttribute("userName", displayName(u));

        // contagem de membros ativos por grupo (seletor)
        Map<Integer,Integer> groupActiveCounts = new HashMap<>();
        for (Haushaltsgruppe g : concat(owned, member)) {
            int gid = g.getGruppeId();
            List<HaushaltsgruppeMitglied> ms = gruppeDAO.findMembers(gid);
            int active = (int) ms.stream().filter(HaushaltsgruppeMitglied::isAktiv).count();
            groupActiveCounts.put(gid, active);
        }
        model.addAttribute("groupActiveCounts", groupActiveCounts);

        // lista existente (pessoal por enquanto)
        List<Sparziel> existing = sparzielDAO.findByNutzerId(u.getNutzerId());
        model.addAttribute("existingSparziele", existing);

        // sem grupo selecionado:
        model.addAttribute("gruppe", null);
        model.addAttribute("activeMemberCount", 1); // default para pessoal
        return "sparziele";
    }

    /** Página de Sparziele para a GRUPPE (mesmo template "sparziele") */
    @GetMapping("/hg/gruppe/{id}/sparziele")
    public String gruppenSparziele(@PathVariable("id") int id,
                                   Model model,
                                   HttpSession session,
                                   RedirectAttributes ra) throws SQLException {

        Nutzer u = (Nutzer) session.getAttribute("eingeloggterNutzer");
        if (u == null) return "redirect:/nutzer";

        Haushaltsgruppe g = gruppeDAO.findById(id);
        if (g == null) {
            ra.addFlashAttribute("hgError", "Gruppe nicht gefunden.");
            return "redirect:/haushaltsgruppe";
        }

        // acesso: owner ou membro ativo
        List<HaushaltsgruppeMitglied> members = gruppeDAO.findMembers(id);
        boolean owner = g.getOwnerNutzerId() == u.getNutzerId();
        boolean aktiv = members.stream().anyMatch(m -> m.getNutzerId() == u.getNutzerId() && m.isAktiv());
        if (!owner && !aktiv) {
            ra.addFlashAttribute("hgError", "Kein Zugriff auf diese Gruppe.");
            return "redirect:/haushaltsgruppe";
        }

        // navbar
        var owned  = gruppeDAO.findOwnedBy(u.getNutzerId());
        var member = gruppeDAO.findMemberOfButNotOwner(u.getNutzerId());
        owned.sort(Comparator.comparingInt(Haushaltsgruppe::getGruppeId).reversed());
        member.sort(Comparator.comparingInt(Haushaltsgruppe::getGruppeId).reversed());
        model.addAttribute("ownedGroups", owned);
        model.addAttribute("memberGroups", member);

        // contagem de membros ATIVOS desta gruppe
        int activeCount = (int) members.stream().filter(HaushaltsgruppeMitglied::isAktiv).count();
        model.addAttribute("gruppe", g);
        model.addAttribute("activeMemberCount", Math.max(activeCount, 0));

        // counts por todos os grupos (para o seletor)
        Map<Integer,Integer> groupActiveCounts = new HashMap<>();
        for (Haushaltsgruppe gx : concat(owned, member)) {
            int gid = gx.getGruppeId();
            List<HaushaltsgruppeMitglied> ms = gruppeDAO.findMembers(gid);
            int active = (int) ms.stream().filter(HaushaltsgruppeMitglied::isAktiv).count();
            groupActiveCounts.put(gid, active);
        }
        model.addAttribute("groupActiveCounts", groupActiveCounts);

        // existente (pessoal do usuário logado, por enquanto)
        List<Sparziel> existing = sparzielDAO.findByNutzerId(u.getNutzerId());
        model.addAttribute("existingSparziele", existing);

        model.addAttribute("userName", displayName(u));
        return "sparziele";
    }

    // helpers
    private static <T> List<T> concat(List<T> a, List<T> b) {
        List<T> all = new ArrayList<>();
        if (a != null) all.addAll(a);
        if (b != null) all.addAll(b);
        return all;
    }

    private static String displayName(Nutzer u) {
        String vor = u.getVorname() == null ? "" : u.getVorname().trim();
        String nach = u.getNachname() == null ? "" : u.getNachname().trim();
        String name = (vor + " " + nach).trim();
        if (name.isEmpty()) name = (u.getEmail() == null ? "" : u.getEmail());
        return name.isEmpty() ? "Nutzer" : name;
    }
}
