package airplane;

import airplane.components.Engine;
import airplane.components.FuelSystem;
import airplane.components.LandingGear;
import airplane.components.NavigationSystem;
import airplane.exceptions.FlightException;
import airplane.models.FlightStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AircraftTest {

    private Engine engine;
    private FuelSystem fuelSystem;
    private LandingGear landingGear;
    private NavigationSystem navigationSystem;
    private Aircraft aircraft;

    @BeforeEach
    void setUp() {
        engine = mock(Engine.class);
        fuelSystem = mock(FuelSystem.class);
        landingGear = mock(LandingGear.class);
        navigationSystem = mock(NavigationSystem.class);

        aircraft = new Aircraft(engine, fuelSystem, landingGear, navigationSystem);
    }

    private void stubReadyForTakeoff() {
        when(engine.isOperational()).thenReturn(true);
        when(fuelSystem.hasEnoughFuel()).thenReturn(true);
        when(navigationSystem.isRouteAvailable()).thenReturn(true);
    }

    @Test
    @DisplayName("Test 1: prepareForTakeoff succeeds when all components are ready")
    void prepareForTakeoff_whenAllConditionsMet_transitionsToReady() {
        stubReadyForTakeoff();
        aircraft.prepareForTakeoff();
        assertEquals(FlightStatus.READY, aircraft.getStatus());
        verify(engine, times(1)).isOperational();
        verify(fuelSystem, times(1)).hasEnoughFuel();
        verify(navigationSystem, times(1)).isRouteAvailable();
        verify(engine, times(0)).start();
    }

    @Test
    @DisplayName("Test 2: prepareForTakeoff throws FlightException on component failure and uses reset()")
    void prepareForTakeoff_whenPreconditionsFail_throwsFlightException() {
        when(engine.isOperational()).thenReturn(false);

        assertThrows(FlightException.class, () -> aircraft.prepareForTakeoff());
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verify(engine, times(1)).isOperational();
        verify(fuelSystem, times(0)).hasEnoughFuel();
        reset(engine, fuelSystem, navigationSystem);
        when(engine.isOperational()).thenReturn(true);
        when(fuelSystem.hasEnoughFuel()).thenReturn(false);

        assertThrows(FlightException.class, () -> aircraft.prepareForTakeoff());
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verify(engine, times(1)).isOperational();
        verify(fuelSystem, times(1)).hasEnoughFuel();
        verify(navigationSystem, times(0)).isRouteAvailable();
        reset(engine, fuelSystem, navigationSystem);
        when(engine.isOperational()).thenReturn(true);
        when(fuelSystem.hasEnoughFuel()).thenReturn(true);
        when(navigationSystem.isRouteAvailable()).thenReturn(false);

        assertThrows(FlightException.class, () -> aircraft.prepareForTakeoff());
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verify(navigationSystem, times(1)).isRouteAvailable();
    }

    @Test
    @DisplayName("Test 3: prepareForTakeoff propagates exception when navigation system fails using thenThrow()")
    void prepareForTakeoff_whenNavigationThrowsException_propagatesAndRemainsGrounded() {
        when(engine.isOperational()).thenReturn(true);
        when(fuelSystem.hasEnoughFuel()).thenReturn(true);
        when(navigationSystem.isRouteAvailable())
                .thenThrow(new IllegalStateException("Navigation sensor hardware fault"));
        assertThrows(IllegalStateException.class, () -> aircraft.prepareForTakeoff());
        assertEquals(FlightStatus.GROUNDED, aircraft.getStatus());
        verify(navigationSystem, times(1)).isRouteAvailable();
    }

    @Test
    @DisplayName("Test 4: takeOff starts engine, retracts landing gear, and changes status to IN_FLIGHT")
    void takeOff_whenAircraftIsReady_startsEngineRetractsGearAndFlies() {
        stubReadyForTakeoff();
        aircraft.prepareForTakeoff();
        doNothing().when(engine).start();
        doNothing().when(landingGear).retract();
        aircraft.takeOff();
        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        verify(engine, times(1)).start();
        verify(landingGear, times(1)).retract();
    }

    @Test
    @DisplayName("Test 5: takeOff changes status to ABORTED and throws FlightException if engine fails to start")
    void takeOff_whenEngineStartThrowsException_abortsTakeoff() {
        stubReadyForTakeoff();
        aircraft.prepareForTakeoff();

        doThrow(new IllegalStateException("Engine failed during ignition")).when(engine).start();
        assertThrows(FlightException.class, () -> aircraft.takeOff());
        assertEquals(FlightStatus.ABORTED, aircraft.getStatus());
        verify(engine, times(1)).start();
        verify(landingGear, times(0)).retract();
    }

    @Test
    @DisplayName("Test 6: cruise consumes fuel normally when engine is operational using doNothing() and any()")
    void cruise_whenEngineIsOperational_consumesFuel() {
        stubReadyForTakeoff();
        aircraft.prepareForTakeoff();
        aircraft.takeOff();

        when(engine.isOperational()).thenReturn(true);
        doNothing().when(fuelSystem).consumeFuel(any(Double.class));
        aircraft.cruise(150.0);
        aircraft.cruise(85.5);
        assertEquals(FlightStatus.IN_FLIGHT, aircraft.getStatus());
        verify(fuelSystem, times(2)).consumeFuel(any(Double.class));
        verify(landingGear, times(0)).deploy();
    }

    @Test
    @DisplayName("Test 7: cruise performs emergency landing without consuming fuel when engine is not operational")
    void cruise_whenEngineFailsInFlight_performsEmergencyLanding() {
        stubReadyForTakeoff();
        aircraft.prepareForTakeoff();
        aircraft.takeOff();
        reset(engine, fuelSystem, landingGear);
        when(engine.isOperational()).thenReturn(false);
        doNothing().when(landingGear).deploy();
        doNothing().when(engine).stop();
        aircraft.cruise(100.0);
        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(engine, times(1)).isOperational();
        verify(fuelSystem, times(0)).consumeFuel(any(Double.class));
        verify(landingGear, times(1)).deploy();
        verify(engine, times(1)).stop();
    }

    @Test
    @DisplayName("Test 8: cruise performs emergency landing and throws FlightException when fuel consumption fails")
    void cruise_whenFuelConsumptionThrowsException_emergencyLandsAndThrowsFlightException() {
        stubReadyForTakeoff();
        aircraft.prepareForTakeoff();
        aircraft.takeOff();

        when(engine.isOperational()).thenReturn(true);
        doThrow(new IllegalStateException("Not enough fuel."))
                .when(fuelSystem).consumeFuel(any(Double.class));
        doNothing().when(landingGear).deploy();
        doNothing().when(engine).stop();
        assertThrows(FlightException.class, () -> aircraft.cruise(500.0));
        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(fuelSystem, times(1)).consumeFuel(any(Double.class));
        verify(landingGear, times(1)).deploy();
        verify(engine, times(1)).stop();
    }

    @Test
    @DisplayName("Test 9: land deploys gear, stops engine, changes status to LANDED, and allows re-preparation")
    void land_whenInFlight_completesLandingAndAllowsSubsequentPreparation() {
        stubReadyForTakeoff();
        aircraft.prepareForTakeoff();
        aircraft.takeOff();

        doNothing().when(landingGear).deploy();
        doNothing().when(engine).stop();
        aircraft.land();
        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        verify(landingGear, times(1)).deploy();
        verify(engine, times(1)).stop();
        reset(engine, fuelSystem, navigationSystem);
        stubReadyForTakeoff();

        aircraft.prepareForTakeoff();
        assertEquals(FlightStatus.READY, aircraft.getStatus());
    }

    @Test
    @DisplayName("Test 10: invalid lifecycle calls and non-positive fuel amounts throw expected exceptions")
    void invalidOperations_whenLifecycleCallsOrFuelAmountsAreInvalid_throwsExpectedExceptions() {
        assertThrows(FlightException.class, () -> aircraft.takeOff());
        assertThrows(FlightException.class, () -> aircraft.cruise(50.0));
        assertThrows(FlightException.class, () -> aircraft.land());
        assertThrows(FlightException.class, () -> aircraft.emergencyLand());
        stubReadyForTakeoff();
        aircraft.prepareForTakeoff();
        aircraft.takeOff();

        assertThrows(IllegalArgumentException.class, () -> aircraft.cruise(0.0));
        assertThrows(IllegalArgumentException.class, () -> aircraft.cruise(-25.0));
        verify(fuelSystem, times(0)).consumeFuel(any(Double.class));
    }
}

