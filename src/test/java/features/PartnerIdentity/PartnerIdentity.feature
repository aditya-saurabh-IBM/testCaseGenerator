#@RunAll
Feature: Partner Portal identities


#  Background:
#    When get partner contextRecordID of partner portal
#                ######### --- add identity ---- ##########
  Scenario Outline: Add identity to partner with value <idType>-<idValue>
    Then create partner identity
      | <idType> | <idValue> | <status> |
    Then get identity with identityType as "<idType>", identityValue as "<idValue>" and status code as "<status>"
    Examples:
      | idType | idValue     | status |
      | DUNS   | automation1 | 200    |
      | DUNS   | automation2 | 200    |

  Scenario: B2B-Approve created partner Identity
    Then approve all the assets in b2b


#               ######### --- update identity ---- ##########
  Scenario Outline: Update partner identity with <idType_updated> - <idValue_updated>
    Then update partner identity
      | <idType_old> | <idValue_old> | <idType_updated> | <idValue_updated> | <status> |

    Then get identity with identityType as "<idType_updated>", identityValue as "<idValue_updated>" and status code as "<status>"
    Examples:
      | idType_old | idValue_old         | idType_updated | idValue_updated      | status |
      | DUNS       | automation1         | DUNS           | automation1_updated  | 200    |
      | DUNS       | automation1_updated | DUNS           | automation1_updated  | 200    |
      | DUNS       | automation1_updated |                | automation1_updated2 | 400    |

  Scenario: B2B-Approve Updated partner Identity
    Then approve all the assets in b2b

#              ######### --- delete identity ---- ##########
  Scenario Outline: Delete partner identity with identity as <idType> - <idValue>
    Then delete partner identity
      | <idType> | <idValue> | <status> |
    Examples:
      | idType | idValue             | status |
      | DUNS   | automation2         | 200    |
      | DUNS   | automation1_updated | 200    |
      | DUNS   | automation2         | 400    |

  Scenario: B2B-Approve Deleted partner Identity
    Then approve all the assets in b2b