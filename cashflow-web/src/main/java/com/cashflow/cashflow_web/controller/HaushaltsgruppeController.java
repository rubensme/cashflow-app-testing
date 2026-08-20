package com.cashflow.cashflow_web.controller;

import com.cashflow.app.dao.BankkontoDAO;
import com.cashflow.app.dao.HaushaltsgruppeDAO;
import com.cashflow.app.dao.NutzerDAO;
import com.cashflow.app.model.Bankkonto;
import com.cashflow.app.model.Haushaltsgruppe;
import com.cashflow.app.model.HaushaltsgruppeMitglied;
import com.cashflow.app.model.Nutzer;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HaushaltsgruppeController {

	private final NutzerDAO nutzerDAO = new NutzerDAO();
	private final HaushaltsgruppeDAO gruppeDAO = new HaushaltsgruppeDAO();
	private final BankkontoDAO bankkontoDAO = new BankkontoDAO();

	// ===================== PÁGINA: Criar / Integrar =====================
	@GetMapping("/haushaltsgruppe")
	public String page(Model model, HttpSession session,
			@RequestParam(value = "accept", required = false) String acceptInviteId) throws SQLException {

		Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
		if (n == null)
			return "redirect:/nutzer";
		final int nutzerId = n.getNutzerId();

		// Mailbox -> convites + infos
		String mailbox = nutzerDAO.getMailbox(nutzerId);
		List<Map<String, String>> invites = parseInvites(mailbox);
		model.addAttribute("invites", invites);

		List<Map<String, String>> infos = parseInfos(mailbox);

		// ---- TOP alerts (uma vez) para GROUP_DELETED ----
		List<String> topAlerts = new ArrayList<>();
		for (Map<String, String> infoMap : infos) {
			if ("GROUP_DELETED".equals(infoMap.get("type"))) {
				String groupName = infoMap.get("groupName");
				String byName = infoMap.get("byName");
				topAlerts.add(byName + " hat die Gruppe \"" + groupName + "\" gelöscht.");
			}
		}
		if (!topAlerts.isEmpty()) {
			model.addAttribute("topAlerts", topAlerts);
			model.addAttribute("groupDeletedTop", topAlerts);
		}

		// ---- LEFT (quem saiu) ----
		Map<Integer, List<Integer>> leftByGroupUser = new HashMap<>();
		Map<Integer, List<String>> leftMsgsByGroup = new HashMap<>();
		for (Map<String, String> infoMap : infos) {
			if ("USER_LEFT".equals(infoMap.get("type"))) {
				int gid = parseIntSafe(infoMap.get("groupId"));
				int byId = parseIntSafe(infoMap.get("byId"));
				String byName = infoMap.get("byName");

				leftByGroupUser.computeIfAbsent(gid, k -> new ArrayList<>());
				if (!leftByGroupUser.get(gid).contains(byId))
					leftByGroupUser.get(gid).add(byId);

				leftMsgsByGroup.computeIfAbsent(gid, k -> new ArrayList<>());
				leftMsgsByGroup.get(gid).add(byName + " ist aus der Gruppe ausgetreten.");
			}
		}

		// ---- JOINED (quem entrou) ----
		Map<Integer, List<Integer>> joinedByGroupUser = new HashMap<>();
		Map<Integer, List<String>> joinedMsgsByGroup = new HashMap<>();
		for (Map<String, String> infoMap : infos) {
			if ("USER_JOINED".equals(infoMap.get("type"))) {
				int gid = parseIntSafe(infoMap.get("groupId"));
				int byId = parseIntSafe(infoMap.get("byId"));
				String byName = infoMap.get("byName");

				joinedByGroupUser.computeIfAbsent(gid, k -> new ArrayList<>());
				if (!joinedByGroupUser.get(gid).contains(byId))
					joinedByGroupUser.get(gid).add(byId);

				joinedMsgsByGroup.computeIfAbsent(gid, k -> new ArrayList<>());
				joinedMsgsByGroup.get(gid).add(byName + " ist der Gruppe beigetreten.");
			}
		}

		model.addAttribute("leftByGroupUser", leftByGroupUser);
		model.addAttribute("leftMsgsByGroup", leftMsgsByGroup);
		model.addAttribute("joinedByGroupUser", joinedByGroupUser);
		model.addAttribute("joinedMsgsByGroup", joinedMsgsByGroup);

		// Carrega SEMPRE os konten (para o bloco de consentimento do convite)
		List<Bankkonto> konten;
		try {
			konten = bankkontoDAO.findByNutzerId(nutzerId);
		} catch (SQLException e) {
			konten = Collections.emptyList();
		}
		model.addAttribute("bankkonten", konten);

		// ===== Minhas listas =====
		List<Haushaltsgruppe> ownedGroups = gruppeDAO.findOwnedBy(nutzerId);
		List<Haushaltsgruppe> memberGroups = gruppeDAO.findMemberOfButNotOwner(nutzerId);

		// (DAO já ordena, ainda assim padronizamos)
		ownedGroups.sort(Comparator.comparingInt(Haushaltsgruppe::getGruppeId).reversed());
		memberGroups.sort(Comparator.comparingInt(Haushaltsgruppe::getGruppeId).reversed());

		Map<Integer, List<HaushaltsgruppeMitglied>> membersByGroup = new HashMap<>();
		Map<Integer, List<String>> pendingEmailsByGroup = new HashMap<>();

		for (Haushaltsgruppe g : concat(ownedGroups, memberGroups)) {
			int gid = g.getGruppeId();

			leftByGroupUser.computeIfAbsent(gid, k -> new ArrayList<>());
			leftMsgsByGroup.computeIfAbsent(gid, k -> new ArrayList<>());
			joinedByGroupUser.computeIfAbsent(gid, k -> new ArrayList<>());
			joinedMsgsByGroup.computeIfAbsent(gid, k -> new ArrayList<>());

			// membros do grupo
			List<HaushaltsgruppeMitglied> members = gruppeDAO.findMembers(gid);
			membersByGroup.put(gid, members);

			// e-mails convidados que ainda não viraram membros (só pro owner no HTML)
			Set<String> memberEmails = members.stream().map(HaushaltsgruppeMitglied::getEmail).filter(Objects::nonNull)
					.map(String::trim).collect(Collectors.toSet());

			List<String> invited = splitEmails(g.getInvitedEmails());
			List<String> leftover = invited.stream().filter(e -> !memberEmails.contains(e))
					.collect(Collectors.toList());
			pendingEmailsByGroup.put(gid, leftover);
		}

		// Mostrar grupos "member" apenas quando o usuário estiver AKTIV naquele grupo
		Map<Integer, Boolean> activeByGroupForMe = new HashMap<>();
		Iterator<Haushaltsgruppe> it = memberGroups.iterator();
		while (it.hasNext()) {
			Haushaltsgruppe g = it.next();
			int gid = g.getGruppeId();
			List<HaushaltsgruppeMitglied> ms = membersByGroup.getOrDefault(gid, Collections.emptyList());
			boolean aktiv = ms.stream().anyMatch(m -> m.getNutzerId() == nutzerId && m.isAktiv());
			activeByGroupForMe.put(gid, aktiv);
			if (!aktiv)
				it.remove(); // não mostrar ainda
		}

		// Disponibiliza para a página (inclui navbar)
		model.addAttribute("ownedGroups", ownedGroups);
		model.addAttribute("memberGroups", memberGroups);
		model.addAttribute("membersByGroup", membersByGroup);
		model.addAttribute("pendingEmailsByGroup", pendingEmailsByGroup);
		model.addAttribute("activeByGroupForMe", activeByGroupForMe);

		// Consumir (remover) as linhas INFO| já exibidas (one-shot)
		if (mailbox != null && mailbox.contains("INFO|")) {
			Set<String> consume = new HashSet<>(Arrays.asList("USER_LEFT", "USER_JOINED", "GROUP_DELETED"));
			String cleaned = removeInfoLines(mailbox, consume);
			if (!Objects.equals(cleaned, mailbox)) {
				nutzerDAO.setMailbox(nutzerId, cleaned);
			}
		}

		return "haushaltsgruppe";
	}

	public String gruppeSeite(@PathVariable("id") int id, Model model, HttpSession session, RedirectAttributes ra)
			throws SQLException {

		Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
		if (n == null)
			return "redirect:/nutzer";
		final int userId = n.getNutzerId();

// Busca o grupo
		Haushaltsgruppe g = gruppeDAO.findById(id);
		if (g == null) {
			ra.addFlashAttribute("hgError", "Gruppe nicht gefunden.");
			return "redirect:/haushaltsgruppe";
		}

// (opcional) Garantir que o usuário tem acesso: owner ou membro ativo
		List<HaushaltsgruppeMitglied> members = gruppeDAO.findMembers(id);
		boolean isOwner = g.getOwnerNutzerId() == userId;
		boolean isMemberAktiv = members.stream().anyMatch(m -> m.getNutzerId() == userId && m.isAktiv());
		if (!isOwner && !isMemberAktiv) {
			ra.addFlashAttribute("hgError", "Kein Zugriff auf diese Gruppe.");
			return "redirect:/haushaltsgruppe";
		}

// Listas para popular o dropdown da navbar (igual ao Home)
		List<Haushaltsgruppe> ownedGroups = gruppeDAO.findOwnedBy(userId);
		List<Haushaltsgruppe> memberGroups = gruppeDAO.findMemberOfButNotOwner(userId);
		ownedGroups.sort(Comparator.comparingInt(Haushaltsgruppe::getGruppeId).reversed());
		memberGroups.sort(Comparator.comparingInt(Haushaltsgruppe::getGruppeId).reversed());

		model.addAttribute("ownedGroups", ownedGroups);
		model.addAttribute("memberGroups", memberGroups);
		model.addAttribute("gruppe", g); // para mostrar o nome

		return "hg_gruppe";
	}

	// ===================== POST: Criar grupo (envia convites)
	// =====================
	@PostMapping("/hg/anlegen")
	public String anlegen(HttpSession session, @RequestParam("name") String name,
			@RequestParam(value = "beschr", required = false) String beschr,
			@RequestParam(value = "emails", required = false) List<String> emails, RedirectAttributes ra)
			throws SQLException {

		Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
		if (n == null)
			return "redirect:/nutzer";

		List<String> cleaned = (emails == null ? List.<String>of() : emails).stream().filter(Objects::nonNull)
				.map(String::trim).filter(s -> !s.isEmpty()).distinct().collect(Collectors.toList());

		if (cleaned.isEmpty()) {
			ra.addFlashAttribute("hgError", "Bitte geben Sie mindestens eine E-Mail für Einladungen ein.");
			ra.addFlashAttribute("prefillName", name);
			ra.addFlashAttribute("prefillBeschr", beschr);
			return "redirect:/haushaltsgruppe";
		}

		String nameTrim = name == null ? "" : name.trim();
		String beschrTrim = beschr == null ? "" : beschr.trim();

		// cria grupo
		Haushaltsgruppe created = gruppeDAO.createGroup(n.getNutzerId(), nameTrim, beschrTrim, cleaned);

		// owner já entra como membro ativo
		gruppeDAO.addMember(created.getGruppeId(), n.getNutzerId(), true);

		// envia convites
		for (String email : cleaned) {
			Nutzer target = nutzerDAO.findByEmail(email);
			if (target != null) {
				String line = makeInviteLine(UUID.randomUUID().toString(), created.getGruppeId(), created.getName(),
						n.getNutzerId(), displayName(n));
				nutzerDAO.appendToMailbox(target.getNutzerId(), line);

				// aparece como "ausstehend" até aceitar
				gruppeDAO.addMember(created.getGruppeId(), target.getNutzerId(), false);
			}
		}

		ra.addFlashAttribute("successHG", "Gruppe \"" + created.getName() + "\" erstellt und Einladungen an "
				+ String.join(", ", cleaned) + " gesendet. Wartet auf Bestätigung.");
		return "redirect:/haushaltsgruppe";
	}

	// ===================== POST: Ignorar convite =====================
	@PostMapping("/hg/einladung/ignorieren")
	public String ignoreInvite(HttpSession session, @RequestParam("inviteId") String inviteId, RedirectAttributes ra)
			throws SQLException {

		Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
		if (n == null)
			return "redirect:/nutzer";

		String mailbox = nutzerDAO.getMailbox(n.getNutzerId());
		String updated = removeInviteLine(mailbox, inviteId);
		nutzerDAO.setMailbox(n.getNutzerId(), updated);

		ra.addFlashAttribute("successHG", "Einladung ignoriert.");
		return "redirect:/haushaltsgruppe";
	}

	// ===================== POST: Aceitar convite =====================
	@PostMapping("/hg/einladung/annahme")
	public String acceptInvite(HttpSession session, @RequestParam("inviteId") String inviteId,
			@RequestParam("groupId") int groupId,
			@RequestParam(value = "share_name", defaultValue = "false") boolean shareName,
			@RequestParam(value = "share_tx", defaultValue = "false") boolean shareTx,
			@RequestParam(value = "share_betrag", defaultValue = "false") boolean shareBetrag,
			@RequestParam(value = "konten", required = false) List<Integer> kontoIds, RedirectAttributes ra)
			throws SQLException {

		Nutzer n = (Nutzer) session.getAttribute("eingeloggterNutzer");
		if (n == null)
			return "redirect:/nutzer";

		Haushaltsgruppe g = gruppeDAO.findById(groupId);
		String groupName = (g != null ? g.getName() : "");

		// remove o convite
		String mailbox = nutzerDAO.getMailbox(n.getNutzerId());
		String updated = removeInviteLine(mailbox, inviteId);
		nutzerDAO.setMailbox(n.getNutzerId(), updated);

		// ativa membership
		gruppeDAO.addMember(groupId, n.getNutzerId(), true);

		// notifica demais membros (INFO|USER_JOINED)
		List<HaushaltsgruppeMitglied> members = gruppeDAO.findMembers(groupId);
		String joinInfo = makeInfoLine("USER_JOINED", groupId, groupName, n.getNutzerId(), displayName(n));
		for (HaushaltsgruppeMitglied m : members) {
			if (m.getNutzerId() == n.getNutzerId())
				continue;
			try {
				nutzerDAO.appendToMailbox(m.getNutzerId(), joinInfo);
			} catch (SQLException ignored) {
			}
		}

		String kontenStr = (kontoIds == null || kontoIds.isEmpty()) ? "keine"
				: kontoIds.stream().map(String::valueOf).collect(Collectors.joining(", "));
		String resumo = "Sie sind der Haushaltsgruppe \"" + groupName + "\" beigetreten. " + "Geteilte Daten: "
				+ (shareName ? "Kontoname, " : "") + (shareTx ? "Transaktionen, " : "")
				+ (shareBetrag ? "Betrag, " : "") + "Konten: " + kontenStr + ".";
		ra.addFlashAttribute("successHG", resumo);

		return "redirect:/haushaltsgruppe";
	}

	// ===================== POST: Owner apaga a grupo =====================
	@PostMapping("/hg/gruppe/loeschen")
	public String deleteGroup(HttpSession session, @RequestParam("groupId") int groupId, RedirectAttributes ra)
			throws SQLException {

		Nutzer owner = (Nutzer) session.getAttribute("eingeloggterNutzer");
		if (owner == null)
			return "redirect:/nutzer";

		Haushaltsgruppe g = gruppeDAO.findById(groupId);
		if (g == null || g.getOwnerNutzerId() != owner.getNutzerId()) {
			ra.addFlashAttribute("hgError", "Gruppe nicht gefunden oder keine Berechtigung.");
			return "redirect:/haushaltsgruppe";
		}

		// Notifica todos os membros (INFO|GROUP_DELETED)
		List<HaushaltsgruppeMitglied> members = gruppeDAO.findMembers(groupId);
		String msg = makeInfoLine("GROUP_DELETED", groupId, g.getName(), owner.getNutzerId(), displayName(owner));
		for (HaushaltsgruppeMitglied m : members) {
			try {
				nutzerDAO.appendToMailbox(m.getNutzerId(), msg);
			} catch (SQLException ignored) {
			}
		}

		// Remove membros e depois o grupo
		gruppeDAO.deleteAllMembers(groupId);
		gruppeDAO.deleteById(groupId);

		;
		return "redirect:/haushaltsgruppe";
	}

	// ===================== POST: Membro sai do grupo =====================
	@PostMapping("/hg/gruppe/austreten")
	public String leaveGroup(HttpSession session, @RequestParam("groupId") int groupId, RedirectAttributes ra)
			throws SQLException {

		Nutzer user = (Nutzer) session.getAttribute("eingeloggterNutzer");
		if (user == null)
			return "redirect:/nutzer";

		Haushaltsgruppe g = gruppeDAO.findById(groupId);
		if (g == null) {
			ra.addFlashAttribute("hgError", "Gruppe nicht gefunden.");
			return "redirect:/haushaltsgruppe";
		}
		if (g.getOwnerNutzerId() == user.getNutzerId()) {
			ra.addFlashAttribute("hgError", "Als Owner können Sie nicht austreten. Löschen Sie die Gruppe.");
			return "redirect:/haushaltsgruppe";
		}

		gruppeDAO.setMemberAktiv(groupId, user.getNutzerId(), false);

		// Notifica restantes (INFO|USER_LEFT)
		List<HaushaltsgruppeMitglied> members = gruppeDAO.findMembers(groupId);
		String msg = makeInfoLine("USER_LEFT", groupId, g.getName(), user.getNutzerId(), displayName(user));
		for (HaushaltsgruppeMitglied m : members) {
			if (m.getNutzerId() == user.getNutzerId())
				continue;
			try {
				nutzerDAO.appendToMailbox(m.getNutzerId(), msg);
			} catch (SQLException ignored) {
			}
		}

		ra.addFlashAttribute("successHG", "Sie haben die Gruppe \"" + g.getName() + "\" verlassen.");
		return "redirect:/haushaltsgruppe";
	}

	// ===================== Helpers =====================
	private static <T> List<T> concat(List<T> a, List<T> b) {
		List<T> all = new ArrayList<>();
		if (a != null)
			all.addAll(a);
		if (b != null)
			all.addAll(b);
		return all;
	}

	private static int parseIntSafe(String s) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return 0;
		}
	}

	private static String displayName(Nutzer u) {
		String vor = u.getVorname() == null ? "" : u.getVorname().trim();
		String nach = u.getNachname() == null ? "" : u.getNachname().trim();
		String full = (vor + " " + nach).trim();
		String email = u.getEmail() == null ? "" : u.getEmail().trim();
		if (!email.isEmpty())
			return (full.isEmpty() ? email : full) + " (" + email + ")";
		return full.isEmpty() ? "Unbekannt" : full;
	}

	private static String makeInviteLine(String id, int groupId, String groupName, int fromId, String fromName) {
		// INVITE|<uuid>|<groupId>|<groupName>|<fromId>|<fromName>
		return "INVITE|" + id + "|" + groupId + "|" + esc(groupName) + "|" + fromId + "|" + esc(fromName);
	}

	private static String makeInfoLine(String type, int groupId, String groupName, int byId, String byName) {
		// INFO|TYPE|groupId|groupName|byId|byName
		return "INFO|" + type + "|" + groupId + "|" + esc(groupName) + "|" + byId + "|" + esc(byName);
	}

	private static String esc(String s) {
		return s == null ? "" : s.replace("\n", " ").replace("|", "/");
	}

	private static List<Map<String, String>> parseInvites(String mailbox) {
		List<Map<String, String>> out = new ArrayList<>();
		if (mailbox == null || mailbox.isBlank())
			return out;
		for (String line : mailbox.split("\\R")) {
			if (!line.startsWith("INVITE|"))
				continue;
			String[] p = line.split("\\|", -1);
			if (p.length < 6)
				continue;
			Map<String, String> m = new HashMap<>();
			m.put("raw", line);
			m.put("id", p[1]);
			m.put("groupId", p[2]);
			m.put("groupName", p[3]);
			m.put("fromId", p[4]);
			m.put("fromName", p[5]);
			out.add(m);
		}
		return out;
	}

	private static List<Map<String, String>> parseInfos(String mailbox) {
		List<Map<String, String>> out = new ArrayList<>();
		if (mailbox == null || mailbox.isBlank())
			return out;
		for (String line : mailbox.split("\\R")) {
			if (!line.startsWith("INFO|"))
				continue;
			// INFO|TYPE|groupId|groupName|byId|byName
			String[] p = line.split("\\|", -1);
			if (p.length < 6)
				continue;
			Map<String, String> m = new HashMap<>();
			m.put("type", p[1]);
			m.put("groupId", p[2]);
			m.put("groupName", p[3]);
			m.put("byId", p[4]);
			m.put("byName", p[5]);
			out.add(m);
		}
		return out;
	}

	private static String removeInviteLine(String mailbox, String inviteId) {
		if (mailbox == null)
			return null;
		StringBuilder sb = new StringBuilder();
		for (String line : mailbox.split("\\R")) {
			if (line.startsWith("INVITE|")) {
				String[] p = line.split("\\|", -1);
				if (p.length >= 2 && p[1].equals(inviteId))
					continue; // remove
			}
			if (!line.isBlank())
				sb.append(line).append("\n");
		}
		return sb.toString();
	}

	/** Remove da mailbox apenas INFO|<types>... (consumo one-shot) */
	private static String removeInfoLines(String mailbox, Set<String> types) {
		if (mailbox == null || mailbox.isBlank())
			return mailbox;
		StringBuilder sb = new StringBuilder();
		for (String line : mailbox.split("\\R")) {
			if (line.startsWith("INFO|")) {
				String[] p = line.split("\\|", -1);
				if (p.length >= 2 && types.contains(p[1])) {
					// consome (não reapresenta)
					continue;
				}
			}
			if (!line.isBlank())
				sb.append(line).append("\n");
		}
		return sb.toString();
	}

	private static List<String> splitEmails(String invitedEmails) {
		if (invitedEmails == null || invitedEmails.isBlank())
			return Collections.emptyList();
		return Arrays.stream(invitedEmails.split("\\R")).map(String::trim).filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
	}
}
