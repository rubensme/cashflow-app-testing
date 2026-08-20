\# CashFlow App



Eine Java-/Spring-Boot-Anwendung zur Verwaltung persönlicher Finanzen mit Konten, Transaktionen, geplanten Ausgaben, Sparzielen und Haushaltsgruppen.



Das Projekt entstand ursprünglich im Rahmen meiner Umschulung zum Fachinformatiker für Anwendungsentwicklung und wurde anschließend gezielt um automatisierte Tests auf mehreren Ebenen erweitert.



\## Technologien



\- Java

\- Spring Boot

\- Maven

\- Thymeleaf

\- SQLite / JDBC

\- JUnit 5

\- Spring MockMvc

\- Selenium WebDriver

\- ChromeDriver / Selenium Manager



\## Projektstruktur



Das Repository besteht aus zwei Modulen:



\### `cashflow-app`



Enthält die zentrale Geschäftslogik sowie:



\- Models

\- DAOs

\- Services

\- SQLite-Datenzugriff

\- Unit Tests



\### `cashflow-web`



Spring-Boot-Webanwendung mit:



\- MVC-Controllern

\- Thymeleaf-Templates

\- HTML/CSS/JavaScript

\- Integrationstests mit MockMvc

\- End-to-End-Tests mit Selenium WebDriver



\## Teststrategie



Das Projekt enthält Tests auf mehreren Ebenen.



\### Unit Tests



JUnit-Tests prüfen unter anderem:



\- Berechnung der nächsten Fälligkeit fester Ausgaben

\- Monats- und Jahreswechsel

\- Grenzfälle bei Datumsberechnungen

\- Cashflow-Prognosen

\- Erkennung bereits bezahlter Ausgaben



Beispiele:



\- `FesteAusgabeTest`

\- `PrognoseServiceTest`



\### Integrationstests



Mit Spring Boot und MockMvc werden Controller und MVC-Verhalten ohne echten Browser getestet.



Beispiele:



\- Laden der Nutzerseite

\- Prüfung des zurückgegebenen Views und Models

\- Validierung unvollständiger Registrierungsdaten



\### End-to-End-Tests



Selenium WebDriver steuert einen echten Chrome-Browser und prüft die Anwendung aus Benutzersicht.



Beispiele:



\- Laden der Login-/Registrierungsseite

\- Eingabe in Formularfelder

\- HTML5-Validierung bei fehlendem Passwort

\- Ablehnung ungültiger Login-Daten und Prüfung der Fehlermeldung



\## Durch Tests gefundene Fehler



Beim Ausbau der Tests wurden unter anderem zwei Fehler in der Prognoselogik entdeckt.



\### Erkennung bereits bezahlter Ausgaben



Ausgaben wurden als positive Beträge gespeichert, Transaktionen dagegen als negative Beträge. Dadurch konnte eine bereits bezahlte Ausgabe fälschlicherweise erneut von der Prognose abgezogen werden.



Die Vergleichslogik wurde korrigiert und anschließend durch einen Regressionstest abgesichert.



\### Berechnung des Datumsabstands



Zur Prüfung, ob eine Transaktion zeitlich nahe an einer Fälligkeit lag, wurde ursprünglich `LocalDate.compareTo()` verwendet. Diese Methode liefert jedoch keine Anzahl von Tagen.



Die Berechnung wurde auf `ChronoUnit.DAYS.between()` umgestellt und mit Grenzwerttests für drei bzw. vier Tage abgesichert.



\## Datenbank



Standardmäßig verwendet die Anwendung eine lokale SQLite-Datenbank:



```text

jdbc:sqlite:cashflow.db

