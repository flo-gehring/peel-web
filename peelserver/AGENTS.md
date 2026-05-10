# Project Scope

Read @docs/ and @README.md.
This is a pre-production project. 
There is no need to migrate any data or be backwards compatible.
The Purpose of this project is to build a web app, that can be used to create and edit documents that can display the result of peel scripts.

## General Principles
* Don't over-engineer. Build the simplest thing that could possibly work.
* Don't under-engineer. Build something that is maintainable and extensible.
* Don't build something that is not needed. If you don't need it, don't build it.
* Don't build something that is not useful. If it doesn't solve a problem, don't build it
* Don't assume you know my intent. If something is not clearly specced, ask! 
* Don't assume I know your intent. If something is not clearly specced, ask! 
* Don't assume anything. If something is not clearly specced, ask!

* Caveat: If i ask you to YOLO it, build it to your best knowledge in one shot.

## Coding Guidelines
* Use descriptive variable and function names. If you can't think of a good name, ask for help.
* Write comments that explain why something is done, not what is done. If you can't explain why something is done, ask for help.
* Write tests that cover the happy path, edge cases, and error cases. If you can't think of a test case, ask for help.
* Prefer Test-Driven Development (TDD). Write tests before writing code. If you can't write a test, ask for help.
* Use consistent formatting. If you can't decide on a formatting style, ask for help.

* KISS -> Keep it simple, stupid.
* YAGNI -> You ain't gonna need it.
* SOLID -> Single Responsibility, Open-Closed, Liskov Substitution, Interface Segregation, Dependency Inversion.
  * Single Responsibility: A class should have only one reason to change.
  * Open-Closed: Software entities should be open for extension, but closed for modification.
  * Liskov Substitution: Subtypes must be substitutable for their base types.
  * Interface Segregation: Clients should not be forced to depend on interfaces they do not use.
  * Dependency Inversion: High-level modules should not depend on low-level modules. Both should depend on abstractions. Abstractions should not depend on details. Details should depend on abstractions.
* Additional interfaces (classes and functions count towards this) should only be added with care as they increase the complexity of the system. Often it is better to have a deep and complex class than to have a shallow and complex class hierarchy.