package steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.restassured.response.Response;
import utils.GenericMethods;
import utils.LoadEnvironment;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVPrinter;
import org.testng.Assert;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TC_GenerationAndExecution {

    Response responseFromPOST;
    Response responseFromGET;
    Response responseFromPUT;
    Response responseFromDELETE;
    Response response;

    GenericMethods genericMethods = new GenericMethods();
    ScenarioHooks hooks = new ScenarioHooks();

    ArrayList<String> params = new ArrayList<>(); // collected if needed elsewhere
    Map<String, String> b2bParamKeyValues = new HashMap<>();

    private static final String FILE_PATH = "src/test/java/resources/parametersNotHandled/parameters.txt";
    private static final String OUTPUT_CSV = "src/test/java/resources/GeneratedTestCases/modifiedTestCases.csv";

    private static boolean cleared = false; // ensures clearing happens only once
    private static final Set<String> writtenWords = new HashSet<>(); // track unique entries

    // -------- precompiled patterns for speed & clarity --------
    private static final Pattern CAMEL_SPLIT = Pattern.compile("([a-z])([A-Z])");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern LEN_TOKEN = Pattern.compile("^(\\d{2,4})(?:char|chars|character|characters)$");
    private static final Pattern PLACEHOLDER = Pattern.compile("^\\$\\{([^}]+)\\}$");
    private static final Pattern PLACEHOLDER_ANYWHERE = Pattern.compile("\\$\\{([^}]+)\\}");

    // ---- Pair holder to return both resolved endpoint & payload (plus the map)
    // ----
    public static final class ResolvedPair {
        public final String endpoint;
        public final String payload;
        public final Map<String, Object> resolved; // optional debug

        public ResolvedPair(String endpoint, String payload, Map<String, Object> resolved) {
            this.endpoint = endpoint;
            this.payload = payload;
            this.resolved = resolved;
        }
    }

    // =================================================================================
    // STEP 1: Replace placeholders and WRITE modifiedTestCases.csv (no execution
    // here)
    // =================================================================================
    @Given("Replace parameters in file {string} and write modified CSV")
    public void replaceParametersAndWriteModified(String csvPath) throws Exception {
        // Ensure parent directory exists
        File outFile = new File(OUTPUT_CSV);
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        // Build explicit CSV formats to be robust with commas/newlines in payloads
        CSVFormat inFmt = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreSurroundingSpaces()
                .withTrim();

        CSVFormat outFmt = CSVFormat.DEFAULT
                .withRecordSeparator(System.lineSeparator());

        int total = 0, written = 0, fallback = 0;
        List<String> fallbackReasons = new ArrayList<>();

        try (Reader in = new FileReader(csvPath);
                CSVPrinter out = new CSVPrinter(new FileWriter(outFile, false), outFmt)) {

            // Write header (Step-2 expects these exact names)
            out.printRecord("HTTP Method", "test case name", "endpoint", "payload", "expected status");

            Iterable<CSVRecord> recs = inFmt.parse(in);

            for (CSVRecord r : recs) {
                total++;
                // Read raw values first (so we can always write *something*)
                String methodRaw = safeGet(r, "HTTP method");
                String nameRaw = safeGet(r, "test case name");
                String endpointRaw = safeGet(r, "endpoint");
                String payloadRaw = safeGet(r, "payload");
                String expectedRaw = safeGet(r, "expected status code"); // keep as STRING in step-1

                // Prepend base URL now (even if replacement fails we still keep it consistent)
                String epCombinedRaw = LoadEnvironment.tenant + "/b2b" + endpointRaw;

                String methodOut = methodRaw;
                String nameOut = nameRaw;
                String endpointOut = epCombinedRaw;
                String payloadOut = payloadRaw;
                String expectedOut = expectedRaw;

                try {
                    // Resolve endpoint & payload TOGETHER so placeholders are consistent
                    ResolvedPair rp = replaceParameters(epCombinedRaw, payloadRaw);
                    endpointOut = rp.endpoint;
                    payloadOut = rp.payload;

                    // DO NOT parse expected here—keep string; step-2 will parse when executing
                    // Just log for visibility
                    System.out.println("---- Prepare Row " + total + ": " + nameRaw + " ----");
                    System.out.println(" HTTP Method  : " + methodRaw);
                    System.out.println(" Endpoint     : " + endpointOut);
                    System.out.println(" Payload      : " + payloadOut);
                    System.out.println(" Expected     : " + expectedOut);

                } catch (Exception ex) {
                    // On any error, still write a row using *raw* values and note the reason
                    fallback++;
                    String reason = "[FALLBACK] row " + total + " (" + nameRaw + "): " + ex.getMessage();
                    System.err.println(reason);
                    fallbackReasons.add(reason);
                    // endpointOut/payloadOut already default to raw versions above
                }

                // Always write a row
                out.printRecord(
                        nullToEmpty(methodOut).toUpperCase(Locale.ROOT),
                        nullToEmpty(nameOut),
                        nullToEmpty(endpointOut),
                        nullToEmpty(payloadOut),
                        nullToEmpty(expectedOut));
                written++;
            }

            out.flush();
        }

        System.out.println("✅ Modified CSV written to: " + OUTPUT_CSV);
        System.out.println(
                "Rows in source: " + total + " | Rows written: " + written + " | Fallback/Errors: " + fallback);
        if (!fallbackReasons.isEmpty()) {
            System.out.println("---- Fallback details (first 10) ----");
            for (int i = 0; i < Math.min(10, fallbackReasons.size()); i++) {
                System.out.println(fallbackReasons.get(i));
            }
        }
    }

    // Safe cell access (avoid NPEs or MissingColumnException)
    private static String safeGet(CSVRecord r, String header) {
        try {
            String s = r.get(header);
            return s == null ? "" : s;
        } catch (Exception e) {
            return "";
        }
    }

    private static String nullToEmpty(String s) {
        return (s == null) ? "" : s;
    }

    // ================================================================================
    // STEP 2: Read modifiedTestCases.csv and EXECUTE the test cases (one by one)
    // ================================================================================
    @Given("Execute modified test cases from file")
    public void executeModifiedTestCases() throws Exception {
        File inFile = new File(OUTPUT_CSV);
        if (!inFile.exists() || inFile.length() == 0) {
            throw new IllegalStateException("Modified test cases file not found or empty: " + OUTPUT_CSV);
        }

        try (Reader in = new FileReader(inFile)) {
            Iterable<CSVRecord> recs = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(in);

            int rowNum = 0;
            List<String> failures = new ArrayList<>();

            for (CSVRecord r : recs) {
                rowNum++;
                try {
                    String method = r.get("HTTP Method").trim().toUpperCase(Locale.ROOT);
                    String name = r.get("test case name");
                    String resolvedEP = r.get("endpoint");
                    String payload = r.get("payload");
                    int expected = Integer.parseInt(r.get("expected status"));

                    System.out.println("---- Executing Row " + rowNum + ": " + name + " ----");
                    System.out.println(" HTTP Method  : " + method);
                    System.out.println(" Endpoint     : " + resolvedEP);
                    System.out.println(" Payload      : " + payload);
                    System.out.println(" Expected     : " + expected);

                    // Make the call
                    switch (method.toLowerCase()) {
                        case "post":
                            response = genericMethods.B2B_POST_Request(resolvedEP, payload);
                            break;
                        case "patch":
                            response = genericMethods.B2B_PATCH_Request(resolvedEP, payload);
                            break;
                        case "put":
                            response = genericMethods.B2B_PUT_Request(resolvedEP, payload);
                            break;
                        case "get":
                            response = genericMethods.B2B_GET_Request(resolvedEP);
                            break;
                        case "delete":
                            response = genericMethods.B2B_DELETE_Request(resolvedEP);
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
                    }

                    int actual = response.statusCode();
                    System.out.println(" Actual Status : " + actual);

                    if (actual != expected) {
                        String msg = "[FAIL] Row " + rowNum + " (" + name + ") Expected=" + expected + " | Actual="
                                + actual;
                        System.err.println(msg);
                        failures.add(msg);
                    } else {
                        System.out.println("[PASS] Row " + rowNum + " (" + name + ")");
                    }
                } catch (Exception e) {
                    String msg = "[ERROR] Exec Row " + rowNum + " failed: " + e.getMessage();
                    System.err.println(msg);
                    e.printStackTrace();
                    failures.add(msg);
                }
            }

            // Summarize at end (do not stop mid-run)
            if (!failures.isEmpty()) {
                System.err.println("======== EXECUTION SUMMARY: FAILURES ========");
                for (String f : failures)
                    System.err.println(f);
                System.err.println("=============================================");
                // Optionally fail the scenario:
                // Assert.fail("Some test cases failed:\n" + String.join("\n", failures));
            } else {
                System.out.println("✅ All modified test cases passed.");
            }
        }
    }

    // ============================================================
    // ============== Typed placeholder replacement ===============
    // ============================================================

    /**
     * Preferred: resolve endpoint & payload together for consistent values.
     */
    public ResolvedPair replaceParameters(String endpoint, String payload) {
        // Collect params from both strings
        LinkedHashSet<String> all = new LinkedHashSet<>();
        all.addAll(extractParams(endpoint));
        all.addAll(extractParams(payload));

        // Resolve each param ONCE
        Map<String, Object> resolved = resolveAllOnce(all);

        // Endpoint is plain text
        String resolvedEndpoint = replaceParametersInText(endpoint, resolved);

        // Payload may be JSON
        String resolvedPayload = payload;
        if (resolvedPayload != null && !resolvedPayload.trim().isEmpty()) {
            String trimmed = resolvedPayload.trim();
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                    (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                resolvedPayload = replaceParametersInJson(resolvedPayload, resolved);
            } else {
                resolvedPayload = replaceParametersInText(resolvedPayload, resolved);
            }
        }

        return new ResolvedPair(resolvedEndpoint, resolvedPayload, resolved);
    }

    /**
     * Backward-compatible single-input variant (resolves only that string).
     */
    public String replaceParameters(String input) {
        if (input == null || input.trim().isEmpty())
            return input;

        LinkedHashSet<String> all = extractParams(input);
        Map<String, Object> resolved = resolveAllOnce(all);

        String trimmed = input.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return replaceParametersInJson(input, resolved);
        }
        return replaceParametersInText(input, resolved);
    }

    private static LinkedHashSet<String> extractParams(String s) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (s == null || s.isEmpty())
            return out;
        Matcher m = PLACEHOLDER_ANYWHERE.matcher(s);
        while (m.find())
            out.add(m.group(1));
        return out;
    }

    private Map<String, Object> resolveAllOnce(Set<String> params) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String p : params) {
            Object v = resolveParamValue(p);
            map.put(p, v);
        }
        return map;
    }

    // JSON-aware replacement using pre-resolved map
    private String replaceParametersInJson(String json, Map<String, Object> resolved) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode replaced = replaceJsonNodePlaceholders(root, resolved);
            return mapper.writeValueAsString(replaced);
        } catch (Exception e) {
            System.err.println("[WARN] JSON parse failed; falling back to text: " + e.getMessage());
            return replaceParametersInText(json, resolved);
        }
    }

    private JsonNode replaceJsonNodePlaceholders(JsonNode node, Map<String, Object> resolved) {
        ObjectMapper mapper = new ObjectMapper();

        if (node.isObject()) {
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                String field = it.next();
                JsonNode child = node.get(field);
                ((com.fasterxml.jackson.databind.node.ObjectNode) node)
                        .replace(field, replaceJsonNodePlaceholders(child, resolved));
            }
            return node;
        }

        if (node.isArray()) {
            com.fasterxml.jackson.databind.node.ArrayNode arr = (com.fasterxml.jackson.databind.node.ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                arr.set(i, replaceJsonNodePlaceholders(arr.get(i), resolved));
            }
            return arr;
        }

        if (node.isTextual()) {
            String text = node.asText();

            // Entire value is a single placeholder?
            Matcher mExact = PLACEHOLDER.matcher(text);
            if (mExact.matches()) {
                String paramName = mExact.group(1);
                Object value = resolved.get(paramName);
                if (value == null)
                    return mapper.nullNode();
                if (value instanceof Boolean)
                    return mapper.getNodeFactory().booleanNode((Boolean) value);
                if (value instanceof Integer)
                    return mapper.getNodeFactory().numberNode((Integer) value);
                return mapper.getNodeFactory().textNode(String.valueOf(value));
            }

            // Embedded placeholders inside a string
            StringBuffer sb = new StringBuffer();
            Matcher mAny = PLACEHOLDER_ANYWHERE.matcher(text);
            while (mAny.find()) {
                String param = mAny.group(1);
                Object value = resolved.getOrDefault(param, "${" + param + "}");
                mAny.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(value)));
            }
            mAny.appendTail(sb);
            return mapper.getNodeFactory().textNode(sb.toString());
        }

        return node; // non-text scalars left as-is
    }

    // Plain-text replacement using pre-resolved map
    private String replaceParametersInText(String input, Map<String, Object> resolved) {
        if (input == null)
            return null;
        Matcher m = PLACEHOLDER_ANYWHERE.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String param = m.group(1);
            Object value = resolved.getOrDefault(param, "${" + param + "}");
            m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(value)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // ============================================================
    // ============== Value resolution (typed) ====================
    // ============================================================

    /**
     * Convert a parameter name (from ${...}) to a **typed** value.
     * - Uses mapToIntent(...) to classify
     * - Converts to Integer/Boolean/null/String accordingly
     * - If UNKNOWN, logs to file and returns the **original placeholder** so you
     * can fix rules later.
     */
    private Object resolveParamValue(String paramName) {
        String intentOrLiteral = mapToIntent(paramName);
        if (intentOrLiteral == null)
            return null;

        // Handle unknowns: log & keep placeholder unchanged
        if ("UNKNOWN".equals(intentOrLiteral)) {
            writeLine(paramName); // log for future rule update
            return "${" + paramName + "}";
        }

        // Simple boolean name hints (expand if needed)
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
        public static final String PARTNER_USER_ID_VALID = "2134243234342324"; // adjust if you later fetch live
        public static final String PARTNER_USER_ID_INVALID = "2134243234342324";

        public static final String EMPTY = " ";
        public static final String NULLL = "null";
        public static final String LONG = "long";
        public static final String DEFAULT_ID_VALID = "default_id_valid";

        public static final String negative = "-1";
        public static final String zero = "0";
        public static final String largeNumber = String.valueOf(Integer.MAX_VALUE);

        public static final String char128 = generateString("B2B_API_Auto_", "A", 128);
        public static final String char127 = generateString("B2B_API_Auto_", "A", 127);
        public static final String char129 = generateString("B2B_API_Auto_", "A", 129);
        public static final String char254 = generateString("B2B_API_Auto_", "A", 254);
        public static final String char255 = generateString("B2B_API_Auto_", "A", 255);
        public static final String char256 = generateString("B2B_API_Auto_", "A", 256);

        public static final String specialCharsString = "@#$%^??";
        public static final String leadingSpaceString = "     leading";
        public static final String trailingSpaceString = "trailing     ";

        // ====== channels ======
        public static final String STATUS_VALID = "ACTIVE"; // adjust to your enum
        public static final String INVALID_ENUM = "__INVALID__"; // guaranteed invalid
        public static final String SCOPE_VALID = "PARTNER"; // adjust to your enum
        public static final String POSITIVE_NUMBER = "1"; // parsed to int if needed
        public static final String SORT_VALID = "name,asc"; // common sort syntax
        public static final String Q_VALID = "name:Test"; // example query
        public static final String DESCRIPTION_VALID = "Auto-generated description";
        public static final String CHANNEL_TYPE_VALID = "AS2"; // adjust to your enum
        public static final String STATE_VALID = "Active"; // adjust to your enum
        public static final String PARTNER_SPECIFIC_VALID = "true"; // parsed to boolean in JSON
        public static final String WHITE_SPACE_ONLY = "     "; // 5 spaces
        public static final String ID_INVALID = "invalid-id"; // definitely invalid format

    }

    // ============================================================
    // ============== Heuristic Param → Intent mapping ============
    // ============================================================

    // Tokenize once and use a Set for O(1) membership checks
    private static Set<String> tokensOf(String raw) {
        if (raw == null)
            return Collections.emptySet();

        // 1) Insert space between camelCase boundaries
        String s = CAMEL_SPLIT.matcher(raw).replaceAll("$1 $2");

        // 2) Lowercase FIRST, then strip non-alphanumerics (pattern expects lowercase)
        s = s.toLowerCase(Locale.ROOT);
        s = NON_ALNUM.matcher(s).replaceAll(" ").trim();

        if (s.isEmpty())
            return Collections.emptySet();

        // 3) Split and collect; use LinkedHashSet to preserve order for
        // debugging/printing
        String[] parts = s.split("\\s+");
        Set<String> set = new LinkedHashSet<>(parts.length * 2);
        for (String p : parts) {
            if (!p.isEmpty())
                set.add(p);
        }
        return set;
    }

    private static String detectLengthToken(Set<String> tokens) {
        for (String t : tokens) {
            Matcher m = LEN_TOKEN.matcher(t);
            if (m.matches())
                return m.group(1); // e.g., "256"
            // also support a plain numeric token
            boolean allDigits = true;
            for (int i = 0; i < t.length(); i++) {
                char c = t.charAt(i);
                if (c < '0' || c > '9') {
                    allDigits = false;
                    break;
                }
            }
            if (allDigits)
                return t;
        }
        return null;
    }

    public String mapToIntent(String paramName) {
        Set<String> t = tokensOf(paramName);
        if (t.isEmpty())
            return "UNKNOWN";

        // quick edges
        if (t.contains("empty"))
            return Intent.EMPTY;
        if (t.contains("null"))
            return Intent.NULLL;
        if (t.contains("long"))
            return Intent.LONG;
        if (t.contains("negative"))
            return Intent.negative;
        if (t.contains("zero"))
            return Intent.zero;
        if (t.contains("leading"))
            return Intent.leadingSpaceString;
        if (t.contains("trailing"))
            return Intent.trailingSpaceString;

        // general length detection like 127chars / 256characters / plain "256"
        String len = detectLengthToken(t);
        if (len != null) {
            switch (len) {
                case "127":
                    return Intent.char127;
                case "128":
                    return Intent.char128;
                case "129":
                    return Intent.char129;
                case "254":
                    return Intent.char254;
                case "255":
                    return Intent.char255;
                case "256":
                    return Intent.char256;
                default:
                    break;
            }
        }

        // special character strings
        if (t.contains("special") || t.contains("specialcharacters") ||
                t.contains("specialchar") || t.contains("specialcharacter")) {
            return Intent.specialCharsString;
        }
        System.out.println("Paramter is :: " + t);

        boolean hasPartner = t.contains("partner") || t.contains("tradingpartner") || t.contains("prtnr");
        boolean hasUser = t.contains("user") || t.contains("usr") || t.contains("member");
        boolean hasName = t.contains("name") || t.contains("username") || (t.contains("full") && t.contains("name"));
        boolean hasId = t.contains("id") || t.contains("userid") || t.contains("user_id") || t.contains("identifier");
        boolean hasSpaces = t.contains("with") && t.contains("spaces");
        boolean isDuplicate = t.contains("another") || t.contains("duplicate") || t.contains("exist")
                || t.contains("exists");
        boolean hasPassword = t.contains("password") || t.contains("pwd");

        // channels
        boolean hasStatus = t.contains("status");
        boolean hasScope = t.contains("scope");
        boolean hasSort = t.contains("sort") || t.contains("orderby") || t.contains("order");
        boolean hasQ = t.contains("q") || t.contains("query");
        boolean hasDescription = t.contains("description") || t.contains("desc");
        boolean hasChannelType = t.contains("channeltype") || (t.contains("channel") && t.contains("type"));
        boolean hasState = t.contains("state");
        boolean hasPartnerSpec = (t.contains("partner") && t.contains("specific")) || t.contains("partnerspecific");
        boolean isWhiteSpaceOnly = (t.contains("white") && t.contains("space") && t.contains("only"))
                || t.contains("whitespaceonly");
        boolean isPositiveNumber = t.contains("positive") && (t.contains("number") || t.contains("num"));
        boolean hasEnum = t.contains("enum");
        boolean hasChannel = t.contains("channel") || t.contains("chnl");

        boolean isValid = t.contains("valid") || t.contains("existing") || t.contains("correct") || t.contains("right");
        boolean isInvalid = t.contains("invalid") || (t.contains("non") && t.contains("existent")) ||
                t.contains("bad") || (t.contains("not") && t.contains("found"));

        // specific → generic
        // if (hasPartner && hasUser && hasName && isValid) {
        // response = genericMethods.B2B_GET_Request(LoadEnvironment.tenant +
        // "/b2b/partner-users");
        // return response.jsonPath().getString("['partner-users'][0].user_name");
        // }

        // if (hasPartner && hasUser && hasName && isInvalid) {
        // return Intent.PARTNER_USER_NAME_INVALID;
        // }
        // if (hasPartner && hasUser && hasId && isValid) {
        // response = genericMethods.B2B_GET_Request(LoadEnvironment.tenant +
        // "/b2b/partner-users");
        // return response.jsonPath().getString("['partner-users'][0].id");
        // }
        // if (hasPartner && hasUser && hasId && isInvalid) {
        // return Intent.PARTNER_USER_ID_INVALID;
        // }

        // // use valid username for generic "user name valid" cases
        // if (hasUser && hasName && isValid) {
        // response = genericMethods.B2B_GET_Request(LoadEnvironment.tenant +
        // "/b2b/partner-users");
        // return response.jsonPath().getString("['partner-users'][0].user_name");
        // }

        // if (hasUser && hasName && hasSpaces && isValid) {
        // response = genericMethods.B2B_GET_Request(LoadEnvironment.tenant +
        // "/b2b/partner-users");
        // String val = response.jsonPath().getString("['partner-users'][0].user_name");
        // return (val != null && val.length() > 1) ? val.substring(0, 1) + " " +
        // val.substring(1) : "A B";
        // }

        // if (hasPartner && hasUser && hasId && isValid)
        // return Intent.DEFAULT_ID_VALID;

        // if (hasUser && hasName && isDuplicate) {
        // response = genericMethods.B2B_GET_Request(LoadEnvironment.tenant +
        // "/b2b/partner-users");
        // return response.jsonPath().getString("['partner-users'][0].user_name");
        // }

        // if (hasName && hasSpaces) {
        // String nameVal = "B2B_TCG_" + UUID.randomUUID().toString().replace("-",
        // "").substring(0, 6);
        // return (nameVal.length() > 1) ? nameVal.substring(0, 1) + " " +
        // nameVal.substring(1) : "A B";
        // }

        // if (hasName && isDuplicate) {
        // response = genericMethods.B2B_GET_Request(LoadEnvironment.tenant +
        // "/b2b/partner-users");
        // return response.jsonPath().getString("['partner-users'][0].user_name");
        // }

        // if (hasName && isValid) {
        // response = genericMethods.B2B_GET_Request(LoadEnvironment.tenant +
        // "/b2b/partner-users");
        // return response.jsonPath().getString("['partner-users'][0].user_name");
        // }

        // if (hasPassword && isValid) {
        // return "Password@123";
        // }

        // channels

        if (hasChannel && hasId && isValid) {
            response = genericMethods.B2B_GET_Request(LoadEnvironment.tenant + "/b2b/channels");
            String id = response.jsonPath().getString("configurations[0].id");

            return id;
        }
        if (hasChannel && hasName && isValid) {
            response = genericMethods.B2B_GET_Request(LoadEnvironment.tenant + "/b2b/channels");
            String name = response.jsonPath().getString("configurations[0].name");

            return name;
        }

        if (hasStatus && isValid)
            return Intent.STATUS_VALID; // statusValidValue
        if (hasEnum && isInvalid)
            return Intent.INVALID_ENUM; // invalidEnumValue
        if (hasScope && isValid)
            return Intent.SCOPE_VALID; // scopeValidValue
        if (isPositiveNumber)
            return Intent.POSITIVE_NUMBER; // positiveNumber
        if (hasSort && isValid)
            return Intent.SORT_VALID; // sortValidValue
        if (hasQ && isValid)
            return Intent.Q_VALID; // qValidValue
        if (hasDescription && isValid)
            return Intent.DESCRIPTION_VALID; // descriptionValidValue
        if (hasChannelType && isValid)
            return Intent.CHANNEL_TYPE_VALID; // channeltypeValidValue
        if (hasState && isValid)
            return Intent.STATE_VALID; // stateValidValue
        if (hasPartnerSpec && isValid)
            return Intent.PARTNER_SPECIFIC_VALID; // partnerSpecificValidValue
        if (isWhiteSpaceOnly)
            return Intent.WHITE_SPACE_ONLY; // whiteSpaceOnly
        if (hasChannel && hasId && isInvalid)
            return Intent.ID_INVALID; // idInvalidValue

        return "UNKNOWN"; // caller logs and preserves placeholder
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

            cleared = true;
            writtenWords.add(text);
            return "parameter added to file";
        } catch (IOException e) {
            e.printStackTrace();
            return "error writing parameter to file";
        }
    }

    // (kept for backward compatibility—no longer used by mapToIntent)
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
