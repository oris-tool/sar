package org.oristool.timebased.results;/* This program is part of the ORIS Tool.
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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.knowm.xchart.AnnotationLine;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.oristool.models.stpn.RewardRate;
import org.oristool.models.stpn.SteadyStateSolution;
import org.oristool.models.stpn.steady.RegSteadyState;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.timebased.models.PeriodicRejuvenationSkipIfDown;

/**
 * Steady-state unavailability, undetectedFailure, average metric for many rejuvenation periods
 */
public class BestPeriod {
    public static void main(String[] args) throws FileNotFoundException {
        
        int periodMin = 60;
        int periodMax = 4320;
        int periodDelta = 30;
        List<BigDecimal> periods = new ArrayList<>();
        for (int period = periodMin; period <= periodMax; period += periodDelta) {
            periods.add(new BigDecimal(period));
        }
        
        RewardRate unavailability = RewardRate.fromString("Down + Detected + Rej > 0");
        RewardRate uselessUnavailability = RewardRate.fromString("Ok + Rej > 0");
        RewardRate undetectedFailure = RewardRate.fromString("Down > 0");
        RewardRate averageReward = RewardRate.fromString("((Down + Detected + Rej > 0) + (Down > 0))/2");
        


        for (boolean enablingRestriction : List.of(true, false)) {
            System.out.println(">> Enabling restriction: " + enablingRestriction);
            PrintWriter csv = new PrintWriter(System.getProperty("user.dir") + "/plots/data/timebased/bestPeriod" + (enablingRestriction ? "EXP" : "GEN") + ".csv");
            csv.printf("period,unavailability,undetectedFailure,useless,averageReward\n");

            Map<RewardRate, List<BigDecimal>> series = new LinkedHashMap<>();       
            series.put(unavailability, new ArrayList<>());
            series.put(undetectedFailure, new ArrayList<>());
            series.put(uselessUnavailability, new ArrayList<>());
            series.put(averageReward, new ArrayList<>());

            BigDecimal bestPeriod = null;
            BigDecimal bestMetric = null;
            long start = System.nanoTime();
            for (int i = 0; i < periods.size(); i++) {
                Pair<PetriNet, Marking> model = PeriodicRejuvenationSkipIfDown.build(periods.get(i), enablingRestriction);
                SteadyStateSolution<Marking> solution = RegSteadyState.builder().build().compute(model.getFirst(), model.getSecond());
                SteadyStateSolution<RewardRate> rewards = SteadyStateSolution.computeRewards(solution, unavailability, undetectedFailure, uselessUnavailability, averageReward);
                series.get(unavailability).add(rewards.getSteadyState().get(unavailability));
                series.get(undetectedFailure).add(rewards.getSteadyState().get(undetectedFailure));
                series.get(uselessUnavailability).add(rewards.getSteadyState().get(uselessUnavailability));
                series.get(averageReward).add(rewards.getSteadyState().get(averageReward));
                
                BigDecimal metric = rewards.getSteadyState().get(averageReward);
                if (bestMetric == null || metric.compareTo(bestMetric) < 0) {
                    bestPeriod = periods.get(i);
                    bestMetric = metric;
                }
                csv.printf("%.3f,%.9f,%.9f,%.9f,%.9f\n",
                        periods.get(i), 
                        rewards.getSteadyState().get(unavailability),
                        rewards.getSteadyState().get(undetectedFailure),
                        rewards.getSteadyState().get(uselessUnavailability),
                        rewards.getSteadyState().get(averageReward));

                int j = 0;
            }
            csv.close();
            System.out.println(">> " + (System.nanoTime()-start)/1000000 + " ms");
            System.out.println(">> Best period is " + bestPeriod + " -> " + bestMetric);

            
            XYChart chart = new XYChartBuilder().width(800).height(600)
                    .xAxisTitle("Rejuvenation Period (hours)")
                    .yAxisTitle("Probability").build();
            chart.getStyler().setXAxisMin(1.0);
            chart.getStyler().setLegendPosition(LegendPosition.InsideNE);        
            chart.addSeries("P(System Unavailable)", periods, series.get(unavailability));
            chart.addSeries("P(Undetected Failure)", periods, series.get(undetectedFailure));
            chart.addSeries("P(Usless)", periods, series.get(uselessUnavailability));
            chart.addSeries("[P(System Unavailable)+P(Undetected Failure)]/2", periods, series.get(averageReward));
            chart.addAnnotation(new AnnotationLine(bestPeriod.doubleValue(), true, false));
            new SwingWrapper<>(chart).displayChart();
        }
    }
}
