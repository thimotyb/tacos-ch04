package tacos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

@Disabled("HtmlUnitDriver interactions are unstable with Selenium 4 on Spring Boot 3; replace with a reliable driver before reenabling")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class DesignAndOrderTacosBrowserTest {

  private static HtmlUnitDriver browser;

  @LocalServerPort
  private int port;

  @Autowired
  TestRestTemplate rest;

  @BeforeAll
  static void setup() {
    browser = new HtmlUnitDriver(true);
    browser.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
  }

  @AfterAll
  static void closeBrowser() {
    if (browser != null) {
      browser.quit();
    }
  }

  @Test
  void testDesignATacoPage_HappyPath() {
    browser.get(homePageUrl());
    clickDesignATaco();
    assertDesignPageElements();
    buildAndSubmitATaco("Basic Taco", "FLTO", "GRBF", "CHED", "TMTO", "SLSA");
    clickBuildAnotherTaco();
    buildAndSubmitATaco("Another Taco", "COTO", "CARN", "JACK", "LETC", "SRCR");
    fillInAndSubmitOrderForm();
    assertEquals(homePageUrl(), browser.getCurrentUrl());
  }

  @Test
  void testDesignATacoPage_EmptyOrderInfo() {
    browser.get(homePageUrl());
    clickDesignATaco();
    assertDesignPageElements();
    buildAndSubmitATaco("Basic Taco", "FLTO", "GRBF", "CHED", "TMTO", "SLSA");
    submitEmptyOrderForm();
    fillInAndSubmitOrderForm();
    assertEquals(homePageUrl(), browser.getCurrentUrl());
  }

  @Test
  void testDesignATacoPage_InvalidOrderInfo() {
    browser.get(homePageUrl());
    clickDesignATaco();
    assertDesignPageElements();
    buildAndSubmitATaco("Basic Taco", "FLTO", "GRBF", "CHED", "TMTO", "SLSA");
    submitInvalidOrderForm();
    fillInAndSubmitOrderForm();
    assertEquals(homePageUrl(), browser.getCurrentUrl());
  }

  private void buildAndSubmitATaco(String name, String... ingredients) {
    assertDesignPageElements();

    for (String ingredient : ingredients) {
      browser.findElement(By.cssSelector("input[value='" + ingredient + "']")).click();
    }
    browser.findElement(By.cssSelector("input#name")).sendKeys(name);
    submitCurrentForm();
    waitForUrl(orderDetailsPageUrl());
  }

  private void assertDesignPageElements() {
    waitForUrl(designPageUrl());
    assertEquals(designPageUrl(), browser.getCurrentUrl());
    List<WebElement> ingredientGroups = browser.findElements(By.className("ingredient-group"));
    assertEquals(5, ingredientGroups.size());

    WebElement wrapGroup = browser.findElement(By.cssSelector("div.ingredient-group#wraps"));
    List<WebElement> wraps = wrapGroup.findElements(By.tagName("div"));
    assertEquals(2, wraps.size());
    assertIngredient(wrapGroup, 0, "FLTO", "Flour Tortilla");
    assertIngredient(wrapGroup, 1, "COTO", "Corn Tortilla");

    WebElement proteinGroup = browser.findElement(By.cssSelector("div.ingredient-group#proteins"));
    List<WebElement> proteins = proteinGroup.findElements(By.tagName("div"));
    assertEquals(2, proteins.size());
    assertIngredient(proteinGroup, 0, "GRBF", "Ground Beef");
    assertIngredient(proteinGroup, 1, "CARN", "Carnitas");

    WebElement cheeseGroup = browser.findElement(By.cssSelector("div.ingredient-group#cheeses"));
    List<WebElement> cheeses = cheeseGroup.findElements(By.tagName("div"));
    assertEquals(2, cheeses.size());
    assertIngredient(cheeseGroup, 0, "CHED", "Cheddar");
    assertIngredient(cheeseGroup, 1, "JACK", "Monterrey Jack");

    WebElement veggieGroup = browser.findElement(By.cssSelector("div.ingredient-group#veggies"));
    List<WebElement> veggies = veggieGroup.findElements(By.tagName("div"));
    assertEquals(2, veggies.size());
    assertIngredient(veggieGroup, 0, "TMTO", "Diced Tomatoes");
    assertIngredient(veggieGroup, 1, "LETC", "Lettuce");

    WebElement sauceGroup = browser.findElement(By.cssSelector("div.ingredient-group#sauces"));
    List<WebElement> sauces = sauceGroup.findElements(By.tagName("div"));
    assertEquals(2, sauces.size());
    assertIngredient(sauceGroup, 0, "SLSA", "Salsa");
    assertIngredient(sauceGroup, 1, "SRCR", "Sour Cream");
  }

  private void fillInAndSubmitOrderForm() {
    assertTrue(browser.getCurrentUrl().startsWith(orderDetailsPageUrl()));
    fillField("input#deliveryName", "Ima Hungry");
    fillField("input#deliveryStreet", "1234 Culinary Blvd.");
    fillField("input#deliveryCity", "Foodsville");
    fillField("input#deliveryState", "CO");
    fillField("input#deliveryZip", "81019");
    fillField("input#ccNumber", "4111111111111111");
    fillField("input#ccExpiration", "10/29");
    fillField("input#ccCVV", "123");
    submitCurrentForm();
    waitForUrl(homePageUrl());
  }

  private void submitEmptyOrderForm() {
    assertEquals(currentOrderDetailsPageUrl(), browser.getCurrentUrl());
    submitCurrentForm();
    waitForUrl(orderDetailsPageUrl());
    assertEquals(orderDetailsPageUrl(), browser.getCurrentUrl());

    List<String> validationErrors = getValidationErrorTexts();
    assertEquals(9, validationErrors.size());
    assertTrue(validationErrors.contains("Please correct the problems below and resubmit."));
    assertTrue(validationErrors.contains("Delivery name is required"));
    assertTrue(validationErrors.contains("Street is required"));
    assertTrue(validationErrors.contains("City is required"));
    assertTrue(validationErrors.contains("State is required"));
    assertTrue(validationErrors.contains("Zip code is required"));
    assertTrue(validationErrors.contains("Not a valid credit card number"));
    assertTrue(validationErrors.contains("Must be formatted MM/YY"));
    assertTrue(validationErrors.contains("Invalid CVV"));
  }

  private List<String> getValidationErrorTexts() {
    List<WebElement> validationErrorElements = browser.findElements(By.className("validationError"));
    return validationErrorElements.stream()
        .map(WebElement::getText)
        .collect(Collectors.toList());
  }

  private void submitInvalidOrderForm() {
    assertTrue(browser.getCurrentUrl().startsWith(orderDetailsPageUrl()));
    fillField("input#deliveryName", "I");
    fillField("input#deliveryStreet", "1");
    fillField("input#deliveryCity", "F");
    fillField("input#deliveryState", "C");
    fillField("input#deliveryZip", "8");
    fillField("input#ccNumber", "1234432112344322");
    fillField("input#ccExpiration", "14/91");
    fillField("input#ccCVV", "1234");
    submitCurrentForm();
    waitForUrl(orderDetailsPageUrl());
    assertEquals(orderDetailsPageUrl(), browser.getCurrentUrl());

    List<String> validationErrors = getValidationErrorTexts();
    assertEquals(4, validationErrors.size());
    assertTrue(validationErrors.contains("Please correct the problems below and resubmit."));
    assertTrue(validationErrors.contains("Not a valid credit card number"));
    assertTrue(validationErrors.contains("Must be formatted MM/YY"));
    assertTrue(validationErrors.contains("Invalid CVV"));
  }

  private void fillField(String fieldName, String value) {
    WebElement field = browser.findElement(By.cssSelector(fieldName));
    field.clear();
    field.sendKeys(value);
  }

  private void assertIngredient(WebElement ingredientGroup, int ingredientIdx, String id, String name) {
    List<WebElement> elements = ingredientGroup.findElements(By.tagName("div"));
    WebElement ingredient = elements.get(ingredientIdx);
    assertEquals(id, ingredient.findElement(By.tagName("input")).getAttribute("value"));
    assertEquals(name, ingredient.findElement(By.tagName("span")).getText());
  }

  private void clickDesignATaco() {
    waitForUrl(homePageUrl());
    assertEquals(homePageUrl(), browser.getCurrentUrl());
    browser.findElement(By.cssSelector("a[id='design']")).click();
  }

  private void clickBuildAnotherTaco() {
    waitForUrl(orderDetailsPageUrl());
    assertTrue(browser.getCurrentUrl().startsWith(orderDetailsPageUrl()));
    browser.findElement(By.cssSelector("a[id='another']")).click();
    waitForUrl(designPageUrl());
  }

  private String designPageUrl() {
    return homePageUrl() + "design";
  }

  private String homePageUrl() {
    return "http://localhost:" + port + "/";
  }

  private String orderDetailsPageUrl() {
    return homePageUrl() + "orders";
  }

  private String currentOrderDetailsPageUrl() {
    return homePageUrl() + "orders/current";
  }

  private void submitCurrentForm() {
    WebElement form = browser.findElement(By.cssSelector("form"));
    WebElement submit = form.findElement(By.cssSelector("button, input[type='submit']"));
    submit.click();
  }

  private void waitForUrl(String expectedPrefix) {
    waitForUrl(expectedPrefix, 5000L);
  }

  private void waitForUrl(String expectedPrefix, long timeoutMillis) {
    long deadline = System.currentTimeMillis() + timeoutMillis;
    while (System.currentTimeMillis() < deadline) {
      if (browser.getCurrentUrl().startsWith(expectedPrefix)) {
        return;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }
}
