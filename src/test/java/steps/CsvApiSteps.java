package steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import utils.LoadEnvironment;
import utils.RestAssuredUtils_old;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.formula.eval.StringValueEval;
import org.testng.Assert;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.*;
import java.util.*;

import static io.restassured.RestAssured.given;

public class CsvApiSteps {

    Response responseFromPOST;
    Response responseFromGET;
    Response responseFromPUT;
    Response responseFromDELETE;
    Response response;
    RestAssuredUtils_old restAssuredUtils = new RestAssuredUtils_old();
    ScenarioHooks hooks = new ScenarioHooks();
    ArrayList<String> params = new ArrayList<String>();
    Map<String, String> b2bParamKeyValues = new HashMap<>();
    private static final String FILE_PATH = "src/test/java/resources/parametersNotHandled/parameters.txt";
    private static boolean cleared = false; // ensures clearing happens only once

    @Given("Iterate each row and run the test case of file {string}")
    public void iterateEachRowAndRunTheTestCaseOfFile(String csvPath) throws Exception {
        // Configure your base URL here or from a property
        String baseUrl = System.getProperty("BASE_URL", "https://your.api.server");
        RestAssured.baseURI = baseUrl;

        ObjectMapper mapper = new ObjectMapper();

        Reader in = new FileReader(csvPath);
        Iterable<CSVRecord> recs = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);

        int rowNum = 0;
        for (CSVRecord r : recs) {
            rowNum++;

            String method = r.get("HTTP method").trim().toUpperCase(Locale.ROOT);
            String name = r.get("test case name");
            String ep = LoadEnvironment.tenant + r.get("endpoint");
            String updatedEP = replacePlaceholders(ep);
            storeParamsInArrayList(updatedEP);
            // add step to update B2B parameters to EP
            String payload = r.get("payload");
            payload = replacePlaceholders(payload);
            // add step to update B2B parameters to payload

            int expected = Integer.parseInt(r.get("expected status code"));

            System.out.println("---- Executing Row " + rowNum + ": " + name + " ----");

            System.out.println("===========================================");
            System.out.println(" Test Case Name   : " + name);
            System.out.println(" HTTP Method      : " + method);
            System.out.println(" Endpoint         : " + updatedEP);
            System.out.println(" Payload          : " + payload);
            System.out.println(" Expected Status  : " + expected);
            System.out.println("===========================================");

            // // Build request
            // var req = given().relaxedHTTPSValidation().header("Accept",
            // "application/json");
            // if (payload != null && !payload.trim().equals("{}")) {
            // JsonNode body = mapper.readTree(payload);
            // if (body.size() > 0) {
            // req = req.contentType("application/json").body(body.toString());
            // }
            // }

            // // Send request
            // Response resp;
            // if ("POST".equals(method)) {
            // resp = req.post(ep, hooks.cookie).then().extract().response();
            // } else if ("GET".equals(method)) {
            // resp = req.get(ep);
            // } else if ("PUT".equals(method)) {
            // resp = req.put(ep);
            // } else if ("PATCH".equals(method)) {
            // resp = req.patch(ep);
            // } else if ("DELETE".equals(method)) {
            // resp = req.delete(ep);
            // } else {
            // throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            // }

            // // Validate
            // int actual = resp.getStatusCode();
            // System.out.println("Expected=" + expected + " | Actual=" + actual);
            // Assert.assertEquals(actual, expected,
            // "[FAIL] Row " + rowNum + " (" + name + ") => expected " + expected + " but
            // got " + actual);
        }

