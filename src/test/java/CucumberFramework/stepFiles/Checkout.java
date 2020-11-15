package CucumberFramework.stepFiles;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import org.junit.Assert;
import org.openqa.selenium.support.ui.Select;

public class Checkout {
    WebDriver driver;
    int itemsCount = 0;
    double itemsTotalPrice;
    private final double POSTAGE_PRICE = 2.00;

    @Before
    public void setup() {
        System.setProperty("webdriver.chrome.driver", "./src/test/resources/chromedriver.exe");
        this.driver = new ChromeDriver();
        this.driver.manage().window().maximize();
        this.driver.manage().timeouts().pageLoadTimeout(4000, TimeUnit.SECONDS);
        this.driver.manage().timeouts().setScriptTimeout(4000, TimeUnit.SECONDS);
        this.driver.manage().timeouts().implicitlyWait(4000, TimeUnit.SECONDS);
        //TODO: clean all screenshot* files from ./target
    }

    @After
    public void tearDown() {
        this.driver.manage().deleteAllCookies();
        this.driver.quit();
        this.driver = null;
    }


    @Given("^I am logged in$")
    public void iAmLoggedIn() {
        driver.get("http://automationpractice.com/");
        loginDefault();
    }

    @Given("^I find an item size \"([^\"]*)\" in my basket$")
    public void iFindAnItemWithSizeInMyBasket(String itemSize) {

        goToBasket();
        // check size
        // TODO: refactor; this method assumes only a single item in the basket...
        String sizeInBasket = driver.findElement(By.className("cart_item"))
                .findElement(By.className("cart_description"))
                .findElement(By.xpath("./small/a")).getText()
                .replaceAll("^.*: ", "");
        customAssertion("Item size in basket does not match initial product listing!",
                itemSize, sizeInBasket,
                "sizeinbasket");
    }

    @Given("^I add a new \"([^\"]*)\" size \"([^\"]*)\" to my basket$")
    public void iAddANewItemWithSizeToMyBasket(String itemName, String itemSize) throws Exception {

        searchAndQuickView(itemName);

        // update size in quickView iframe
        driver.switchTo().frame(0);
        WebElement selectDropdown = driver.findElement(By.id("group_1"));
        new Select(selectDropdown).selectByVisibleText(itemSize);

        // capture displayed price for later assertion
        String originalItemPrice = driver.findElement(By.id("our_price_display")).getText();

        // add to basket
        driver.findElement(By.id("add_to_cart")).findElement(By.tagName("button")).click();
        updateTotalBasketExpectedPrice(originalItemPrice);

        Thread.sleep(3000);
        // ^^ ordinarily would avoid but could not find any other way to proceed

        String sizeInBasket = driver.findElement(By.id("layer_cart_product_attributes")).getText()
                .replaceAll("^.*, ", "");
        customAssertion("Item size in basket does not match initial product listing!",
                itemSize, sizeInBasket,
                "sizeinbasket");
        String priceInBasket = driver.findElement(By.id("layer_cart_product_price")).getText();
        customAssertion("Item price in basket does not match initial product listing!",
                originalItemPrice, priceInBasket,
                "priceinbasket");

        // exit modal
        driver.findElement(By.xpath("//span[@title='Continue shopping']")).click();
    }

    private void customAssertion(String message, String comparison1, String comparison2, String screenshotFilename) {
        try {
            Assert.assertEquals(message, comparison1, comparison2);
        } catch (AssertionError ae) {
            System.out.println(ae);
            File file = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            try {
                String fileName = screenshotFilename + "-" +
                        new Timestamp(System.currentTimeMillis()).toString().replaceAll("[^0-9]", "");
                FileUtils.moveFile(file, new File("./target/screenshot-" + fileName + ".png"));
                throw new AssertionError("Screenshot of failure saved to: target/screenshot-" + fileName + ".png", ae);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }

        }
    }

    @Given("^I find the expected total price of my basket$")
    public void iFindTheExpectedTotalPriceOfMyBasket() {

        goToBasket();
        customAssertion("Basket total does not match expected",
                "$" + new DecimalFormat("#.00").format(itemsTotalPrice + POSTAGE_PRICE),
                driver.findElement(By.id("total_price")).getText(),
                "totalprice");
    }

    private void updateTotalBasketExpectedPrice(String price) {
        itemsTotalPrice += Double.parseDouble(price.replaceAll("\\$", ""));
        itemsCount++;
    }


    @Given("^I checkout successfully$")
    public void iCheckoutSuccessfully() {
        // 1.
        goToBasket();
        // 2.
        driver.findElement(By.className("cart_navigation")).findElement(By.className("button")).click();
        // 3.
        driver.findElement(By.className("cart_navigation")).findElement(By.className("button")).click();
        // 4.
        driver.findElement(By.id("cgv")).click();
        driver.findElement(By.xpath("//button[@name='processCarrier']")).click();
        // 5. pay by wire
        driver.findElement(By.className("bankwire")).click();
        // confirm
        driver.findElement(By.xpath("//button[@type='submit']")).click();


    }

    private void loginDefault() {
        driver.findElement(By.className("header_user_info")).click();
        driver.findElement(By.id("email")).sendKeys("mydummyemail@somedomain.com");
        driver.findElement(By.id("passwd")).sendKeys("youlend");
        driver.findElement(By.id("SubmitLogin")).click();
        Assert.assertTrue("Did not log in successfully",
                driver.findElement(By.className("logout")).getText().contains("Sign out"));
    }


    private void searchAndQuickView(String itemName) throws Exception {
        // search for "itemName"
        driver.findElement(By.id("search_query_top")).clear();
        driver.findElement(By.id("search_query_top")).sendKeys(itemName);
        driver.findElement(By.className("button-search")).click();
        // select one at random
        List<WebElement> productList = driver.findElement(By.className("product_list")).findElements(By.xpath("./li"));
        if (productList.isEmpty()) {
            throw new Exception("Search results list is empty, unable to continue test");
        }
        int selectedItem = new Random().ints(0, productList.size()).findAny().getAsInt();
        WebElement item = productList.get(selectedItem);
        System.out.println(String.format("Selecting number %d of %d", selectedItem + 1, productList.size() + 1));
        item.click();
        item.findElement(By.className("quick-view")).click();
    }


    private void goToBasket() {
        driver.findElement(By.xpath("//a[@title='View my shopping cart']")).click();
    }

}
