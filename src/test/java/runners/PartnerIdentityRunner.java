package runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = {"src/test/java/features/PartnerIdentity/PartnerIdentity.feature"},
        glue = {"steps"}
//        plugin = {"pretty", "html:target/cucumber-reports"}
)
public class PartnerIdentityRunner extends AbstractTestNGCucumberTests {
}
