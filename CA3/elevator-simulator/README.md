# CA3 Elevator Simulator

A small elevator simulator with JUnit tests for reviewing code smells and design patterns.

## Project structure

- `src/main/java` contains the elevator model, service, hardware interfaces and adapters, event notifications, and application wiring.
- `src/main/resources` contains the elevator configuration.
- `src/test/java` contains the supplied JUnit 5 tests.
- `src/test/resources` contains the floor data fixture.

## Run the starter tests

The Maven project targets Java 17 and uses JUnit Jupiter 5.10.2. From the repository root, run:

```sh
mvn -f CA3/elevator-simulator/pom.xml clean test
```

## Starter source

Imported from [UT-SWT/F05-CA3](https://github.com/UT-SWT/F05-CA3) at commit `779e5df0bd9948b0bf6b36dfabd4ab0da912d8a5`. The upstream [MIT license](LICENSE) is included.
