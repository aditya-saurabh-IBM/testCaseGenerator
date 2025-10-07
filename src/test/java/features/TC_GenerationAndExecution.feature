Feature: Replace parameters in test cases and execute

Scenario: Prepare and execute partner user tests
  # Given Replace parameters in file "src/test/java/resources/GeneratedTestCases/partner_user_v3.csv" and write modified CSV
  Given Replace parameters in file "src/test/java/resources/GeneratedTestCases/channel_api_all_endpoints_testcases_final.csv" and write modified CSV

  And Execute modified test cases from file