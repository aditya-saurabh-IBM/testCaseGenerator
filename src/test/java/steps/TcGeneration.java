package steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.restassured.response.Response;
import utils.LoadEnvironment;
import utils.RestAssuredUtils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.testng.Assert;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.*;
import java.util.*;

import static io.restassured.RestAssured.given;

public class TcGeneration {

    Response responseFromPOST;
    Response responseFromGET;
    Response responseFromPUT;
    Response responseFromDELETE;
    Response response;

    RestAssuredUtils restAssuredUtils = new RestAssuredUtils();
    ScenarioHooks hooks = new ScenarioHooks();

    ArrayList<String> params = new ArrayList<String>(); // still available, though replacement no longer depends on it
    Map<String, String> b2bParamKeyValues = new HashMap<>();
    private static final String FILE_PATH = "src/test/java/resources/parametersNotHandled/parameters.txt";
    private static boolean cleared = false; // ensures clearing happens only once
    private static final Set<String> writtenWords = new HashSet<>(); // track unique entries

    // ============ CSV Runner ============
    @Given("Iterate each row and replace the parameters in file {string}")
    public void iterateEachRowAndRunTheTestCaseOfFile(String csvPath) throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        Reader in = new FileReader(csvPath);
        Iterable<CSVRecord> recs = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);

        int rowNum = 0;
        for (CSVRecord r : recs) {
            rowNum++;

            String method = r.get("HTTP method").trim().toUpperCase(Locale.ROOT);
            String name = r.get("test case name");

            // Endpoint
            String ep = LoadEnvironment.tenant + r.get("endpoint");
            storeParamsInList(ep); // still collects parameters if you need them elsewhere
            String resolvedEP = replaceParameters(ep);

            // Payload
            String payload = r.get("payload");
            payload = replaceParameters(payload); // JSON-aware replacement (preserves types)

            int expected = Integer.parseInt(r.get("expected status code"));

            System.out.println("---- Executing Row " + rowNum + ": " + name + " ----");
            System.out.println("===========================================");
            System.out.println(" Test Case Name   : " + name);
            System.out.println(" HTTP Method      : " + method);
            System.out.println(" Endpoint         : " + resolvedEP);
            System.out.println(" Payload          : " + payload);
            System.out.println(" Expected Status  : " + expected);
            System.out.println("===========================================");

            // ==== Example request wiring (kept commented) ====
            // var req = given().relaxedHTTPSValidation().header("Accept",
            // "application/json");
            // if (payload != null && !payload.trim().equals("{}")) {
            // JsonNode body = mapper.readTree(payload);
            // if (body.size() > 0) {
            // req = req.contentType("application/json").body(body.toString());
            // }
            // }
            //
            // Response resp;
            // if ("POST".equals(method)) {
            // resp = req.post(resolvedEP, hooks.cookie).then().extract().response();
            // } else if ("GET".equals(method)) {
            // resp = req.get(resolvedEP);
            // } else if ("PUT".equals(method)) {
            // resp = req.put(resolvedEP);
            // } else if ("PATCH".equals(method)) {
            // resp = req.patch(resolvedEP);
            // } else if ("DELETE".equals(method)) {
            // resp = req.delete(resolvedEP);
            // } else {
            // throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            // }
            //
            // int actual = resp.getStatusCode();
            // System.out.println("Expected=" + expected + " | Actual=" + actual);
            // Assert.assertEquals(actual, expected, "[FAIL] Row " + rowNum + " (" + name +
            // ") => expected " + expected + " but got " + actual);
        }

        in.close();
    }

    // ============================================================
    // ============== NEW: Typed placeholder replacement ==========
    // ============================================================

    /**
     * Public entry point. Auto-detects JSON vs non-JSON and preserves types
     * inside JSON (Integer/Boolean/null/String).
     */
    public String replaceParameters(String input) {
        if (input == null || input.trim().isEmpty())
            return input;
        String trimmed = input.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            // JSON payload → replace as typed JSON values
            return replaceParametersInJson(input);
        }
        // Non-JSON (e.g., endpoint/query/header) → stringify replacements
        return replaceParametersInText(input);
    }

    // JSON-aware replacement
    private String replaceParametersInJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode replaced = replaceJsonNodePlaceholders(root);
            return mapper.writeValueAsString(replaced);
        } catch (Exception e) {
            System.err.println(
                    "[WARN] JSON parse failed in replaceParametersInJson; falling back to text: " + e.getMessage());
            return replaceParametersInText(json);
        }
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("^\\$\\{([^}]+)\\}$");
    private static final Pattern PLACEHOLDER_ANYWHERE = Pattern.compile("\\$\\{([^}]+)\\}");

    private JsonNode replaceJsonNodePlaceholders(JsonNode node) {
        ObjectMapper mapper = new ObjectMapper();

        if (node.isObject()) {
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                String field = it.next();
                JsonNode child = node.get(field);
                ((com.fasterxml.jackson.databind.node.ObjectNode) node)
                        .replace(field, replaceJsonNodePlaceholders(child));
            }
            return node;
        }

        if (node.isArray()) {
            com.fasterxml.jackson.databind.node.ArrayNode arr = (com.fasterxml.jackson.databind.node.ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                arr.set(i, replaceJsonNodePlaceholders(arr.get(i)));
            }
            return arr;
        }

        if (node.isTextual()) {
            String text = node.asText();

            // Case A: entire value is a single placeholder
            Matcher mExact = PLACEHOLDER.matcher(text);
            if (mExact.matches()) {
                String paramName = mExact.group(1);
                Object value = resolveParamValue(paramName);
                if (value == null)
                    return mapper.nullNode();
                if (value instanceof Boolean)
                    return mapper.getNodeFactory().booleanNode((Boolean) value);
                if (value instanceof Integer)
                    return mapper.getNodeFactory().numberNode((Integer) value);
                return mapper.getNodeFactory().textNode(String.valueOf(value));
            }

            // Case B: value contains one/more placeholders inside a string literal
            StringBuffer sb = new StringBuffer();
            Matcher mAny = PLACEHOLDER_ANYWHERE.matcher(text);
            while (mAny.find()) {
                String param = mAny.group(1);
                Object value = resolveParamValue(param);
                // Inside strings, we must stringify
                mAny.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(value)));
            }
            mAny.appendTail(sb);
            return mapper.getNodeFactory().textNode(sb.toString());
        }

        return node; // non-text scalars left as-is
    }

    // Non-JSON (plain text) replacement — everything stringified
    private String replaceParametersInText(String input) {
        if (input == null)
            return null;
        Matcher m = PLACEHOLDER_ANYWHERE.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String param = m.group(1);
            Object value = resolveParamValue(param); // Integer/Boolean/String/null
            String replacement = String.valueOf(value); // plain text → stringify (null => "null")
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Convert a parameter name (from ${...}) to a **typed** value.
     * Uses mapToIntent(...) to classify, then converts to
     * Integer/Boolean/null/String accordingly.
     */
    private Object resolveParamValue(String paramName) {
        String intentOrLiteral = mapToIntent(paramName);
        if (intentOrLiteral == null)
            return null;

        // Simple boolean name hints (you can expand this)
        String canon = paramName.toLowerCase(Locale.ROOT);
        if (canon.contains("true"))
            return Boolean.TRUE;
        if (canon.contains("false"))
            return Boolean.FALSE;

        // Explicit null intent
        if (Intent.NULLL.equals(intentOrLiteral))
            return null;

        // Numeric intents
        if (Intent.negative.equals(intentOrLiteral))
            return Integer.valueOf(-1);
        if (Intent.zero.equals(intentOrLiteral))
            return Integer.valueOf(0);
        if (Intent.largeNumber.equals(intentOrLiteral))
            return Integer.valueOf(Integer.MAX_VALUE);

        // If the literal itself looks like an integer, parse it
        if (isInteger(intentOrLiteral)) {
            try {
                return Integer.valueOf(intentOrLiteral);
            } catch (NumberFormatException ignored) {
            }
        }

        // Otherwise, treat as plain string
        return intentOrLiteral;
    }

    private boolean isInteger(String s) {
        if (s == null || s.isEmpty())
            return false;
        int i = 0;
        if (s.charAt(0) == '-' || s.charAt(0) == '+')
            i = 1;
        for (; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
                return false;
        }
        return true;
    }

    // ============================================================
    // =================== Parameter discovery ====================
    // ============================================================

    public void storeParamsInList(String payload) {
        // Regex to match ${...}
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(payload);

        while (matcher.find()) {
            params.add(matcher.group(1)); // inside ${...}
        }

        // Print ArrayList
        // System.out.println(params);
    }

    private static String generateString(String prefix, String c, int length) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = prefix.length(); i < length; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    // ============================================================
    // ========================= Intents ==========================
    // ============================================================

    public static final class Intent {
        public static final String PARTNER_USER_NAME_VALID = "partner_user_name_valid";
        public static final String PARTNER_USER_NAME_INVALID = "partner_user_name_invalid";
        public static final String PARTNER_USER_ID_VALID = "2134243234342324";
        public static final String PARTNER_USER_ID_INVALID = "partner_user_id_invalid";

        // For empty we used a single space earlier; often better to emit "".
        // Keeping as-is to preserve your existing semantics.
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

    // ============================================================
    // ============== Heuristic Param → Intent mapping ============
    // ============================================================

    public static String mapToIntent(String paramName) {
        if (paramName == null)
            return Intent.EMPTY;

        // // Canonicalize once: lowercase, non-alnum -> space
        // String c = paramName.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();

        // Insert spaces before capitals, then lowercase
        String c = paramName.replaceAll("([a-z])([A-Z])", "$1 $2")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .trim();

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
            return Intent.zero; // << fix: zero should map to "0"
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
        boolean hasSpaces = containsAny(c, new String[] { "spaces", "space", "withspaces", "with spaces" });

        boolean isValid = containsWord(c, "valid") || containsWord(c, "existing") || containsWord(c, "correct")
                || containsWord(c, "right");
        boolean isInvalid = containsWord(c, "invalid") || containsWord(c, "non existent") || containsWord(c, "bad")
                || containsWord(c, "not found");

        // specific → generic
        if (hasPartner && hasUser && hasName && isValid)
            return Intent.PARTNER_USER_NAME_VALID;
        if (hasPartner && hasUser && hasName && isInvalid)
            return Intent.PARTNER_USER_NAME_INVALID;
        if (hasPartner && hasUser && hasId && isValid)
            return Intent.PARTNER_USER_ID_VALID;
        if (hasPartner && hasUser && hasId && isInvalid)
            return Intent.PARTNER_USER_ID_INVALID;

        // (questionable mapping in original code; keeping close to original)
        if (hasUser && hasName && isValid)
            return Intent.PARTNER_USER_ID_INVALID;
        if (hasUser && hasName && hasSpaces && isValid)
            return "valid user name with spaces";

        if (hasId && isValid)
            return Intent.DEFAULT_ID_VALID;

        // Add parameter name in a file so that it can be added to conditions in next
        // run
        return writeLine(paramName);
    }

    // Writes a new line to the file (only if not duplicate), appending after first
    // write
    public static synchronized String writeLine(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "skipped empty parameter";
        }
        if (writtenWords.contains(text)) {
            return "parameter already exists in file";
        }

        // ensure parent directory exists
        try {
            File f = new File(FILE_PATH);
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // first write -> overwrite; subsequent writes -> append
            boolean append = cleared; // false on first call, true afterwards
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(f, append))) {
                writer.write(text);
                writer.newLine();
            }

            // mark that we've done the first write and remember this parameter
            cleared = true;
            writtenWords.add(text);
            return "parameter added to file";
        } catch (IOException e) {
            e.printStackTrace();
            return "error writing parameter to file";
        }
    }

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
}