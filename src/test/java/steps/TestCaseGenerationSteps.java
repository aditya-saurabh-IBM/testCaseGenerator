package steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import steps.AIGeneratedCases.RowLike;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generates API test cases (happy/edge/negative) from an OpenAPI/Swagger spec
 * and outputs Markdown + Excel/CSV. Java 11 compatible.
 *
 * Per latest requirements:
 * - Parameters are appended to the endpoint as query string (?a=1&b=2) in the
 * Test case column.
 * - Payload column contains ONLY the request body JSON (no query/path/headers).
 * - Expected result column contains ONLY the response code.
 * - Deeply resolves $ref and prefers examples/defaults/enums from the spec.
 */
public class TestCaseGenerationSteps {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private boolean aiEnabled = false;

    // Tuning knobs
    private static final int MAX_ARRAY_EXAMPLES = 3;
    private static final int STRING_MAXLEN = 10;
    private static final int ARRAY_MAX_ITEMS_FOR_OVERSIZE = 5;

    private enum SampleType {
        HAPPY, EDGE, NEGATIVE
    }

    private static final class Row {
        final String method, name, payload, expected;

        Row(String m, String n, String p, String e) {
            method = m;
            name = n;
            payload = p;
            expected = e;
        }
    }

    // ------ state carried across steps ------
    private String specPathOrUrl;
    private OpenAPI openAPI;
    private final List<Row> rows = new ArrayList<>();
    private String lastPrintedMarkdown;

    // ----------------- Gherkin steps -----------------

    @Given("an OpenAPI spec at {string}")
    public void givenAnOpenApiSpecAt(String pathOrUrl) {
        this.specPathOrUrl = pathOrUrl;
        this.openAPI = new OpenAPIV3Parser().read(pathOrUrl);
        if (this.openAPI == null) {
            throw new IllegalStateException("Failed to parse OpenAPI spec: " + pathOrUrl);
        }
    }

    @When("I generate the API test matrix")
    public void iGenerateTheApiTestMatrix() {
        rows.clear();

        if (openAPI.getPaths() == null)
            return;

        for (Map.Entry<String, PathItem> pe : openAPI.getPaths().entrySet()) {
            String path = pe.getKey();
            PathItem pi = pe.getValue();
            Map<PathItem.HttpMethod, Operation> ops = pi.readOperationsMap();
            if (ops == null)
                continue;

            for (Map.Entry<PathItem.HttpMethod, Operation> oe : ops.entrySet()) {
                PathItem.HttpMethod http = oe.getKey();
                Operation op = oe.getValue();
                String opId = (op.getOperationId() != null && !op.getOperationId().isEmpty())
                        ? op.getOperationId()
                        : (http + " " + path);

                List<Parameter> params = (op.getParameters() != null) ? op.getParameters() : Collections.emptyList();
                RequestBody reqBody = op.getRequestBody();
                Schema<?> bodySchema = extractRequestBodySchema(reqBody);

                // HAPPY
                ObjectNode happy = buildPayload(path, http, params, reqBody, bodySchema, SampleType.HAPPY);
                String happyUrl = buildUrl(path, happy.get("path"), happy.get("query"));
                rows.add(new Row(http.toString(),
                        opId + " - happy path - " + happyUrl,
                        bodyOnlyJson(happy),
                        chooseExpected(op.getResponses(), true)));

                // EDGE (one-at-a-time)
                for (ObjectNode edge : edgeCasePayloads(path, http, params, reqBody, bodySchema)) {
                    String edgeUrl = buildUrl(path, edge.get("path"), edge.get("query"));
                    rows.add(new Row(http.toString(),
                            opId + " - edge case - " + edgeUrl,
                            bodyOnlyJson(edge),
                            chooseExpected(op.getResponses(), true)));
                }

                // NEGATIVE
                for (ObjectNode neg : negativePayloads(path, http, params, reqBody, bodySchema)) {
                    String negUrl = buildUrl(path, neg.get("path"), neg.get("query"));
                    rows.add(new Row(http.toString(),
                            opId + " - negative case - " + negUrl,
                            bodyOnlyJson(neg),
                            chooseExpected(op.getResponses(), false)));
                }
            }
        }
    }

