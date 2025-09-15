package steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds a strong prompt for Ollama (via langchain4j) to generate additional
 * test cases.
 * Ensures the LLM returns STRICT JSON array with: method, name, payload (body
 * only), expected (code only).
 */
public class AIGeneratedCases {

    public static final class RowLike {
        public String method;
        public String name;
        public String payload;
        public String expected;

        public RowLike() {
        }

        public RowLike(String m, String n, String p, String e) {
            this.method = m;
            this.name = n;
            this.payload = p;
            this.expected = e;
        }
    }

    private static final ObjectMapper M = new ObjectMapper();

    private final ChatLanguageModel model;

    public AIGeneratedCases() {
        String baseUrl = System.getProperty("OLLAMA_BASE_URL",
                System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"));
        String modelName = System.getProperty("OLLAMA_MODEL",
                System.getenv().getOrDefault("OLLAMA_MODEL", "mistral")); // or "llama3"
        this.model = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }

    /**
     * Build an instruction-rich prompt and return AI-generated rows (parsed).
     */
    public java.util.List<RowLike> suggestForOperation(
            String path,
            String httpMethod,
            Operation op,
            java.util.List<Parameter> params,
            RequestBody requestBody,
            ApiResponses responses) {

        String paramsDesc = describeParameters(params);
        Schema<?> bodySchema = extractBodySchema(requestBody);
        String bodyDesc = describeBodySchema(bodySchema);
        String bodyHints = deriveFieldHints(bodySchema); // realistic data by field name + constraints
        String responseCodes = (responses == null) ? "" : String.join(", ", responses.keySet());

        // ---------- PROMPT ----------
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert API test designer.\n")
                .append("Generate additional test cases for the API operation below.\n\n")

                .append("API Operation:\n")
                .append("- Method: ").append(httpMethod).append("\n")
                .append("- Path: ").append(path).append("\n")
                .append("- Parameters (query/path/header):\n")
                .append(paramsDesc.isEmpty() ? "  (none)\n" : paramsDesc).append("\n")
                .append("- Request Body Schema (JSON):\n")
                .append(bodyDesc.isEmpty() ? "  (no body)\n" : indent(bodyDesc, "  ")).append("\n")
                .append("- Known Response Codes: ").append(responseCodes.isEmpty() ? "(unspecified)" : responseCodes)
                .append("\n\n")

                // How to output (very important for our pipeline)
                .append("Output format (MANDATORY):\n")
                .append("- Respond with a JSON array ONLY. No prose, no markdown.\n")
                .append("- Each array item must be an object with EXACT keys: ")
                .append("[\"method\", \"name\", \"payload\", \"expected\"].\n")
                .append("  * \"method\": must be \"").append(httpMethod).append("\".\n")
                .append("  * \"name\": a descriptive testcase name that appends the resolved URL\n")
                .append("            including parameters (e.g., \"... - ").append(path)
                .append(paramsDesc.isEmpty() ? "" : "?{query-params}").append("\").\n")
                .append("  * \"payload\": strictly the JSON request body (no query/path/headers). Use realistic values.\n")
                .append("  * \"expected\": response code ONLY (e.g., \"200\", \"400\").\n\n")

                // What kinds of cases we want
                .append("Categories to include:\n")
                .append("- Happy path cases.\n")
                .append("- Edge cases: min/max boundaries, empty arrays/objects where allowed.\n")
                .append("- Negative cases: invalid types, missing required fields, oversize values.\n\n")

                // Parameter handling
                .append("Parameters handling:\n")
                .append("- Put ALL parameters into the URL (after the path). Do NOT put parameters in payload.\n")
                .append("- Build URL like: /endpoint?key1=value1&key2=value2. Repeat key for arrays.\n")
                .append("- For path variables {id} etc., substitute realistic values.\n\n")

                // Body handling + hints
                .append("Body handling:\n")
                .append("- The \"payload\" must contain ONLY the JSON request body. If there is no body, use {}.\n")
                .append("- Use field names to produce realistic values. For guidance:\n")
                .append(indent(bodyHints.isEmpty() ? "(no body hints)\n" : bodyHints, "  ")).append("\n")

                // Additional considerations per user's request
                .append("Additional Considerations:\n")
                .append("- Empty Strings and Null Values: include cases containing \"\" and null.\n")
                .append("- Maximum Allowed Lengths: if max length is known (e.g., 255), test at 254, 255, 256.\n")
                .append("- Special Characters and Whitespace: include strings with symbols, spaces, and escape sequences.\n")
                .append("- String Boundary Values (if a field has min=6, max=10) test:\n")
                .append("   * 5 chars (just below min): \"abcde\"\n")
                .append("   * 6 chars (at min): \"abcdef\"\n")
                .append("   * 7 chars (just above min): \"abcdefg\"\n")
                .append("   * 9 chars (just below max): \"abcdefghi\"\n")
                .append("   * 10 chars (at max): \"abcdefghij\"\n")
                .append("   * 11 chars (just above max): \"abcdefghijk\"\n\n")

                // Concrete constraints from schema (min/max length, patterns, enums)
                .append("Schema-derived constraints (if any, apply them):\n")
                .append(indent(describeConstraints(bodySchema), "  ")).append("\n")

                // Response code guidance
                .append("Expected codes guidance:\n")
                .append("- Use 2xx for valid cases when defined. Use 4xx/5xx for invalid.\n")
                .append("- If the spec lists specific codes, prefer them. Otherwise, infer common ones (200/201, 400/422).\n\n")

                // Count / uniqueness
                .append("Quantity & uniqueness:\n")
                .append("- Provide 10-18 varied test cases total. Avoid duplicates.\n\n")

                // FINAL strictness
                .append("Return ONLY the JSON array. No extra text, no markdown.\n");

        String completion = model.generate(prompt.toString());
        String json = extractJsonArray(completion);

        try {
            JsonNode arr = M.readTree(json);
            if (!arr.isArray()) {
                // If the model didn't follow instructions, fallback: wrap single object or
                // return empty.
                if (arr.isObject()) {
                    RowLike r = toRowLike(arr);
                    return (r == null) ? Collections.emptyList() : Collections.singletonList(r);
                }
                return Collections.emptyList();
            }
            java.util.List<RowLike> out = new ArrayList<>();
            for (JsonNode n : arr) {
                RowLike r = toRowLike(n);
                if (r != null)
                    out.add(r);
            }
            return out;
        } catch (Exception e) {
            // If parse fails, return empty to avoid breaking deterministic flow
            return Collections.emptyList();
        }
    }

    // ------------------------- helpers -------------------------

    private static Schema<?> extractBodySchema(RequestBody rb) {
        if (rb == null || rb.getContent() == null)
            return null;
        MediaType mt = rb.getContent().get("application/json");
        if (mt != null && mt.getSchema() != null)
            return mt.getSchema();
        for (MediaType any : rb.getContent().values()) {
            if (any.getSchema() != null)
                return any.getSchema();
        }
        return null;
    }

    /** Human-ish parameter description for the prompt. */
    private static String describeParameters(java.util.List<Parameter> params) {
        if (params == null || params.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (Parameter p : params) {
            Schema<?> s = p.getSchema();
            String t = (s == null) ? "string" : typeOf(s);
            sb.append("  - ").append(p.getName())
                    .append(" (in=").append(p.getIn())
                    .append(", type=").append(t);
            if (Boolean.TRUE.equals(p.getRequired()))
                sb.append(", required");
            if (s != null) {
                if (s.getEnum() != null && !s.getEnum().isEmpty()) {
                    sb.append(", enum=").append(s.getEnum());
                }
                if (s.getMinLength() != null)
                    sb.append(", minLen=").append(s.getMinLength());
                if (s.getMaxLength() != null)
                    sb.append(", maxLen=").append(s.getMaxLength());
                if (s.getMinimum() != null)
                    sb.append(", min=").append(s.getMinimum());
                if (s.getMaximum() != null)
                    sb.append(", max=").append(s.getMaximum());
                if (s.getPattern() != null)
                    sb.append(", pattern=").append(s.getPattern());
                if (s.getFormat() != null)
                    sb.append(", format=").append(s.getFormat());
            }
            sb.append(")\n");
        }
        return sb.toString();
    }

    /** Compact textual description of a body schema. */
    @SuppressWarnings("unchecked")
    private static String describeBodySchema(Schema<?> schema) {
        if (schema == null)
            return "";
        StringBuilder sb = new StringBuilder();
        String type = typeOf(schema);

        if (schema instanceof ArraySchema) {
            sb.append("array of ").append(typeOf(((ArraySchema) schema).getItems())).append("\n");
        } else if ("object".equals(type)) {
            sb.append("{\n");
            Map<String, Schema> props = (schema.getProperties() == null) ? Collections.emptyMap()
                    : (Map<String, Schema>) schema.getProperties();
            List<String> required = (schema.getRequired() == null) ? Collections.emptyList() : schema.getRequired();
            for (Map.Entry<String, Schema> e : props.entrySet()) {
                Schema<?> ps = e.getValue();
                sb.append("  ").append(e.getKey()).append(": ").append(typeOf(ps));
                if (required.contains(e.getKey()))
                    sb.append(" (required)");
                if (ps.getFormat() != null)
                    sb.append(", format=").append(ps.getFormat());
                if (ps.getEnum() != null && !ps.getEnum().isEmpty())
                    sb.append(", enum=").append(ps.getEnum());
                if (ps.getMinLength() != null)
                    sb.append(", minLen=").append(ps.getMinLength());
                if (ps.getMaxLength() != null)
                    sb.append(", maxLen=").append(ps.getMaxLength());
                if (ps.getMinimum() != null)
                    sb.append(", min=").append(ps.getMinimum());
                if (ps.getMaximum() != null)
                    sb.append(", max=").append(ps.getMaximum());
                if (ps.getPattern() != null)
                    sb.append(", pattern=").append(ps.getPattern());
                sb.append("\n");
            }
            sb.append("}\n");
        } else {
            sb.append(type).append("\n");
        }
        return sb.toString();
    }

    /** Build hints from field names to encourage realistic values. */
    @SuppressWarnings("unchecked")
    private static String deriveFieldHints(Schema<?> schema) {
        if (schema == null)
            return "";
        StringBuilder sb = new StringBuilder();

        if (schema instanceof ObjectSchema || "object".equals(schema.getType()) || schema.getProperties() != null) {
            Map<String, Schema> props = (schema.getProperties() == null) ? Collections.emptyMap()
                    : (Map<String, Schema>) schema.getProperties();
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, Schema> e : props.entrySet()) {
                String name = e.getKey();
                Schema<?> s = e.getValue();
                String hint = guessValueByName(name, s);
                if (!hint.isEmpty())
                    lines.add(" - " + name + ": " + hint);
            }
            if (!lines.isEmpty())
                sb.append("Suggested realistic values by field:\n")
                        .append(lines.stream().collect(Collectors.joining("\n")))
                        .append("\n");
        }

        // array item hints
        if (schema instanceof ArraySchema) {
            sb.append("Array items: ").append(typeOf(((ArraySchema) schema).getItems())).append("\n");
        }
        return sb.toString();
    }

    private static String guessValueByName(String name, Schema<?> s) {
        String n = name.toLowerCase(Locale.ROOT);
        String t = typeOf(s);
        String fmt = (s != null) ? s.getFormat() : null;

        if ("email".equals(fmt) || n.contains("email"))
            return "use realistic email like \"user@example.com\"";
        if ("uuid".equals(fmt) || n.contains("uuid") || n.endsWith("id"))
            return "use UUID like \"123e4567-e89b-12d3-a456-426614174000\"";
        if ("date-time".equals(fmt) || n.contains("timestamp"))
            return "use ISO date-time like \"2024-01-01T12:00:00Z\"";
        if ("date".equals(fmt) || n.contains("date"))
            return "use ISO date like \"2024-01-01\"";
        if (n.contains("phone") || n.contains("mobile"))
            return "use phone like \"+1-202-555-0142\"";
        if (n.contains("amount") || n.contains("price") || "number".equals(t))
            return "use numeric like 123.45";
        if (n.contains("url") || "uri".equals(fmt))
            return "use URL like \"https://example.com\"";
        if (n.contains("zip") || n.contains("postal"))
            return "use code like \"94043\"";
        if (n.contains("code"))
            return "use short code like \"ABC123\"";
        if (n.contains("name"))
            return "use human name like \"Alice Smith\"";
        if (n.contains("address"))
            return "use address like \"221B Baker Street\"";
        if (n.contains("password") || n.contains("secret"))
            return "use strong pass like \"P@ssw0rd!234\"";
        return ""; // no special hint
    }

    /** Enumerate constraints for boundary cases. */
    @SuppressWarnings("unchecked")
    private static String describeConstraints(Schema<?> schema) {
        if (schema == null)
            return "(none)";
        StringBuilder sb = new StringBuilder();
        if (schema instanceof ArraySchema) {
            ArraySchema as = (ArraySchema) schema;
            if (as.getMinItems() != null)
                sb.append("array minItems=").append(as.getMinItems()).append("\n");
            if (as.getMaxItems() != null)
                sb.append("array maxItems=").append(as.getMaxItems()).append("\n");
            sb.append("items type=").append(typeOf(as.getItems())).append("\n");
            return sb.toString();
        }
        if ("object".equals(typeOf(schema)) || schema.getProperties() != null) {
            Map<String, Schema> props = (schema.getProperties() == null) ? Collections.emptyMap()
                    : (Map<String, Schema>) schema.getProperties();
            List<String> required = (schema.getRequired() == null) ? Collections.emptyList() : schema.getRequired();
            if (!required.isEmpty())
                sb.append("required: ").append(required).append("\n");
            for (Map.Entry<String, Schema> e : props.entrySet()) {
                Schema<?> ps = e.getValue();
                String nm = e.getKey();
                List<String> bits = new ArrayList<>();
                if (ps.getMinLength() != null)
                    bits.add("minLen=" + ps.getMinLength());
                if (ps.getMaxLength() != null)
                    bits.add("maxLen=" + ps.getMaxLength());
                if (ps.getMinimum() != null)
                    bits.add("min=" + ps.getMinimum());
                if (ps.getMaximum() != null)
                    bits.add("max=" + ps.getMaximum());
                if (ps.getPattern() != null)
                    bits.add("pattern=" + ps.getPattern());
                if (ps.getEnum() != null && !ps.getEnum().isEmpty())
                    bits.add("enum=" + ps.getEnum());
                if (!bits.isEmpty())
                    sb.append(nm).append(": ").append(String.join(", ", bits)).append("\n");
            }
            return sb.toString().isEmpty() ? "(none)" : sb.toString();
        }
        // primitives
        List<String> bits = new ArrayList<>();
        if (schema.getMinLength() != null)
            bits.add("minLen=" + schema.getMinLength());
        if (schema.getMaxLength() != null)
            bits.add("maxLen=" + schema.getMaxLength());
        if (schema.getMinimum() != null)
            bits.add("min=" + schema.getMinimum());
        if (schema.getMaximum() != null)
            bits.add("max=" + schema.getMaximum());
        if (schema.getPattern() != null)
            bits.add("pattern=" + schema.getPattern());
        if (schema.getEnum() != null && !schema.getEnum().isEmpty())
            bits.add("enum=" + schema.getEnum());
        return bits.isEmpty() ? "(none)" : String.join(", ", bits);
    }

    private static String typeOf(Schema<?> s) {
        if (s == null)
            return "object";
        if (s instanceof ArraySchema)
            return "array";
        String t = s.getType();
        return t == null ? "object" : t;
    }

    /** Extract a JSON array from raw model completion (handles fenced code). */
    private static String extractJsonArray(String raw) {
        if (raw == null)
            return "[]";
        String s = raw.trim();
        // strip fences ```json ... ```
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0)
                s = s.substring(nl + 1);
            if (s.endsWith("```"))
                s = s.substring(0, s.length() - 3);
            s = s.trim();
        }
        // find first '[' and last ']'
        int i = s.indexOf('[');
        int j = s.lastIndexOf(']');
        if (i >= 0 && j >= i)
            return s.substring(i, j + 1);
        // if it's a single object, wrap it
        if (s.startsWith("{") && s.endsWith("}"))
            return "[" + s + "]";
        return "[]";
    }

    private static RowLike toRowLike(JsonNode n) {
        if (n == null || !n.isObject())
            return null;
        ObjectNode o = (ObjectNode) n;

        String method = textOrEmpty(o.get("method"));
        String name = textOrEmpty(o.get("name"));
        String expected = textOrEmpty(o.get("expected"));

        // payload node might be object or string; normalize to string
        String payload;
        JsonNode p = o.get("payload");
        if (p == null || p.isNull()) {
            payload = "{}";
        } else if (p.isTextual()) {
            payload = p.asText();
        } else {
            try {
                payload = M.writerWithDefaultPrettyPrinter().writeValueAsString(p);
            } catch (Exception e) {
                payload = p.toString();
            }
        }
        return new RowLike(method, name, payload, expected);
    }

    private static String textOrEmpty(JsonNode n) {
        return (n == null || n.isNull()) ? "" : n.asText();
    }

    private static String indent(String s, String pad) {
        if (s == null || s.isEmpty())
            return "";
        String[] lines = s.split("\\r?\\n");
        StringBuilder out = new StringBuilder();
        for (String line : lines)
            out.append(pad).append(line).append("\n");
        return out.toString();
    }
}