Feature: Generate API test cases from Swagger/OpenAPI

  Scenario Outline: Generate test matrix and write Excel
    Given an OpenAPI spec at "<specPath>"
    When I generate the API test matrix
    Then I print the matrix as a Markdown table
    And I write the matrix to "<mdPath>"
    And I write the matrix to excel "<xlsxPath>"

    Examples:
      | specPath                                                  | mdPath                        | xlsxPath                                              |
      | src/test/java/resources/swaggers/van-onboard-swagger.yaml | target/generated-testcases.md | src/test/java/resources/GeneratedTestCases/Test1.xlsx |
