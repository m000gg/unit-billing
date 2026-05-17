# Security Overview

## Authentication
Session-based authentication via Spring Security.
Users log in through a form at /login.
Sessions managed via JSESSIONID cookie.

## Authorization
All authenticated users have equal access.
Public endpoints: /login, 

## Headers & XSS Protection
XSS Protection headers enabled.


## CORS
Credentials allowed.

## CSRF
Enabled globally.