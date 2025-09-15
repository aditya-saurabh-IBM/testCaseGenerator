package steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.testng.Assert;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static io.restassured.RestAssured.given;

public class ApiCsvSteps {
    private final List<CSVRecord> rows = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private String baseUrl;

    @Given("I load testcases from {string}")
    public void i_load_testcases_from(String csvPath) throws Exception {
        Reader in = new FileReader(csvPath);
        Iterable<CSVRecord> recs = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);
        for (CSVRecord r : recs) {
            rows.add(r);
        }
    }

    @When("I execute all testcases against {string}")
    public void i_execute_all_testcases_against(String base) {
        this.baseUrl = base;
        RestAssured.baseURI = this.baseUrl;
    }

    @Then("all responses should match expected status codes")
    public void all_responses_should_match_expected_status_codes() throws Exception {
        for (CSVRecord r : rows) {
            String method = r.get("HTTP method").trim().toUpperCase(Locale.ROOT);
            String name = r.get("test case name");
            String ep = r.get("endpoint");
            String payload = r.get("payload");
            int expected = Integer.parseInt(r.get("expected status code"));

            var req = given().relaxedHTTPSValidation().header("Accept", "application/json");
            if (payload != null && !payload.trim().equals("{}")) {
                JsonNode node = mapper.readTree(payload);
                if (node.size() > 0) {
                    req = req.contentType("application/json").body(node.toString());
                }
            }

            Response resp;
            if ("POST".equals(method))
                resp = req.post(ep);
            else if ("GET".equals(method))
                resp = req.get(ep);
            else if ("PUT".equals(method))
                resp = req.put(ep);
            else if ("PATCH".equals(method))
                resp = req.patch(ep);
            else if ("DELETE".equals(method))
                resp = req.delete(ep);
            else
                throw new IllegalArgumentException("Unsupported method: " + method);

            Assert.assertEquals(resp.getStatusCode(), expected,
                    "[FAIL] " + name + " => expected " + expected + " but got " + resp.getStatusCode());
        }
    }
}