# Software Testing

University of Tehran software testing coursework. Each assignment contains its own Maven project.

## Assignments

### CA1: JUnit and parameterized tests

Unit testing an autonomous vehicle simulator with JUnit 5, including parameterized tests for vehicle controllers and tests for the main driving coordinator.

- [Assignment description](CA1/SWT-05-CA1.pdf)
- [Project and simulator overview](CA1/autopilot/README.md)
- Project directory: `CA1/autopilot`
- [Original starter repository](https://github.com/UT-SWT/F05-CA1)
- Imported starter commit: `ea4cd26de69fe2f37569559eeb3b5b5c2831fec1`

### CA2: Test doubles with Mockito

Testing an airplane simulator with JUnit 5 and Mockito. The assignment focuses on replacing aircraft components with test doubles, controlling their responses, and verifying the aircraft's interactions with them during normal operation and failures.

- [Assignment description](CA2/SWT-05-CA2.pdf)
- [Project and simulator overview](CA2/airplane-simulator/README.md)
- Project directory: `CA2/airplane-simulator`
- [Original starter repository](https://github.com/UT-SWT/F05-CA2)
- Imported starter commit: `ee770866a602de06785bcd8a06585398d50a52e2`

### CA3: Code smells, refactoring, and design patterns

Reviewing an elevator simulator and its supplied JUnit tests to identify code smells and design patterns.

- [Project and review overview](CA3/elevator-simulator/README.md)
- Project directory: `CA3/elevator-simulator`
- [Original starter repository](https://github.com/UT-SWT/F05-CA3)
- Imported starter commit: `779e5df0bd9948b0bf6b36dfabd4ab0da912d8a5`

## Build and run

The projects use Maven and target Java 17 bytecode. The current setup has been verified with JDK 26 and Maven 3.9.

Install a JDK and Maven, set `JAVA_HOME` to the JDK installation, and make Java and Maven available on `PATH`. Check the selected versions with:

```sh
java --version
javac --version
mvn --version
```

Run an assignment's tests from the repository root:

```sh
# CA1
mvn -f CA1/autopilot/pom.xml clean test

# CA2
mvn -f CA2/airplane-simulator/pom.xml clean test

# CA3
mvn -f CA3/elevator-simulator/pom.xml clean test
```

Maven downloads the dependencies on the first run. Build output and test reports are generated in each project's `target` directory and excluded from version control.

## Dependencies and JDK compatibility

Each project's `pom.xml` defines its own dependencies:

| Assignment | JUnit Jupiter | Mockito |
| --- | --- | --- |
| CA1 | 5.10.2 | Not used |
| CA2 | 5.11.4 | 5.14.2 |
| CA3 | 5.10.2 | Not used |

The different JUnit versions belong to separate Maven projects and do not conflict in the documented builds.

CA2 currently includes JVM options for experimental Byte Buddy support and dynamic agent loading on newer JDKs. The tests pass on JDK 26, but Mockito agent and deprecated API warnings remain. These options do not guarantee compatibility with every JDK; Java 17 is the compilation target, not a claim that the current test configuration runs on every Java 17-or-newer runtime.

## Licenses

The starter projects' MIT licenses are retained in [CA1](CA1/autopilot/LICENSE), [CA2](CA2/airplane-simulator/LICENSE), and [CA3](CA3/elevator-simulator/LICENSE).
