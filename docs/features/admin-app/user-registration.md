# Feature #1 *User Registration*: Admin can registrate new users 



## 👔Part 1: Product & Business


### Executive Summary
This feature allows administrators to register new users into the ISP billing system, after a prospect submits an application. To register a new user, the administrator will need to provide necessary information such as full name, email,
& living address. The system will validate the input data and create a new user account in the database. The password will be generated automatically and shown to the admin. This feature is essential for managing subscribers and ensuring that only authorized users can access the platform.

### Feature objectives
- Specific: Standardize and streamline the process for administrators to create new subscriber accounts (based on submitted applications) in the ISP billing system, 
            including automatic secure password generation.
- Measurable: The feature will be considered successful if at least 90% of administrators can successfully register new users without errors.
- Attainable: Feasible as a standard Function built from scratch.
- Realistic: Speeds up onboarding & improves security.
- Time-bound: 3 Days for development and testing.

### Feature scope
In-scope includes creating the user database schema, building the admin form with validation (full name, email, address), and displaying an auto-generated password upon successful creation.
Out-of-scope explicitly excludes user login flows, automated email notifications, and account editing or deletion to ensure delivery within the 3-day timeframe.

### User flow
```mermaid
flowchart LR
    Start((Start)) --> OpenForm[Open 'Create User' form]
    OpenForm --> FillData[Input Full name, Email, Living Address]
    FillData --> Submit(Click 'Register')
    Submit --> Validate{System Validation}
    
    Validate -- Invalid Input --> ShowError[Display Error Message]
    ShowError -.-> FillData
    
    Validate -- Valid Input --> Generate[Save User & Auto-generate Password]
    Generate --> ShowPassword[Show Success Modal with Generated Password]
    ShowPassword --> End((Finish))
```

### Use cases
- **Successful Creation:** Admin inputs valid data (Full name, Email, Address) ➔ System creates user and displays the auto-generated password.
- **Validation Error:** Admin leaves required fields empty or enters an invalid email format ➔ System blocks submission and shows a validation error.
- **Duplicate User:** Admin enters an Email that already exists in the database ➔ System blocks creation and shows a "Duplicate" error.

### Functional Requirements
- The system must provide an admin form with fields: Full name, Email, and Living Address.
- The system must validate that all fields are populated before submission.
- The system must validate the Email field for correct formatting (e.g., user@domain.com).
- The system must verify that the Email are unique in the database.
- The system must automatically generate a random password upon successful form submission.
- The system must display the newly created user's data and the generated password to the admin.

### Non-Functional Requirements
- **Security:** The auto-generated password must be at least 8 characters long, containing a mix of letters and numbers.
- **Performance:** The user creation process and password generation must complete in under 2 seconds.
- **Usability:** The generated password must be presented in a way that is easily copyable by the administrator.



## 🛠Part 2: Technical Realisation

### Architecture & Integrations
*Implemented tools:* Java / Spring Boot, Thymeleaf (Server-Side Rendering), Spring Security (for password hashing), Database (e.g., PostgreSQL/MySQL).

*Sequence diagram:*
```mermaid
sequenceDiagram
    participant Admin (Browser)
    participant Spring Controller
    participant Database

    Admin (Browser)->>Spring Controller: GET /admin/users/new
    Spring Controller-->>Admin (Browser): Return HTML (Thymeleaf Create Form)
    
    Admin (Browser)->>Spring Controller: POST /admin/users/new (Form Data)
    Spring Controller->>Spring Controller: Validate input & Form binding
    Spring Controller->>Database: Check for duplicate Email/Username
    Database-->>Spring Controller: Return clear
    Spring Controller->>Spring Controller: Generate random password & hash it
    Spring Controller->>Database: INSERT new user (with hashed password)
    Database-->>Spring Controller: Return created user ID
    Spring Controller-->>Admin (Browser): Return HTML (Success view with generated password)
```


### Open questions
*No questions.*

