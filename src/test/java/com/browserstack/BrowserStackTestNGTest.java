package com.browserstack;

import java.io.FileReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.browserstack.local.Local;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;


public class BrowserStackTestNGTest {
    public WebDriver driver;
    private Local bsLocal;

    @BeforeMethod(alwaysRun=true)
    @org.testng.annotations.Parameters(value={"config", "environment"})
    @SuppressWarnings("unchecked")
    public void setUp(String config_file, String environment) throws Exception {
        JSONParser parser = new JSONParser();
        JSONObject config = (JSONObject) parser.parse(new FileReader("src/test/resources/conf/" + config_file));
        JSONObject envs = (JSONObject) config.get("environments");

        DesiredCapabilities capabilities = new DesiredCapabilities();

        Map<String, Object> envCapabilities = (Map<String, Object>) envs.get(environment);
        Iterator<Map.Entry<String, Object>> it = envCapabilities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> pair = (Map.Entry<String, Object>) it.next();
            capabilities.setCapability(pair.getKey().toString(), pair.getValue());
        }

        Map<String, Object> commonCapabilities = (Map<String, Object>) config.get("capabilities");
        it = commonCapabilities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> pair = (Map.Entry<String, Object>) it.next();
            Object envData = capabilities.getCapability(pair.getKey().toString());
            Object resultData = pair.getValue();
            if (envData != null && envData.getClass() == JSONObject.class) {
                ((JSONObject) resultData).putAll((JSONObject) envData);
            }
            capabilities.setCapability(pair.getKey().toString(), resultData);
        }

        String username = System.getenv("BROWSERSTACK_USERNAME");
        if(username == null) {
            username = (String) config.get("user");
        }

        String accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");
        if(accessKey == null) {
            accessKey = (String) config.get("key");
        }

        this.checkAndStartBrowserStackLocal(capabilities, accessKey);

        driver = new RemoteWebDriver(new URL("https://"+username+":"+accessKey+"@"+config.get("server")+"/wd/hub"), capabilities);
    }

    public void checkAndStartBrowserStackLocal(DesiredCapabilities capabilities, String accessKey) throws Exception {
        if (bsLocal != null) {
            return;
        }
        if (capabilities.getCapability("bstack:options") != null
                && ((JSONObject) capabilities.getCapability("bstack:options")).get("local") != null
                && ((Boolean) ((JSONObject) capabilities.getCapability("bstack:options")).get("local")) == true) {
            bsLocal = new Local();
            Map<String, String> options = new HashMap<String, String>();
            options.put("key", accessKey);
            try {
                bsLocal.start(options);
            } catch (Exception e) {
                System.out.println("Error: could not start browserstack local");
                e.printStackTrace();
                throw e;
            }
        }
    }

    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        driver.quit();
        if (bsLocal != null) {
            bsLocal.stop();
        }
    }
}
