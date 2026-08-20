package com.cashflow.cashflow_web.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NutzerSeleniumTest {

    private WebDriver driver;

    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
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
}