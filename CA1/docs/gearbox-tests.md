# GearBox test notes

Test file: `../autopilot/src/test/java/autopilot/core/GearBoxTest.java`.
Scope: `changeTo()` and its observable effects. Constructors and trivial accessors are not tested independently.

## Transition rules

At zero speed, all 16 ordered pairs of starting and requested gears are covered:

| From / To | P | R | N | D |
| --- | --- | --- | --- | --- |
| P | Stay | Allow | Reject | Reject |
| R | Reject | Stay | Allow | Allow |
| N | Reject | Reject | Stay | Allow |
| D | Reject | Allow | Reject | Stay |

These are the supplied model's rules, not assumptions about a real car. In particular, `P -> D` and returning to park are rejected.

## Scenarios and purpose

| Test condition | Expected outcome / reason | Cases |
| --- | --- | ---: |
| Allowed transitions | Target gear becomes current. Includes `P -> R` immediately below 5 km/h and moving `R -> N` / `N -> D` at 50 km/h. | 8 |
| Forbidden transitions | `IllegalGearChangeException`; current gear unchanged. Seven forbidden pairs at zero, `P -> R` at/just above 5, and `R <-> D` at the smallest positive speed and at 50. | 13 |
| Request current gear | Gear unchanged even when the configured sensor would throw. Checks the early-return behavior for all gears. | 4 |
| Invalid sensor reading | `IllegalArgumentException`; gear stays P. Values: smallest negative double, -1, NaN, positive and negative infinity. | 5 |
| Null requested gear | `NullPointerException`; gear stays D. | 1 |
| Sensor failure during a change | `SensorFailureException`; gear stays D. The current test also requires the original exception instance. | 1 |
| **Total invocations** | **Six test methods, including parameterized cases.** | **32** |

## Design reminders

- Naming: `method_whenCondition_expectedBehavior`.
- `@BeforeEach` creates a fresh gearbox and sensor for each invocation.
- `@MethodSource` holds transition rows and invalid speeds; `@EnumSource` supplies the four same-gear cases.
- `moveToInitialGear()` is delegated setup: at zero speed, it follows public legal transitions to the required starting gear. Tests do not depend on earlier tests.
- The stub controls the speed or throws a configured failure. The production gearbox remains the object under test.
- `Math.nextDown(5.0)` / `Math.nextUp(5.0)` test the nearest representable doubles around the strict speed limit.
- Exception plus unchanged-state checks describe one rejection behavior; getters are observation points.

## Limits and review notes

- Representative speeds and all gear pairs are covered; this is not exhaustive coverage of every speed, history, or internal path.
- The same-gear throwing-stub test would not detect an implementation that read and silently swallowed a sensor failure.
- `assertSame` in the sensor-failure test is stricter than checking exception type and preserved state; retain it only if unchanged exception propagation is intended. It remains in the reviewed test.
- No real sensors, deployment, or vehicle safety properties are exercised.

Run from the repository root:

```powershell
mvn -f CA1/autopilot/pom.xml "-Dtest=GearBoxTest" test
```
