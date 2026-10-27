package autopilot.core;

import autopilot.exception.SensorFailureException;
import autopilot.sensor.LaneSensor;
import autopilot.sensor.ObstacleSensor;
import autopilot.sensor.SpeedSensor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoDriverTest {

    private static final double TARGET_SPEED_KMH = 80.0;
    private static final double CRUISING_SPEED_KMH = 80.0;
    private static final double CLEAR_OBSTACLE_DISTANCE_METERS = 100.0;
    private static final double CENTERED_LANE_OFFSET_METERS = 0.0;
    private static final double POSITIVE_LANE_OFFSET_METERS = 1.0;
    private static final double NEGATIVE_LANE_OFFSET_METERS = -1.0;
    private static final double SAFE_MODE_BRAKE_PRESSURE = 30.0;
    private static final double CENTERED_STEERING_ANGLE_DEGREES = 0.0;
    private static final double DOUBLE_TOLERANCE = 1.0e-9;
    private static final String SENSOR_FAILURE_MESSAGE = "Sensor failure";

    private SpeedSensor speedSensor;
    private ObstacleSensor obstacleSensor;
    private LaneSensor laneSensor;
    private AutoDriver autoDriver;

    @BeforeEach
    void setUp() {
        speedSensor = () -> CRUISING_SPEED_KMH;
        obstacleSensor = () -> CLEAR_OBSTACLE_DISTANCE_METERS;
        laneSensor = () -> CENTERED_LANE_OFFSET_METERS;
        autoDriver = createAutoDriver(speedSensor, obstacleSensor, laneSensor);
    }

    @Test
    void tick_whenSensorsReportNormalConditions_returnsNormalDriveResult() {
        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);

        assertNotNull(result);
        assertFalse(result.safeMode());
        assertEquals(AdaptiveCruise.Mode.HOLD, result.cruiseMode());
        assertEquals(CENTERED_STEERING_ANGLE_DEGREES, result.steeringAngleDegrees(),
                DOUBLE_TOLERANCE);
    }

    @ParameterizedTest(name = "lane offset {0} meters produces steering angle with sign {1}")
    @CsvSource({
            "1.0, NEGATIVE",
            "-1.0, POSITIVE"
    })
    void tick_whenLaneOffsetExceedsThreshold_correctsSteeringDirection(
            double laneOffsetMeters, SteeringDirection expectedDirection) {
        laneSensor = () -> laneOffsetMeters;
        autoDriver = createAutoDriver(speedSensor, obstacleSensor, laneSensor);

        double steeringAngle = autoDriver.tick(GearBox.Gear.D).steeringAngleDegrees();

        if (expectedDirection == SteeringDirection.NEGATIVE) {

            assertTrue(steeringAngle < CENTERED_STEERING_ANGLE_DEGREES);

        } else {

            assertTrue(steeringAngle > CENTERED_STEERING_ANGLE_DEGREES);
        }
    }

    @ParameterizedTest(name = "failure of {0} sensor activates safe mode")
    @EnumSource(SensorType.class)
    void tick_whenSensorFails_entersSafeModeWithControlledBraking(SensorType sensorType) {
        autoDriver = createAutoDriverWithFailedSensor(sensorType);

        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);

        assertSafeModeResult(result);
    }

    private static AutoDriver createAutoDriver(
            SpeedSensor speedSensor,
            ObstacleSensor obstacleSensor,
            LaneSensor laneSensor) {
        return new AutoDriver(speedSensor, obstacleSensor, laneSensor, TARGET_SPEED_KMH);
    }

    private AutoDriver createAutoDriverWithFailedSensor(SensorType sensorType) {

        SpeedSensor speed = speedSensor;
        ObstacleSensor obstacle = obstacleSensor;
        LaneSensor lane = laneSensor;

        switch (sensorType) {
            case SPEED -> speed = () -> {
                throw new SensorFailureException(SENSOR_FAILURE_MESSAGE);
            };

            case OBSTACLE -> obstacle = () -> {
                throw new SensorFailureException(SENSOR_FAILURE_MESSAGE);
            };

            case LANE -> lane = () -> {
                throw new SensorFailureException(SENSOR_FAILURE_MESSAGE);
            };

        }

        return createAutoDriver(speed, obstacle, lane);
    }

    private void assertSafeModeResult(AutoDriver.DriveResult result) {

        assertTrue(result.safeMode());
        assertEquals(SAFE_MODE_BRAKE_PRESSURE, result.brakePressure(), DOUBLE_TOLERANCE);
        assertEquals(CENTERED_STEERING_ANGLE_DEGREES, result.steeringAngleDegrees(),DOUBLE_TOLERANCE);
        assertEquals(AdaptiveCruise.Mode.BRAKE, result.cruiseMode());
        assertFalse(result.warningActive());
    }

    private enum SensorType {
        SPEED,
        OBSTACLE,
        LANE
    }

    private enum SteeringDirection {
        NEGATIVE,
        POSITIVE
    }
}
