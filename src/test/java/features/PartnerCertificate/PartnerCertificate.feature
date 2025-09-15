Feature: Partner Portal Certificates

  Background:
    When get partner contextRecordID of partner portal

  Scenario Outline: Add certificate to partner - <scenario>
    When get certificate recordID with "<usage_type>" and "<primary>"
    Given the excel file is "partnerCertificate" and sheet is "postPartnerCertificate" and row is "<row>"
    Then create a partner contacts from provided excel row
      | <scenario> | <row> |
    Examples:
      | scenario                        | usage_type      | primary | row |
      | Add sign verify certificate     | sign-verify     | true    | 1   |
      | Add encrypt decrypt certificate | encrypt-decrypt | true    | 2   |


  Scenario Outline: Get partner certificate - <scenario>
    When get partner certificate by "<usage_type>"
    Examples:
      | scenario                        | usage_type      |
      | Get sign verify certificate     | sign-verify     |
      | Get encrypt decrypt certificate | encrypt-decrypt |

  Scenario Outline: Delete partner certificate - <scenario>
    When get certificate recordID with "<usage_type>" and "<primary>"
    When delete partner certificate
    Examples:
      | scenario                        | usage_type      | primary |
      | Get sign verify certificate     | sign-verify     | true    |
      | Get encrypt decrypt certificate | encrypt-decrypt | true    |

