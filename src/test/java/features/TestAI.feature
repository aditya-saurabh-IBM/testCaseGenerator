Feature: Generate API test cases from OpenAPI and write to Excel (with optional AI)
  # Enable this step when you want AI-suggested cases as well.

  # Scenario Outline: Build matrix (deterministic + AI) and export to Excel
  #   Given an OpenAPI spec at "<specPath>"
  #   And AI suggestions are enabled
  #   When I generate the API test matrix from AI
  #   Then I print the matrix as a Markdown table
  #   # And I write the matrix to excel "<xlsxPath>"
  #   Then I write the matrix to csv "src/test/java/resources/GeneratedTestCases/TestAI.csv"

  #   Examples:
  #     | specPath                                                  | xlsxPath                                              |
  #     | src/test/java/resources/swaggers/van-onboard-swagger.yaml | src/test/java/resources/GeneratedTestCases/Test1.xlsx |


# response = restAssuredUtils.sendGetRequest(pprrestEndpoints.GET_Partner_ContextRecordID, hooks.cookie).then().extract().response();
#         contextRecordID = response.then().extract().response().jsonPath().getString("portalInfo.contextRecordId");
# src/test/java/resources/GeneratedTestCases/partner-user-tests-sequenced-v2.csv
Scenario: Run API cases
    # Given Iterate each row and run the test case of file "src/test/java/resources/GeneratedTestCases/partner_user_v3.csv"
  #  Given Iterate each row and run the test case of file "src/test/java/resources/GeneratedTestCases/partner_user_api_testcases_v4.csv"
      Given Iterate each row and replace the parameters in file "src/test/java/resources/GeneratedTestCases/partner_user_v3.csv"