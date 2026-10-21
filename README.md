# Software Testing

University of Tehran software testing coursework. Keep this repository private until the course permits publication.

## CA1: JUnit and parameterized tests

- Assignment: `CA1/SWT-05-CA1.pdf`
- Maven project: `CA1/autopilot` (open this directory in IntelliJ)
- Starter: https://github.com/UT-SWT/F05-CA1
- Imported starter commit: `ea4cd26de69fe2f37569559eeb3b5b5c2831fec1`
- The starter's MIT license is retained in `CA1/autopilot/LICENSE`.
- Starter source code, tests, and POM are unchanged.

## Environment

Use Eclipse Temurin JDK 26. The starter POM intentionally targets Java 17 bytecode and uses JUnit Jupiter 5.10.2. Running Maven and the tests on JDK 26 does not require changing that compilation target.

On Amin's Windows installation, Temurin 26.0.2.1+1 is installed separately under `%LOCALAPPDATA%/Programs/Eclipse Adoptium/`. Java 21 remains installed for later use. Temurin 26 is the selected terminal default. Maven 3.9.16 is bundled with IntelliJ IDEA 2026.2.0.1; its bin directory is on PATH.

After changing environment variables, open a fresh terminal (restart IntelliJ before opening a new embedded terminal).

From `CA1/autopilot` in PowerShell:

```powershell
java --version
javac --version
mvn --version
mvn clean test
```

`JAVA_HOME` selects the JDK used by Maven. PATH makes `java`, `javac`, and `mvn` available as ordinary commands. No helper script is needed.

## IntelliJ settings

Local IDE configuration is excluded from Git. On another machine configure:

1. File > Project Structure > Project > SDK: add your Temurin 26 installation.
2. Settings > Build, Execution, Deployment > Build Tools > Maven: use bundled Maven (or your own Maven).
3. Maven > Importing > JDK for importer: choose Temurin 26 / Project JDK.
4. Maven > Runner > JRE: choose Temurin 26 / Project JDK.

The project SDK supports Java editing, compilation, and default run/test configurations. The importer JDK runs Maven synchronization. The runner JRE runs Maven goals. These are separate from the IDE's own runtime and the Java selected by an ordinary terminal.

## Starting baseline

The unmodified starter passed all 5 supplied AutoDriver tests on Temurin 26.0.2.1+1 with Maven 3.9.16 on 2026-10-21. This confirms the initial build works; it does not establish that the supplied tests are well designed.

## Collaboration

Clone this private repository after accepting the invitation. Use a branch per task, commit small changes, push the branch, and review it with a pull request before merging. Pull main before beginning new work. Keep generated target directories and local IDE files out of commits.

## Assignment checklist

- Add unit tests for AdaptiveCruise, BrakeController, GearBox, and SteeringController.
- Review and improve the existing AutoDriver tests for their existing scenarios.
- Use consistent descriptive test naming and independent tests.
- Use ValueSource, EnumSource, CsvSource, CsvFileSource, and MethodSource on different methods.
- Constructors and getters/setters do not need tests.
- Understand and be able to explain each test for the oral evaluation.
