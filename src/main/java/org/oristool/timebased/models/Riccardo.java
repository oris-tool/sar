package org.oristool.timebased.models;/* This program is part of the ORIS Tool.
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.GEN;
import org.oristool.math.function.PartitionedGEN;
import org.oristool.models.pn.PostUpdater;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.RewardRate;
import org.oristool.models.stpn.SteadyStateSolution;
import org.oristool.models.stpn.steady.RegSteadyState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.EnablingFunction;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;

/**
 * Models received by email.
 * WARNING: Some have inconsistent parameters.
 */
public class Riccardo {

    private final static double lambda1 = 3.17365;
    private final static double lambda2 = 1.51126;
    private final static double b1 = 0.426906;
    private final static double b2 = 0.896504;
    public static void build_simple_GEN(PetriNet net, Marking marking, BigDecimal rejuvenationPeriod) {
        //Generating Nodes
        Place Clock = net.addPlace("Clock");
        Place Detected = net.addPlace("Detected");
        Place Err = net.addPlace("Err");
        Place Ko = net.addPlace("Ko");
        Place Ok = net.addPlace("Ok");
        Place Rej = net.addPlace("Rej");
        Transition detect = net.addTransition("detect");
        Transition error = net.addTransition("error");
        Transition fail = net.addTransition("fail");
        Transition rejFromErr = net.addTransition("rejFromErr");
        Transition rejFromOk = net.addTransition("rejFromOk");
        Transition repair = net.addTransition("repair");
        Transition waitClock = net.addTransition("waitClock");

        //Generating Connectors
        net.addInhibitorArc(Rej, fail);
        net.addInhibitorArc(Detected, waitClock);
        net.addInhibitorArc(Rej, error);
        net.addPrecondition(Rej, rejFromErr);
        net.addPostcondition(error, Err);
        net.addPrecondition(Err, fail);
        net.addPrecondition(Err, rejFromErr);
        net.addPostcondition(rejFromOk, Clock);
        net.addPostcondition(rejFromOk, Ok);
        net.addPrecondition(Clock, waitClock);
        net.addPostcondition(repair, Ok);
        net.addPrecondition(Detected, repair);
        net.addPostcondition(waitClock, Rej);
        net.addPrecondition(Ok, rejFromOk);
        net.addPrecondition(Ko, detect);
        net.addPostcondition(rejFromErr, Ok);
        net.addPrecondition(Ok, error);
        net.addPostcondition(rejFromErr, Clock);
        net.addPostcondition(detect, Detected);
        net.addPostcondition(fail, Ko);
        net.addPrecondition(Rej, rejFromOk);

        //Generating Properties
        marking.setTokens(Clock, 1);
        marking.setTokens(Detected, 0);
        marking.setTokens(Err, 0);
        marking.setTokens(Ko, 0);
        marking.setTokens(Ok, 1);
        marking.setTokens(Rej, 0);
        detect.addFeature(StochasticTransitionFeature.newUniformInstance(new BigDecimal("0"), new BigDecimal("0.3")));
        error.addFeature(StochasticTransitionFeature.newErlangInstance(4, new BigDecimal("0.01666666")));
        fail.addFeature(StochasticTransitionFeature.newErlangInstance(4, new BigDecimal("0.00181851852")));

        repair.addFeature(new PostUpdater("Clock=1, Rej=0", net));
        error.addFeature(StochasticTransitionFeature.newErlangInstance(4, BigDecimal.valueOf(1. / 60.)));
        fail.addFeature(StochasticTransitionFeature.newErlangInstance(12,  BigDecimal.valueOf(7. / 480.)));
        detect.addFeature(StochasticTransitionFeature.newUniformInstance("0","4"));

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


        waitClock.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriod, MarkingExpr.from("1", net)));
        waitClock.addFeature(new Priority(0));
    }

    public static void build_c1_GEN(PetriNet net, Marking marking, List<BigDecimal> rejuvenationPeriods) {

        //Generating Nodes
        Place Clock = net.addPlace("Clock");
        Place Clock1 = net.addPlace("Clock1");
        Place Clock2 = net.addPlace("Clock2");
        Place Detected = net.addPlace("Detected");
        Place Err = net.addPlace("Err");
        Place Green = net.addPlace("Green");
        Place Ko = net.addPlace("Ko");
        Place Ok = net.addPlace("Ok");
        Place Red = net.addPlace("Red");
        Place Rej = net.addPlace("Rej");
        Place Sample = net.addPlace("Sample");

        Transition detect = net.addTransition("detect");
        Transition error = net.addTransition("error");
        Transition fail = net.addTransition("fail");
        Transition rejFromErr = net.addTransition("rejFromErr");
        Transition rejFromOk = net.addTransition("rejFromOk");
        Transition repair = net.addTransition("repair");
        Transition t2 = net.addTransition("t2");
        Transition t3 = net.addTransition("t3");
        Transition t4 = net.addTransition("t4");
        Transition t5 = net.addTransition("t5");
        Transition t6 = net.addTransition("t6");
        Transition waitClock = net.addTransition("waitClock");
        Transition waitClock1 = net.addTransition("waitClock1");
        Transition waitClock2 = net.addTransition("waitClock2");

        //Generating Connectors
        net.addInhibitorArc(Detected, waitClock2);
        net.addInhibitorArc(Detected, waitClock);
        net.addInhibitorArc(Detected, waitClock1);
        net.addInhibitorArc(Rej, error);
        net.addInhibitorArc(Rej, fail);
        net.addPostcondition(waitClock, Rej);
        net.addPrecondition(Red, t6);
        net.addPostcondition(rejFromErr, Ok);
        net.addPostcondition(rejFromOk, Ok);
        net.addPrecondition(Ok, rejFromOk);
        net.addPrecondition(Rej, rejFromErr);
        net.addPostcondition(waitClock2, Clock2);
        net.addPrecondition(Clock1, waitClock2);
        net.addPostcondition(t5, Red);
        net.addPostcondition(rejFromErr, Clock);
        net.addPostcondition(detect, Detected);
        net.addPostcondition(t3, Green);
        net.addPostcondition(t4, Red);
        net.addPostcondition(waitClock1, Sample);
        net.addPrecondition(Clock2, waitClock);
        net.addPostcondition(error, Err);
        net.addPostcondition(repair, Ok);
        net.addPostcondition(waitClock1, Clock1);
        net.addPrecondition(Sample, t5);
        net.addPostcondition(rejFromOk, Clock);
        net.addPrecondition(Err, rejFromErr);
        net.addPrecondition(Sample, t3);
        net.addPrecondition(Clock, waitClock1);
        net.addPostcondition(t6, Rej);
        net.addPrecondition(Sample, t2);
        net.addPrecondition(Ko, detect);
        net.addPrecondition(Sample, t4);
        net.addPrecondition(Err, fail);
        net.addPostcondition(fail, Ko);
        net.addPostcondition(t2, Green);
        net.addPostcondition(waitClock2, Sample);
        net.addPrecondition(Detected, repair);
        net.addPrecondition(Rej, rejFromOk);
        net.addPrecondition(Ok, error);

        //Generating Properties
        marking.setTokens(Clock, 1);
        marking.setTokens(Clock1, 0);
        marking.setTokens(Clock2, 0);
        marking.setTokens(Detected, 0);
        marking.setTokens(Err, 0);
        marking.setTokens(Green, 0);
        marking.setTokens(Ko, 0);
        marking.setTokens(Ok, 1);
        marking.setTokens(Red, 0);
        marking.setTokens(Rej, 0);
        marking.setTokens(Sample, 0);
        detect.addFeature(StochasticTransitionFeature.newUniformInstance(new BigDecimal("0"), new BigDecimal("0.3")));
        error.addFeature(StochasticTransitionFeature.newErlangInstance(4, new BigDecimal("0.01666666")));
        fail.addFeature(StochasticTransitionFeature.newErlangInstance(4, new BigDecimal("0.00181851852")));
        rejFromErr.addFeature(StochasticTransitionFeature.newExpolynomial(lambda1 / (1 - Math.exp(-lambda1 * b1)) + " * Exp[-" + lambda1 + " x]", new OmegaBigDecimal("0"), new OmegaBigDecimal(String.valueOf(b1))));
        rejFromOk.addFeature(StochasticTransitionFeature.newExpolynomial(lambda1 / (1 - Math.exp(-lambda1 * b1)) + " * Exp[-" + lambda1 + " x]", new OmegaBigDecimal("0"), new OmegaBigDecimal(String.valueOf(b1))));
        repair.addFeature(new PostUpdater("Clock=1, Clock1=0, Clock2=0, Rej=0, Green=0, Red=0", net));
        repair.addFeature(StochasticTransitionFeature.newExpolynomial(lambda2 / (1 - Math.exp(-lambda2 * b2)) + " * Exp[-" + lambda2 + " x]", new OmegaBigDecimal("0"), new OmegaBigDecimal(String.valueOf(b2))));
        t2.addFeature(new EnablingFunction("Ok>0"));
        t2.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", net)));
        t2.addFeature(new Priority(0));
        t3.addFeature(new EnablingFunction("Ok==0"));
        t3.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        t3.addFeature(new Priority(0));
        t4.addFeature(new EnablingFunction("Ok==0"));
        t4.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", net)));
        t4.addFeature(new Priority(0));
        t5.addFeature(new EnablingFunction("Ok>0"));
        t5.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        t5.addFeature(new Priority(0));
        t6.addFeature(new PostUpdater("Green=0, Clock1=0, Clock2=0", net));
        t6.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        t6.addFeature(new Priority(0));
        waitClock.addFeature(new PostUpdater("Green=0, Red=0", net));
        waitClock.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriods.get(0), MarkingExpr.from("1", net)));
        waitClock.addFeature(new Priority(0));
        waitClock1.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriods.get(1), MarkingExpr.from("1", net)));
        waitClock1.addFeature(new Priority(0));
        waitClock2.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriods.get(2), MarkingExpr.from("1", net)));
        waitClock2.addFeature(new Priority(0));
    }

    public static void build_c2_GEN(PetriNet net, Marking marking, List<BigDecimal> rejuvenationPeriods) {

        //Generating Nodes
        Place Clock = net.addPlace("Clock");
        Place Clock1 = net.addPlace("Clock1");
        Place Clock2 = net.addPlace("Clock2");
        Place Detected = net.addPlace("Detected");
        Place Err = net.addPlace("Err");
        Place Green = net.addPlace("Green");
        Place Ko = net.addPlace("Ko");
        Place Ok = net.addPlace("Ok");
        Place Red = net.addPlace("Red");
        Place Rej = net.addPlace("Rej");
        Place Sample = net.addPlace("Sample");

        Transition detect = net.addTransition("detect");
        Transition error = net.addTransition("error");
        Transition fail = net.addTransition("fail");
        Transition rejFromErr = net.addTransition("rejFromErr");
        Transition rejFromOk = net.addTransition("rejFromOk");
        Transition repair = net.addTransition("repair");
        Transition t2 = net.addTransition("t2");
        Transition t3 = net.addTransition("t3");
        Transition t4 = net.addTransition("t4");
        Transition t5 = net.addTransition("t5");
        Transition t6 = net.addTransition("t6");
        Transition waitClock = net.addTransition("waitClock");
        Transition waitClock1 = net.addTransition("waitClock1");
        Transition waitClock2 = net.addTransition("waitClock2");

        //Generating Connectors
        net.addInhibitorArc(Detected, waitClock);
        net.addInhibitorArc(Rej, fail);
        net.addInhibitorArc(Detected, waitClock1);
        net.addInhibitorArc(Rej, error);
        net.addInhibitorArc(Detected, waitClock2);
        net.addPostcondition(rejFromOk, Clock);
        net.addPrecondition(Rej, rejFromOk);
        net.addPostcondition(repair, Ok);
        net.addPostcondition(waitClock2, Clock2);
        net.addPrecondition(Err, rejFromErr);
        net.addPrecondition(Ok, error);
        net.addPrecondition(Red, t6);
        net.addPrecondition(Detected, repair);
        net.addPostcondition(waitClock1, Sample);
        net.addPrecondition(Err, fail);
        net.addPostcondition(rejFromErr, Ok);
        net.addPostcondition(t3, Green);
        net.addPostcondition(waitClock, Rej);
        net.addPostcondition(detect, Detected);
        net.addPostcondition(waitClock2, Sample);
        net.addPrecondition(Rej, rejFromErr);
        net.addPrecondition(Ok, rejFromOk);
        net.addPrecondition(Sample, t4);
        net.addPostcondition(t4, Red);
        net.addPrecondition(Sample, t2);
        net.addPostcondition(error, Err);
        net.addPostcondition(t6, Rej);
        net.addPostcondition(t2, Green);
        net.addPostcondition(waitClock1, Clock1);
        net.addPrecondition(Sample, t3);
        net.addPrecondition(Sample, t5);
        net.addPrecondition(Clock1, waitClock2);
        net.addPrecondition(Ko, detect);
        net.addPostcondition(rejFromOk, Ok);
        net.addPostcondition(rejFromErr, Clock);
        net.addPrecondition(Clock, waitClock1);
        net.addPostcondition(t5, Red);
        net.addPostcondition(fail, Ko);
        net.addPrecondition(Clock2, waitClock);

        //Generating Properties
        marking.setTokens(Clock, 1);
        marking.setTokens(Clock1, 0);
        marking.setTokens(Clock2, 0);
        marking.setTokens(Detected, 0);
        marking.setTokens(Err, 0);
        marking.setTokens(Green, 0);
        marking.setTokens(Ko, 0);
        marking.setTokens(Ok, 1);
        marking.setTokens(Red, 0);
        marking.setTokens(Rej, 0);
        marking.setTokens(Sample, 0);
        detect.addFeature(StochasticTransitionFeature.newUniformInstance(new BigDecimal("0"), new BigDecimal("0.3")));
        error.addFeature(StochasticTransitionFeature.newErlangInstance(4, new BigDecimal("0.016"))); // TODO This is wrong!
        fail.addFeature(StochasticTransitionFeature.newErlangInstance(4, new BigDecimal("0.0016"))); // TODO This is wrong!
        rejFromErr.addFeature(StochasticTransitionFeature.newExpolynomial(lambda1 / (1 - Math.exp(-lambda1 * b1)) + " * Exp[-" + lambda1 + " x]", new OmegaBigDecimal("0"), new OmegaBigDecimal(String.valueOf(b1))));
        rejFromOk.addFeature(StochasticTransitionFeature.newExpolynomial(lambda1 / (1 - Math.exp(-lambda1 * b1)) + " * Exp[-" + lambda1 + " x]", new OmegaBigDecimal("0"), new OmegaBigDecimal(String.valueOf(b1))));
        repair.addFeature(new PostUpdater("Clock=1, Clock1=0, Clock2=0, Rej=0, Green=0, Red=0", net));
        repair.addFeature(StochasticTransitionFeature.newExpolynomial(lambda2 / (1 - Math.exp(-lambda2 * b2)) + " * Exp[-" + lambda2 + " x]", new OmegaBigDecimal("0"), new OmegaBigDecimal(String.valueOf(b2))));
        t2.addFeature(new EnablingFunction("Ok>0"));
        t2.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", net)));
        t2.addFeature(new Priority(0));
        t3.addFeature(new EnablingFunction("Ok==0"));
        t3.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        t3.addFeature(new Priority(0));
        t4.addFeature(new EnablingFunction("Ok==0"));
        t4.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", net)));
        t4.addFeature(new Priority(0));
        t5.addFeature(new EnablingFunction("Ok>0"));
        t5.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        t5.addFeature(new Priority(0));
        t6.addFeature(new EnablingFunction("Red==2"));
        t6.addFeature(new PostUpdater("Green=0, Clock1=0, Clock2=0", net));
        t6.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        t6.addFeature(new Priority(0));
        waitClock.addFeature(new PostUpdater("Green=0, Red=0", net));
        waitClock.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriods.get(0), MarkingExpr.from("1", net)));
        waitClock.addFeature(new Priority(0));
        waitClock1.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriods.get(1), MarkingExpr.from("1", net)));
        waitClock1.addFeature(new Priority(0));
        waitClock2.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriods.get(2), MarkingExpr.from("1", net)));
        waitClock2.addFeature(new Priority(0));
    }

    public static void build_simple_EXP(PetriNet net, Marking marking, BigDecimal rejuvenationPeriod, BigDecimal minTimeAnalysis) {

        //Generating Nodes
        Place Clock = net.addPlace("Clock");
        Place Detected = net.addPlace("Detected");
        Place Err = net.addPlace("Err");
        Place Ko = net.addPlace("Ko");
        Place Ok = net.addPlace("Ok");
        Place Rej = net.addPlace("Rej");
        Place Min = net.addPlace("Min");
        Place Start = net.addPlace("Start");

        Transition detect = net.addTransition("detect");
        Transition error = net.addTransition("error");
        Transition fail = net.addTransition("fail");
        Transition rejFromErr = net.addTransition("rejFromErr");
        Transition rejFromOk = net.addTransition("rejFromOk");
        Transition repair = net.addTransition("repair");
        Transition waitClock = net.addTransition("waitClock");
        Transition min = net.addTransition("min");

        //Generating Connectors
        net.addInhibitorArc(Rej, fail);
        net.addInhibitorArc(Detected, waitClock);
        net.addInhibitorArc(Rej, error);
        net.addPostcondition(waitClock, Rej);
        net.addPostcondition(detect, Detected);
        net.addPostcondition(fail, Ko);
        net.addPrecondition(Err, rejFromErr);
        net.addPrecondition(Detected, repair);
        net.addPostcondition(rejFromOk, Ok);
        net.addPrecondition(Rej, rejFromOk);
        net.addPostcondition(error, Err);
        net.addPrecondition(Err, fail);
        net.addPostcondition(rejFromErr, Ok);
        net.addPrecondition(Ok, rejFromOk);
        net.addPrecondition(Rej, rejFromErr);
        net.addPrecondition(Clock, waitClock);
        net.addPrecondition(Ko, detect);
        net.addPostcondition(rejFromErr, Clock);
        net.addPostcondition(repair, Ok);
        net.addPrecondition(Ok, error);
        net.addPostcondition(rejFromOk, Clock);
        net.addPostcondition(min, Min);
        net.addPrecondition(Start, min);

        //Generating Properties
        marking.setTokens(Start, 1);
        marking.setTokens(Clock, 1);
        marking.setTokens(Detected, 0);
        marking.setTokens(Err, 0);
        marking.setTokens(Ko, 0);
        marking.setTokens(Ok, 1);
        marking.setTokens(Rej, 0);
        detect.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("4"), MarkingExpr.from("1", net)));
        error.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.004", net)));
        fail.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.0004", net)));
        rejFromErr.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("6", net)));
        rejFromOk.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("6", net)));
        repair.addFeature(new PostUpdater("Clock=1, Rej=0", net));
        repair.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("4"), MarkingExpr.from("1", net)));
        waitClock.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriod, MarkingExpr.from("1", net)));
        waitClock.addFeature(new Priority(0));
        min.addFeature(StochasticTransitionFeature.newDeterministicInstance(minTimeAnalysis, MarkingExpr.from("1", net)));
        min.addFeature(new Priority(0));
    }

    public static void build_c1_EXP(PetriNet net, Marking marking, ArrayList<BigDecimal> rejuvenationPeriods, BigDecimal minTimeAnalysis) {

        //Generating Nodes
        Place Clock = net.addPlace("Clock");
        Place Clock1 = net.addPlace("Clock1");
        Place Clock2 = net.addPlace("Clock2");
        Place Detected = net.addPlace("Detected");
        Place Err = net.addPlace("Err");
        Place Green = net.addPlace("Green");
        Place Ko = net.addPlace("Ko");
        Place Ok = net.addPlace("Ok");
        Place Red = net.addPlace("Red");
        Place Rej = net.addPlace("Rej");
        Place Sample = net.addPlace("Sample");
        Place Min = net.addPlace("Min");
        Place Start = net.addPlace("Start");

        Transition detect = net.addTransition("detect");
        Transition error = net.addTransition("error");
        Transition fail = net.addTransition("fail");
        Transition rejFromErr = net.addTransition("rejFromErr");
        Transition rejFromOk = net.addTransition("rejFromOk");
        Transition repair = net.addTransition("repair");
        Transition t2 = net.addTransition("t2");
        Transition t3 = net.addTransition("t3");
        Transition t4 = net.addTransition("t4");
        Transition t5 = net.addTransition("t5");
        Transition t6 = net.addTransition("t6");
        Transition waitClock = net.addTransition("waitClock");
        Transition waitClock1 = net.addTransition("waitClock1");
        Transition waitClock2 = net.addTransition("waitClock2");
        Transition min = net.addTransition("min");

        //Generating Connectors
        net.addInhibitorArc(Detected, waitClock2);
        net.addInhibitorArc(Detected, waitClock);
        net.addInhibitorArc(Detected, waitClock1);
        net.addInhibitorArc(Rej, error);
        net.addInhibitorArc(Rej, fail);
        net.addPostcondition(waitClock, Rej);
        net.addPrecondition(Red, t6);
        net.addPostcondition(rejFromErr, Ok);
        net.addPostcondition(rejFromOk, Ok);
        net.addPrecondition(Ok, rejFromOk);
        net.addPrecondition(Rej, rejFromErr);
        net.addPostcondition(waitClock2, Clock2);
        net.addPrecondition(Clock1, waitClock2);
        net.addPostcondition(t5, Red);
        net.addPostcondition(rejFromErr, Clock);
        net.addPostcondition(detect, Detected);
        net.addPostcondition(t3, Green);
        net.addPostcondition(t4, Red);
        net.addPostcondition(waitClock1, Sample);
        net.addPrecondition(Clock2, waitClock);
        net.addPostcondition(error, Err);
        net.addPostcondition(repair, Ok);
        net.addPostcondition(waitClock1, Clock1);
        net.addPrecondition(Sample, t5);
        net.addPostcondition(rejFromOk, Clock);
        net.addPrecondition(Err, rejFromErr);
        net.addPrecondition(Sample, t3);
        net.addPrecondition(Clock, waitClock1);
        net.addPostcondition(t6, Rej);
        net.addPrecondition(Sample, t2);
        net.addPrecondition(Ko, detect);
        net.addPrecondition(Sample, t4);
        net.addPrecondition(Err, fail);
        net.addPostcondition(fail, Ko);
        net.addPostcondition(t2, Green);
        net.addPostcondition(waitClock2, Sample);
        net.addPrecondition(Detected, repair);
        net.addPrecondition(Rej, rejFromOk);
        net.addPrecondition(Ok, error);
        net.addPostcondition(min, Min);
        net.addPrecondition(Start, min);

        //Generating Properties
        marking.setTokens(Start, 1);
        marking.setTokens(Clock, 1);
        marking.setTokens(Clock1, 0);
        marking.setTokens(Clock2, 0);
        marking.setTokens(Detected, 0);
        marking.setTokens(Err, 0);
        marking.setTokens(Green, 0);
        marking.setTokens(Ko, 0);
        marking.setTokens(Ok, 1);
        marking.setTokens(Red, 0);
        marking.setTokens(Rej, 0);
        marking.setTokens(Sample, 0);
        detect.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("4"), MarkingExpr.from("1", net)));
        error.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.004", net)));
        fail.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.0004", net)));
        rejFromErr.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("6", net)));
        rejFromOk.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("6", net)));
        repair.addFeature(new PostUpdater("Clock=1, Clock1=0, Clock2=0, Rej=0, Green=0, Red=0", net));
        repair.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("4"), MarkingExpr.from("1", net)));
        t2.addFeature(new EnablingFunction("Ok>0"));
        t2.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", net)));
        t2.addFeature(new Priority(0));
        t3.addFeature(new EnablingFunction("Ok==0"));
        t3.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        t3.addFeature(new Priority(0));
        t4.addFeature(new EnablingFunction("Ok==0"));
        t4.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", net)));
        t4.addFeature(new Priority(0));
        t5.addFeature(new EnablingFunction("Ok>0"));
        t5.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        t5.addFeature(new Priority(0));
        t6.addFeature(new PostUpdater("Green=0, Clock1=0, Clock2=0", net));
        t6.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        t6.addFeature(new Priority(0));
        waitClock.addFeature(new PostUpdater("Green=0, Red=0", net));
        waitClock.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriods.get(0), MarkingExpr.from("1", net)));
        waitClock.addFeature(new Priority(0));
        waitClock1.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriods.get(1), MarkingExpr.from("1", net)));
        waitClock1.addFeature(new Priority(0));
        waitClock2.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriods.get(2), MarkingExpr.from("1", net)));
        waitClock2.addFeature(new Priority(0));
        min.addFeature(StochasticTransitionFeature.newDeterministicInstance(minTimeAnalysis, MarkingExpr.from("1", net)));
        min.addFeature(new Priority(0));
    }

    public static void build_c2_EXP(PetriNet net, Marking marking, ArrayList<BigDecimal> rejuvenationPeriods, BigDecimal minTimeAnalysis) {

        //Generating Nodes
        Place Clock = net.addPlace("Clock");
        Place Clock1 = net.addPlace("Clock1");
        Place Clock2 = net.addPlace("Clock2");
        Place Detected = net.addPlace("Detected");
        Place Err = net.addPlace("Err");
        Place Green = net.addPlace("Green");
        Place Ko = net.addPlace("Ko");
        Place Ok = net.addPlace("Ok");
        Place Red = net.addPlace("Red");
        Place Rej = net.addPlace("Rej");
        Place Sample = net.addPlace("Sample");
        Place Min = net.addPlace("Min");
        Place Start = net.addPlace("Start");

        Transition detect = net.addTransition("detect");
        Transition error = net.addTransition("error");
        Transition fail = net.addTransition("fail");
        Transition rejFromErr = net.addTransition("rejFromErr");
        Transition rejFromOk = net.addTransition("rejFromOk");
        Transition repair = net.addTransition("repair");
        Transition t2 = net.addTransition("t2");
        Transition t3 = net.addTransition("t3");
        Transition t4 = net.addTransition("t4");
        Transition t5 = net.addTransition("t5");
        Transition t6 = net.addTransition("t6");
        Transition waitClock = net.addTransition("waitClock");
        Transition waitClock1 = net.addTransition("waitClock1");
        Transition waitClock2 = net.addTransition("waitClock2");
        Transition min = net.addTransition("min");

        //Generating Connectors
        net.addInhibitorArc(Detected, waitClock);
        net.addInhibitorArc(Rej, fail);
        net.addInhibitorArc(Detected, waitClock1);
        net.addInhibitorArc(Rej, error);
        net.addInhibitorArc(Detected, waitClock2);
        net.addPostcondition(rejFromOk, Clock);
        net.addPrecondition(Rej, rejFromOk);
        net.addPostcondition(repair, Ok);
        net.addPostcondition(waitClock2, Clock2);
        net.addPrecondition(Err, rejFromErr);
        net.addPrecondition(Ok, error);
        net.addPrecondition(Red, t6);
        net.addPrecondition(Detected, repair);
        net.addPostcondition(waitClock1, Sample);
        net.addPrecondition(Err, fail);
        net.addPostcondition(rejFromErr, Ok);
        net.addPostcondition(t3, Green);
        net.addPostcondition(waitClock, Rej);
        net.addPostcondition(detect, Detected);
        net.addPostcondition(waitClock2, Sample);
        net.addPrecondition(Rej, rejFromErr);
        net.addPrecondition(Ok, rejFromOk);
        net.addPrecondition(Sample, t4);
        net.addPostcondition(t4, Red);
        net.addPrecondition(Sample, t2);
        net.addPostcondition(error, Err);
        net.addPostcondition(t6, Rej);
        net.addPostcondition(t2, Green);
        net.addPostcondition(waitClock1, Clock1);
        net.addPrecondition(Sample, t3);
        net.addPrecondition(Sample, t5);
        net.addPrecondition(Clock1, waitClock2);
        net.addPrecondition(Ko, detect);
        net.addPostcondition(rejFromOk, Ok);
        net.addPostcondition(rejFromErr, Clock);
        net.addPrecondition(Clock, waitClock1);
        net.addPostcondition(t5, Red);
        net.addPostcondition(fail, Ko);
        net.addPrecondition(Clock2, waitClock);
        net.addPostcondition(min, Min);
        net.addPrecondition(Start, min);

        //Generating Properties
        marking.setTokens(Start, 1);
        marking.setTokens(Clock, 1);
        marking.setTokens(Clock1, 0);
        marking.setTokens(Clock2, 0);
        marking.setTokens(Detected, 0);
        marking.setTokens(Err, 0);
        marking.setTokens(Green, 0);
        marking.setTokens(Ko, 0);
        marking.setTokens(Ok, 1);
        marking.setTokens(Red, 0);
        marking.setTokens(Rej, 0);
        marking.setTokens(Sample, 0);
        detect.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("4"), MarkingExpr.from("1", net)));
        error.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.004", net)));
        fail.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("0.0004", net)));
        rejFromErr.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("6", net)));
        rejFromOk.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from("6", net)));
        repair.addFeature(new PostUpdater("Clock=1, Clock1=0, Clock2=0, Rej=0, Green=0, Red=0", net));
        repair.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("4"), MarkingExpr.from("1", net)));
        t2.addFeature(new EnablingFunction("Ok>0"));
        t2.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", net)));
        t2.addFeature(new Priority(0));
        t3.addFeature(new EnablingFunction("Ok==0"));
        t3.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        t3.addFeature(new Priority(0));
        t4.addFeature(new EnablingFunction("Ok==0"));
        t4.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("9", net)));
        t4.addFeature(new Priority(0));
        t5.addFeature(new EnablingFunction("Ok>0"));
        t5.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        t5.addFeature(new Priority(0));
        t6.addFeature(new EnablingFunction("Red==2"));
        t6.addFeature(new PostUpdater("Green=0, Clock1=0, Clock2=0", net));
        t6.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from("1", net)));
        t6.addFeature(new Priority(0));
        waitClock.addFeature(new PostUpdater("Green=0, Red=0", net));
        waitClock.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriods.get(0), MarkingExpr.from("1", net)));
        waitClock.addFeature(new Priority(0));
        waitClock1.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriods.get(1), MarkingExpr.from("1", net)));
        waitClock1.addFeature(new Priority(0));
        waitClock2.addFeature(StochasticTransitionFeature.newDeterministicInstance(rejuvenationPeriods.get(2), MarkingExpr.from("1", net)));
        waitClock2.addFeature(new Priority(0));
        min.addFeature(StochasticTransitionFeature.newDeterministicInstance(minTimeAnalysis, MarkingExpr.from("1", net)));
        min.addFeature(new Priority(0));
    }

    public static double getB(double lambda, double mu){
        return Math.log((lambda * mu - 1) / (lambda * mu)) / (-lambda);
    }

    public static String getExpol1(double mu, double std){

        return "";
    }
    
    public static void periodic() {
        RewardRate unavailability = RewardRate.fromString("Ko + Detected > 0 || Rej > 0");
        RewardRate undetectedFailure = RewardRate.fromString("Ko > 0");
        PetriNet net = new PetriNet();
        Marking marking = new Marking();
        build_simple_GEN(net, marking, new BigDecimal("1300"));
        SteadyStateSolution<Marking> solution = RegSteadyState.builder().build().compute(net, marking);
        SteadyStateSolution<RewardRate> rewards = SteadyStateSolution.computeRewards(solution, unavailability, undetectedFailure);
        System.out.println(rewards.getSteadyState().get(undetectedFailure));        
        System.out.println(rewards.getSteadyState().get(unavailability));
    }
    
    public static void warning1() {
        RewardRate unavailability = RewardRate.fromString("Ko + Detected > 0 || Rej > 0");
        RewardRate undetectedFailure = RewardRate.fromString("Ko > 0");
        PetriNet net = new PetriNet();
        Marking marking = new Marking();
        build_c1_GEN(net, marking, List.of(new BigDecimal("433.33"), new BigDecimal("433.33"), new BigDecimal("433.33")));
        SteadyStateSolution<Marking> solution = RegSteadyState.builder().build().compute(net, marking);
        SteadyStateSolution<RewardRate> rewards = SteadyStateSolution.computeRewards(solution, unavailability, undetectedFailure);
        System.out.println(rewards.getSteadyState().get(undetectedFailure));
        System.out.println(rewards.getSteadyState().get(unavailability));
    }

    public static void warning2() {
        RewardRate unavailability = RewardRate.fromString("Ko + Detected > 0 || Rej > 0");
        RewardRate undetectedFailure = RewardRate.fromString("Ko > 0");
        PetriNet net = new PetriNet();
        Marking marking = new Marking();
        build_c2_GEN(net, marking, List.of(new BigDecimal("433.33"), new BigDecimal("433.33"), new BigDecimal("433.33")));
        SteadyStateSolution<Marking> solution = RegSteadyState.builder().build().compute(net, marking);
        SteadyStateSolution<RewardRate> rewards = SteadyStateSolution.computeRewards(solution, unavailability, undetectedFailure);
        System.out.println(rewards.getSteadyState().get(undetectedFailure));
        System.out.println(rewards.getSteadyState().get(unavailability));
    }

    public static void main(String[] args) {
//        periodic();
//        warning1();
        warning2();
    }
}
