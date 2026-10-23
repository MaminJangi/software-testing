# BrakeController test notes

Test file: `../autopilot/src/test/java/autopilot/core/BrakeControllerTest.java`.
Data file: `../autopilot/src/test/resources/autopilot/core/brake-pressure-cases.csv`.
Scope: `apply()` / `emergencyBrake()` and observable braking state. No standalone constructor/getter tests.

## Model to remember

- Ordinary pressure is finite and non-negative, capped at 100. Speed must also be finite and non-negative.
- ABS applies only when bounded pressure is **above 80** AND speed is **above 30 km/h**. Consecutive ABS calls produce 90, 60, 90, 60.
- A non-ABS call resets the next ABS pulse to 90. Emergency braking also resets it, sets pressure to 100 and activates the warning.
- Both operations allow R/N/D, reject P with `IllegalStateException`, and reject null with `NullPointerException`.
- The supplied code leaves the emergency warning active during later ordinary braking; there is no warning-reset operation.

## Scenarios and purpose

| Test condition | Expected outcome / reason | Cases |
| --- | --- | ---: |
| Valid pressure/speed table | Zero pressure and speed, 79/80/81 pressure, 29/30/31 speed, and 100/101/max-finite pressure. Covers the strict ABS thresholds, all four threshold decision combinations, pressure cap and ABS precedence. | 12 |
| Allowed gears for ordinary braking | R/N/D each return requested pressure 40. | 3 |
| Continuous ABS | Returns 90, 60, 90, 60; detects a fixed output or failure to alternate. | 1 |
| Leave and re-enter ABS | Release to zero, reduce pressure to 80, or speed to 30; re-entry starts at 90 instead of continuing with 60. | 3 |
| Emergency braking in allowed gears | Explicitly verify the initial ABS pressure is 90; R/N/D each then set pressure 100 and warning true. | 3 |
| Emergency resets ABS | After a high pulse and emergency braking, subsequent ABS returns 90 then 60. | 1 |
| Ordinary braking after emergency | Pressure becomes 30 while warning stays true. | 1 |
| Invalid pressure | Smallest negative double, -1, NaN and both infinities rejected without changing pressure, warning or next ABS pulse. | 5 |
| Invalid speed | Same five partitions as pressure, checked separately with valid pressure. Same preserved-state checks. | 5 |
| Forbidden/missing gear | Each public operation rejects P and null; preserves pressure, warning and next ABS pulse. | 4 |
| **Total invocations** | **Thirteen test methods, including parameterized cases.** | **38** |

## Design reminders

- Naming: `method_whenCondition_expectedBehavior`; fresh controller for every invocation via `@BeforeEach`.
- `@CsvFileSource` loads the decision/boundary table from the test classpath; the first CSV row is a skipped header. `1.7976931348623157E308` is `Double.MAX_VALUE`.
- `@CsvSource` supplies ABS-reset scenarios, `@EnumSource` supplies allowed gears, and `@MethodSource` supplies invalid numbers.
- `activateWarningAndStartAbs()` is delegated setup using public methods. It creates non-default pressure/warning and leaves a pending low pulse, making unwanted resets visible.
- Rejection tests observe stored pressure and warning, then make a valid ABS call to observe the pending pulse. These assertions describe one preserved-state behavior.
- Sequence assertions check successive steps within a single test; tests never rely on another test having run.

## Limits and review notes

- README explicitly mentions the cap, alternating ABS and emergency braking. Exact thresholds, exception types, reset behavior and warning persistence are characterized from the supplied code; confirm them if a separate specification says otherwise.
- Cases cover representative boundaries and state transitions, not every floating-point value, gear/input combination or possible history. The table uses integer neighbors of thresholds, not adjacent representable doubles.
- Rejected `apply()` calls preserve an active warning; rejected emergency calls preserve an inactive warning. All start with a pending low pulse. Every possible pre-failure state and precedence between simultaneous invalid inputs are not enumerated.
- These are tests of the educational model, not real ABS hardware or vehicle safety.

Run from the repository root:

```powershell
mvn -f CA1/autopilot/pom.xml "-Dtest=BrakeControllerTest" test
```