    @When("I generate the API test matrix from AI")
    public void iGenerateTheApiTestMatrixFromAI() {
        rows.clear();

        if (openAPI == null || openAPI.getPaths() == null)
            return;

        for (Map.Entry<String, PathItem> pe : openAPI.getPaths().entrySet()) {
            String path = pe.getKey();
            PathItem pi = pe.getValue();
            Map<PathItem.HttpMethod, Operation> ops = pi.readOperationsMap();
            if (ops == null)
                continue;

            for (Map.Entry<PathItem.HttpMethod, Operation> oe : ops.entrySet()) {
                PathItem.HttpMethod http = oe.getKey();
                Operation op = oe.getValue();
                String opId = (op.getOperationId() != null && !op.getOperationId().isEmpty())
                        ? op.getOperationId()
                        : (http + " " + path);

                List<Parameter> params = (op.getParameters() != null)
                        ? op.getParameters()
                        : java.util.Collections.emptyList();
                RequestBody reqBody = op.getRequestBody();
                Schema<?> bodySchema = extractRequestBodySchema(reqBody);

                // ------------------ Deterministic cases ------------------

                // HAPPY
                ObjectNode happy = buildPayload(path, http, params, reqBody, bodySchema, SampleType.HAPPY);
                String happyUrl = buildUrl(path, happy.get("path"), happy.get("query"));
                rows.add(new Row(
                        http.toString(),
                        opId + " - happy path - " + happyUrl,
                        bodyOnlyJson(happy),
                        chooseExpected(op.getResponses(), true)));

                // EDGE (one-at-a-time)
                for (ObjectNode edge : edgeCasePayloads(path, http, params, reqBody, bodySchema)) {
                    String edgeUrl = buildUrl(path, edge.get("path"), edge.get("query"));
                    rows.add(new Row(
                            http.toString(),
                            opId + " - edge case - " + edgeUrl,
                            bodyOnlyJson(edge),
                            chooseExpected(op.getResponses(), true)));
                }

                // NEGATIVE
                for (ObjectNode neg : negativePayloads(path, http, params, reqBody, bodySchema)) {
                    String negUrl = buildUrl(path, neg.get("path"), neg.get("query"));
                    rows.add(new Row(
                            http.toString(),
                            opId + " - negative case - " + negUrl,
                            bodyOnlyJson(neg),
                            chooseExpected(op.getResponses(), false)));
                }

                // ------------------ AI-suggested cases ------------------
                try {
                    AIGeneratedCases ai = new AIGeneratedCases();
                    java.util.List<RowLike> aiRows = ai.suggestForOperation(
                            path,
                            http.toString(),
                            op,
                            params,
                            reqBody,
                            op.getResponses());

                    // Merge with simple de-dupe (method + name + payload)
                    for (RowLike rr : aiRows) {
                        boolean exists = rows.stream().anyMatch(r -> java.util.Objects.equals(r.method, rr.method) &&
                                java.util.Objects.equals(r.name, rr.name) &&
                                java.util.Objects.equals(r.payload, rr.payload));
                        if (!exists) {
                            // normalize AI payload before storing
                            String normalized = normalizeAiPayload(rr.payload);
                            rows.add(new Row(rr.method, rr.name, normalized, rr.expected));
                        }
                    }
                } catch (Throwable t) {
                    // If AI is unavailable or returns malformed output, ignore and keep
                    // deterministic rows.
                }
            }
        }
    }

    // -------------------- Expected results --------------------
    // Now returns ONLY the response code string.
    private String chooseExpected(ApiResponses responses, boolean positive) {
        if (responses == null || responses.isEmpty()) {
            return positive ? "2xx" : "4xx/5xx";
        }

        List<String> pos = Arrays.asList("200", "201", "202", "204");
        List<String> neg = Arrays.asList("400", "401", "403", "404", "409", "422", "429", "500");

        if (positive) {
            for (String k : pos)
                if (responses.get(k) != null)
                    return k;
            for (String k : responses.keySet())
                if (k.startsWith("2"))
                    return k;
        } else {
            for (String k : neg)
                if (responses.get(k) != null)
                    return k;
            for (String k : responses.keySet())
                if (k.startsWith("4") || k.startsWith("5"))
                    return k;
        }
        return responses.keySet().iterator().next(); // fallback
    }

    private static String safeDesc(ApiResponse r) {
        return (r.getDescription() == null ? "" : r.getDescription());
    }

    private Schema<?> extractResponseSchema(ApiResponse resp) {
        if (resp == null || resp.getContent() == null)
            return null;
        MediaType mt = resp.getContent().get("application/json");
        if (mt != null && mt.getSchema() != null)
            return resolveRefIfAny(mt.getSchema());
        for (MediaType any : resp.getContent().values()) {
            if (any.getSchema() != null)
                return resolveRefIfAny(any.getSchema());
        }
        return null;
    }

