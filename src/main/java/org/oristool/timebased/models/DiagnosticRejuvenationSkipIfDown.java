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
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.EnablingFunction;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;


public class DiagnosticRejuvenationSkipIfDown {
    public static Pair<PetriNet, Marking> build(BigDecimal rejuvenationPeriod,
            boolean enablingRestriction, int warnings) {
        
        PetriNet pn = new PetriNet();
        
        Place Up = pn.addPlace("Up");
        Place Err = pn.addPlace("Err");
        Place Down = pn.addPlace("Down");
        Place Detected = pn.addPlace("Detected");
        Place Clock = pn.addPlace("Clock");
        Place Clock1 = pn.addPlace("Clock1");
        Place Clock2 = pn.addPlace("Clock2");
        Place Sample = pn.addPlace("Sample");
        Place Green = pn.addPlace("Green");
        Place Red = pn.addPlace("Red");
        Place Rej = pn.addPlace("Rej");
        
        Transition error = pn.addTransition("error");
        Transition fail = pn.addTransition("fail");
        Transition detect = pn.addTransition("detect");
        Transition repair = pn.addTransition("repair");
        Transition waitClock = pn.addTransition("waitClock");
        Transition waitClock1 = pn.addTransition("waitClock1");
        Transition waitClock2 = pn.addTransition("waitClock2");
        Transition t2 = pn.addTransition("t2");
        Transition t3 = pn.addTransition("t3");
        Transition t4 = pn.addTransition("t4");
        Transition t5 = pn.addTransition("t5");
        Transition t6 = pn.addTransition("t6");
        Transition rejFromErr = pn.addTransition("rejFromErr");
        Transition rejFromUp = pn.addTransition("rejFromUp");
        
        // rejuvenation stopped when repair starts
        pn.addInhibitorArc(Detected, waitClock);
        pn.addInhibitorArc(Detected, waitClock1);
        pn.addInhibitorArc(Detected, waitClock2);

        // clocks eventually start the rejuvenation
        pn.addPrecondition(Clock, waitClock1);
        pn.addPostcondition(waitClock1, Clock1);
        pn.addPrecondition(Clock1, waitClock2);
        pn.addPostcondition(waitClock2, Clock2);
        pn.addPrecondition(Clock2, waitClock);
        pn.addPostcondition(waitClock, Rej);

        // intermediate clocks also try to sample diagnostics
        pn.addPostcondition(waitClock1, Sample);
        pn.addPostcondition(waitClock2, Sample);
        
        // only two enabled, depending on state
        pn.addPrecondition(Sample, t2);
        pn.addPostcondition(t2, Green);
        pn.addPrecondition(Sample, t3);
        pn.addPostcondition(t3, Green);
        pn.addPrecondition(Sample, t4);
        pn.addPostcondition(t4, Red);
        pn.addPrecondition(Sample, t5);
        pn.addPostcondition(t5, Red);

        // red diagnostics start rejuvenation too
        pn.addPrecondition(Red, t6);
        pn.addPostcondition(t6, Rej);

        // cannot degrade or fail during rejuvenation
        pn.addInhibitorArc(Rej, error);
        pn.addInhibitorArc(Rej, fail);

        // rejFromUp starts after the clock if in Up;
        // at the end it enables the clock again, state is Up
        pn.addPrecondition(Rej, rejFromUp);
        pn.addPrecondition(Up, rejFromUp);
        pn.addPostcondition(rejFromUp, Clock);
        pn.addPostcondition(rejFromUp, Up);

        // rejFromErr starts after the clock if in Err;
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
        pn.addPrecondition(Down, detect);
        pn.addPostcondition(detect, Detected);
        pn.addPrecondition(Detected, repair);
        pn.addPostcondition(repair, Up);

        // repair resets rejuvenation 
        repair.addFeature(new PostUpdater("Clock=1, Clock1=0, Clock2=0, Rej=0, Green=0, Red=0", pn));
        
        // first rejuvenation clock clears counters (necessary?)
        waitClock.addFeature(new PostUpdater("Green=0, Red=0", pn));

        // rejuvenation clocks deterministic with priority
        waitClock.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriod));
        waitClock1.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriod));
        waitClock2.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriod));

        // t2,t5 enabled in Up while t3,t4 enabled in Err or Down
        t2.addFeature(new EnablingFunction("Up>0"));
        t3.addFeature(new EnablingFunction("Up==0"));
        t4.addFeature(new EnablingFunction("Up==0"));
        t5.addFeature(new EnablingFunction("Up>0"));
        
        // t2,t4 have 0.9 probability while t3,t5 have 0.1
        t2.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", pn)));
        t3.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", pn)));
        t4.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", pn)));
        t5.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", pn)));

        // with enough warnings, t6 resets the green counter and the clock state
        t6.addFeature(new EnablingFunction("Red>=" + warnings));
        t6.addFeature(new PostUpdater("Green=0, Clock1=0, Clock2=0", pn));
        t6.addFeature(StochasticTransitionFeature.newDeterministicInstance(BigDecimal.ZERO));
        
        // same priority for all
        waitClock.addFeature(new Priority(0));
        waitClock1.addFeature(new Priority(0));
        waitClock2.addFeature(new Priority(0));
        t2.addFeature(new Priority(0));
        t3.addFeature(new Priority(0));
        t4.addFeature(new Priority(0));
        t5.addFeature(new Priority(0));
        t6.addFeature(new Priority(0));
        
        if (!enablingRestriction) {
            //error.addFeature(StochasticTransitionFeature.newErlangInstance(4, BigDecimal.valueOf(1. / 60.)));
            error.addFeature(StochasticTransitionFeature.newHypoExp(BigDecimal.valueOf(0.00208), BigDecimal.valueOf(0.00277)));
            //fail.addFeature(StochasticTransitionFeature.newErlangInstance(12,  BigDecimal.valueOf(7. / 480.)));
            fail.addFeature(StochasticTransitionFeature.newHypoExp(BigDecimal.valueOf(0.00615), BigDecimal.valueOf(0.01289)));
            detect.addFeature(StochasticTransitionFeature.newUniformInstance("0","4"));

            repair.addFeature(StochasticTransitionFeature.newExpolynomial(
                    "2.0366996375183555 * Exp[-1.51126 x]", OmegaBigDecimal.ZERO, new OmegaBigDecimal("0.896504")));
            /*List<GEN> repair_gens = new ArrayList<>();
            DBMZone repair_d_0 = new DBMZone(new Variable("x"));
            Expolynomial repair_e_0 = Expolynomial.fromString("-0.496617 * Exp[-0.250548 x] + 0.279347 * x^1 * Exp[-0.250548 x] + -0.0155193 * x^2 * Exp[-0.250548 x]");
            repair_d_0.setCoefficient(new Variable("x"), new Variable("t*"), new OmegaBigDecimal("16"));
            repair_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("-2"));
            GEN repair_gen_0 = new GEN(repair_d_0, repair_e_0);
            repair_gens.add(repair_gen_0);
            PartitionedGEN repair_pFunction = new PartitionedGEN(repair_gens);
            StochasticTransitionFeature repair_feature = StochasticTransitionFeature.of(repair_pFunction);
            repair.addFeature(repair_feature);*/

            rejFromErr.addFeature(StochasticTransitionFeature.newExpolynomial(
                    "4.277075236452792 * Exp[-3.17365 x]", OmegaBigDecimal.ZERO, new OmegaBigDecimal("0.426906")));
            /*List<GEN> RejFromErr_gens = new ArrayList<>();
            DBMZone RejFromErr_d_0 = new DBMZone(new Variable("x"));
            Expolynomial RejFromErr_e_0 = Expolynomial.fromString("-45.9805 * Exp[-1.86195 x] + 57.4756 * x^1 * Exp[-1.86195 x] + -11.4951 * x^2 * Exp[-1.86195 x]");
            RejFromErr_e_0.multiply(new BigDecimal(0.9999992202792896));
            RejFromErr_d_0.setCoefficient(new Variable("x"), new Variable("t*"), new OmegaBigDecimal("4"));
            RejFromErr_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("-1"));
            GEN RejFromErr_gen_0 = new GEN(RejFromErr_d_0, RejFromErr_e_0);
            RejFromErr_gens.add(RejFromErr_gen_0);
            PartitionedGEN RejFromErr_pFunction = new PartitionedGEN(RejFromErr_gens);
            StochasticTransitionFeature RejFromErr_feature = StochasticTransitionFeature.of(RejFromErr_pFunction);
            rejFromErr.addFeature(RejFromErr_feature);*/

            rejFromUp.addFeature(StochasticTransitionFeature.newExpolynomial(
                    "4.277075236452792 * Exp[-3.17365 x]", OmegaBigDecimal.ZERO, new OmegaBigDecimal("0.426906")));
            /*List<GEN> rejFromOk_gens = new ArrayList<>();
            DBMZone rejFromOk_d_0 = new DBMZone(new Variable("x"));
            Expolynomial rejFromOk_e_0 = Expolynomial.fromString("-58.9422 * Exp[-1.08048 x] + 88.4133 * x^1 * Exp[-1.08048 x] + -29.4711 * x^2 * Exp[-1.08048 x]");
            rejFromOk_e_0.multiply(new BigDecimal(1.0000040682736648));
            rejFromOk_d_0.setCoefficient(new Variable("x"), new Variable("t*"), new OmegaBigDecimal("2"));
            rejFromOk_d_0.setCoefficient(new Variable("t*"), new Variable("x"), new OmegaBigDecimal("-1"));
            GEN rejFromOk_gen_0 = new GEN(rejFromOk_d_0, rejFromOk_e_0);
            rejFromOk_gens.add(rejFromOk_gen_0);
            PartitionedGEN rejFromOk_pFunction = new PartitionedGEN(rejFromOk_gens);
            StochasticTransitionFeature rejFromOk_feature = StochasticTransitionFeature.of(rejFromOk_pFunction);
            rejFromUp.addFeature(rejFromOk_feature);*/
        } else {
            error.addFeature(StochasticTransitionFeature.newExponentialInstance("0.004166666667"));
            fail.addFeature(StochasticTransitionFeature.newExponentialInstance("0.00119047619"));
            detect.addFeature(StochasticTransitionFeature.newExponentialInstance("0.5"));  // rate was 1 in build_c1_EXP
            repair.addFeature(StochasticTransitionFeature.newExponentialInstance("0.148484493"));  // rate was 1 in build_c1_EXP
            rejFromUp.addFeature(StochasticTransitionFeature.newExponentialInstance("0.6913621217"));
            rejFromErr.addFeature(StochasticTransitionFeature.newExponentialInstance("0.5550375205"));
        }
        
        Marking marking = new Marking();
        marking.setTokens(Clock, 1);
        marking.setTokens(Up, 1);
        
        return new Pair<>(pn, marking);
    }
    
    public static void main(String[] args) {

//        SuccessionGraphViewer.show(
//                TimedAnalysis.builder().markRegenerations(true).excludeZeroProb(true).build()
//                .compute(modelManyGEN.getFirst(), modelManyGEN.getSecond()));

//        Pair<PetriNet, Marking> modelOneGEN = build(1000, true, 1);
//        SuccessionGraphViewer.show(
//                TimedAnalysis.builder().markRegenerations(true).excludeZeroProb(true).build()
//                .compute(modelOneGEN.getFirst(), modelOneGEN.getSecond()));
    }
}
