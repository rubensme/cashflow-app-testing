package com.cashflow.cashflow_web.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.chrome.ChromeOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;


import java.util.List;
import java.util.stream.Stream;

@Tag("e2e")
public class NutzerSeleniumTest {

    private WebDriver driver;

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();

        if (System.getenv("CI") != null) {
            options.addArguments(
                    "--headless=new",
                    "--no-sandbox",
                    "--disable-dev-shm-usage"
            );
        }

        driver = new ChromeDriver(options);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void sollteNutzerSeiteImBrowserLaden() {

        driver.get("http://localhost:8080/nutzer");

        assertEquals(
                "Login oder Registrierung",
                driver.getTitle()
        );
    }

    @Test
    public void sollteEmailInLoginFeldEingeben() {

        driver.get("http://localhost:8080/nutzer");

        WebElement emailFeld =
                driver.findElement(By.id("login-email"));

        emailFeld.sendKeys("max@example.com");

        assertEquals(
                "max@example.com",
                emailFeld.getAttribute("value")
        );
    }

    @Test
    public void sollteLoginMitLeeremPasswortNichtAbsenden() {

        driver.get("http://localhost:8080/nutzer");

        WebElement emailFeld =
                driver.findElement(By.id("login-email"));

        WebElement loginButton =
                driver.findElement(By.id("login-button"));

        emailFeld.sendKeys("max@example.com");

        loginButton.click();

        assertEquals(
                "http://localhost:8080/nutzer",
                driver.getCurrentUrl()
        );

        assertEquals(
                "max@example.com",
                emailFeld.getAttribute("value")
        );
    }

    @Test
    public void sollteUngueltigenLoginAblehnen() {

        driver.get("http://localhost:8080/nutzer");

        WebElement emailFeld =
                driver.findElement(By.id("login-email"));

        WebElement passwortFeld =
                driver.findElement(By.id("login-passwort"));

        WebElement loginButton =
                driver.findElement(By.id("login-button"));

        emailFeld.sendKeys("nichtvorhanden@example.com");
        passwortFeld.sendKeys("falschesPasswort");

        loginButton.click();

        WebElement fehlerMeldung =
                driver.findElement(By.cssSelector("p.err"));

        assertEquals(
                "E-Mail oder Passwort ist ungültig.",
                fehlerMeldung.getText()
        );
    }

    static Stream<Arguments> viewports() {
        return Stream.of(
                Arguments.of("Desktop", 1440, 900, true),
                Arguments.of("Tablet", 768, 1024, false),
                Arguments.of("Mobile", 390, 844, false)
        );
    }

    @ParameterizedTest(name = "{0}: {1}x{2}")
    @MethodSource("viewports")
    void sollteFormularLayoutAnViewportAnpassen(
            String geraet,
            int breite,
            int hoehe,
            boolean zweispaltig
    ) {
        driver.manage().window().setSize(
                new Dimension(breite, hoehe)
        );

        driver.get("http://localhost:8080/nutzer");

        List<WebElement> cards = driver.findElements(
                By.cssSelector(".auth-grid .card")
        );

        assertEquals(2, cards.size());

        Rectangle loginCard = cards.get(0).getRect();
        Rectangle registrierungCard = cards.get(1).getRect();

        if (zweispaltig) {
            assertEquals(
                    loginCard.getY(),
                    registrierungCard.getY(),
                    "Die Formulare sollten nebeneinander stehen."
            );

            assertTrue(
                    registrierungCard.getX() > loginCard.getX(),
                    "Die Registrierung sollte rechts vom Login stehen."
            );
        } else {
            assertTrue(
                    registrierungCard.getY()
                            >= loginCard.getY() + loginCard.getHeight(),
                    "Die Registrierung sollte unterhalb des Logins stehen."
            );
        }

        JavascriptExecutor javascriptExecutor =
                (JavascriptExecutor) driver;

        long scrollWidth = ((Number) javascriptExecutor.executeScript(
                "return document.documentElement.scrollWidth;"
        )).longValue();

        long clientWidth = ((Number) javascriptExecutor.executeScript(
                "return document.documentElement.clientWidth;"
        )).longValue();

        assertTrue(
                scrollWidth <= clientWidth,
                "Die Seite sollte keinen horizontalen Overflow haben."
        );

        WebElement loginButton = driver.findElement(
                By.id("login-button")
        );

        assertTrue(loginButton.isDisplayed());
        assertTrue(loginButton.isEnabled());
    }
}