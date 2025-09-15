Feature: Partner Portal Endpoints

  Background:
    When get partner contextRecordID of partner portal


  Scenario Outline: Add channel in Partner portal - <scenario>
    Then create partner endpoint
      | <type> | <url> | <is_preferred> | <username> | <password> | <status> |
    Given get endpoint id of partner whose email is "<url>"
    Then get and assert endpoint details
      | <type> | <url> | <is_preferred> | <username> | <password> | <status> |
    Examples:
      | scenario            | type     | url                     | is_preferred | username | password      | status |
      | Create AS4 channel  | AS4-OUT  | https://googleAS4.com   | false        | abcd     | Password@1234 | 200    |
      | Create AS2 channel  | AS2-OUT  | https://googleAS2.com   | false        | abdd     | Password@1234 | 200    |
      | Create HTTP channel | HTTP-OUT | https://google.com/path | false        | abcc     | Password@1234 | 200    |
      | Create RNIF channel | RNIF-OUT | https://googleRNIF.com  | false        | abbb     | Password@1234 | 200    |

  Scenario Outline: Update channel in Partner portal - <scenario>
    Given get endpoint id of partner whose email is "<url>"
    Then update partner endpoint
      | <type> | <url_new> | <is_preferred> | <username> | <password> | <status> |
    Then get and assert endpoint details
      | <type> | <url_new> | <is_preferred> | <username> | <password> | <status> |
    Examples:
      | scenario            | type     | url                     | is_preferred | username | password      | url_new                        | status |
      | Update AS4 channel  | AS4-OUT  | https://googleAS4.com   | false        | abcd1    | Password@1234 | https://googleAS4Updated.com   | 200    |
      | Update AS2 channel  | AS2-OUT  | https://googleAS2.com   | false        | abdd1    | Password@1234 | https://googleAS2Updated.com   | 200    |
      | Update HTTP channel | HTTP-OUT | https://google.com/path | false        | abcc1    | Password@1234 | https://googleUpdated.com/http | 200    |
      | Update RNIF channel | RNIF-OUT | https://googleRNIF.com  | false        | abbb1    | Password@1234 | https://googleRNIFUpdated.com  | 200    |

  Scenario Outline: Delete channel in Partner portal - <scenario>
    Given get endpoint id of partner whose email is "<url>"
    Then delete partner endpoint
      | <status> |
    Examples:
      | scenario            | url                            | status |
      | Delete AS4 channel  | https://googleAS4Updated.com   | 200    |
      | Delete AS2 channel  | https://googleAS2Updated.com   | 200    |
      | Delete HTTP channel | https://googleUpdated.com/http | 200    |
      | Delete RNIF channel | https://googleRNIFUpdated.com  | 200    |
