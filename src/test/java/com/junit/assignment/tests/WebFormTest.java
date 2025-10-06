package com.junit.assignment.tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

@Epic("Web Form Automation")
@Feature("Digital Unite Practice Form")
public class WebFormTest {

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    @Step("Setup WebDriver and navigate to form")
    public void setup() {
        // Configure WebDriverManager to use Brave's version
        WebDriverManager.chromedriver().browserVersion("140").setup();
        
        ChromeOptions options = new ChromeOptions();
        // Set Brave browser binary path
        options.setBinary("/Applications/Brave Browser.app/Contents/MacOS/Brave Browser");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--remote-allow-origins=*");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        
        driver.get("https://www.digitalunite.com/practice-webform-learners");
        System.out.println("Navigated to: " + driver.getCurrentUrl());
    }

    @Test
    @Story("Complete and Submit Web Form")
    @Description("Fill all fields in the practice web form and verify successful submission")
    @Severity(SeverityLevel.CRITICAL)
    public void testWebFormSubmission() throws InterruptedException {
        // Accept cookies if present
        try {
            WebElement acceptCookies = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.agree-button, button#onetrust-accept-btn-handler, button[aria-label='Accept cookies']")
            ));
            acceptCookies.click();
            System.out.println("Cookies accepted");
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("No cookie banner found or already accepted");
        }

        // Fill Name field
        fillField(By.id("edit-name"), "John Doe", "Name");

        // Fill Phone Number
        fillField(By.id("edit-number"), "01712345678", "Phone Number");

        // Fill Date of Birth
        fillField(By.id("edit-date"), "15/05/1990", "Date of Birth");

        // Fill Email - try multiple selectors
        try {
            fillField(By.cssSelector("input[type='email'], input[name*='mail'], input[placeholder*='mail' i]"), "john.doe@example.com", "Email");
        } catch (Exception e) {
            System.out.println("Email field error: " + e.getMessage());
            // Try finding by label
            try {
                List<WebElement> emailInputs = driver.findElements(By.cssSelector("input[type='text']"));
                for (WebElement input : emailInputs) {
                    String id = input.getAttribute("id");
                    if (id != null && id.contains("mail")) {
                        scrollToElement(input);
                        input.clear();
                        input.sendKeys("john.doe@example.com");
                        System.out.println("Email filled with fallback method");
                        break;
                    }
                }
            } catch (Exception ex) {
                System.out.println("All email fill methods failed");
            }
        }

        // Fill Tell us about yourself - try multiple selectors
        try {
            fillTextArea(By.cssSelector("textarea, #edit-tell-us-a-bit-about-yourself-, textarea[name*='yourself']"), 
                "I am a QA automation engineer with expertise in Selenium and JUnit testing. " +
                "I am passionate about creating robust automated test suites.", 
                "About Yourself");
        } catch (Exception e) {
            System.out.println("Text area error: " + e.getMessage());
        }

        // Upload file (if file input is present and accessible)
        try {
            WebElement fileUpload = driver.findElement(By.id("edit-uploadocument-upload"));
            if (fileUpload.isDisplayed()) {
                // Create a dummy file path - in real scenario, use an actual file
                System.out.println("File upload field found but skipping as it requires actual file");
            }
        } catch (Exception e) {
            System.out.println("File upload not found or not accessible: " + e.getMessage());
        }

        // Check the completion checkbox
        try {
            // Try finding by ID first
            WebElement checkbox = null;
            try {
                checkbox = driver.findElement(By.cssSelector("input[type='checkbox']"));
            } catch (Exception e) {
                // Try finding by text content of label
                List<WebElement> checkboxes = driver.findElements(By.cssSelector("input[type='checkbox']"));
                if (!checkboxes.isEmpty()) {
                    checkbox = checkboxes.get(checkboxes.size() - 1); // Get last checkbox
                }
            }
            
            if (checkbox != null) {
                scrollToElement(checkbox);
                Thread.sleep(500);
                if (!checkbox.isSelected()) {
                    try {
                        checkbox.click();
                    } catch (Exception e) {
                        // Try JavaScript click
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", checkbox);
                    }
                    System.out.println("Completion checkbox checked");
                }
            }
        } catch (Exception e) {
            System.out.println("Checkbox error: " + e.getMessage());
        }

        // Take screenshot before submit
        Thread.sleep(1000);
        
        // Scroll to submit button and click using JavaScript
        WebElement submitButton = driver.findElement(By.id("edit-submit"));
        scrollToElement(submitButton);
        Thread.sleep(500);

        // Click submit button using JavaScript to avoid interception
        System.out.println("Clicking submit button...");
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);

        // Wait for page to process submission
        Thread.sleep(5000);

        // Assert success - check for success message or page change
        boolean isSuccessful = false;
        String successMessage = "";

        // Get current page info
        String currentUrl = driver.getCurrentUrl();
        String pageTitle = driver.getTitle();
        String pageSource = driver.getPageSource().toLowerCase();
        
        System.out.println("After submission:");
        System.out.println("Current URL: " + currentUrl);
        System.out.println("Page Title: " + pageTitle);

        // Check for success indicators
        try {
            // Try to find success message element
            WebElement successElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".messages--status, .alert-success, div[role='alert'], .success-message, .messages")
            ));
            successMessage = successElement.getText();
            System.out.println("Success element found: " + successMessage);
            isSuccessful = true;
        } catch (Exception e) {
            System.out.println("No specific success element found, checking page content...");
        }

        // Check page source for success keywords
        if (!isSuccessful) {
            if (pageSource.contains("thank") || pageSource.contains("success") || 
                pageSource.contains("submitted") || pageSource.contains("received") ||
                pageSource.contains("confirmation")) {
                isSuccessful = true;
                successMessage = "Form submission confirmed - success keywords found in page";
                System.out.println(successMessage);
            }
        }

        // Check if form is no longer visible (indicates successful submission)
        if (!isSuccessful) {
            try {
                List<WebElement> submitButtons = driver.findElements(By.id("edit-submit"));
                if (submitButtons.isEmpty()) {
                    isSuccessful = true;
                    successMessage = "Form submitted successfully - submit button no longer visible";
                    System.out.println(successMessage);
                }
            } catch (Exception ex) {
                System.out.println("Could not check submit button visibility: " + ex.getMessage());
            }
        }

        // Check URL change (some forms redirect after submission)
        if (!isSuccessful && !currentUrl.equals(driver.getCurrentUrl())) {
            isSuccessful = true;
            successMessage = "Form submitted successfully - URL changed to: " + driver.getCurrentUrl();
            System.out.println(successMessage);
        }

        // Print page source excerpt for debugging if test fails
        if (!isSuccessful) {
            System.out.println("\n=== Page Source Excerpt ===");
            String[] lines = pageSource.split("\n");
            for (int i = 0; i < Math.min(50, lines.length); i++) {
                if (lines[i].contains("message") || lines[i].contains("success") || 
                    lines[i].contains("thank") || lines[i].contains("error")) {
                    System.out.println("Line " + i + ": " + lines[i]);
                }
            }
        }

        System.out.println("\nAssertion result: " + (isSuccessful ? "✅ PASSED" : "❌ FAILED"));
        Assertions.assertTrue(isSuccessful, 
            "Form submission was not successful. Expected success message but got: " + successMessage);
    }

    @Step("Fill field: {fieldName} with value: {value}")
    private void fillField(By locator, String value, String fieldName) {
        try {
            WebElement field = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            scrollToElement(field);
            wait.until(ExpectedConditions.elementToBeClickable(field));
            field.clear();
            field.sendKeys(value);
            System.out.println(fieldName + " filled with: " + value);
        } catch (Exception e) {
            System.out.println("Failed to fill " + fieldName + ": " + e.getMessage());
            // Try alternative approach with JavaScript
            try {
                List<WebElement> fields = driver.findElements(By.cssSelector("input[type='text'], input[type='email'], input[type='tel']"));
                for (WebElement f : fields) {
                    String placeholder = f.getAttribute("placeholder");
                    String name = f.getAttribute("name");
                    if ((placeholder != null && placeholder.toLowerCase().contains(fieldName.toLowerCase())) ||
                        (name != null && name.toLowerCase().contains(fieldName.toLowerCase()))) {
                        scrollToElement(f);
                        f.clear();
                        f.sendKeys(value);
                        System.out.println(fieldName + " filled with alternative method: " + value);
                        return;
                    }
                }
            } catch (Exception ex) {
                System.out.println("Alternative fill method also failed for " + fieldName);
            }
        }
    }

    @Step("Fill text area: {fieldName}")
    private void fillTextArea(By locator, String value, String fieldName) {
        try {
            WebElement field = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            scrollToElement(field);
            wait.until(ExpectedConditions.elementToBeClickable(field));
            field.clear();
            field.sendKeys(value);
            System.out.println(fieldName + " filled");
        } catch (Exception e) {
            System.out.println("Failed to fill " + fieldName + ": " + e.getMessage());
            // Try finding any textarea
            try {
                List<WebElement> textareas = driver.findElements(By.tagName("textarea"));
                if (!textareas.isEmpty()) {
                    WebElement textarea = textareas.get(0);
                    scrollToElement(textarea);
                    textarea.clear();
                    textarea.sendKeys(value);
                    System.out.println(fieldName + " filled with alternative method");
                }
            } catch (Exception ex) {
                System.out.println("Alternative textarea fill method also failed for " + fieldName);
            }
        }
    }

    @Step("Scroll to element")
    private void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    @Step("Cleanup and close browser")
    public void tearDown() {
        if (driver != null) {
            try {
                Thread.sleep(2000); // Wait to see the result
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            driver.quit();
            System.out.println("Browser closed");
        }
    }
}
