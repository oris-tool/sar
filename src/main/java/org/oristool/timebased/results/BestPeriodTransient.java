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
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.oristool.analyzer.log.NoOpLogger;
import org.oristool.models.stpn.RewardRate;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.TransientSolutionViewer;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.simulator.Sequencer;
import org.oristool.simulator.TimeSeriesRewardResult;
import org.oristool.simulator.rewards.ContinuousRewardTime;
import org.oristool.simulator.rewards.RewardEvaluator;
import org.oristool.simulator.stpn.STPNSimulatorComponentsFactory;
import org.oristool.simulator.stpn.TransientMarkingConditionProbability;
import org.oristool.wosar22.models.PeriodicRejuvenationSkipIfDown;

/**
 * Transient unreliability and unavailability 
 * for period=1300,2200 with or without enabling restriction
 */
public class BestPeriodTransient {
    
    public static void unavailability(Pair<PetriNet, Marking> model, String timeS, String timeB, String csvName) {
        BigDecimal timeBound = new BigDecimal(timeB);
        BigDecimal timeStep = new BigDecimal(timeS);
        BigDecimal error = new BigDecimal("0");

        RegTransient.Builder builder = RegTransient.builder();
        builder.timeBound(timeBound);
        builder.timeStep(timeStep);
        builder.greedyPolicy(timeBound, error);

        RewardRate unavailability = RewardRate.fromString("Down + Detected + Rej > 0");
        builder.markingFilter(RewardRate.nonZero(0.0, new RewardRate[] { unavailability }));
        
        TransientSolution<DeterministicEnablingState, Marking> probs = builder.build().compute(model.getFirst(), model.getSecond());
        TransientSolution<DeterministicEnablingState, RewardRate> instant = TransientSolution.computeRewards(false, probs, unavailability);
        TransientSolution<DeterministicEnablingState, RewardRate> cumulative = TransientSolution.computeRewards(true, probs, unavailability);

        new TransientSolutionViewer(instant);
        new TransientSolutionViewer(cumulative);

        instant.writeCSV("wosar22/plots/data/timebased/instantUnavailability_" + csvName + "_" + timeS + "_" + timeB + ".csv", 9);
        cumulative.writeCSV("wosar22/plots/data/timebased/cumulativeUnavailability_" + csvName + "_" + timeS + "_" + timeB + ".csv", 9);


    }
    
    public static void unreliability(Pair<PetriNet, Marking> model, String timeS, String timeB, String csvName) {
        BigDecimal timeBound = new BigDecimal(timeB);
        BigDecimal timeStep = new BigDecimal(timeS);
        BigDecimal error = new BigDecimal("0");

        RegTransient.Builder builder = RegTransient.builder();
        builder.timeBound(timeBound);
        builder.timeStep(timeStep);
        builder.greedyPolicy(timeBound, error);

        RewardRate unreliability = RewardRate.fromString("Down > 0");
        builder.stopOn(MarkingCondition.fromString("Down > 0"));
        builder.markingFilter(RewardRate.nonZero(0.0, new RewardRate[] { unreliability }));

        TransientSolution<DeterministicEnablingState, Marking> probs = builder.build().compute(model.getFirst(), model.getSecond());
        TransientSolution<DeterministicEnablingState, RewardRate> instant = TransientSolution.computeRewards(false, probs, unreliability);

        new TransientSolutionViewer(instant);

        instant.writeCSV("wosar22/plots/data/timebased/instantUnreliability_" + csvName + "_" + timeS + "_" + timeB + ".csv", 9);
    }

    public static void main(String[] args) {
        StringBuilder times = new StringBuilder(
                "EnablingRestriction, Period, Reward, Times"
        );

        for (BigDecimal period : List.of(new BigDecimal("600"), new BigDecimal("420"))) {
            for (boolean enablingRestriction : List.of(false, true)) {
                Pair<PetriNet, Marking> model = PeriodicRejuvenationSkipIfDown.build(period, enablingRestriction);
                String csvName = (enablingRestriction ? "EXP" : "GEN") + period;
                times.append("\n").append(csvName).append(", ").append(period.toString()).append(", Unavailability, ");
                long time = System.nanoTime();
                unavailability(model, "1.", "4320", csvName);
                times.append((System.nanoTime() - time)/1e9 + "s");

                times.append("\n").append(csvName).append(", ").append(period.toString()).append(", Unreliability, ");
                time = System.nanoTime();
                unreliability(model, "1.", "4320", csvName);
                times.append((System.nanoTime() - time)/1e9 + "s");
            }
        }

        System.out.println(times.toString());
    }
}
