/* This program is part of the ORIS Tool.
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

package org.oristool.wosar22.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.GEN;
import org.oristool.math.function.PartitionedGEN;
import org.oristool.models.pn.PostUpdater;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;


public class PeriodicRejuvenationTrivedi {
    public static Pair<PetriNet, Marking> build(int rejuvenationPeriod,
            boolean enablingRestriction) {
        
        PetriNet pn = new PetriNet();
        
        Place Ok = pn.addPlace("Ok");
        Place Err = pn.addPlace("Err");
        Place Ko = pn.addPlace("Ko");
        Place Clock = pn.addPlace("Clock");
        Place Rej = pn.addPlace("Rej");
        
        Transition error = pn.addTransition("error");
        Transition fail = pn.addTransition("fail");
        Transition repair = pn.addTransition("repair");
        Transition rejFromErr = pn.addTransition("rejFromErr");
        Transition rejFromOk = pn.addTransition("rejFromOk");
        Transition waitClock = pn.addTransition("waitClock");
        
        // cannot fail or degrade during rejuvenation
        pn.addInhibitorArc(Rej, fail);
        pn.addInhibitorArc(Rej, error);

        // rejuvenation stops when repair starts
        pn.addInhibitorArc(Ko, waitClock);

        // clock starts the rejuvenation
        pn.addPrecondition(Clock, waitClock);
        pn.addPostcondition(waitClock, Rej);

        // rejFromOk starts after the clock if in Ok;
        // at the end it enables the clock again, state is Ok
        pn.addPrecondition(Rej, rejFromOk);
        pn.addPrecondition(Ok, rejFromOk);
        pn.addPostcondition(rejFromOk, Clock);
        pn.addPostcondition(rejFromOk, Ok);

        // rejFromReg starts after the clock if in Err;
        // at the end it enables the clock again, state is Ok
        pn.addPrecondition(Rej, rejFromErr);
        pn.addPrecondition(Err, rejFromErr);
        pn.addPostcondition(rejFromErr, Clock);
        pn.addPostcondition(rejFromErr, Ok);
        
        // lifecycle: Ok -> Err -> Ko -> Detected -> Ok 
        pn.addPrecondition(Ok, error);
        pn.addPostcondition(error, Err);
        pn.addPrecondition(Err, fail);
        pn.addPostcondition(fail, Ko);
        pn.addPrecondition(Ko, repair);
        pn.addPostcondition(repair, Ok);

        // repair resets rejuvenation 
        repair.addFeature(new PostUpdater("Clock=1, Rej=0", pn));
        
        // rejuvenation clock deterministic with priority
        waitClock.addFeature(new Priority(0));
        waitClock.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(rejuvenationPeriod)));

        if (!enablingRestriction) {
            error.addFeature(StochasticTransitionFeature.newErlangInstance(4, BigDecimal.valueOf(1. / 60.)));
            fail.addFeature(StochasticTransitionFeature.newErlangInstance(12,  BigDecimal.valueOf(7. / 480.)));

            //repair.addFeature(StochasticTransitionFeature.newExpolynomial(
            //        "2.0366996375183555 * Exp[-1.51126 x]", OmegaBigDecimal.ZERO, new OmegaBigDecimal("0.896504")));
            List<GEN> repair_gens = new ArrayList<>();
            DBMZone repair_d_0 = new DBMZone(new Variable("x"));
            Expolynomial repair_e_0 = Expolynomial.fromString("-0.496617 * Exp[-0.250548 x] + 0.279347 * x^1 * Exp[-0.250548 x] + -0.0155193 * x^2 * Exp[-0.250548 x]");
            repair_d_0.setCoefficient(new Variable("x"), new Variable("t*"), new OmegaBigDecimal("16"));
            repair_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("-2"));
            GEN repair_gen_0 = new GEN(repair_d_0, repair_e_0);
            repair_gens.add(repair_gen_0);
            PartitionedGEN repair_pFunction = new PartitionedGEN(repair_gens);
            StochasticTransitionFeature repair_feature = StochasticTransitionFeature.of(repair_pFunction);
            repair.addFeature(repair_feature);

            //rejFromErr.addFeature(StochasticTransitionFeature.newExpolynomial(
            //        "4.277075236452792 * Exp[-3.17365 x]", OmegaBigDecimal.ZERO, new OmegaBigDecimal("0.426906")));
            List<GEN> RejFromErr_gens = new ArrayList<>();
            DBMZone RejFromErr_d_0 = new DBMZone(new Variable("x"));
            Expolynomial RejFromErr_e_0 = Expolynomial.fromString("-45.9805 * Exp[-1.86195 x] + 57.4756 * x^1 * Exp[-1.86195 x] + -11.4951 * x^2 * Exp[-1.86195 x]");
            RejFromErr_e_0.multiply(new BigDecimal(0.9999992202792896));
            RejFromErr_d_0.setCoefficient(new Variable("x"), new Variable("t*"), new OmegaBigDecimal("4"));
            RejFromErr_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("-1"));
            GEN RejFromErr_gen_0 = new GEN(RejFromErr_d_0, RejFromErr_e_0);
            RejFromErr_gens.add(RejFromErr_gen_0);
            PartitionedGEN RejFromErr_pFunction = new PartitionedGEN(RejFromErr_gens);
            StochasticTransitionFeature RejFromErr_feature = StochasticTransitionFeature.of(RejFromErr_pFunction);
            rejFromErr.addFeature(RejFromErr_feature);

            //rejFromUp.addFeature(StochasticTransitionFeature.newExpolynomial(
            //        "4.277075236452792 * Exp[-3.17365 x]", OmegaBigDecimal.ZERO, new OmegaBigDecimal("0.426906")));
            List<GEN> rejFromOk_gens = new ArrayList<>();
            DBMZone rejFromOk_d_0 = new DBMZone(new Variable("x"));
            Expolynomial rejFromOk_e_0 = Expolynomial.fromString("-58.9422 * Exp[-1.08048 x] + 88.4133 * x^1 * Exp[-1.08048 x] + -29.4711 * x^2 * Exp[-1.08048 x]");
            rejFromOk_e_0.multiply(new BigDecimal(1.0000040682736648));
            rejFromOk_d_0.setCoefficient(new Variable("x"), new Variable("t*"), new OmegaBigDecimal("2"));
            rejFromOk_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("-1"));
            GEN rejFromOk_gen_0 = new GEN(rejFromOk_d_0, rejFromOk_e_0);
            rejFromOk_gens.add(rejFromOk_gen_0);
            PartitionedGEN rejFromOk_pFunction = new PartitionedGEN(rejFromOk_gens);
            StochasticTransitionFeature rejFromOk_feature = StochasticTransitionFeature.of(rejFromOk_pFunction);
            rejFromOk.addFeature(rejFromOk_feature);
        } else {
            error.addFeature(StochasticTransitionFeature.newExponentialInstance("0.004166666667"));
            fail.addFeature(StochasticTransitionFeature.newExponentialInstance("0.00119047619"));
            repair.addFeature(StochasticTransitionFeature.newExponentialInstance("0.148484493"));  // rate was 1 in build_c1_EXP
            rejFromOk.addFeature(StochasticTransitionFeature.newExponentialInstance("0.6913621217"));
            rejFromErr.addFeature(StochasticTransitionFeature.newExponentialInstance("0.5550375205"));
        }
        
        Marking marking = new Marking();
        marking.setTokens(Clock, 1);
        marking.setTokens(Ok, 1);
        
        return new Pair<>(pn, marking);
    }
}