    @Then("I print the matrix as a Markdown table")
    public void iPrintTheMatrixAsAMarkdownTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("| HTTP method | Testcase name | Payload | Expected result |\n");
        sb.append("|---|---|---|---|\n");
        for (Row r : rows) {
            sb.append("| ").append(esc(r.method))
                    .append(" | ").append(esc(r.name))
                    .append(" | ").append(esc(r.payload))
                    .append(" | ").append(esc(r.expected))
                    .append(" |\n");
        }
        lastPrintedMarkdown = sb.toString();
        System.out.println(lastPrintedMarkdown);
    }

    @Then("I write the matrix to {string}")
    public void iWriteTheMatrixTo(String outputPath) throws IOException {
        if (outputPath == null || outputPath.trim().isEmpty() || "-".equals(outputPath.trim())) {
            return;
        }
        if (lastPrintedMarkdown == null) {
            iPrintTheMatrixAsAMarkdownTable();
        }
        Path p = Path.of(outputPath);
        if (p.getParent() != null)
            Files.createDirectories(p.getParent());
        Files.write(p, lastPrintedMarkdown.getBytes());
        System.out.println("Wrote matrix to: " + p.toAbsolutePath());
    }

    // --------------- helpers: payload & schema sampling ---------------

    private static Schema<?> extractRequestBodySchema(RequestBody rb) {
        if (rb == null || rb.getContent() == null)
            return null;
        MediaType mtJson = rb.getContent().get("application/json");
        if (mtJson != null && mtJson.getSchema() != null)
            return mtJson.getSchema();
        for (MediaType mt : rb.getContent().values()) {
            if (mt.getSchema() != null)
                return mt.getSchema();
        }
        return null;
    }

    // Prefer example/default/enum at MediaType (request body) level
    private JsonNode extractRequestBodyExample(RequestBody rb) {
        if (rb == null || rb.getContent() == null)
            return null;
        MediaType mt = rb.getContent().get("application/json");
        if (mt == null) {
            for (MediaType any : rb.getContent().values()) {
                mt = any;
                break;
            }
        }
        if (mt == null)
            return null;

        if (mt.getExample() != null) {
            try {
                if (mt.getExample() instanceof String)
                    return MAPPER.readTree((String) mt.getExample());
                else
                    return MAPPER.valueToTree(mt.getExample());
            } catch (Exception ignore) {
            }
        }
        if (mt.getExamples() != null && !mt.getExamples().isEmpty()) {
            var ex = mt.getExamples().values().iterator().next();
            if (ex != null && ex.getValue() != null) {
                try {
                    if (ex.getValue() instanceof String)
                        return MAPPER.readTree((String) ex.getValue());
                    else
                        return MAPPER.valueToTree(ex.getValue());
                } catch (Exception ignore) {
                }
            }
        }
        return null;
    }

    // Resolve $ref using components
    private static Schema<?> resolveRefIfAnyStatic(OpenAPI openAPI, Schema<?> schema) {
        if (schema == null)
            return null;
        String ref = schema.get$ref();
        if (ref == null || openAPI == null || openAPI.getComponents() == null
                || openAPI.getComponents().getSchemas() == null) {
            return schema;
        }
        String name = ref.substring(ref.lastIndexOf('/') + 1);
        Schema<?> resolved = openAPI.getComponents().getSchemas().get(name);
        return (resolved != null) ? resolved : schema;
    }

    // instance wrapper
    private Schema<?> resolveRefIfAny(Schema<?> schema) {
        return resolveRefIfAnyStatic(openAPI, schema);
    }

    // Example-aware value from a schema (example/default/enum)
    private JsonNode valueFromSchemaHints(Schema<?> s) {
        if (s == null)
            return null;
        Object ex = s.getExample();
        if (ex != null)
            return MAPPER.valueToTree(ex);
        Object def = s.getDefault();
        if (def != null)
            return MAPPER.valueToTree(def);
        if (s.getEnum() != null && !s.getEnum().isEmpty())
            return MAPPER.valueToTree(s.getEnum().get(0));
        return null;
    }

    // Deep sampler that resolves $ref at every level and respects
    // examples/defaults/enums and shallow allOf.
    @SuppressWarnings("unchecked")
    private JsonNode sampleForSchemaDeep(Schema<?> schema, SampleType type, boolean oversize) {
        if (schema == null)
            return NullNode.getInstance();

        Schema<?> s = resolveRefIfAny(schema);
        if (s == null)
            return NullNode.getInstance();

        // Prefer hints if positive cases
        JsonNode hinted = valueFromSchemaHints(s);
        if (hinted != null && type != SampleType.NEGATIVE)
            return hinted;

        // Merge shallow allOf
        if (s.getAllOf() != null && !s.getAllOf().isEmpty()) {
            ObjectNode merged = MAPPER.createObjectNode();
            for (Schema<?> part : s.getAllOf()) {
                JsonNode node = sampleForSchemaDeep(part, type, oversize);
                if (node != null && node.isObject())
                    merged.setAll((ObjectNode) node);
            }
            return merged;
        }

        // Arrays
        if (s instanceof ArraySchema) {
            ArraySchema as = (ArraySchema) s;
            Schema<?> item = as.getItems();
            ArrayNode arr = MAPPER.createArrayNode();

            if (type == SampleType.EDGE) {
                return arr; // empty array
            }
            if (type == SampleType.NEGATIVE) {
                int n = (as.getMaxItems() != null) ? as.getMaxItems() + 1 : ARRAY_MAX_ITEMS_FOR_OVERSIZE;
                for (int i = 0; i < n; i++)
                    arr.add(sampleForSchemaDeep(item, SampleType.HAPPY, false));
                return arr;
            }
            int cnt = Math.max(1, Math.min(MAX_ARRAY_EXAMPLES, 2));
            for (int i = 0; i < cnt; i++)
                arr.add(sampleForSchemaDeep(item, SampleType.HAPPY, false));
            return arr;
        }

        String typeName = s.getType();
        if (typeName == null && s.getEnum() != null && !s.getEnum().isEmpty()) {
            Object first = s.getEnum().get(0);
            if (first instanceof Integer)
                typeName = "integer";
            else if (first instanceof Number)
                typeName = "number";
            else if (first instanceof Boolean)
                typeName = "boolean";
            else
                typeName = "string";
        }

        // Strings
        if ("string".equals(typeName)) {
            String format = s.getFormat();
            if (type == SampleType.EDGE) {
                Integer min = s.getMinLength();
                int len = (min != null) ? min : 0;
                return TextNode.valueOf(repeat('a', len));
            }
            if (type == SampleType.NEGATIVE) {
                Integer max = s.getMaxLength();
                int len = (max != null) ? (max + 5) : (STRING_MAXLEN + 5);
                return TextNode.valueOf(repeat('x', len));
            }
            if ("date".equals(format))
                return TextNode.valueOf("2024-01-01");
            if ("date-time".equals(format))
                return TextNode.valueOf("2024-01-01T12:00:00Z");
            if ("email".equals(format))
                return TextNode.valueOf("user@example.com");
            if ("uuid".equals(format))
                return TextNode.valueOf("123e4567-e89b-12d3-a456-426614174000");
            if ("uri".equals(format))
                return TextNode.valueOf("https://example.com/resource");
            return TextNode.valueOf("sample");
        }

        // Numbers / Integers
        if ("integer".equals(typeName) || "number".equals(typeName)) {
            java.math.BigDecimal minimum = s.getMinimum();
            java.math.BigDecimal maximum = s.getMaximum();
            if (type == SampleType.EDGE) {
                if (minimum != null)
                    return IntNode.valueOf(minimum.intValue());
                if (maximum != null)
                    return IntNode.valueOf(maximum.intValue());
                return IntNode.valueOf(0);
            }
            if (type == SampleType.NEGATIVE)
                return TextNode.valueOf("NaN");
            if (minimum != null) {
                int v = minimum.intValue() + 1;
                if (maximum != null && v > maximum.intValue())
                    v = minimum.intValue();
                return IntNode.valueOf(v);
            }
            return IntNode.valueOf(1);
        }

        // Boolean
        if ("boolean".equals(typeName)) {
            return BooleanNode.valueOf(type != SampleType.NEGATIVE);
        }

        // Objects
        if (isObjectSchema(s)) {
            ObjectNode obj = MAPPER.createObjectNode();
            Map<String, Schema> props = (s.getProperties() == null) ? Collections.emptyMap()
                    : (Map<String, Schema>) s.getProperties();
            for (Map.Entry<String, Schema> e : props.entrySet()) {
                obj.set(e.getKey(), sampleForSchemaDeep(e.getValue(), type, oversize));
            }
            return obj;
        }

        // Fallback
        return TextNode.valueOf("val");
    }

    private ObjectNode buildPayload(String path,
            PathItem.HttpMethod method,
            List<Parameter> params,
            RequestBody requestBody,
            Schema<?> bodySchema,
            SampleType type) {
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode query = MAPPER.createObjectNode();
        ObjectNode pathParams = MAPPER.createObjectNode();
        ObjectNode headers = MAPPER.createObjectNode();

        for (Parameter p : params) {
            Schema<?> ps = (p.getSchema() != null) ? p.getSchema() : new StringSchema();
            JsonNode value = sampleForSchemaDeep(ps, type, false);

            String in = p.getIn();
            if ("query".equals(in))
                query.set(p.getName(), value);
            else if ("path".equals(in))
                pathParams.set(p.getName(), value);
            else if ("header".equals(in))
                headers.set(p.getName(), value);
        }

        // Always include these buckets
        root.set("query", query);
        root.set("path", pathParams);
        root.set("headers", headers);

        // Body: prefer requestBody-level example first
        if (bodySchema != null || requestBody != null) {
            JsonNode example = extractRequestBodyExample(requestBody);
            if (example != null) {
                root.set("body", example);
            } else if (bodySchema != null) {
                root.set("body", sampleForSchemaDeep(bodySchema, type, type == SampleType.NEGATIVE));
            } else {
                root.set("body", MAPPER.createObjectNode());
            }
        } else {
            root.set("body", MAPPER.createObjectNode());
        }

        return root;
    }

    private String buildUrl(String rawPath, JsonNode pathVars, JsonNode queryObj) {
        String resolvedPath = substitutePathParams(rawPath, pathVars);
        String qs = buildQueryString(queryObj);
        return qs.isEmpty() ? resolvedPath : resolvedPath + "?" + qs;
    }

    private String substitutePathParams(String rawPath, JsonNode pathVars) {
        if (rawPath == null)
            return "";
        if (pathVars == null || !pathVars.isObject())
            return rawPath;
        String result = rawPath;
        Iterator<String> names = pathVars.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            String token = "{" + name + "}";
            String val = asScalarForUrl(((ObjectNode) pathVars).get(name));
            result = result.replace(token, urlEnc(val));
        }
        return result;
    }

    private String buildQueryString(JsonNode queryObj) {
        if (queryObj == null || !queryObj.isObject())
            return "";
        List<String> parts = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> it = ((ObjectNode) queryObj).fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            String key = e.getKey();
            JsonNode v = e.getValue();
            if (v.isArray()) {
                for (JsonNode item : v) {
                    parts.add(urlEnc(key) + "=" + urlEnc(asScalarForUrl(item)));
                }
            } else if (v.isObject()) {
                parts.add(urlEnc(key) + "=" + urlEnc(v.toString()));
            } else {
                parts.add(urlEnc(key) + "=" + urlEnc(asScalarForUrl(v)));
            }
        }
        return String.join("&", parts);
    }

    private String asScalarForUrl(JsonNode n) {
        if (n == null || n.isNull())
            return "";
        if (n.isTextual())
            return n.asText();
        if (n.isNumber())
            return n.numberValue().toString();
        if (n.isBoolean())
            return String.valueOf(n.booleanValue());
        return n.toString();
    }

    private String urlEnc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    // Body-only JSON for the payload column
    private String bodyOnlyJson(ObjectNode payloadRoot) {
        JsonNode body = payloadRoot.get("body");
        try {
            return MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(body == null ? MAPPER.createObjectNode() : body);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private List<ObjectNode> edgeCasePayloads(String path,
            PathItem.HttpMethod method,
            List<Parameter> params,
            RequestBody requestBody,
            Schema<?> bodySchema) {
        List<ObjectNode> list = new ArrayList<>();
        ObjectNode base = buildPayload(path, method, params, requestBody, bodySchema, SampleType.HAPPY);

        // tweak params one by one
        for (Parameter p : params) {
            ObjectNode clone = base.deepCopy();
            Schema<?> ps = (p.getSchema() != null) ? p.getSchema() : new StringSchema();
            JsonNode edge = sampleForSchemaDeep(ps, SampleType.EDGE, false);

            String in = p.getIn();
            if ("query".equals(in) && clone.has("query"))
                ((ObjectNode) clone.get("query")).set(p.getName(), edge);
            else if ("path".equals(in) && clone.has("path"))
                ((ObjectNode) clone.get("path")).set(p.getName(), edge);
            else if ("header".equals(in) && clone.has("headers"))
                ((ObjectNode) clone.get("headers")).set(p.getName(), edge);

            list.add(clone);
        }

        // edge body
        if (bodySchema != null) {
            ObjectNode clone = base.deepCopy();
            clone.set("body", sampleForSchemaDeep(bodySchema, SampleType.EDGE, false));
            list.add(clone);
        }
        return list;
    }

    private List<ObjectNode> negativePayloads(String path,
            PathItem.HttpMethod method,
            List<Parameter> params,
            RequestBody requestBody,
            Schema<?> bodySchema) {
        List<ObjectNode> list = new ArrayList<>();
        ObjectNode base = buildPayload(path, method, params, requestBody, bodySchema, SampleType.HAPPY);

        // Missing required params
        for (Parameter p : params) {
            if (Boolean.TRUE.equals(p.getRequired())) {
                ObjectNode clone = base.deepCopy();
                String in = p.getIn();
                if ("query".equals(in) && clone.has("query"))
                    ((ObjectNode) clone.get("query")).remove(p.getName());
                else if ("path".equals(in) && clone.has("path"))
                    ((ObjectNode) clone.get("path")).remove(p.getName());
                else if ("header".equals(in) && clone.has("headers"))
                    ((ObjectNode) clone.get("headers")).remove(p.getName());
                list.add(clone);
            }
        }

        // Invalid/oversize body
        if (bodySchema != null) {
            ObjectNode clone = base.deepCopy();
            clone.set("body", sampleForSchemaDeep(bodySchema, SampleType.NEGATIVE, true));
            list.add(clone);

            // Missing each required body field (object only, resolve first)
            if (isObjectSchema(resolveRefIfAny(bodySchema))) {
                Set<String> req = requiredFieldsResolved(bodySchema);
                for (String r : req) {
                    ObjectNode c = base.deepCopy();
                    if (c.has("body") && c.get("body").isObject()) {
                        ((ObjectNode) c.get("body")).remove(r);
                        list.add(c);
                    }
                }
            }
        }

        return list;
    }

    private static boolean isObjectSchema(Schema<?> s) {
        if (s == null)
            return false;
        if ("object".equals(s.getType()))
            return true;
        return s.getProperties() != null && !s.getProperties().isEmpty();
    }

    private Set<String> requiredFieldsResolved(Schema<?> schema) {
        Schema<?> s = resolveRefIfAny(schema);
        if (s == null)
            return Collections.emptySet();
        List<String> req = s.getRequired();
        return (req == null) ? Collections.emptySet() : new HashSet<>(req);
    }

    // ---------------- (legacy sampler kept, but no longer used for body/params)
    // ----------------
    @SuppressWarnings("unchecked")
    private static JsonNode sampleForSchema(Schema<?> schema, SampleType type, boolean oversize) {
        if (schema == null)
            return NullNode.getInstance();

        if (schema.get$ref() != null) {
            // legacy path emitted _ref; not used anymore for body/params
            ObjectNode ref = MAPPER.createObjectNode();
            String simple = schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
            ref.put("_ref", simple);
            return ref;
        }

        if (schema instanceof ArraySchema) {
            ArraySchema as = (ArraySchema) schema;
            Schema<?> item = as.getItems();
            ArrayNode arr = MAPPER.createArrayNode();

            if (type == SampleType.EDGE)
                return arr;
            if (type == SampleType.NEGATIVE) {
                int n = (as.getMaxItems() != null) ? as.getMaxItems() + 1 : ARRAY_MAX_ITEMS_FOR_OVERSIZE;
                for (int i = 0; i < n; i++)
                    arr.add(sampleForSchema(item, SampleType.HAPPY, false));
                return arr;
            }
            int cnt = Math.max(1, Math.min(MAX_ARRAY_EXAMPLES, 2));
            for (int i = 0; i < cnt; i++)
                arr.add(sampleForSchema(item, SampleType.HAPPY, false));
            return arr;
        }

        String typeName = schema.getType();
        if (typeName == null && schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            Object first = schema.getEnum().get(0);
            if (first instanceof Integer)
                typeName = "integer";
            else if (first instanceof Number)
                typeName = "number";
            else if (first instanceof Boolean)
                typeName = "boolean";
            else
                typeName = "string";
        }

        if ("string".equals(typeName)) {
            if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
                if (type == SampleType.NEGATIVE)
                    return TextNode.valueOf("__NOT_IN_ENUM__");
                return TextNode.valueOf(String.valueOf(schema.getEnum().get(0)));
            }
            Integer max = schema.getMaxLength();
            if (type == SampleType.EDGE) {
                Integer min = schema.getMinLength();
                int len = (min != null) ? min : 0;
                return TextNode.valueOf(repeat('a', len));
            }
            if (type == SampleType.NEGATIVE && (oversize || max != null)) {
                int len = (max != null) ? (max + 5) : (STRING_MAXLEN + 5);
                return TextNode.valueOf(repeat('x', len));
            }
            return TextNode.valueOf("sample");
        }

        if ("integer".equals(typeName) || "number".equals(typeName)) {
            java.math.BigDecimal minimum = schema.getMinimum();
            java.math.BigDecimal maximum = schema.getMaximum();

            if (type == SampleType.EDGE) {
                if (minimum != null)
                    return IntNode.valueOf(minimum.intValue());
                if (maximum != null)
                    return IntNode.valueOf(maximum.intValue());
                return IntNode.valueOf(0);
            }
            if (type == SampleType.NEGATIVE)
                return TextNode.valueOf("NaN");
            return IntNode.valueOf((minimum != null) ? minimum.intValue() + 1 : 1);
        }

        if ("boolean".equals(typeName)) {
            return BooleanNode.valueOf(type != SampleType.NEGATIVE);
        }

        if (isObjectSchema(schema)) {
            ObjectNode obj = MAPPER.createObjectNode();
            Map<String, Schema> props = Collections.emptyMap();
            if (schema.getProperties() != null) {
                try {
                    props = (Map<String, Schema>) schema.getProperties();
                } catch (ClassCastException ignored) {
                }
            }
            for (Map.Entry<String, Schema> e : props.entrySet()) {
                obj.set(e.getKey(), sampleForSchema(e.getValue(), type, oversize));
            }
            return obj;
        }

        return TextNode.valueOf("val");
    }

    private static String esc(String s) {
        if (s == null)
            return "";
        return s.replace("|", "\\|").replace("\n", "\\n");
    }

    private static String json(JsonNode n) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(n);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static String repeat(char ch, int len) {
        if (len <= 0)
            return "";
        return String.valueOf(ch).repeat(len);
    }

    // ---------------- Excel output ----------------

    @Then("I write the matrix to excel {string}")
    public void iWriteTheMatrixToExcel(String xlsxPath) throws Exception {
        if (xlsxPath == null || xlsxPath.trim().isEmpty() || "-".equals(xlsxPath.trim())) {
            return; // skip
        }
        writeRowsToExcel(xlsxPath, rows);
        System.out.println("Wrote Excel to: " + java.nio.file.Path.of(xlsxPath).toAbsolutePath());
    }

    private void writeRowsToExcel(String xlsxPath, java.util.List<Row> rows) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("API Test Cases");

            // Header style (bold)
            CellStyle headerStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            int r = 0;
            org.apache.poi.ss.usermodel.Row hr = sh.createRow(r++);
            createCell(hr, 0, "HTTP Method", headerStyle);
            createCell(hr, 1, "test case", headerStyle);
            createCell(hr, 2, "paylaod", headerStyle); // as requested spelling
            createCell(hr, 3, "expected result", headerStyle);

            // Body rows
            CellStyle wrap = wb.createCellStyle();
            wrap.setWrapText(true);

            for (Row row : rows) {
                org.apache.poi.ss.usermodel.Row er = sh.createRow(r++);
                createCell(er, 0, (row.method == null ? "" : row.method), null);
                createCell(er, 1, (row.name == null ? "" : row.name), null);
                String safePayload = sanitizePayloadForExcel(row.payload);
                createCell(er, 2, safePayload, wrap);
                createCell(er, 3, (row.expected == null ? "" : row.expected), null);
            }

            for (int c = 0; c <= 3; c++)
                sh.autoSizeColumn(c);

            java.nio.file.Path p = java.nio.file.Path.of(xlsxPath);
            if (p.getParent() != null)
                java.nio.file.Files.createDirectories(p.getParent());
            try (FileOutputStream fos = new FileOutputStream(p.toFile())) {
                wb.write(fos);
            }
        }
    }

    private void createCell(org.apache.poi.ss.usermodel.Row row, int col, String val, CellStyle style) {
        Cell cell = row.createCell(col, CellType.STRING);
        cell.setCellValue(val == null ? "" : val);
        if (style != null)
            cell.setCellStyle(style);
    }

    @Given("AI suggestions are enabled")
    public void aiSuggestionsAreEnabled() {
        this.aiEnabled = true;
    }

    // Excel cells max 32_767 chars. Also strip markdown fences and fix newlines.
    private static String sanitizePayloadForExcel(String s) {
        if (s == null)
            return "{}";
        String x = s.trim();

        // Remove markdown code fences if the AI added them
        if (x.startsWith("```")) {
            int idx = x.indexOf('\n');
            if (idx > 0)
                x = x.substring(idx + 1);
            if (x.endsWith("```"))
                x = x.substring(0, x.length() - 3);
            x = x.trim();
        }

        // If AI returned a quoted JSON string, unquote once
        if ((x.startsWith("\"") && x.endsWith("\"")) || (x.startsWith("'") && x.endsWith("'"))) {
            x = x.substring(1, x.length() - 1);
        }

        // Replace CRLF/CR with LF so POI inserts proper line breaks
        x = x.replace("\r\n", "\n").replace("\r", "\n");

        // Excel single-cell hard limit
        final int EXCEL_CELL_LIMIT = 32767;
        if (x.length() > EXCEL_CELL_LIMIT) {
            x = x.substring(0, EXCEL_CELL_LIMIT - 20) + "...(truncated)";
        }
        return x;
    }

    // --- Normalize AI payloads into proper pretty JSON (safe for CSV/Excel) ---
    private static String normalizeAiPayload(String s) {
        if (s == null)
            return "{}";
        String x = s.trim();

        // Strip markdown fences ```json ... ```
        if (x.startsWith("```")) {
            int nl = x.indexOf('\n');
            if (nl > 0)
                x = x.substring(nl + 1);
            if (x.endsWith("```"))
                x = x.substring(0, x.length() - 3);
            x = x.trim();
        }

        // Try parse directly (maybe already JSON)
        try {
            JsonNode n = MAPPER.readTree(x);
            if (n.isTextual()) {
                String inner = n.asText();
                try {
                    JsonNode j = MAPPER.readTree(inner);
                    return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(j);
                } catch (Exception ignored) {
                    return inner.replace("\r\n", "\n").replace("\r", "\n");
                }
            }
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(n);
        } catch (Exception ignore) {
        }

        // Remove outer quotes and fix doubled quotes "" -> "
        if ((x.startsWith("\"") && x.endsWith("\"")) || (x.startsWith("'") && x.endsWith("'"))) {
            x = x.substring(1, x.length() - 1);
        }
        if (x.contains("\"\""))
            x = x.replace("\"\"", "\"");

        // Convert literal \n to real newlines
        x = x.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\r\n", "\n").replace("\r", "\n");

        // Try JSON again after cleanup
        try {
            JsonNode j = MAPPER.readTree(x);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(j);
        } catch (Exception ignore) {
        }

        return x; // fallback raw
    }

    // --- CSV output (Apache Commons CSV expected in pom) ---
    @Then("I write the matrix to csv {string}")
    public void iWriteTheMatrixToCsv(String csvPath) {
        try {
            if (csvPath == null || csvPath.trim().isEmpty() || "-".equals(csvPath.trim())) {
                System.out.println("[CSV] Skipped: empty path argument");
                return;
            }
            writeRowsToCsv(csvPath, rows);
            System.out.println("[CSV] Wrote CSV to: " + java.nio.file.Path.of(csvPath).toAbsolutePath());
        } catch (Exception e) {
            System.err.println("[CSV] Failed to write CSV: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }

    private void writeRowsToCsv(String csvPath, java.util.List<Row> rows) throws Exception {
        java.nio.file.Path p = java.nio.file.Path.of(csvPath).toAbsolutePath();
        if (p.getParent() != null)
            java.nio.file.Files.createDirectories(p.getParent());

        try (java.io.Writer w = java.nio.file.Files.newBufferedWriter(p, java.nio.charset.StandardCharsets.UTF_8)) {
            org.apache.commons.csv.CSVFormat fmt = org.apache.commons.csv.CSVFormat.DEFAULT.builder()
                    .setHeader("HTTP Method", "Test Case", "Payload", "Expected Result")
                    .setQuoteMode(org.apache.commons.csv.QuoteMode.ALL)
                    .build();

            try (org.apache.commons.csv.CSVPrinter csv = new org.apache.commons.csv.CSVPrinter(w, fmt)) {
                if (rows != null) {
                    for (Row r : rows) {
                        String method = (r == null || r.method == null) ? "" : r.method;
                        String testcase = (r == null || r.name == null) ? "" : r.name;

                        String payloadRaw = (r == null || r.payload == null) ? "{}" : r.payload;
                        String payload = normalizeAiPayload(payloadRaw);

                        String expected = (r == null || r.expected == null) ? "" : r.expected;

                        csv.printRecord(method, testcase, payload, expected);
                    }
                }
            }
        }
    }
}