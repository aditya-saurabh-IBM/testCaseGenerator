Feature: Partner Portal Contacts

  Background:
    When get partner contextRecordID of partner portal

##  Scenario Outline: Add contacts to partner in partner portal - <scenario>
##    Given the excel file is "partnerContacts" and sheet is "postPartnerContact" and row is "<row>"
##    Then create a partner contacts from provided excel row
##      | <scenario> | <row> |
##    Examples:
##      | scenario                               | row |
##      | create contact with all valid values   | 1   |
##      | create contact with all valid values 2 | 2   |

  Scenario Outline: Add contacts to partner in partner portal - <scenario>
    Then create partner contact
      | <first_name> | <last_name> | <contact_type> | <role> | <email> | <fax> | <tel_num> | <tel_ext> | <add1> | <add2> | <add3> | <add_type> | <country> | <state> | <city> | <zip> |
    Given get contact id of partner whose email is "<email>"
    Then get and assert contact details
      | <first_name> | <last_name> | <contact_type> | <role> | <email> | <fax> | <tel_num> | <tel_ext> | <add1> | <add2> | <add3> | <add_type> | <country> | <state> | <city> | <zip> |
    Examples:
      | scenario                               | first_name  | last_name  | contact_type   | role | email             | fax  | tel_num | tel_ext | add1  | add2  | add3  | add_type | country | state | city | zip   | status |
      | Create contact with all valid values   | first_name  | last_name  | Administrative | role | testmail@sag.com  | 1233 | 122     | 91      | add1  | add2  | add3  | add_Type | IND     | MH    | BOM  | 0123  | 200    |
      | Create contact with all valid valuesc2 | first_name1 | last_name1 | Administrative | role | test1mail@sag.com | 153  | 1252    | 91      | ad5d1 | ad5d2 | ad5d3 | add_Type | IND5    | MH5   | BOM5 | 01523 | 200    |


  Scenario Outline: Update partner profile contact - <scenario>
    Given get contact id of partner whose email is "test1mail@sag.com"
    Then update partner contact
      | <first_name> | <last_name> | <contact_type> | <role> | <email> | <fax> | <tel_num> | <tel_ext> | <add1> | <add2> | <add3> | <add_type> | <country> | <state> | <city> | <zip> |
    Then get and assert contact details
      | <first_name> | <last_name> | <contact_type> | <role> | <email> | <fax> | <tel_num> | <tel_ext> | <add1> | <add2> | <add3> | <add_type> | <country> | <state> | <city> | <zip> |
    Examples:
      | scenario               | first_name   | last_name   | contact_type   | role  | email              | fax   | tel_num | tel_ext | add1  | add2  | add3  | add_type  | country | state | city | zip   | status |
      | update partner contact | first_name11 | last_name11 | Administrative | role1 | test1mail1@sag.com | 12331 | 1221    | 911     | add11 | add21 | add31 | add_Type1 | IND1    | MH1   | BOM1 | 01231 | 200    |

  Scenario Outline: Delete partner profile contact - <scenario>
    Given get contact id of partner whose email is "<email>"
    Then Delete partner contact
      | <status> |
    Examples:
      | scenario                 | email              | status |
      | Delete partner contact   | test1mail1@sag.com | 200    |
      | Delete partner contact 2 | testmail@sag.com   | 200    |


