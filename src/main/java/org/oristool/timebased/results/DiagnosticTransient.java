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

package org.oristool.timebased.results;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.timebased.models.DiagnosticRejuvenationSkipIfDown;

/**
 * Transient unreliability and unavailability 
 * for the models with event-driven preemption
 */
public class DiagnosticTransient {
    public static void main(String[] args) {
        for (int warnings : List.of(2)) {
            Pair<PetriNet, Marking> model = DiagnosticRejuvenationSkipIfDown.build(new BigDecimal("433.333"), false, warnings);
            String csvName = "W" + warnings;        
            BestPeriodTransient.unavailability(model, "0.1", "2000", csvName);
//            BestPeriodTransient.unreliability(model, "1", "400000", csvName);
        }
    }
}
