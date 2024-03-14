/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2021 The ORIS Authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package results;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import org.apache.commons.math3.util.Pair;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.oristool.models.pn.PostUpdater;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.RewardRate;
import org.oristool.models.stpn.SteadyStateSolution;
import org.oristool.models.stpn.steady.RegSteadyState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

/**
 * Steady-state unavailability for many rejuvenation periods
 */
public class BestPeriodTrivedi {
    
    private static enum State {
        UP, ERR, DOWN
    }
    static double errorRate = 1.0 / 240.0;
    static double failRate = 1.0 / 2160.0;
    static double repairRate = 2.0;
    static double rejRate = 60.0;
    
    private static BigDecimal simulation(BigDecimal period) {

        RandomGenerator rand = RandomGenerator.getDefault();
        int events = 10000000;
        
        // initial state
        State state = State.UP;
        BigDecimal ttf = new BigDecimal(rand.nextExponential() / errorRate);
        BigDecimal clock = period; 

        BigDecimal unavailTime = BigDecimal.ZERO;
        BigDecimal totalTime = BigDecimal.ZERO;
        
        for (int i = 0; i < events; i++) {
            if (clock != null && clock.compareTo(ttf) < 0) {
                // the rejuvenation clock is first
                BigDecimal rejTime = new BigDecimal(rand.nextExponential() / rejRate);
                unavailTime = unavailTime.add(rejTime);
                totalTime = totalTime.add(clock).add(rejTime);
                // resample ttf in up state after rejuvenation, restart clock
                state = State.UP;
                ttf = new BigDecimal(rand.nextExponential() / errorRate);
                clock = period;
            } else {
                // the model event is first
                totalTime = totalTime.add(ttf);
                switch (state) {
                case UP:    // decrease rej clock, move to ERR
                    clock = clock.subtract(ttf);
                    state = State.ERR;
                    ttf = new BigDecimal(rand.nextExponential() / failRate);
                    break;
                case ERR:   // disable rej clock, move to DOWN
                    clock = null;  // no rejuvenation during repair
                    state = State.DOWN;
                    ttf = new BigDecimal(rand.nextExponential() / repairRate);
                    break;
                case DOWN:  // enable rej clock, track DOWN time, move to UP
                    clock = period;
                    unavailTime = unavailTime.add(ttf);
                    state = State.UP;
                    ttf = new BigDecimal(rand.nextExponential() / errorRate);
                    break;
                }
            }
        }
        
        return unavailTime.divide(totalTime, MathContext.DECIMAL128); 
    }
    
    private static BigDecimal analysis(BigDecimal period) {
        RewardRate unavailability = RewardRate.fromString("Down + Detected + Rej > 0");
        Pair<PetriNet, Marking> model = model(period);
        SteadyStateSolution<Marking> solution = RegSteadyState.builder().build().compute(model.getFirst(), model.getSecond());
        SteadyStateSolution<RewardRate> rewards = SteadyStateSolution.computeRewards(solution, unavailability);
        BigDecimal result = rewards.getSteadyState().get(unavailability);
        return result;

    }

