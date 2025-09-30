# 🧪 TestCaseGenerator

## Overview
**TestCaseGenerator** is an automated test framework built with **Java, TestNG, Cucumber, and Maven**.  
It generates and executes API test cases using CSV test definitions derived from **Swagger**.

The framework automates the following steps:
1. **Test Data Generation**  
   - Using a prompt to ChatGPT:  
     ```
     you are a test developer given a swagger file develop me all the test data for the services within the swagger file write test details in csv file with following columns - HTTP method, test case name, endpoint ,payload, expected status code consider all the boundary conditions while giving the test data example: if the type is number consider negative , positive, 0 and very large number if it is a string consider giving empty, null, large String value, string with leading / trailing spaces consider parameter name as well while generating test values. Also give value of the field as parameter and note directly value for example  if we have {partner{id: "${partnerID}",name: "${partnerName}"}, for valid value give fieldName followed by valid value like for valid partnerName partnerNameValidValue, for long string give as {longString} like this follow everywhere.
 
     Additional Considerations Empty Strings and Null Values: It's important to test how the system handles empty strings ("") and null values, as these are common edge cases. Code Intelligence Maximum Allowed Lengths: If the system has a maximum allowed string length (e.g., 255 characters), test strings at lengths of 254, 255, and 256 characters to ensure proper handling and give parameter as ${256Characters} and so on. Special Characters and Whitespace: Include tests with strings that contain special characters, whitespace, or escape sequences to verify that the system processes them correctly.
 
     Make all value of field as parameter in format ${...} also do refer to the webmethods B2B documentation for better test cases
     ```
   - This generates a CSV file (`partner_user_v3.csv`, etc.) with test cases:
     - HTTP Method  
     - Test Case Name  
     - Endpoint  
     - Payload  
     - Expected Status Code  

2. **Parameter Replacement**  
   - All test inputs are defined as **parameters** in `${...}` format (e.g., `${partnerUserIdValid}`).
   - A Cucumber step (`replaceParameters`) replaces these with:
     - Valid values (from backend calls)  
     - Edge cases (`null`, `empty`, `long strings`, `special characters`, etc.)  
     - Auto-generated values (e.g., UUID, 256-character string).  
   - Unknown parameters are logged to `resources/parametersNotHandled/parameters.txt`.

3. **Test Execution**  
   - Resolved test cases are written to `resources/GeneratedTestCases/modifiedTestCases.csv`.
   - Another Cucumber step executes each test case using **RestAssured**, verifying status codes.  

---

## 📂 Project Structure



src
├── main/java
│    └── resources
│         ├── properties/Environment.xml   # Env config
│         └── TestData/                    # Sample test data
│
└── test/java
├── features/                         # Cucumber feature files
│    ├── TestAI.feature
│    └── TestCaseGeneration.feature
│
├── framework/
│    └── resources/
│         ├── GeneratedTestCases/     # Generated/modified CSVs
│         └── parametersNotHandled/   # Unmapped parameters
│
├── runners/                          # TestNG+Cucumber runners
│    ├── TestAiRunner.java
│    └── TestCaseGenerationRunner.java
│
├── steps/                            # Step definitions
│    ├── TcGeneration.java
│    └── TestCaseGenerationSteps.java
│
└── utils/                            # Helper utilities
├── GenericMethods.java
├── LoadEnvironment.java
└── RestAssuredUtils.java


---

## ⚡ Workflow

### Step 1: Generate CSV from Swagger using AI (prefered Chat GPT)
Use ChatGPT with the special prompt to generate boundary-condition test cases.  
Save CSV to:  src/test/java/resources/GeneratedTestCases/partner_user_v3.csv

### Step 2: Replace Parameters
Run the first Cucumber step:  Given Replace parameters in file “src/test/java/resources/GeneratedTestCases/partner_user_v3.csv” and write modified CSV


This outputs:src/test/java/resources/GeneratedTestCases/modifiedTestCases.csv

### Step 3: Execute Test Cases
Run the second Cucumber step:  And Execute modified test cases from file
- For products other than B2B this step can be commented out or steps should be added to make calls ( login as well if required)


---

## ▶️ Running the Tests
Run with Maven + TestNG:

```bash
mvn clean
mvn -q test

Reports are generated under:
target/cucumber-html
target/cucumber.json
target/surefire-reports


📊 Flowchart
flowchart TD
    A[Swagger File] -->|ChatGPT Prompt| B[CSV Test Data]
    B -->|Step 1: Replace Parameters| C[modifiedTestCases.csv]
    C -->|Step 2: Execute Tests| D[API Execution via RestAssured]
    D --> E[Test Reports]
    C --> F[parameters.txt for unmapped params]


    ✅ Features
	•	CSV-driven test case management.
	•	Intelligent parameter replacement:
	•	null, empty, long, specialChars, leading/trailing spaces.
	•	Auto-generated values (UUID, 128–256 chars).
	•	Fetch live values from backend APIs for valid cases.
	•	Cucumber + TestNG integration.
	•	Automatic logging of unmapped parameters.
	•	Extensible intent-based parameter mapping.

⸻

🔧 Future Enhancements
	•	Explore/Use BOB for test case generation
	•	Expand mapToIntent for new parameter patterns.
	•	Support request headers and query params in CSV.
	•	Generate allure reports in addition to Cucumber HTML.