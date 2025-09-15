# testCaseGenerator
# API Test Automation Framework

This repository contains an automated framework for validating APIs against business requirements.  
Test cases are defined in simple CSV files and executed with Cucumber + RestAssured.

## Key Highlights
- **CSV-driven tests** – No code changes required, just add rows to CSV.
- **Dynamic parameters** – Placeholders like `${partnerUserIdValid}` are replaced at runtime.
- **Smart data generation** – Supports valid/invalid IDs, boundary values, empty/null checks, and long strings.
- **Typed substitution** – Numbers, booleans, and nulls are handled as true JSON types, not strings.
- **Error tracking** – Unhandled parameters are automatically logged for easy follow-up.

## Why this matters
- Faster onboarding of new test cases  
- Repeatable and consistent validation  
- Extendable for any future APIs  
- Ready for CI/CD integration

# CSV-Driven API Test Runner

This project is a **Cucumber + RestAssured** framework that runs API tests directly from CSV files.  
It supports **intelligent parameter resolution** with dynamic placeholder replacement.

---

## 📂 Structure
src/test/java/steps/TcGeneration.java           # Core runner & replacement logic
src/test/java/resources/testcases.csv           # CSV with test cases
src/test/java/resources/parametersNotHandled/   # Logs unknown placeholders


---

## ✨ Features
- **CSV-based test cases**
  - `HTTP method`, `test case name`, `endpoint`, `payload`, `expected status code`
- **Dynamic placeholders**
  - `${partnerUserIdValid}`, `${empty}`, `${char128}`, `${negative}`
- **Type-safe replacement**
  - JSON: numbers, booleans, null → preserved as real types
  - Endpoints: string replacement
- **Boundary testing**
  - Large strings, special chars, leading/trailing spaces, negative/zero values
- **Unknown parameter logging**
  - Automatically stored in `parameters.txt` for review

---

## 🚀 Running Tests
1. Add your test cases in `testcases.csv`.
2. Execute with Maven:
   ```bash
   mvn clean test


   Example :
   POST,Create User,/partners/${partnerId}/users,
{"userId": "${partnerUserIdValid}", "active": "${true}"},201

Output :

POST /partners/2134243234342324/users
Content-Type: application/json

{
  "userId": 2134243234342324,
  "active": true
}
