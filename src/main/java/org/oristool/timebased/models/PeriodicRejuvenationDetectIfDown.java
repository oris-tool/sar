package org.oristool.timebased.models;/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2020 The ORIS Authors.
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


import java.math.BigDecimal;

import org.apache.commons.math3.util.Pair;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;
import org.oristool.timebased.models.PeriodicRejuvenationSkipIfDown;


public class PeriodicRejuvenationDetectIfDown {
    public static Pair<PetriNet, Marking> build(
            BigDecimal rejuvenationPeriod,
            boolean enablingRestriction) {
        
        Pair<PetriNet, Marking> model = PeriodicRejuvenationSkipIfDown.build(
                rejuvenationPeriod, enablingRestriction);
        PetriNet pn = model.getFirst();
        
        Place Rej = pn.getPlace("Rej");
        Place Down = pn.getPlace("Down");
        Place Detected = pn.getPlace("Detected");
        
        Transition rejFromDown = pn.addTransition("rejFromDown");

        // rejFromKo starts after the clock if in Ko
        pn.addPrecondition(Rej, rejFromDown);
        pn.addPrecondition(Down, rejFromDown);
        
        // it just moves the token to Detected
        pn.addPostcondition(rejFromDown, Detected);

        // immediate
        rejFromDown.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.ZERO));
        
        return new Pair<>(pn, model.getSecond());
    }
}
