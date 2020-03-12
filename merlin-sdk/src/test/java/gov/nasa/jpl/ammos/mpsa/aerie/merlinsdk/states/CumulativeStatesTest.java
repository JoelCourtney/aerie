package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class CumulativeStatesTest {


    public String name1 = "state 1";
    public String name2 = "state 2";

    public int value1 = 10;
    public double value2 = 15.5;

    public CumulativeState<Integer> state1 = new CumulativeState.Integer(name1, value1);
    public CumulativeState<Double> state2 = new CumulativeState.Double(name2, value2);

    public class MockStateContainer implements StateContainer {
        public List<State<?>> getStateList() {
            return List.of();
        }
    }
    public SimulationEngine mockEngine = new SimulationEngine(
        SimulationInstant.origin(),
        List.of(),
        new MockStateContainer());


    @Before
    public void setup(){
        state1.setEngine(mockEngine);
        state2.setEngine(mockEngine);
        assert(state1.get() == value1);
        assert(state2.get() == value2);
    }

    @After
    public void teardown(){
        state1 = null;
        state2 = null;
    }

    @Test
    public void getValue(){
        assert(state1.get().equals(value1));
        assert(state2.get().equals(value2));
    }

    @Test
    public void increment(){
        int inc1 = 10;
        double inc2 = 10.0;
        state1.increment(inc1);
        state2.increment(inc2);
        assert(state1.get().equals(value1 + inc1));
        assert(state2.get().equals(value2 + inc2));
    }

    @Test
    public void decrement(){
        int dec1 = 13;
        double dec2 = -34.2;
        state1.decrement(dec1);
        state2.decrement(dec2);
        assert(state1.get().equals(value1 - dec1));
        assert(state2.get().equals(value2 - dec2));
    }
}
