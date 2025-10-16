Feature: Subscription

  Scenario: A subscription is created when subscription does not exist

    Given no previous events
    When you create on a Subscription using a subscription details
    Then subscribed

  Scenario: A subscription filter is rejected when a subscription is cancelled

    Given subscribed, unsubscribed
    When you updateFilter on a Subscription with a new filter
    Then no events occurred

  Scenario: A subscription is cancelled when a subscription exists

    Given subscribed
    When you cancel on a Subscription
    Then unsubscribed

  Scenario: A subscription is cancelled when a subscription does not exist

    Given no previous events
    When you cancel on a Subscription
    Then no events occurred

  Scenario: A subscription filter is updated when a subscription exists

    Given subscribed
    When you updateFilter on a Subscription with a new filter
    Then filter updated

  Scenario: A subscription filter is not updated when a subscription does not exist

    Given no previous events
    When you updateFilter on a Subscription with a new filter
    Then no events occurred

