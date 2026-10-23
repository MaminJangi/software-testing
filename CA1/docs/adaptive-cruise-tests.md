# AdaptiveCruise test notes

Test file: `../autopilot/src/test/java/autopilot/core/AdaptiveCruiseTest.java`.
Data file: `../autopilot/src/test/resources/autopilot/core/cruise-decision-cases.csv`.
Scope: `safeDistanceFor()` / `update()`, returned decisions and observable braking history. No standalone constructor or record-accessor tests.

## Model to remember

- Safe distance in meters is half the current speed in km/h. Emergency distance is half that safe distance; release distance is 1.2 times safe distance.
- At or below emergency distance, return `EMERGENCY_BRAKE` with amount 100, even if already braking.
- Below safe distance, return `BRAKE` with proportional pressure. At 80 km/h, safe distance is 40 m: distances 30 and 21 give pressures 25 and 47.5.
- Once braking begins, it persists until distance reaches the release threshold. Between safe and release distances, pressure is 10. This hysteresis avoids rapid switching around the safe-distance boundary.
- After release, or before braking has started, a gap above safe distance and speed below target produce `ACCELERATE`, capped at 5 km/h per decision and at the remaining target-speed gap. Otherwise return `HOLD`, amount 0.
- At exactly safe distance, a fresh controller holds; an already-braking controller continues braking. `amount` means pressure for braking modes and a speed increment for acceleration. The controller returns a decision; it does not change a vehicle's speed itself.

## Scenarios and purpose

| Test condition | Expected outcome / reason | Cases |
| --- | --- | ---: |
| Safe distance | Speeds 0, 1, 80 and 200 map to 0, 0.5, 40 and 100 meters. | 4 |
| Fresh-controller decisions | At speed 80, distances 19/20/21 surround emergency distance and 39/40/41 surround safe distance. Includes holding at exactly safe distance even below target speed, zero distance, representative proportional pressure, acceleration capped at 5 or limited to 4/1 near target, speed above target, and zero speed with zero/positive distance. Zero target holds at speeds 0/10; target 63 at speed 60 and distance 40 requests acceleration 3, compared with 5 for target 80 at the same inputs. Checks both mode and amount. | 19 |
| Recovery after ordinary/emergency braking | From either initial braking mode: 40/47 meters retain pressure 10; exactly 48 releases to HOLD. Returning to 40 still holds, proving state was cleared; 30 starts braking again. | 2 |
| Speed changes during braking | From speed 80 braking, changing to speed 60 at distance 40 releases and accelerates; changing to speed 100 at distance 50 retains pressure 10. Both distances lie between old and new release thresholds, so stale thresholds give the wrong result. | 2 |
| Growing gap at fixed speed | At speed 60 throughout, distances 20 -> 30 -> 35 -> 36 give BRAKE about 33.333 -> BRAKE 10 -> BRAKE 10 -> ACCELERATE 5. Isolates distance-driven recovery and exact release equality. | 1 |
| Shrinking gap while braking | At speed 80, distances 30 -> 21 -> 20 give BRAKE 25 -> BRAKE 47.5 -> EMERGENCY_BRAKE 100. | 1 |
| Invalid safe-distance speed | Smallest negative double, -1, NaN and both infinities throw `IllegalArgumentException`. | 5 |
| Invalid update speed | Same invalid partitions; exception followed by a valid call proves braking history is retained. | 5 |
| Invalid update distance | Same invalid partitions, checked with valid speed; braking history is retained. | 5 |
| **Total invocations** | **Nine test methods, including parameterized cases.** | **44** |

## Design reminders

- Naming: `method_whenCondition_expectedBehavior`; a fresh controller with target 80 for each invocation via `@BeforeEach`. The decision-table test replaces it with a fresh controller using the row's target.
- `@CsvFileSource` supplies the larger decision table (header skipped), `@CsvSource` supplies small distance/history tables, and `@MethodSource` supplies invalid numbers. Zero-target cases are included in the decision file instead of a separate test.
- `assertDecision()` checks both parts of a decision together: the correct mode with an incorrect amount is still a failure. Amount tolerance is 1e-9 to allow floating-point rounding in proportional pressure calculations.
- Mode history is observed through later decisions; no reflection or direct access to the private `braking` field.
- Explicit setup assertions establish the initial braking state. Each sequence is one behavior and does not depend on another test.

## Limits and review notes

- README describes half-speed safe distance, driving modes and hysteresis. Exact threshold inclusivity, release factor, amounts, zero-speed/zero-distance emergency behavior and preserved-state rejection are characterized from the supplied implementation.
- Cases cover representative input partitions and state transitions, not every speed/distance/history combination. Boundary neighbors use simple values, not adjacent representable doubles.
- Invalid target-speed constructor tests are omitted to follow the assignment's constructor exclusion. Simultaneously invalid inputs and every pre-failure state are not enumerated.
- Tests exercise decisions in the educational model, not sensors, actual acceleration, braking hardware or vehicle safety.

Run from the repository root:

```powershell
mvn -f CA1/autopilot/pom.xml "-Dtest=AdaptiveCruiseTest" test
```
