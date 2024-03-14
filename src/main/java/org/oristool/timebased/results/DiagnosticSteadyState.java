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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.oristool.models.stpn.RewardRate;
import org.oristool.models.stpn.SteadyStateSolution;
import org.oristool.models.stpn.steady.RegSteadyState;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.wosar22.models.DiagnosticRejuvenationSkipIfDown;
import org.oristool.wosar22.models.PeriodicRejuvenationSkipIfDown;

/**
 * Steady-state unavailability for many rejuvenation periods
 */
public class DiagnosticSteadyState {
    
    public static void main(String[] args) {
        List<String> modelNames = List.of("Periodic", "Periodic 1-Warning", "Periodic 2-Warning");
        List<Pair<PetriNet, Marking>> models = new ArrayList<>();
        models.add(PeriodicRejuvenationSkipIfDown.build(new BigDecimal("1300"), false));
        models.add(DiagnosticRejuvenationSkipIfDown.build(new BigDecimal("433.333"), false, 1));
        models.add(DiagnosticRejuvenationSkipIfDown.build(new BigDecimal("433.333"), false, 2));
        
        RewardRate undetectedFailure = RewardRate.fromString("Down > 0");
        RewardRate unavailability = RewardRate.fromString("Down + Detected + Rej > 0");

        for (int i = 0; i < models.size(); i++) {
            Pair<PetriNet, Marking> model = models.get(i);
            SteadyStateSolution<Marking> solution = RegSteadyState.builder().build().compute(model.getFirst(), model.getSecond());
            SteadyStateSolution<RewardRate> rewards = SteadyStateSolution.computeRewards(solution, undetectedFailure, unavailability);
            System.out.println(modelNames.get(i));
            System.out.printf("%.9f\n", rewards.getSteadyState().get(undetectedFailure));
            System.out.printf("%.9f\n\n",rewards.getSteadyState().get(unavailability));
        }
    }
}
