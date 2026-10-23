package autopilot.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SteeringControllerTest {
    private static final double CENTER_DEGREES = 0.0;
    private static final double SMALL_ANGLE_DEGREES = 30.0;
    private static final double MAX_CHANGE_PER_TICK_DEGREES = 60.0;
    private static final double PHYSICAL_LIMIT_DEGREES = 450.0;
    private static final double HIGH_SPEED_LIMIT_DEGREES = 90.0;
    private static final double NORMAL_SPEED_KMH = 80.0;
    private static final double HIGH_SPEED_THRESHOLD_KMH = 120.0;
    private static final double JUST_ABOVE_HIGH_SPEED_KMH = Math.nextUp(HIGH_SPEED_THRESHOLD_KMH);
    private static final double DISTANT_TARGET_DEGREES = 150.0;

    private SteeringController steering;

    @BeforeEach
    void setUp() {
        // A fresh controller starts centered at 0 degrees.
        steering = new SteeringController();
    }

    @ParameterizedTest(name = "request {0} degrees at {1} km/h from center -> {2} degrees")
    @CsvSource({
            "0.0,   80.0,   0.0",
            "30.0,  80.0,  30.0",
            "-30.0, 80.0, -30.0",
            "60.0,  80.0,  60.0",
            "-60.0, 80.0, -60.0",
            "61.0,  80.0,  60.0",
            "-61.0, 80.0, -60.0",
            "60.0,   0.0,  60.0"
    })
    void update_whenStartingCentered_movesAtMostOneStep(
            double requestedAngle, double speedKmh, double expectedAngle) {
        double actualAngle = steering.update(requestedAngle, speedKmh);

        assertEquals(expectedAngle, actualAngle);
    }

    @ParameterizedTest(name = "request {0} degrees at {1} km/h -> target {2} degrees")
    @MethodSource("targetLimitCases")
    void update_whenRequestIsNearLimit_clampsTargetToSpeedDependentRange(
            double requestedAngle, double speedKmh, double expectedTarget) {
        steering.update(requestedAngle, speedKmh);

        assertEquals(expectedTarget, steering.getTargetAngleDegrees());
    }

    static List<Arguments> targetLimitCases() {
        return List.of(
                Arguments.of(449.0, HIGH_SPEED_THRESHOLD_KMH, 449.0),
                Arguments.of(PHYSICAL_LIMIT_DEGREES, HIGH_SPEED_THRESHOLD_KMH, PHYSICAL_LIMIT_DEGREES),
                Arguments.of(451.0, HIGH_SPEED_THRESHOLD_KMH, PHYSICAL_LIMIT_DEGREES),
                Arguments.of(-449.0, HIGH_SPEED_THRESHOLD_KMH, -449.0),
                Arguments.of(-PHYSICAL_LIMIT_DEGREES, HIGH_SPEED_THRESHOLD_KMH, -PHYSICAL_LIMIT_DEGREES),
                Arguments.of(-451.0, HIGH_SPEED_THRESHOLD_KMH, -PHYSICAL_LIMIT_DEGREES),

                Arguments.of(89.0, JUST_ABOVE_HIGH_SPEED_KMH, 89.0),
                Arguments.of(HIGH_SPEED_LIMIT_DEGREES, JUST_ABOVE_HIGH_SPEED_KMH, HIGH_SPEED_LIMIT_DEGREES),
                Arguments.of(91.0, JUST_ABOVE_HIGH_SPEED_KMH, HIGH_SPEED_LIMIT_DEGREES),
                Arguments.of(-89.0, JUST_ABOVE_HIGH_SPEED_KMH, -89.0),
                Arguments.of(-HIGH_SPEED_LIMIT_DEGREES, JUST_ABOVE_HIGH_SPEED_KMH, -HIGH_SPEED_LIMIT_DEGREES),
                Arguments.of(-91.0, JUST_ABOVE_HIGH_SPEED_KMH, -HIGH_SPEED_LIMIT_DEGREES));
    }

    @ParameterizedTest(name = "repeated updates toward {0} degrees stop at the target")
    @CsvSource({
            "150.0,    60.0,  120.0",
            "-150.0,  -60.0, -120.0"
    })
    void update_whenTargetNeedsSeveralSteps_reachesTargetWithoutOvershooting(
            double requestedAngle, double expectedFirstAngle, double expectedSecondAngle) {
        assertEquals(expectedFirstAngle, steering.update(requestedAngle, NORMAL_SPEED_KMH));
        assertEquals(expectedSecondAngle, steering.update(requestedAngle, NORMAL_SPEED_KMH));
        assertEquals(requestedAngle, steering.update(requestedAngle, NORMAL_SPEED_KMH));
        assertEquals(requestedAngle, steering.update(requestedAngle, NORMAL_SPEED_KMH));
    }

    @Test
    void update_whenTargetChangesDirection_movesFromCurrentAngleTowardNewTarget() {
        steering.update(DISTANT_TARGET_DEGREES, NORMAL_SPEED_KMH);

        double actualAngle = steering.update(-DISTANT_TARGET_DEGREES, NORMAL_SPEED_KMH);

        assertEquals(CENTER_DEGREES, actualAngle);
    }

    @ParameterizedTest(name = "speed increase from {0} degrees -> current {1}, target {2}")
    @CsvSource({
            "300.0,   240.0,  90.0",
            "-300.0, -240.0, -90.0"
    })
    void update_whenSpeedIncreases_reducesTargetWithoutJumpingCurrentAngle(
            double initialAngle, double expectedAngle, double expectedTarget) {

        int setupTicks = 5;
        for (int tick = 0; tick < setupTicks; tick++) {
            steering.update(initialAngle, NORMAL_SPEED_KMH);
        }
        assertEquals(initialAngle, steering.getCurrentAngleDegrees());

        double actualAngle = steering.update(initialAngle, JUST_ABOVE_HIGH_SPEED_KMH);

        assertEquals(expectedAngle, actualAngle);
        assertEquals(expectedTarget, steering.getTargetAngleDegrees());
    }

    @Test
    void update_whenSpeedDropsToThreshold_restoresPhysicalTargetRange() {
        steering.update(PHYSICAL_LIMIT_DEGREES, JUST_ABOVE_HIGH_SPEED_KMH);

        double actualAngle = steering.update(PHYSICAL_LIMIT_DEGREES, HIGH_SPEED_THRESHOLD_KMH);

        assertEquals(2 * MAX_CHANGE_PER_TICK_DEGREES, actualAngle);
        assertEquals(PHYSICAL_LIMIT_DEGREES, steering.getTargetAngleDegrees());
    }

    @ParameterizedTest(name = "self-center from {0} degrees reaches zero")
    @ValueSource(doubles = {
            -MAX_CHANGE_PER_TICK_DEGREES, -SMALL_ANGLE_DEGREES, CENTER_DEGREES,
            SMALL_ANGLE_DEGREES, MAX_CHANGE_PER_TICK_DEGREES
    })
    void selfCenter_whenAngleIsWithinOneStep_reachesZero(double initialAngle) {
        steering.update(initialAngle, NORMAL_SPEED_KMH);

        double actualAngle = steering.selfCenter();

        assertEquals(CENTER_DEGREES, actualAngle);
    }

    @ParameterizedTest(name = "self-center after two updates toward {0} degrees")
    @CsvSource({
            "150.0,   60.0",
            "-150.0, -60.0"
    })
    void selfCenter_whenSeveralStepsAreNeeded_movesInStepsAndStopsAtZero(
            double requestedAngle, double expectedFirstCenteredAngle) {
        steering.update(requestedAngle, NORMAL_SPEED_KMH);
        steering.update(requestedAngle, NORMAL_SPEED_KMH);

        assertEquals(expectedFirstCenteredAngle, steering.selfCenter());
        assertEquals(CENTER_DEGREES, steering.selfCenter());
        assertEquals(CENTER_DEGREES, steering.selfCenter());
    }

    @ParameterizedTest(name = "invalid requested angle {0} preserves steering state")
    @MethodSource("nonFiniteAngles")
    void update_whenAngleIsInvalid_throwsAndKeepsCurrentAndTargetAngles(double invalidAngle) {
        steering.update(DISTANT_TARGET_DEGREES, NORMAL_SPEED_KMH);

        assertThrows(IllegalArgumentException.class, () -> steering.update(invalidAngle, NORMAL_SPEED_KMH));
        assertEquals(MAX_CHANGE_PER_TICK_DEGREES, steering.getCurrentAngleDegrees());
        assertEquals(DISTANT_TARGET_DEGREES, steering.getTargetAngleDegrees());
    }

    static List<Double> nonFiniteAngles() {
        return List.of(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    @ParameterizedTest(name = "invalid speed {0} preserves steering state")
    @MethodSource("invalidSpeeds")
    void update_whenSpeedIsInvalid_throwsAndKeepsCurrentAndTargetAngles(double invalidSpeed) {
        steering.update(DISTANT_TARGET_DEGREES, NORMAL_SPEED_KMH);

        assertThrows(IllegalArgumentException.class, () -> steering.update(-DISTANT_TARGET_DEGREES, invalidSpeed));
        assertEquals(MAX_CHANGE_PER_TICK_DEGREES, steering.getCurrentAngleDegrees());
        assertEquals(DISTANT_TARGET_DEGREES, steering.getTargetAngleDegrees());
    }

    static List<Double> invalidSpeeds() {
        return List.of(-Double.MIN_VALUE, -1.0, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }
}
