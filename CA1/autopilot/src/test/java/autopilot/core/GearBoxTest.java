package autopilot.core;

import autopilot.exception.IllegalGearChangeException;
import autopilot.exception.SensorFailureException;
import autopilot.sensor.SpeedSensor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GearBoxTest {
    private static final double STOPPED_SPEED_KMH = 0.0;
    private static final double MOVING_SPEED_KMH = 50.0;
    private static final double REVERSE_ENTRY_LIMIT_KMH = 5.0;
    private static final double JUST_BELOW_REVERSE_LIMIT_KMH = Math.nextDown(REVERSE_ENTRY_LIMIT_KMH);
    private static final double JUST_ABOVE_REVERSE_LIMIT_KMH = Math.nextUp(REVERSE_ENTRY_LIMIT_KMH);
    private static final double SMALLEST_POSITIVE_SPEED_KMH = Double.MIN_VALUE;

    private StubSpeedSensor speedSensor;
    private GearBox gearBox;

    @BeforeEach
    void setUp() {
        speedSensor = new StubSpeedSensor();
        gearBox = new GearBox(speedSensor);
    }

    @ParameterizedTest(name = "{0} -> {1} at {2} km/h is allowed")
    @MethodSource("allowedTransitions")
    void changeTo_whenTransitionIsAllowed_changesCurrentGear(
            GearBox.Gear initialGear, GearBox.Gear targetGear, double speedKmh) {
        moveToInitialGear(initialGear);
        speedSensor.speedKmh = speedKmh;

        gearBox.changeTo(targetGear);

        assertEquals(targetGear, gearBox.getCurrentGear());
    }

    static List<Arguments> allowedTransitions() {
        return List.of(
                Arguments.of(GearBox.Gear.P, GearBox.Gear.R, STOPPED_SPEED_KMH),
                Arguments.of(GearBox.Gear.P, GearBox.Gear.R, JUST_BELOW_REVERSE_LIMIT_KMH),
                Arguments.of(GearBox.Gear.R, GearBox.Gear.D, STOPPED_SPEED_KMH),
                Arguments.of(GearBox.Gear.D, GearBox.Gear.R, STOPPED_SPEED_KMH),
                Arguments.of(GearBox.Gear.R, GearBox.Gear.N, STOPPED_SPEED_KMH),
                Arguments.of(GearBox.Gear.R, GearBox.Gear.N, MOVING_SPEED_KMH),
                Arguments.of(GearBox.Gear.N, GearBox.Gear.D, STOPPED_SPEED_KMH),
                Arguments.of(GearBox.Gear.N, GearBox.Gear.D, MOVING_SPEED_KMH));
    }

    @ParameterizedTest(name = "{0} -> {1} at {2} km/h is rejected")
    @MethodSource("forbiddenTransitions")
    void changeTo_whenTransitionIsForbidden_throwsAndKeepsCurrentGear(
            GearBox.Gear initialGear, GearBox.Gear targetGear, double speedKmh) {
        moveToInitialGear(initialGear);
        speedSensor.speedKmh = speedKmh;

        assertThrows(IllegalGearChangeException.class, () -> gearBox.changeTo(targetGear));
        assertEquals(initialGear, gearBox.getCurrentGear());
    }

    static List<Arguments> forbiddenTransitions() {
        return List.of(
                Arguments.of(GearBox.Gear.P, GearBox.Gear.N, STOPPED_SPEED_KMH),
                Arguments.of(GearBox.Gear.P, GearBox.Gear.D, STOPPED_SPEED_KMH),
                Arguments.of(GearBox.Gear.R, GearBox.Gear.P, STOPPED_SPEED_KMH),
                Arguments.of(GearBox.Gear.N, GearBox.Gear.P, STOPPED_SPEED_KMH),
                Arguments.of(GearBox.Gear.N, GearBox.Gear.R, STOPPED_SPEED_KMH),
                Arguments.of(GearBox.Gear.D, GearBox.Gear.P, STOPPED_SPEED_KMH),
                Arguments.of(GearBox.Gear.D, GearBox.Gear.N, STOPPED_SPEED_KMH),

                Arguments.of(GearBox.Gear.P, GearBox.Gear.R, REVERSE_ENTRY_LIMIT_KMH),
                Arguments.of(GearBox.Gear.P, GearBox.Gear.R, JUST_ABOVE_REVERSE_LIMIT_KMH),

                Arguments.of(GearBox.Gear.R, GearBox.Gear.D, SMALLEST_POSITIVE_SPEED_KMH),
                Arguments.of(GearBox.Gear.R, GearBox.Gear.D, MOVING_SPEED_KMH),
                Arguments.of(GearBox.Gear.D, GearBox.Gear.R, SMALLEST_POSITIVE_SPEED_KMH),
                Arguments.of(GearBox.Gear.D, GearBox.Gear.R, MOVING_SPEED_KMH));
    }

    @ParameterizedTest(name = "requesting current gear {0} does not read the sensor")
    @EnumSource(GearBox.Gear.class)
    void changeTo_whenTargetIsCurrentGear_keepsGearWithoutReadingSensor(GearBox.Gear gear) {
        moveToInitialGear(gear);
        speedSensor.failure = new SensorFailureException("Sensor must not be read");

        gearBox.changeTo(gear);

        assertEquals(gear, gearBox.getCurrentGear());
    }

    @ParameterizedTest(name = "invalid sensor speed {0} is rejected")
    @MethodSource("invalidSpeeds")
    void changeTo_whenSpeedIsInvalid_throwsAndKeepsCurrentGear(double speedKmh) {
        speedSensor.speedKmh = speedKmh;

        assertThrows(IllegalArgumentException.class, () -> gearBox.changeTo(GearBox.Gear.R));
        assertEquals(GearBox.Gear.P, gearBox.getCurrentGear());
    }

    static List<Double> invalidSpeeds() {
        return List.of(
                -SMALLEST_POSITIVE_SPEED_KMH,
                -1.0,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY);
    }

    @Test
    void changeTo_whenTargetIsNull_throwsAndKeepsCurrentGear() {
        moveToInitialGear(GearBox.Gear.D);

        assertThrows(NullPointerException.class, () -> gearBox.changeTo(null));
        assertEquals(GearBox.Gear.D, gearBox.getCurrentGear());
    }

    @Test
    void changeTo_whenSensorFails_propagatesFailureAndKeepsCurrentGear() {
        moveToInitialGear(GearBox.Gear.D);
        SensorFailureException expectedFailure = new SensorFailureException("Speed unavailable");
        speedSensor.failure = expectedFailure;

        SensorFailureException actualFailure = assertThrows(
                SensorFailureException.class, () -> gearBox.changeTo(GearBox.Gear.R));

        assertSame(expectedFailure, actualFailure);
        assertEquals(GearBox.Gear.D, gearBox.getCurrentGear());
    }

    private void moveToInitialGear(GearBox.Gear initialGear) {
        speedSensor.speedKmh = STOPPED_SPEED_KMH;
        // The public API has no direct setter for the current gear.
        switch (initialGear) {
            case P -> { }
            case R -> gearBox.changeTo(GearBox.Gear.R);
            case N -> {
                gearBox.changeTo(GearBox.Gear.R);
                gearBox.changeTo(GearBox.Gear.N);
            }
            case D -> {
                gearBox.changeTo(GearBox.Gear.R);
                gearBox.changeTo(GearBox.Gear.D);
            }
        }
    }

    private static final class StubSpeedSensor implements SpeedSensor {
        private double speedKmh = STOPPED_SPEED_KMH;
        private SensorFailureException failure;

        @Override
        public double getSpeedKmh() {
            if (failure != null) {
                throw failure;
            }
            return speedKmh;
        }
    }
}
