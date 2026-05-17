 # Domain Entities


## Subscriber
Subscriber is a client who has registered and has an active account in the system. 
They can perform various actions such as topping up their balance, subscribing to services, and managing their profile.

Lifecycle: Active -> Blocked

## Transaction
Transaction is initiated when a client performs (increases balance) and service charge (decreases balance). 
It is processed asynchronously, and its status is updated based on the outcome of the operation.

Lifecycle: Pending → Completed | Failed


## Subscription 
Active connection between a subscriber and a service within a billing cycle.
Automatically renews if balance is sufficient at the end of the cycle.

Lifecycle: Active → Canceled 

## Admin 
Admin is a user with elevated privileges who can manage subscribers, services, and transactions and has access to Admin Application.
Lifecycle: Active -> Blocked