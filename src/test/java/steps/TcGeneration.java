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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.*;
import java.util.*;

public class TcGeneration {

    Response responseFromPOST;
    Response responseFromGET;
    Response responseFromPUT;
    Response responseFromDELETE;
    Response response;

    GenericMethods genericMethods = new GenericMethods();
    ScenarioHooks hooks = new ScenarioHooks();

    ArrayList<String> params = new ArrayList<String>(); // collected if needed elsewhere
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

    //// ============ CSV Runner ============

    @Given("Iterate each row and replace the parameters in file {string}")
    public void iterateEachRowAndRunTheTestCaseOfFile(String csvPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Ensure parent directory exists
        File outFile = new File(OUTPUT_CSV);
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        // OVERWRITE file each run (clear), then always write header once
        try (Reader in = new FileReader(csvPath);
                CSVPrinter out = new CSVPrinter(new FileWriter(outFile, false), CSVFormat.DEFAULT)) {

            // Always write header because we just overwrote the file
            out.printRecord("HTTP Method", "test case name", "endpoint", "payload", "expected status");

            Iterable<CSVRecord> recs = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);

            int rowNum = 0;
            for (CSVRecord r : recs) {
                rowNum++;
                try {
                    String method = r.get("HTTP method").trim().toUpperCase(Locale.ROOT);
                    String name = r.get("test case name");

                    // Endpoint
                    String ep = LoadEnvironment.tenant + "/b2b" + r.get("endpoint");
                    storeParamsInList(ep);
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

                    switch (method.toLowerCase()) {
                        case "post": {
                            response = genericMethods.B2B_POST_Request(resolvedEP, payload);
                            break;
                        }
                        case "patch": {
                            response = genericMethods.B2B_PATCH_Request(resolvedEP, payload);
                            break;
                        }
                        case "put": {
                            response = genericMethods.B2B_PUT_Request(resolvedEP, payload);
                            break;
                        }
                        case "get": {
                            response = genericMethods.B2B_GET_Request(resolvedEP);
                            break;
                        }
                        case "delete": {
                            response = genericMethods.B2B_DELETE_Request(resolvedEP);
                            break;
                        }

                    }
                    System.out
                            .println("Status code :: " + response.then().extract().response().getStatusCode());

                    // Write to the output CSV for each row
                    out.printRecord(method, name, resolvedEP, payload, String.valueOf(expected));
                } catch (Exception e) {
                    System.err.println("[ERROR] Row " + rowNum + " failed with exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            out.flush();
        }
    }

    // ============================================================
    // ============== Typed placeholder replacement ===============
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
            System.err.println("[WARN] JSON parse failed; falling back to text: " + e.getMessage());
            return replaceParametersInText(json);
        }
    }

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
                // Inside strings, stringify
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
        public static final String PARTNER_USER_ID_VALID = "2134243234342324"; // adjust if you later fetch live
        public static final String PARTNER_USER_ID_INVALID = "partner_user_id_invalid";

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

        public static final String specialCharsString = "@#$%^??||";
        public static final String leadingSpaceString = "     leading";
        public static final String trailingSpaceString = "trailing     ";
    }

    // ============================================================
    // ============== Heuristic Param → Intent mapping ============
    // ============================================================

    // Tokenize once and use a Set for O(1) membership checks
    private static Set<String> tokensOf(String raw) {
        if (raw == null)
            return Collections.emptySet();
        String s = CAMEL_SPLIT.matcher(raw).replaceAll("$1 $2");
        s = NON_ALNUM.matcher(s).replaceAll(" ").toLowerCase().trim();
        if (s.isEmpty())
            return Collections.emptySet();
        String[] parts = s.split("\\s+");
        HashSet<String> set = new HashSet<>(parts.length * 2);
        for (String p : parts)
            set.add(p);
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

    public static String mapToIntent(String paramName) {
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
                    /* ignore other numbers */ break;
            }
        }

        // special character strings
        if (t.contains("special") || t.contains("specialcharacters") ||
                t.contains("specialchar") || t.contains("specialcharacter")) {
            return Intent.specialCharsString;
        }

        boolean hasPartner = t.contains("partner") || t.contains("tradingpartner") || t.contains("prtnr");
        boolean hasUser = t.contains("user") || t.contains("usr") || t.contains("member");
        boolean hasName = t.contains("name") || t.contains("username") || (t.contains("full") && t.contains("name"));
        boolean hasId = t.contains("id") || t.contains("userid") || t.contains("user_id") || t.contains("identifier");
        boolean hasSpaces = t.contains("with") && t.contains("spaces");

        boolean isValid = t.contains("valid") || t.contains("existing") || t.contains("correct") || t.contains("right");
        boolean isInvalid = t.contains("invalid") || (t.contains("non") && t.contains("existent")) ||
                t.contains("bad") || (t.contains("not") && t.contains("found"));

        // specific → generic
        if (hasPartner && hasUser && hasName && isValid)
            return Intent.PARTNER_USER_NAME_VALID;
        if (hasPartner && hasUser && hasName && isInvalid)
            return Intent.PARTNER_USER_NAME_INVALID;
        if (hasPartner && hasUser && hasId && isValid)
            return Intent.PARTNER_USER_ID_VALID;
        if (hasPartner && hasUser && hasId && isInvalid)
            return Intent.PARTNER_USER_ID_INVALID;

        // (legacy mapping retained—consider revisiting)
        if (hasUser && hasName && isValid)
            return Intent.PARTNER_USER_ID_INVALID;
        if (hasUser && hasName && hasSpaces && isValid)
            return "valid user name with spaces";

        if (hasId && isValid)
            return Intent.DEFAULT_ID_VALID;

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