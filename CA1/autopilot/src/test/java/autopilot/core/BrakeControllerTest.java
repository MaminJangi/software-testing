package autopilot.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrakeControllerTest {
    private static final double MAX_PRESSURE = 100.0;
    private static final double ABS_HIGH_PULSE = 90.0;
    private static final double ABS_LOW_PULSE = 60.0;
    private static final double ABS_SPEED_KMH = 50.0;

    private BrakeController brakes;

    @BeforeEach
    void setUp() {
        brakes = new BrakeController();
    }

    @ParameterizedTest(name = "pressure {0} at {1} km/h -> {2}")
    @CsvFileSource(resources = "/autopilot/core/brake-pressure-cases.csv", numLinesToSkip = 1)
    void apply_whenInputsAreValid_boundsPressureAndUsesAbsOnlyAboveBothThresholds(
            double requestedPressure, double speedKmh, double expectedPressure) {

        double actualPressure = brakes.apply(requestedPressure, speedKmh, GearBox.Gear.D);

        assertEquals(expectedPressure, actualPressure);
        assertEquals(expectedPressure, brakes.getPressure());
        assertFalse(brakes.isWarningActive());
    }

    @ParameterizedTest(name = "ordinary braking is available in {0}")
    @EnumSource(value = GearBox.Gear.class, names = {"R", "N", "D"})
    void apply_whenGearIsNotPark_allowsBraking(GearBox.Gear gear) {

        double actualPressure = brakes.apply(40.0, ABS_SPEED_KMH, gear);

        assertEquals(40.0, actualPressure);
    }

    @Test
    void apply_whenAbsRemainsActive_alternatesHighAndLowPulses() {

        assertEquals(ABS_HIGH_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));
        assertEquals(ABS_LOW_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));
        assertEquals(ABS_HIGH_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));
        assertEquals(ABS_LOW_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));
    }

    @ParameterizedTest(name = "leaving ABS with pressure {0} at {1} km/h resets its next pulse")
    @CsvSource({
            "0.0,   50.0,   0.0",
            "80.0,  50.0,  80.0",
            "100.0, 30.0, 100.0"
    })
    void apply_whenAbsStops_restartsWithHighPulse(
            double requestedPressure, double speedKmh, double expectedPressure) {

        assertEquals(ABS_HIGH_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));

        assertEquals(expectedPressure, brakes.apply(requestedPressure, speedKmh, GearBox.Gear.D));

        assertEquals(ABS_HIGH_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));
    }

    @ParameterizedTest(name = "emergency braking in {0} sets full pressure and warning")
    @EnumSource(value = GearBox.Gear.class, names = {"R", "N", "D"})
    void emergencyBrake_whenGearIsNotPark_setsFullPressureAndWarning(GearBox.Gear gear) {

        assertEquals(ABS_HIGH_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, gear));

        brakes.emergencyBrake(gear);

        assertEquals(MAX_PRESSURE, brakes.getPressure());
        assertTrue(brakes.isWarningActive());
    }

    @Test
    void emergencyBrake_whenAbsWasActive_resetsNextPulseToHigh() {
        assertEquals(ABS_HIGH_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));

        brakes.emergencyBrake(GearBox.Gear.D);

        assertEquals(ABS_HIGH_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));
        assertEquals(ABS_LOW_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));
    }

    @Test
    void apply_whenEmergencyWarningWasActivated_keepsWarningActive() {
        brakes.emergencyBrake(GearBox.Gear.D);

        double actualPressure = brakes.apply(30.0, ABS_SPEED_KMH, GearBox.Gear.D);

        assertEquals(30.0, actualPressure);
        assertTrue(brakes.isWarningActive());
    }

    @ParameterizedTest(name = "invalid pressure {0} preserves braking state")
    @MethodSource("invalidNonNegativeNumbers")
    void apply_whenPressureIsInvalid_throwsAndKeepsBrakingState(double invalidPressure) {

        activateWarningAndStartAbs();

        assertThrows(IllegalArgumentException.class,
                () -> brakes.apply(invalidPressure, ABS_SPEED_KMH, GearBox.Gear.D));

        assertEquals(ABS_HIGH_PULSE, brakes.getPressure());
        assertTrue(brakes.isWarningActive());
        assertEquals(ABS_LOW_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));
    }

    @ParameterizedTest(name = "invalid speed {0} preserves braking state")
    @MethodSource("invalidNonNegativeNumbers")
    void apply_whenSpeedIsInvalid_throwsAndKeepsBrakingState(double invalidSpeed) {

        activateWarningAndStartAbs();

        assertThrows(IllegalArgumentException.class,
                () -> brakes.apply(MAX_PRESSURE, invalidSpeed, GearBox.Gear.D));

        assertEquals(ABS_HIGH_PULSE, brakes.getPressure());
        assertTrue(brakes.isWarningActive());
        assertEquals(ABS_LOW_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));
    }

    static List<Double> invalidNonNegativeNumbers() {
        return List.of(-Double.MIN_VALUE, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    @Test
    void apply_whenGearIsPark_throwsAndKeepsBrakingState() {
        activateWarningAndStartAbs();

        assertThrows(IllegalStateException.class, () -> brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.P));

        assertEquals(ABS_HIGH_PULSE, brakes.getPressure());
        assertTrue(brakes.isWarningActive());
        assertEquals(ABS_LOW_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));
    }

    @Test
    void apply_whenGearIsNull_throwsAndKeepsBrakingState() {
        activateWarningAndStartAbs();

        assertThrows(NullPointerException.class, () -> brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, null));

        assertEquals(ABS_HIGH_PULSE, brakes.getPressure());
        assertTrue(brakes.isWarningActive());
        assertEquals(ABS_LOW_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));
    }

    @Test
    void emergencyBrake_whenGearIsPark_throwsAndKeepsBrakingState() {

        assertEquals(ABS_HIGH_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));

        assertThrows(IllegalStateException.class, () -> brakes.emergencyBrake(GearBox.Gear.P));

        assertEquals(ABS_HIGH_PULSE, brakes.getPressure());
        assertFalse(brakes.isWarningActive());
        assertEquals(ABS_LOW_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));
    }

    @Test
    void emergencyBrake_whenGearIsNull_throwsAndKeepsBrakingState() {

        assertEquals(ABS_HIGH_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));

        assertThrows(NullPointerException.class, () -> brakes.emergencyBrake(null));

        assertEquals(ABS_HIGH_PULSE, brakes.getPressure());
        assertFalse(brakes.isWarningActive());
        assertEquals(ABS_LOW_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));
    }

    private void activateWarningAndStartAbs() {
        brakes.emergencyBrake(GearBox.Gear.D);
        assertEquals(ABS_HIGH_PULSE, brakes.apply(MAX_PRESSURE, ABS_SPEED_KMH, GearBox.Gear.D));
        // The warning is active and the next ABS pulse should be low.
    }
}
