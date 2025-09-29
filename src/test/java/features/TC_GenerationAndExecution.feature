Feature: Replace parameters in test cases and execute

Scenario: Prepare and execute partner user tests
  Given Replace parameters in file "src/test/java/resources/GeneratedTestCases/partner_user_v3.csv" and write modified CSV
  And Execute modified test cases from file