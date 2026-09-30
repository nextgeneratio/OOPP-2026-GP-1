# Architecture

The Academic Management System follows this dependency direction:

Presentation/Swing UI -> Internal API/controllers -> Business services/policies -> Data repositories/DAOs -> JDBC/MySQL.

Domain objects remain independent of Swing and JDBC. Detailed implementation is scheduled for the foundation phase.
