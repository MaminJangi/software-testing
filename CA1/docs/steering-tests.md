# SteeringController test notes

Test file: `../autopilot/src/test/java/autopilot/core/SteeringControllerTest.java`.
Scope: `update()` / `selfCenter()` and observable steering state. Constructors and trivial accessors are not tested independently.

## Model to remember

- **Target angle:** requested angle clamped to +/-450 degrees at speeds up to and including 120 km/h, or +/-90 degrees above 120.
- **Current angle:** moves toward the target by at most 60 degrees per call; it does not overshoot.
- The high-speed restriction clamps the target immediately. An existing current angle outside +/-90 returns gradually, not instantaneously.
- `selfCenter()` changes the target to zero and uses the same movement rule.

## Scenarios and purpose

| Test condition | Expected outcome / reason | Cases |
| --- | --- | ---: |
| Update from center | Requests 0, +/-30 and +/-60 are reached; +/-61 produces +/-60. Zero speed is accepted. | 8 |
| Target clamping | Both signs: 449/450/451 at exactly 120 km/h, and 89/90/91 immediately above 120. Verifies angle limits and the strict speed threshold. | 12 |
| Repeated updates toward +/-150 | Current angles 60, 120, 150, 150, and their negative counterparts: bounded steps, final partial step, no overshoot or drift. | 2 |
| Reverse direction | From +60, requesting -150 moves to zero in one step, rather than jumping or restarting from center. | 1 |
| Increase speed | Verify setup reaches +/-300; a high-speed request then gives target +/-90 and current +/-240. Distinguishes target clamping from immediate current-angle clamping. | 2 |
| Reduce speed to 120 | Restores target 450; current advances from 60 to 120. High-speed restrictions must not remain stuck. | 1 |
| Center within one step | Starting at -60, -30, 0, 30, 60, one call reaches zero. | 5 |
| Center over several steps | Starting at +120: returns 60, 0, 0; starting at -120: returns -60, 0, 0. Checks step size and staying centered. | 2 |
| Non-finite requested angle | NaN and both infinities throw `IllegalArgumentException`; current 60 and target 150 are preserved. | 3 |
| Invalid speed | Smallest negative double, -1, NaN and both infinities throw `IllegalArgumentException`; current and target are preserved. | 5 |
| **Total invocations** | **Ten test methods, including parameterized cases.** | **41** |

## Design reminders

- Naming: `method_whenCondition_expectedBehavior`.
- Fresh controller in `@BeforeEach`; no cross-test ordering or shared mutable fixture.
- `@CsvSource` supplies initial movement and longer sequence rows, `@ValueSource` supplies initial angles for `selfCenter()`, and `@MethodSource` supplies limit/validation cases.
- Getter assertions observe behavior; they are not standalone getter tests.
- Multiple assertions in sequence tests check successive steps of the same behavior.
- Input tables cover both directions and meaningful boundaries, rather than every possible double.

## Limits and review notes

- These are focused examples, not proof that every input, call sequence, or internal path is correct.
- Invalid inputs are checked independently; combinations with both invalid speed and angle are not enumerated.
- Both signs are covered in long convergence, multi-step centering and speed increases. Direction reversal uses one representative sequence; arbitrary histories and every angle combination are not enumerated.
- No real steering hardware or vehicle safety properties are exercised.

Run from the repository root:

```powershell
mvn -f CA1/autopilot/pom.xml "-Dtest=SteeringControllerTest" test
```