        in.close();
    }

    public static String replacePlaceholders(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return payload;
        }

        Map<String, String> replacements = new HashMap<>();

        // Strings
        replacements.put("${emptyString}", "");
        replacements.put("${null}", "null");
        replacements.put("${nullValue}", "null");
        replacements.put("${longString}", generateString('A', 500));
        replacements.put("${256Characters}", generateString('X', 256));
        replacements.put("${254Characters}", generateString('X', 254));
        replacements.put("${255Characters}", generateString('X', 255));
        replacements.put("${127Characters}", generateString('X', 127));
        replacements.put("${128Characters}", generateString('X', 128));
        replacements.put("${129Characters}", generateString('X', 129));
        replacements.put("${leadingSpaceString}", "   leading");
        replacements.put("${trailingSpaceString}", "trailing   ");
        replacements.put("${specialCharsString}", "!@#$%^&*()_+{}|:<>?~");

        // Numbers
        replacements.put("${veryLargeNumber}", String.valueOf(Long.MAX_VALUE));
        replacements.put("${negativeNumber}", "-123");
        replacements.put("${zero}", "0");
        replacements.put("${positiveNumber}", "123");

        String result = payload;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }

    private static String generateString(char c, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String generateString(String prefix, String c, int length) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = prefix.length(); i < length; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    public void storeParamsInArrayList(String payload) {
        // Regex to match ${...}
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(payload);

        while (matcher.find()) {
            params.add(matcher.group(1)); // inside ${...}
        }

        // Print ArrayList
        System.out.println(params);
    }

    public static final class Intent {
        public static final String PARTNER_USER_NAME_VALID = "partner_user_name_valid";
        public static final String PARTNER_USER_NAME_INVALID = "partner_user_name_invalid";
        public static final String PARTNER_USER_ID_VALID = "partner_user_id_valid";
        public static final String PARTNER_USER_ID_INVALID = "partner_user_id_invalid";
        public static final String EMPTY = " ";
        public static final String NULLL = "null";
        public static final String LONG = "long";
        public static final String DEFAULT_ID_VALID = "default_id_valid";
        public static final String negative = "-1";
        public static final String zero = "0";
        public static final String largeNumber = String.valueOf(Integer.MAX_VALUE);
        public static final String char128 = generateString("B2B_API_Auto_", "A", 128);
        public static final String char127 = generateString("B2B_API_Auto_", "A", 128);
        public static final String char129 = generateString("B2B_API_Auto_", "A", 129);
        public static final String char254 = generateString("B2B_API_Auto_", "A", 254);
        public static final String char255 = generateString("B2B_API_Auto_", "A", 255);
        public static final String char256 = generateString("B2B_API_Auto_", "A", 256);
        public static final String specialCharsString = "@#$%^??//||";
        public static final String leadingSpaceString = "     leading";
        public static final String trailingSpaceString = "trailing     ";

    }

    /** Extract all ${...} parameters from any text */
    public static ArrayList<String> extractParams(String text) {
        ArrayList<String> out = new ArrayList<String>();
        if (text == null)
            return out;
        Pattern p = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher m = p.matcher(text);
        while (m.find())
            out.add(m.group(1)); // inside ${...}
        return out;
    }

    // ---------- helpers ----------
    private static boolean containsWord(String text, String word) {
        String t = " " + text + " ";
        String w = " " + word + " ";
        return t.indexOf(w) >= 0;
    }

    private static boolean containsAny(String text, String[] words) {
        for (int i = 0; i < words.length; i++) {
            if (text.indexOf(words[i]) >= 0)
                return true;
        }
        return false;
    }

    public static String mapToIntent(String paramName) {
        if (paramName == null)
            return Intent.EMPTY;

        // Canonicalize once: lowercase, non-alnum -> space
        String c = paramName.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();

        // global quick cases (edge/boundary placeholders)
        if (containsWord(c, "empty"))
            return Intent.EMPTY;
        if (containsWord(c, "null"))
            return Intent.NULLL;
        if (containsWord(c, "long"))
            return Intent.LONG;
        if (containsWord(c, "negative"))
            return Intent.negative;
        if (containsWord(c, "zero"))
            return Intent.LONG;
        if (containsWord(c, "leading"))
            return Intent.leadingSpaceString;
        if (containsWord(c, "trailing"))
            return Intent.trailingSpaceString;
        if (containsAny(c, new String[] { "127", "127char", "127characters", "127character" }))
            return Intent.char127;
        if (containsAny(c, new String[] { "128", "128char", "128characters", "128character" }))
            return Intent.char128;
        if (containsAny(c, new String[] { "129", "129char", "129characters", "129character" }))
            return Intent.char129;
        if (containsAny(c, new String[] { "254", "254char", "254characters", "254character" }))
            return Intent.char254;
        if (containsAny(c, new String[] { "255", "255char", "255characters", "255character" }))
            return Intent.char255;
        if (containsAny(c, new String[] { "256", "256char", "256characters", "256character" }))
            return Intent.char256;
        if (containsAny(c, new String[] { "special", "specialcharacters", "specialchar", "specialcharacter" }))
            return Intent.specialCharsString;

        // synonyms (add freely as your naming evolves)
        boolean hasPartner = containsAny(c, new String[] { "partner", "tradingpartner", "prtnr" });
        boolean hasUser = containsAny(c, new String[] { "user", "usr", "member" });
        boolean hasName = containsAny(c, new String[] { "name", "username", "user name", "full name", "fullname" });
        boolean hasId = containsAny(c, new String[] { "id", "user id", "userid", "identifier" });
        boolean hasProfile = containsAny(c,
                new String[] { "profile", "prof", "tradingpartnerprofile", "partnerprofile" });
        boolean hasSpaces = containsAny(c, new String[] { "spaces", "space", "withspaces" });

        boolean isValid = containsWord(c, "valid") || containsWord(c, "existing") || containsWord(c, "correct")
                || containsWord(c, "right");
        boolean isInvalid = containsWord(c, "invalid") || containsWord(c, "nonexistent") || containsWord(c, "bad")
                || containsWord(c, "notfound");

        // specific → generic
        if (hasPartner && hasUser && hasName && isValid)
            return Intent.PARTNER_USER_NAME_VALID;
        if (hasPartner && hasUser && hasName && isInvalid)
            return Intent.PARTNER_USER_NAME_INVALID;
        if (hasPartner && hasUser && hasId && isValid)
            return Intent.PARTNER_USER_ID_VALID;
        if (hasPartner && hasUser && hasId && isInvalid)
            return Intent.PARTNER_USER_ID_INVALID;
        if (hasUser && hasName && isValid)
            return Intent.PARTNER_USER_ID_INVALID;
        if (hasUser && hasName && hasSpaces && isValid)
            return "valid user name with spaces";

        if (hasId && isValid)
            return Intent.DEFAULT_ID_VALID;

        // add parameter name in a file so that it can be added to above condition in
        // next run

        // fallback (safe default)
        return writeLine(paramName);
    }

    // Writes a new line to the file
    public static String writeLine(String text) {
        try {
            // If not cleared yet → overwrite (clear), else append
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, !cleared))) {
                writer.write(text);
                writer.newLine();
            }
            cleared = true; // after first write, file is cleared once
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "parameter added to file";
    }
}