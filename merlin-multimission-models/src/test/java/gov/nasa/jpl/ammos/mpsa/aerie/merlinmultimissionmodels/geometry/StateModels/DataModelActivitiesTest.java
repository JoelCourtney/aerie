package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.StateModels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.Activities.DownlinkData;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.Activities.InitializeBinDataVolume;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.Activities.TurnInstrumentOff;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.Activities.TurnInstrumentOn;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.BinModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.InstrumentModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.OnboardDataModelStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataModelActivitiesTest {


    @Test
    public void initBinsActivity(){
        System.out.println("\nBin activity init test start");

        //0. choose sim start time
        Instant simStart = SimulationInstant.origin();

        //1. create activities and add to job list
        InitializeBinDataVolume binInitActivity = new InitializeBinDataVolume();
        ActivityJob<OnboardDataModelStates> binInitJob = new ActivityJob<>(binInitActivity, simStart);
        List<ActivityJob<?>> activityJobList = new ArrayList<>();
        activityJobList.add(binInitJob);

        //2. create states
        OnboardDataModelStates states = new OnboardDataModelStates();

        //3. create engine and simulate
        SimulationEngine engine = new SimulationEngine(simStart, activityJobList, states);
        engine.simulate();

        for (BinModel x : states.getBinModelList()){
            x.printHistory();
            assert(x.getHistory().size() == 1.0);
            assert(x.getHistory().containsValue(0.0));
        }

        System.out.println("Bin activity init test end\n");
    }


    @Test
    public void turnInstrumentsOnActivity(){
        System.out.println("\nTurn instruments on activity init test start");

        //0. choose sim start time
        Instant simStart = SimulationInstant.origin();
        Instant simInstOn  = simStart.plus(1, TimeUnit.HOURS);

        //1. create activities and add to job list
        InitializeBinDataVolume binInitActivity = new InitializeBinDataVolume();
        ActivityJob<OnboardDataModelStates> binInitJob = new ActivityJob<>(binInitActivity, simStart);

        TurnInstrumentOn instrumentOn = new TurnInstrumentOn();
        instrumentOn.instrumentName = "instrument 1";
        instrumentOn.instrumentRate = 10.0;
        ActivityJob<OnboardDataModelStates> instrumentOnJob = new ActivityJob<>(instrumentOn, simInstOn);

        List<ActivityJob<?>> activityJobList = List.of(instrumentOnJob, binInitJob);

        //2. create states
        OnboardDataModelStates states = new OnboardDataModelStates();

        //3. create engine and simulate
        SimulationEngine engine = new SimulationEngine(simStart, activityJobList, states);
        engine.simulate();

        Map<Instant, Double> binMap = states.getBinByName("Bin 1").getHistory();
        assert(binMap.size() == 2);
        for (Double value : binMap.values()){
            assert(value == 0.0);
        }

        Map<Instant, Double> instrumentMap = states.getInstrumentByName("instrument 1").getHistory();
        assert(instrumentMap.size() == 1);
        assert(instrumentMap.containsValue(10.0));
        assert(states.getInstrumentByName("instrument 1").onStatus());

        for (InstrumentModel x : states.getInstrumentModelList()){
            x.printHistory();
        }

        for (BinModel x : states.getBinModelList()){
            x.printHistory();
        }


        System.out.println("Turn instruments on activity test end\n");
    }

    @Test
    public void turnInstrumentsOffActivity(){
        System.out.println("Turn instruments off activity test start\n");

        Instant simStart = SimulationInstant.origin();
        Instant simInstOff = simStart.plus(1, TimeUnit.MINUTES);
        Instant simInstOn  = simStart.plus(1, TimeUnit.HOURS);

        InitializeBinDataVolume binInitActivity = new InitializeBinDataVolume();
        ActivityJob<OnboardDataModelStates> binInitJob = new ActivityJob<>(binInitActivity, simStart);

        TurnInstrumentOn instrumentOn = new TurnInstrumentOn();
        instrumentOn.instrumentName = "instrument 1";
        instrumentOn.instrumentRate = 10.0;
        ActivityJob<OnboardDataModelStates> instrumentOnJob = new ActivityJob<>(instrumentOn, simInstOn);

        List<ActivityJob<?>> activityJobList = new ArrayList<>();
        activityJobList.add(binInitJob);
        activityJobList.add(instrumentOnJob);

        OnboardDataModelStates states = new OnboardDataModelStates();

        for (InstrumentModel x : states.getInstrumentModelList()){
            TurnInstrumentOff instrumentOff = new TurnInstrumentOff();
            instrumentOff.instrumentName = x.getName();
            ActivityJob<OnboardDataModelStates> activityJob = new ActivityJob<>(instrumentOff, simInstOff);
            activityJobList.add(activityJob);
        }

        SimulationEngine engine = new SimulationEngine(simStart, activityJobList, states);
        engine.simulate();

        for (InstrumentModel x : states.getInstrumentModelList()){
            x.printHistory();
        }

        for (BinModel x : states.getBinModelList()){
            x.printHistory();
        }

        Map<Instant, Double> binMap = states.getBinByName("Bin 1").getHistory();

        Map<Instant, Double> instrumentMap = states.getInstrumentByName("instrument 1").getHistory();

        assert(instrumentMap.size() == 2);
        assert(instrumentMap.containsValue(0.0));
        assert(instrumentMap.containsValue(10.0));

        assert(binMap.size() == 3);
        for (double x : binMap.values()){
            assert(x == 0.0);
        }
        System.out.println("Turn instruments off activity test end\n");
    }

    @Test
    public void downlinkActivity(){
        System.out.println("Downlink activity test start\n");

        Instant simStart = SimulationInstant.origin();
        Instant simInstOff = simStart.plus(1, TimeUnit.MINUTES);
        Instant simInstOn  = simStart.plus(1, TimeUnit.HOURS);
        Instant simDownlink = simStart.plus(5, TimeUnit.HOURS);

        InitializeBinDataVolume binInitActivity = new InitializeBinDataVolume();
        ActivityJob<OnboardDataModelStates> binInitJob = new ActivityJob<>(binInitActivity, simStart);

        TurnInstrumentOn instrumentOn = new TurnInstrumentOn();
        instrumentOn.instrumentName = "instrument 1";
        instrumentOn.instrumentRate = 10.0;
        ActivityJob<OnboardDataModelStates> instrumentOnJob = new ActivityJob<>(instrumentOn, simInstOn);

        List<ActivityJob<?>> activityJobList = new ArrayList<>();
        activityJobList.add(binInitJob);
        activityJobList.add(instrumentOnJob);

        OnboardDataModelStates states = new OnboardDataModelStates();

        for (InstrumentModel x : states.getInstrumentModelList()){
            TurnInstrumentOff instrumentOff = new TurnInstrumentOff();
            instrumentOff.instrumentName = x.getName();
            ActivityJob<OnboardDataModelStates> activityJob = new ActivityJob<>(instrumentOff, simInstOff);
            activityJobList.add(activityJob);
        }

        DownlinkData downlinkActivity = new DownlinkData();
        downlinkActivity.binID = "Bin 1";
        downlinkActivity.downlinkAll = true;
        ActivityJob<OnboardDataModelStates> downlinkJob = new ActivityJob<>(downlinkActivity, simDownlink);
        activityJobList.add(downlinkJob);

        SimulationEngine engine = new SimulationEngine(simStart, activityJobList, states);
        engine.simulate();

        for (InstrumentModel x : states.getInstrumentModelList()){
            x.printHistoryGraphFormat();
        }

        for (BinModel x : states.getBinModelList()){
            x.printHistoryGraphFormat();
        }

        Map<Instant, Double> binMap = states.getBinByName("Bin 1").getHistory();


        Duration delta = simDownlink.durationFrom(simInstOn);
        System.out.println("DELTA IS " + delta);
        System.out.println("Volume collected " + (delta.durationInMicroseconds / 1000000.0) * 10.0);
        assert((delta.durationInMicroseconds / 1000000.0) * 10 == (10.0 * 4 * 60 * 60));

        Map<Instant, Double> instrumentMap = states.getInstrumentByName("instrument 1").getHistory();

        System.out.println("Downlink activity test end\n");
    }

    @Test
    public void instrumentModel(){
        InstrumentModel ethemis = new InstrumentModel("E-THEMIS", 0.0);
        assert(!ethemis.onStatus());
        assert(ethemis.get() == 0.0);

        InstrumentModel suda = new InstrumentModel("SUDA", 10.0);
        assert(suda.onStatus());
        assert(suda.get() == 10.0);

        ethemis.setDataProtocol("Spacewire");
        assert(ethemis.getDataProtocol().equals("Spacewire"));

        BinModel bin1 = new BinModel("bin 1", ethemis);
        ethemis.setBin(bin1);
        assert(ethemis.getBinName().equals("bin 1"));

        InstrumentModel reason = new InstrumentModel("REASON", 12.0, bin1);
        assert(reason.onStatus());
        assert(reason.getBinName().equals("bin 1"));
        assert(reason.get() == 12.0);

        reason.setDataProtocol("UART");
        assert(reason.getDataProtocol().equals("UART"));
    }
}




























