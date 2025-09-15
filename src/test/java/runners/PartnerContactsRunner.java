package runners;


import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = {"src/test/java/features/PartnerContact/PartnerContact.feature"},
        glue = {"steps"}
//        plugin = {"pretty", "html:target/cucumber-reports"}
)
public class PartnerContactsRunner extends AbstractTestNGCucumberTests {
}