    public static Pair<PetriNet, Marking> model(BigDecimal rejuvenationPeriod) {
        
        PetriNet pn = new PetriNet();
        
        Place Up = pn.addPlace("Up");
        Place Err = pn.addPlace("Err");
        Place Down = pn.addPlace("Down");
        Place Clock = pn.addPlace("Clock");
        Place Rej = pn.addPlace("Rej");
        
        Transition error = pn.addTransition("error");
        Transition fail = pn.addTransition("fail");
        Transition repair = pn.addTransition("repair");
        Transition rejFromErr = pn.addTransition("rejFromErr");
        Transition rejFromUp = pn.addTransition("rejFromUp");
        Transition waitClock = pn.addTransition("waitClock");
        
        // cannot degrade or fail during rejuvenation
        pn.addInhibitorArc(Rej, error);
        pn.addInhibitorArc(Rej, fail);

        // clock stops when repair starts
        pn.addInhibitorArc(Down, waitClock);

        // clock starts the rejuvenation
        pn.addPrecondition(Clock, waitClock);
        pn.addPostcondition(waitClock, Rej);

        // rejFromOk starts after the clock if in Up;
        // at the end, it enables the clock again, state is Up
        pn.addPrecondition(Rej, rejFromUp);
        pn.addPrecondition(Up, rejFromUp);
        pn.addPostcondition(rejFromUp, Clock);
        pn.addPostcondition(rejFromUp, Up);

        // rejFromReg starts after the clock if in Err;
        // at the end it enables the clock again, state is Up
        pn.addPrecondition(Rej, rejFromErr);
        pn.addPrecondition(Err, rejFromErr);
        pn.addPostcondition(rejFromErr, Clock);
        pn.addPostcondition(rejFromErr, Up);
        
        // lifecycle: Up -> Err -> Down -> Detected -> Up 
        pn.addPrecondition(Up, error);
        pn.addPostcondition(error, Err);
        pn.addPrecondition(Err, fail);
        pn.addPostcondition(fail, Down);
        pn.addPrecondition(Down, repair);
        pn.addPostcondition(repair, Up);

        // repair resets rejuvenation 
        repair.addFeature(new PostUpdater("Clock=1, Rej=0", pn));
        
        // rejuvenation clock deterministic with priority
        waitClock.addFeature(new Priority(0));
        waitClock.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriod));

        error.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal(errorRate)));
        fail.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal(failRate)));
        repair.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal(repairRate)));
        rejFromErr.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal(rejRate)));
        rejFromUp.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal(rejRate)));
        
        Marking marking = new Marking();
        marking.setTokens(Clock, 1);
        marking.setTokens(Up, 1);
        
        return new Pair<>(pn, marking);
    }
    
    public static void main(String[] args) {
        
        int periodMin = 100;
        int periodMax = 2000;
        int periodDelta = 50;
        List<BigDecimal> periods = new ArrayList<>();
        for (int period = periodMin; period <= periodMax; period += periodDelta) {
            periods.add(new BigDecimal(period));
        }
        
        System.out.print("Analysis... ");
        List<BigDecimal> analysis = new ArrayList<>();
        long start = System.nanoTime();
        BigDecimal bestPeriodAnalysis = null;
        BigDecimal bestMetricAnalysis = null;
        for (int i = 0; i < periods.size(); i++) {
            BigDecimal result = analysis(periods.get(i));
            analysis.add(result);
            
            if (bestMetricAnalysis == null || bestMetricAnalysis.compareTo(result) > 0) {
                bestPeriodAnalysis = periods.get(i);
                bestMetricAnalysis = result;
            }
        }
        System.out.println((System.nanoTime()-start)/1000000 + " ms");
        System.out.println("Best period: " + bestPeriodAnalysis + ", min is " + bestMetricAnalysis);

               
        System.out.print("Simulation... ");
        List<BigDecimal> simulation = new ArrayList<>();
        start = System.nanoTime();
        for (int i = 0; i < periods.size(); i++) {
            simulation.add(simulation(periods.get(i)));
        }
        System.out.println((System.nanoTime()-start)/1000000 + " ms");

        XYChart chart = new XYChartBuilder().width(800).height(600)
                .title("Optimal Rejuvenation Period")
                .xAxisTitle("Rejuvenation Period (hours)")
                .yAxisTitle("Unavailability = P( Down || Rej )").build();
        chart.getStyler().setXAxisMin(1.0);
        chart.getStyler().setLegendPosition(LegendPosition.InsideNE);        
        chart.addSeries("Analysis", periods, analysis);
        chart.addSeries("Simulation", periods, simulation);
        new SwingWrapper<>(chart).displayChart();
    }
}
