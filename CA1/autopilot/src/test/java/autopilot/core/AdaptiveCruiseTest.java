package autopilot.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdaptiveCruiseTest {
    private static final double TARGET_SPEED_KMH = 80.0;
    private static final double HYSTERESIS_BRAKE_PRESSURE = 10.0;
    private static final double AMOUNT_TOLERANCE = 1.0e-9;

    private AdaptiveCruise cruise;

    @BeforeEach
    void setUp() {
        // A fresh controller has not entered braking mode.
        cruise = new AdaptiveCruise(TARGET_SPEED_KMH);
    }

    @ParameterizedTest(name = "speed {0} km/h -> safe distance {1} meters")
    @CsvSource({
            "0.0,     0.0",
            "1.0,     0.5",
            "80.0,   40.0",
            "200.0, 100.0"
    })
    void safeDistanceFor_whenSpeedIsValid_returnsHalfTheSpeedInMeters(double speedKmh, double expectedDistance) {

        double actualDistance = cruise.safeDistanceFor(speedKmh);

        assertEquals(expectedDistance, actualDistance);
    }

    @ParameterizedTest(name = "target {0}, speed {1}, distance {2} -> {3}, amount {4}")
    @CsvFileSource(resources = "/autopilot/core/cruise-decision-cases.csv", numLinesToSkip = 1)
    void update_whenNotAlreadyBraking_selectsModeAndAmountFromSpeedAndDistance(
            double targetSpeedKmh, double speedKmh, double distanceMeters,
            AdaptiveCruise.Mode expectedMode, double expectedAmount) {

        cruise = new AdaptiveCruise(targetSpeedKmh);

        AdaptiveCruise.Decision decision = cruise.update(speedKmh, distanceMeters);

        assertDecision(expectedMode, expectedAmount, decision);
    }

    @ParameterizedTest(name = "after {1}, braking persists until 48 meters and can start again")
    @CsvSource({
            "10.0, EMERGENCY_BRAKE, 100.0",
            "30.0, BRAKE,            25.0"
    })
    void update_whenDistanceRecovers_keepsBrakingUntilReleaseThresholdThenClearsState(
            double initialDistance, AdaptiveCruise.Mode initialMode, double initialAmount) {

        assertDecision(initialMode, initialAmount, cruise.update(80.0, initialDistance));

        assertDecision(AdaptiveCruise.Mode.BRAKE, HYSTERESIS_BRAKE_PRESSURE, cruise.update(80.0, 40.0));
        assertDecision(AdaptiveCruise.Mode.BRAKE, HYSTERESIS_BRAKE_PRESSURE, cruise.update(80.0, 47.0));
        assertDecision(AdaptiveCruise.Mode.HOLD, 0.0, cruise.update(80.0, 48.0));
        assertDecision(AdaptiveCruise.Mode.HOLD, 0.0, cruise.update(80.0, 40.0));
        assertDecision(AdaptiveCruise.Mode.BRAKE, 25.0, cruise.update(80.0, 30.0));
    }

    @ParameterizedTest(name = "speed changes to {0}, distance {1} -> {2}")
    @CsvSource({
            "60.0,  40.0, ACCELERATE, 5.0",
            "100.0, 50.0, BRAKE,     10.0"
    })
    void update_whenSpeedChanges_recalculatesDistanceThresholds(double speedKmh, double distanceMeters,
            AdaptiveCruise.Mode expectedMode, double expectedAmount) {

        assertDecision(AdaptiveCruise.Mode.BRAKE, 25.0, cruise.update(80.0, 30.0));

        assertDecision(expectedMode, expectedAmount, cruise.update(speedKmh, distanceMeters));
    }

    @Test
    void update_whenGapGrowsAtFixedSpeed_releasesBrakingAndAccelerates() {

        assertDecision(AdaptiveCruise.Mode.BRAKE, 100.0 / 3.0, cruise.update(60.0, 20.0));

        assertDecision(AdaptiveCruise.Mode.BRAKE, HYSTERESIS_BRAKE_PRESSURE, cruise.update(60.0, 30.0));
        assertDecision(AdaptiveCruise.Mode.BRAKE, HYSTERESIS_BRAKE_PRESSURE, cruise.update(60.0, 35.0));
        assertDecision(AdaptiveCruise.Mode.ACCELERATE, 5.0, cruise.update(60.0, 36.0));
    }

    @Test
    void update_whenGapShrinksDuringBraking_increasesPressureAndThenUsesEmergencyBraking() {

        assertDecision(AdaptiveCruise.Mode.BRAKE, 25.0, cruise.update(80.0, 30.0));

        assertDecision(AdaptiveCruise.Mode.BRAKE, 47.5, cruise.update(80.0, 21.0));

        assertDecision(AdaptiveCruise.Mode.EMERGENCY_BRAKE, 100.0, cruise.update(80.0, 20.0));
    }

    @ParameterizedTest(name = "safeDistanceFor rejects speed {0}")
    @MethodSource("invalidNonNegativeNumbers")
    void safeDistanceFor_whenSpeedIsInvalid_throws(double invalidSpeed) {
        assertThrows(IllegalArgumentException.class, () -> cruise.safeDistanceFor(invalidSpeed));
    }

    @ParameterizedTest(name = "invalid speed {0} preserves braking mode")
    @MethodSource("invalidNonNegativeNumbers")
    void update_whenSpeedIsInvalid_throwsAndKeepsBrakingMode(double invalidSpeed) {
        assertDecision(AdaptiveCruise.Mode.BRAKE, 25.0, cruise.update(80.0, 30.0));

        assertThrows(IllegalArgumentException.class, () -> cruise.update(invalidSpeed, 100.0));

        assertDecision(AdaptiveCruise.Mode.BRAKE, HYSTERESIS_BRAKE_PRESSURE, cruise.update(80.0, 40.0));
    }

    @ParameterizedTest(name = "invalid distance {0} preserves braking mode")
    @MethodSource("invalidNonNegativeNumbers")
    void update_whenDistanceIsInvalid_throwsAndKeepsBrakingMode(double invalidDistance) {
        assertDecision(AdaptiveCruise.Mode.BRAKE, 25.0, cruise.update(80.0, 30.0));

        assertThrows(IllegalArgumentException.class, () -> cruise.update(80.0, invalidDistance));

        assertDecision(AdaptiveCruise.Mode.BRAKE, HYSTERESIS_BRAKE_PRESSURE, cruise.update(80.0, 40.0));
    }

    static List<Double> invalidNonNegativeNumbers() {
        return List.of(-Double.MIN_VALUE, -1.0, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    private void assertDecision(AdaptiveCruise.Mode expectedMode, double expectedAmount,
                                AdaptiveCruise.Decision actualDecision) {

        assertEquals(expectedMode, actualDecision.mode());
        assertEquals(expectedAmount, actualDecision.amount(), AMOUNT_TOLERANCE);
    }
}
