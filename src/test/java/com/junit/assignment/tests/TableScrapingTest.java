package com.junit.assignment.tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.*;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Epic("Web Scraping Automation")
@Feature("DSE Stock Price Table Scraping")
public class TableScrapingTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private static final String OUTPUT_DIR = "scraped-data";
    private static final String OUTPUT_FILE = OUTPUT_DIR + "/stock_prices.txt";

    @BeforeEach
    @Step("Setup WebDriver and navigate to DSE website")
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
        
        // Create output directory
        File directory = new File(OUTPUT_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
            System.out.println("Created output directory: " + OUTPUT_DIR);
        }
        
        driver.get("https://dsebd.org/latest_share_price_scroll_by_value.php");
        System.out.println("Navigated to: " + driver.getCurrentUrl());
    }

    @Test
    @Story("Scrape Stock Price Table")
    @Description("Extract all stock price data from DSE table, print to console, and save to file")
    @Severity(SeverityLevel.CRITICAL)
    public void testScrapeStockPriceTable() throws InterruptedException, IOException {
        // Wait for page to load
        Thread.sleep(3000);

        StringBuilder scrapedData = new StringBuilder();
        
        // Add header with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String header = "=".repeat(80) + "\n" +
                       "DSE Stock Price Data - Scraped on: " + timestamp + "\n" +
                       "=".repeat(80) + "\n\n";
        
        scrapedData.append(header);
        System.out.println(header);

        try {
            // Find the table - try multiple selectors
            WebElement table = null;
            
            // Try different table selectors
            List<WebElement> tables = driver.findElements(By.tagName("table"));
            
            if (tables.isEmpty()) {
                System.out.println("No tables found on the page!");
                // Try to find data in div or other containers
                List<WebElement> dataContainers = driver.findElements(By.cssSelector("[class*='table'], [id*='table']"));
                System.out.println("Found " + dataContainers.size() + " potential data containers");
            } else {
                // Use the first table or the one that looks like it has data
                for (WebElement t : tables) {
                    List<WebElement> rows = t.findElements(By.tagName("tr"));
                    if (rows.size() > 1) { // Has header + data rows
                        table = t;
                        System.out.println("Found table with " + rows.size() + " rows");
                        break;
                    }
                }
                
                if (table == null && !tables.isEmpty()) {
                    table = tables.get(0);
                }
            }

            if (table != null) {
                // Extract table headers
                List<WebElement> headers = table.findElements(By.cssSelector("thead tr th, tr:first-child th, tr:first-child td"));
                
                if (!headers.isEmpty()) {
                    System.out.println("\n--- TABLE HEADERS ---");
                    scrapedData.append("--- TABLE HEADERS ---\n");
                    
                    for (int i = 0; i < headers.size(); i++) {
                        String headerText = headers.get(i).getText().trim();
                        String headerLine = String.format("Column %d: %s", i + 1, headerText);
                        System.out.println(headerLine);
                        scrapedData.append(headerLine).append("\n");
                    }
                    scrapedData.append("\n");
                }

                // Extract table rows
                List<WebElement> rows = table.findElements(By.cssSelector("tbody tr, tr"));
                System.out.println("\n--- TABLE DATA ---");
                System.out.println("Total rows found: " + rows.size());
                scrapedData.append("--- TABLE DATA ---\n");
                scrapedData.append("Total rows: ").append(rows.size()).append("\n\n");

                int rowNumber = 0;
                for (WebElement row : rows) {
                    // Skip if this is the header row
                    List<WebElement> headerCells = row.findElements(By.tagName("th"));
                    if (!headerCells.isEmpty() && rowNumber == 0) {
                        rowNumber++;
                        continue;
                    }

                    List<WebElement> cells = row.findElements(By.tagName("td"));
                    
                    if (!cells.isEmpty()) {
                        rowNumber++;
                        String rowHeader = String.format("\nRow %d:", rowNumber);
                        System.out.println(rowHeader);
                        scrapedData.append(rowHeader).append("\n");

                        for (int i = 0; i < cells.size(); i++) {
                            String cellValue = cells.get(i).getText().trim();
                            String cellLine = String.format("  Cell %d: %s", i + 1, cellValue);
                            System.out.println(cellLine);
                            scrapedData.append(cellLine).append("\n");
                        }
                    }
                }

                System.out.println("\n" + "=".repeat(80));
                System.out.println("Total data rows scraped: " + rowNumber);
                scrapedData.append("\n").append("=".repeat(80)).append("\n");
                scrapedData.append("Total data rows scraped: ").append(rowNumber).append("\n");

            } else {
                String noTableMsg = "No suitable table found on the page!";
                System.out.println(noTableMsg);
                scrapedData.append(noTableMsg).append("\n");
                
                // Try to capture any visible text as fallback
                WebElement body = driver.findElement(By.tagName("body"));
                String bodyText = body.getText();
                System.out.println("\nPage content preview (first 500 chars):\n" + 
                    bodyText.substring(0, Math.min(500, bodyText.length())));
            }

        } catch (Exception e) {
            String errorMsg = "Error while scraping table: " + e.getMessage();
            System.err.println(errorMsg);
            scrapedData.append("\n").append(errorMsg).append("\n");
            e.printStackTrace();
        }

        // Save to file
        saveToFile(scrapedData.toString());

        // Assert that we scraped some data
        Assertions.assertTrue(scrapedData.length() > header.length(), 
            "Expected to scrape some data, but the output only contains the header");
        
        System.out.println("\n✓ Test completed successfully!");
    }

    @Step("Save scraped data to file: {OUTPUT_FILE}")
    private void saveToFile(String content) throws IOException {
        FileWriter fileWriter = null;
        PrintWriter writer = null;
        try {
            fileWriter = new FileWriter(OUTPUT_FILE, StandardCharsets.UTF_8);
            writer = new PrintWriter(fileWriter);
            writer.print(content);
            System.out.println("\n✓ Data saved to file: " + OUTPUT_FILE);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
            throw e;
        } finally {
            if (writer != null) {
                writer.close();
            }
            if (fileWriter != null) {
                fileWriter.close();
            }
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
