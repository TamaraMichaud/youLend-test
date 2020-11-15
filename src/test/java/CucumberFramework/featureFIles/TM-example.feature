Feature: Purchase items
  Existing automation.com user should be able to buy items

  Scenario: Purchase two items
    Given I am logged in
    When I add a new "dress" size "L" to my basket
    And I add a new "blouse" size "M" to my basket
    Then I find the expected total price of my basket
    And I checkout successfully

  Scenario: Fail to purchase an item
    Given I am logged in
    When I add a new "top" size "S" to my basket
    Then I find an item size "L" in my basket
    ## ^^ Fails intentionally, attempting bonus points!
    # screenshot prints to target folder, however needs to scroll down to be useful
