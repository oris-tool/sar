package org.oristool.conditionbased;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.Integer;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.lang.Math.*;
import java.util.function.Supplier;

import org.oristool.analyzer.log.AnalysisMonitor;
import org.oristool.analyzer.log.NoOpLogger;
import org.oristool.analyzer.log.PrintStreamLogger;
import org.oristool.math.OmegaBigDecimal;
import org.oristool.math.domain.DBMZone;
import org.oristool.math.expression.Expolynomial;
import org.oristool.math.expression.Variable;
import org.oristool.math.function.GEN;
import org.oristool.math.function.PartitionedGEN;
import org.oristool.models.pn.MarkingConditionStopCriterion;
import org.oristool.models.pn.PostUpdater;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.RewardRate;
import org.oristool.models.stpn.SteadyStateSolution;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.TransientSolutionViewer;
import org.oristool.models.stpn.steady.RegSteadyState;
import org.oristool.models.stpn.trans.RegTransient;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.EnablingFunction;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.MarkingCondition;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Postcondition;
import org.oristool.petrinet.Precondition;
import org.oristool.petrinet.Transition;
import org.oristool.simulator.Sequencer;
import org.oristool.simulator.TimeSeriesRewardResult;
import org.oristool.simulator.rewards.ContinuousRewardTime;
import org.oristool.simulator.rewards.RewardEvaluator;
import org.oristool.simulator.stpn.STPNSimulatorComponentsFactory;
import org.oristool.simulator.stpn.TransientMarkingConditionProbability;

public class Rej_n_samples {
	
	public static int BUILD_TIMEBOUND = 1107;            //transient analysis timebound to calculate the defer times
	public static double EPSILON = 0.00001;	            //max admitted error
	private static final Integer wRedSampleErr = 99;    //correct Red sample
	private static final String wGreenSampleErr = "5";  //wrong Green sample
	private static final String wRedSampleOk = "5";     //wrong Red sample
	private static final String wGreenSampleOk = "95";  //correct Green sample
	public static int N_SAMPLES = 4;					//number of sample

	public static void main(String[] args) {
		PetriNet net = new PetriNet();
		Marking marking = new Marking();

		//Io qui costruisico la rete ottimizzata
		build(net, marking, N_SAMPLES, BUILD_TIMEBOUND, wRedSampleErr);


		// Questi sono i suoi risultati
		Map<RewardRate, BigDecimal> rSSRewards = rejSteadyStateAnalysis(net, marking, "Ko;If(Ko+Detected>0 || Rej>0,1,0);If(Ok>0 && Rej>0,1,0)");
		
		System.out.println("Steady State Analisys ("+ N_SAMPLES + " samples):");
		for(Entry<RewardRate, BigDecimal> e : rSSRewards.entrySet()) {
			System.out.println(" - " + e.getKey() + ": " + e.getValue());
		}

		// Qui invece credo che stia facendo vedere ilo transiente della  unreliability, che poi non + esattamente lei
		try {
			rejTransientAnalysis(net, marking, "Ko", "Ko>0||R>0||GR>0||GGR>0||GGGR>0||GGGGR>0||Rej>0", 4320, 1, wRedSampleErr, N_SAMPLES, false);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
		
	public static Map<RewardRate, BigDecimal> rejSteadyStateAnalysis(PetriNet net, Marking marking, String rewards) {
		RegSteadyState steadyAnalysis = RegSteadyState.builder().build();
        SteadyStateSolution<Marking> result = steadyAnalysis.compute(net, marking);
        SteadyStateSolution<RewardRate> steadyRewards = SteadyStateSolution.computeRewards(result, rewards);
        return steadyRewards.getSteadyState();
	}
	
	public static void rejTransientAnalysis(PetriNet net, Marking marking, String reward, String stopCondition, int timeB, double timeS, int pr, int station, boolean cumulative) throws FileNotFoundException {
		RegTransient analysis;
		
		if(stopCondition != ""){
			analysis = RegTransient.builder()
				    .timeBound(new BigDecimal(timeB))
				    .timeStep(new BigDecimal(timeS))
				    .stopOn(MarkingCondition.fromString(stopCondition))
					.logger(new PrintStreamLogger(new PrintStream(new FileOutputStream(System.getProperty("user.dir").toString()+"/logger.txt"))))

				    .build();
		}
		else {
			analysis = RegTransient.builder()
				    .timeBound(new BigDecimal(timeB))
				    .timeStep(new BigDecimal(timeS))				    
				    .build();
		}
		
		TransientSolution<DeterministicEnablingState, Marking> result = analysis.compute(net, marking);
		TransientSolution<DeterministicEnablingState, RewardRate> transientRewards = TransientSolution.computeRewards(cumulative, result, reward);
		TransientSolution<DeterministicEnablingState, RewardRate> transientRewards2 = cumulative ? TransientSolution.computeRewards(true, result, reward) : null;
		TreeMap<Integer,Double> map = new TreeMap<>();
		TreeMap<Integer,Double> map2 = new TreeMap<>();
		TreeMap<Integer,Double> mapX = new TreeMap<>();
		for(int t=0; t<transientRewards.getSolution().length; t++) {
			map.put(t, transientRewards.getSolution()[t][0][0]);
			map2.put(t, EPSILON * t);
			mapX.put(t, (double) (timeS * t));
		}
		System.out.print("\n Transient Analisys:");
		System.out.println(map.values());
		System.out.print("\n Retta:");
		System.out.println(map2.values());
		System.out.print("\n Asse x:");
		System.out.println(mapX.values());

		transientRewards.writeCSV("PR" + pr + "_Stations" + station + "_" + reward + ".csv", 20);
		if(cumulative)
			transientRewards2.writeCSV("PR" + pr + "_Stations" + station + "_" + reward + "_cumulative.csv", 20);

		new TransientSolutionViewer(transientRewards);
	}

	public static void transientSimulate(PetriNet net, Marking marking, String cond, String stopCondition, int timeB, int timeS, int pr, int station) {
		BigDecimal bound = new BigDecimal(timeB);
		BigDecimal step = new BigDecimal(timeS);
		int samples = bound.divide(step).intValue() + 1;

		// simulate
		Sequencer s = new Sequencer(net, marking,
				new STPNSimulatorComponentsFactory(), NoOpLogger.INSTANCE);
		TransientMarkingConditionProbability reward =
				new TransientMarkingConditionProbability(s,
						new ContinuousRewardTime(step), samples,
						MarkingCondition.fromString(cond));

		RewardEvaluator rewardEvaluator = new RewardEvaluator(reward, 50000);
		s.simulate();

		// evaluate reward
		TimeSeriesRewardResult probs = (TimeSeriesRewardResult) rewardEvaluator.getResult();
		DeterministicEnablingState initialReg = new DeterministicEnablingState(marking, net);

		TransientSolution<DeterministicEnablingState, RewardRate> transientRewards =
				new TransientSolution<>(bound, step, List.of(initialReg),
						List.of(RewardRate.fromString(cond)), initialReg);

		TransientSolution<DeterministicEnablingState, RewardRate> transientRewards2 =
				new TransientSolution<>(bound, step, List.of(initialReg),
						List.of(RewardRate.fromString(cond)), initialReg);

		transientRewards.writeCSV("PR" + pr + "_Stations" + station + "_" + reward + ".csv", 9);
		transientRewards2.writeCSV("PR" + pr + "_Stations" + station + "_" + reward + "_cumulative.csv", 9);
		new TransientSolutionViewer(transientRewards);
	}
	
	public static void build(PetriNet net, Marking marking, int n_samples, int rejuvenation_time, int per) {
		int r_list_dim = 0;
		for(int i=0; i<=n_samples; i++) r_list_dim += Math.pow(2, i);
		List<Place> r_list = Arrays.asList(new Place[r_list_dim]);   //list of places correspondent to a result of a sample
		
		int s_list_dim = 0;
		for(int i=0; i<n_samples; i++) s_list_dim += Math.pow(2, i);
		List<Place> s_list = Arrays.asList(new Place[s_list_dim]);   //list of places before sampling
		
		List<Transition> defer_list = Arrays.asList(new Transition[r_list_dim]); //list of defers transitions
		
		buildStepByStep(net, marking, r_list, s_list, defer_list, n_samples, rejuvenation_time, per);
		optimizeDefer(net, marking, r_list, s_list, defer_list, rejuvenation_time);
		//Ricostruisco la rete con tutti i tempi corretti
		buildRej(net, marking);
	}

	public static void build(PetriNet net, Marking marking, int n_samples, int rejuvenation_time, int per, HashMap<String, Integer> deferMap, Function<Double, Double> sensitivityAt,  Function<Double, Double> specificityAt) {
		int r_list_dim = 0;
		for(int i=0; i<=n_samples; i++) r_list_dim += Math.pow(2, i);
		List<Place> r_list = Arrays.asList(new Place[r_list_dim]);   //list of places correspondent to a result of a sample

		int s_list_dim = 0;
		for(int i=0; i<n_samples; i++) s_list_dim += Math.pow(2, i);
		List<Place> s_list = Arrays.asList(new Place[s_list_dim]);   //list of places before sampling

		List<Transition> defer_list = Arrays.asList(new Transition[r_list_dim]); //list of defers transitions

		buildStepByStep(net, marking, r_list, s_list, defer_list, n_samples, rejuvenation_time, per);
		optimizeDefer(net, marking, r_list, s_list, defer_list, rejuvenation_time, deferMap, sensitivityAt, specificityAt);
		//Ricostruisco la rete con tutti i tempi corretti
		buildRej(net, marking);
	}

	public static void buildStepByStep(PetriNet net, Marking marking, List<Place> r_list, List<Place> s_list, List<Transition> defer_list, int n_samples, int rejuvenationTime, int per) {
		//Build the PN to calculate the defer times
		
		//Generating Nodes
		Place Clock = net.addPlace("Clock");
		Place Detected = net.addPlace("Detected");
		Place Err = net.addPlace("Err");
		Place Ko = net.addPlace("Ko");
		Place Ok = net.addPlace("Ok");
		Place Rej = net.addPlace("Rej");
		
		//Place sample results 
		fillGRlist_place(net, r_list, 1, "");  //list start from index 1 to mantain the indexes for the sequential storage of a tree on an array
		
		//Place pre-sample
		s_list.set(0, net.addPlace("s"));
		fillGRlist_place(net, s_list, 1, "s");
		
		//Transitions
		Transition detect = net.addTransition("detect");
		Transition error = net.addTransition("error");
		Transition fail = net.addTransition("fail");
		
		//Sampling transitions
		int s_trans_list_dim = s_list.size() * 4;
		List<Transition> s_trans_list = Arrays.asList(new Transition[s_trans_list_dim]);
		for(int i=0; i<s_trans_list_dim; i++) {
			s_trans_list.set(i, net.addTransition("t" + i));
		}
		
		//Defering transitions 
		defer_list.set(0, net.addTransition("w"));
		fillGRlist_transition(net, defer_list, 1, "w"); 
	
		//Generating Connectors
		net.addInhibitorArc(Rej, error);
		net.addInhibitorArc(Rej, fail);
		
		net.addPrecondition(Ko, detect);
		net.addPrecondition(Ok, error);
		net.addPrecondition(Err, fail);
		net.addPostcondition(fail, Ko);
		net.addPostcondition(error, Err);
		net.addPostcondition(detect, Detected);		
		net.addPrecondition(Clock, defer_list.get(0));
		
		//last defer pre and post condition
		for(int i = r_list.size() - (int) Math.pow(2, n_samples); i<r_list.size(); i++) { //Pre and Post condition of the last samples places
			net.addPrecondition(r_list.get(i), defer_list.get(i));
			net.addPostcondition(defer_list.get(i), Rej);
		}
		
		//s transition pre and post conditions
		int r_index = 1;
		for(int s_index=0; s_index<s_list.size(); s_index++) {
			net.addPrecondition(s_list.get(s_index), s_trans_list.get(4*s_index));
			net.addPrecondition(s_list.get(s_index), s_trans_list.get(4*s_index+1));
			net.addPrecondition(s_list.get(s_index), s_trans_list.get(4*s_index+2));
			net.addPrecondition(s_list.get(s_index), s_trans_list.get(4*s_index+3));
			
			net.addPostcondition(s_trans_list.get(4*s_index), r_list.get(r_index));
			net.addPostcondition(s_trans_list.get(4*s_index+1), r_list.get(r_index));
			r_index++;
			net.addPostcondition(s_trans_list.get(4*s_index+2), r_list.get(r_index));
			net.addPostcondition(s_trans_list.get(4*s_index+3), r_list.get(r_index));
			r_index++;
		}
		
		//defer pre e post conditions
		net.addPostcondition(defer_list.get(0), s_list.get(0));
		for(int i=1; i<s_list.size(); i++) {
			net.addPrecondition(r_list.get(i), defer_list.get(i));
			net.addPostcondition(defer_list.get(i), s_list.get(i));
		}

		//Marking
		marking.setTokens(Clock, 1);
		marking.setTokens(Detected, 0);
		marking.setTokens(Err, 0);
		marking.setTokens(Ko, 0);
		marking.setTokens(Ok, 1);
		marking.setTokens(Rej, 0);
		
		for(Place p : r_list.subList(1, r_list.size())) marking.setTokens(p, 0);
		for(Place p : s_list) marking.setTokens(p, 0);
		
		//Generating Properties
		String detect_update = "Clock=0,Rej=0";
		for(Place p : r_list.subList(1, r_list.size())) detect_update += "," + p.getName() + "=0";
		detect.addFeature(new PostUpdater(detect_update, net));
		
		for(int i=0; i<s_trans_list_dim; i++) {
			s_trans_list.get(i).addFeature(new EnablingFunction("Ok==0")); // FP
			s_trans_list.get(i).addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(100 - per), net)));
			s_trans_list.get(i).addFeature(new Priority(0));
			i++;
			s_trans_list.get(i).addFeature(new EnablingFunction("Ok==1")); // TP
			s_trans_list.get(i).addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(per), net)));
			s_trans_list.get(i).addFeature(new Priority(0));
			i++;
			s_trans_list.get(i).addFeature(new EnablingFunction("Ok==0")); // TN
			s_trans_list.get(i).addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(per), net)));
			s_trans_list.get(i).addFeature(new Priority(0));
			i++;
			s_trans_list.get(i).addFeature(new EnablingFunction("Ok==1")); // FN
			s_trans_list.get(i).addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(100 - per), net)));
			s_trans_list.get(i).addFeature(new Priority(0));
		}

		detect.addFeature(StochasticTransitionFeature.newUniformInstance("0", "4"));

		fail.addFeature(StochasticTransitionFeature.newHypoExp(BigDecimal.valueOf(0.00208), BigDecimal.valueOf(0.00277)));
		error.addFeature(StochasticTransitionFeature.newHypoExp(BigDecimal.valueOf(0.00615), BigDecimal.valueOf(0.01289)));
		
		//Defers setted to an initial high value
		for(int i=0; i<defer_list.size(); i++) {
			defer_list.get(i).addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(rejuvenationTime), MarkingExpr.from("1", net)));
			defer_list.get(i).addFeature(new Priority(0));
		}
	}
	
	private static void buildRej(PetriNet net, Marking marking) {
		//complete the step by step net
		Transition rejFromErr = net.addTransition("rejFromErr");
		Transition rejFromOk = net.addTransition("rejFromOk");
		Transition repair = net.addTransition("repair");
		
		Place Detected = net.getPlace("Detected");
		Place Rej = net.getPlace("Rej");
		Place Err = net.getPlace("Err");
		Place Ok = net.getPlace("Ok");
		Place Clock = net.getPlace("Clock");
		
		net.addPrecondition(Detected, repair);
		net.addPrecondition(Rej, rejFromErr);
		net.addPrecondition(Rej, rejFromOk);
		net.addPrecondition(Err, rejFromErr);
		net.addPrecondition(Ok, rejFromOk);
		net.addPostcondition(rejFromOk, Clock);
		net.addPostcondition(rejFromErr, Clock);
		net.addPostcondition(rejFromOk, Ok);
		net.addPostcondition(rejFromErr, Ok);
		net.addPostcondition(repair, Ok);

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

		repair.addFeature(new PostUpdater("Clock=1", net));

		//repair.addFeature(new PostUpdater());

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
	}
	
	private static List<Double> optimizeDefer(PetriNet net, Marking marking, List<Place> r_list, List<Place> s_list, List<Transition> defer_list, int rejuvenation_time){
		//find the optimal defer values
		List<Double> defer_time_list = Arrays.asList(new Double[defer_list.size()]);
		Map<Transition, Integer> defered_time_map = new HashMap<>(); //map that associate each defer transition to the sum of all the precedent defers' time 
		double defered_time, defer;	
		Entry<Integer, Double> defered_entry;
		for(int i=0; i<defer_list.size(); i++) {
			// Seleziono le stop condition che mi interessano
			String stop_conditions = getStopConditions(i, r_list, s_list, defer_list);
			defered_entry = getOptimalDefer(net, marking, stop_conditions, rejuvenation_time);
			if(defered_entry == null) defered_time = 0;
			else defered_time = defered_entry.getKey(); //* 0.1;
			if(i == 0) {
				defered_time_map.put(defer_list.get(0), (int) defered_time);
				defer = defered_time;
			}
			else {
				int j;
				String p = r_list.get(i).getName();
				if(p.charAt(p.length()-1) == 'G') { //if G is the last letter of the place name is a left son else is a right son
					j = (i - 1) / 2;
				}
				else {
					j = (i - 2) / 2;
				}
				int father_defered_time = defered_time_map.get(defer_list.get(j));
				defer = defered_time - father_defered_time;
				if(defer > 0) defered_time_map.put(defer_list.get(i), (int) defered_time);
				else {
					defer = 0;
					defered_time_map.put(defer_list.get(i), father_defered_time);
				}
			}

			//Setto il defer calcolato
			defer_time_list.set(i, defer);
			defer_list.get(i).removeFeature(StochasticTransitionFeature.class);
			defer_list.get(i).addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(defer), MarkingExpr.from("1", net)));
			System.out.println(defer_list.get(i).getName() + ": " + defer);
		}
		System.out.println();
		return defer_time_list;
	}

	private static List<Double> optimizeDefer(PetriNet net, Marking marking, List<Place> r_list, List<Place> s_list, List<Transition> defer_list, int rejuvenation_time, HashMap<String, Integer> deferMap, Function<Double, Double> sensitivityAt,  Function<Double, Double> specificityAt){
		//TODO check that PER values sia adeguataente dimensionato (deve avere numero di samples pari a timeLimit/timeStep)
		//find the optimal defer values
		List<Double> defer_time_list = Arrays.asList(new Double[defer_list.size()]);
		Map<Transition, Integer> defered_time_map = new HashMap<>(); //map that associate each defer transition to the sum of all the precedent defers' time
		double defered_time, defer;
		Entry<Integer, Double> defered_entry;
		for(int i=0; i<defer_list.size(); i++) {
			// Seleziono le stop condition che mi interessano
			String stop_conditions = getStopConditions(i, r_list, s_list, defer_list);
			if(deferMap.get(defer_list.get(i).getName()) != null){
				defered_time = deferMap.get(defer_list.get(i).getName());

			} else {
				defered_entry = getOptimalDefer(net, marking, stop_conditions, rejuvenation_time);
				if(defered_entry == null) defered_time = 0;
				else defered_time = defered_entry.getKey(); //* 0.1;
				deferMap.put(defer_list.get(i).getName(), (int)defered_time);
			}

			// cambiare PrecisionAndRecall sulla base del tempo

			if(i == 0) {
				defered_time_map.put(defer_list.get(0), (int) defered_time);
				defer = defered_time;
			}
			else {
				int j;
				String p = r_list.get(i).getName();
				if(p.charAt(p.length()-1) == 'G') { //if G is the last letter of the place name is a left son else is a right son
					j = (i - 1) / 2;
				}
				else {
					j = (i - 2) / 2;
				}
				int father_defered_time = defered_time_map.get(defer_list.get(j));
				defer = defered_time - father_defered_time;
				if(defer > 0) defered_time_map.put(defer_list.get(i), (int) defered_time);
				else {
					defer = 0;
					defered_time_map.put(defer_list.get(i), father_defered_time);
				}
			}

			//Setto il defer calcolato
			defer_time_list.set(i, defer);
			defer_list.get(i).removeFeature(StochasticTransitionFeature.class);
			defer_list.get(i).addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(defer), MarkingExpr.from("1", net)));
			Place thePlace = net.getPostconditions(defer_list.get(i)).stream().findFirst().get().getPlace();
			List<Transition> myImmTransitions = net.getTransitions().stream().filter(t -> net.getPrecondition(thePlace, t) != null).collect(Collectors.toList());
			for(Transition t: myImmTransitions){
				t.removeFeature(StochasticTransitionFeature.class);
				// in realtà si dovrebbe fare una funzione diversa per precision and recall
				Double probability = 0.;

				if(myImmTransitions.indexOf(t) == 0) probability = 1 - sensitivityAt.apply(defered_time); // FP
				if(myImmTransitions.indexOf(t) == 1) probability = sensitivityAt.apply(defered_time); // TP
				if(myImmTransitions.indexOf(t) == 2) probability = specificityAt.apply(defered_time); // TN
				if(myImmTransitions.indexOf(t) == 3) probability = 1 - sensitivityAt.apply(defered_time); // FN

				t.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(String.valueOf(probability), net)));

			}
			System.out.println(defer_list.get(i).getName() + ": " + defer);
		}
		System.out.println();
		return defer_time_list;
	}
	
	public static Entry<Integer,Double> getOptimalDefer(PetriNet pn, Marking m, String stop_conditions, int rejuvenationTime) {
		//compute p(Ko|OBS,t)
		RegTransient analysis = RegTransient.builder()
				.timeBound(new BigDecimal(rejuvenationTime))
				.timeStep(new BigDecimal("1."))
				.stopOn(MarkingCondition.fromString(stop_conditions))
				.build();
		TransientSolution<DeterministicEnablingState, Marking> result = analysis.compute(pn, m);
		TransientSolution<DeterministicEnablingState, RewardRate> transientRewards = TransientSolution.computeRewards(false, result, "Ko");
		//compute p(Ko|OBS,t)/t
		TreeMap<Integer,Double> map = new TreeMap<>();
		for(int t=0; t<transientRewards.getSolution().length; t++) {
			// Qui determino il massimo valore che mi garantisce che il vincolo sia soddisfatto
			//if(transientRewards.getSolution()[t][0][0]/t >= EPSILON) {
			if(transientRewards.getSolution()[t][0][0]/t > EPSILON/* || transientRewards.getSolution()[t][0][0] > EPSILON*/) {
				break;
			}
			map.put(t, transientRewards.getSolution()[t][0][0]/t); 
		}
		return map.lastEntry();
	}
	
	private static String getStopConditions(int index, List<Place> r_list, List<Place> s_list, List<Transition> defer_list) {
		//return the stop condition for the transient analisys in the calcolous of a defer time 
		String stop_conditions = "Ko>0";
		if(index == 0) return (stop_conditions + "||" + s_list.get(0).getName() + ">0");
		String p = r_list.get(index).getName();
		for(int i=0; i<p.length(); i++) {
			if(p.charAt(i) == 'G') stop_conditions += "||" + p.substring(0, i) + "R" + ">0";
			else stop_conditions += "||" + p.substring(0, i) + "G" + ">0";
		}
		
		if(index < s_list.size()) stop_conditions += "||" + s_list.get(index).getName() + ">0";
		else stop_conditions += "||" + "Rej>0";
		return stop_conditions;
	}
	
	private static void fillGRlist_place(PetriNet net, List<Place> plist, int index, String s) {
		//assign the name of the places and add them to the net
		if(index + 1 < plist.size()) {
			plist.set(index, net.addPlace(s + "G")); //left son
			plist.set(index+1, net.addPlace(s + "R"));  //right son
			if((index + 1) * 2 + 2 < plist.size()) {
				fillGRlist_place(net, plist, (index * 2 + 1), s + "G"); //left subtree
				fillGRlist_place(net, plist, ((index + 1) * 2 + 1), s + "R");  //right subtree
			}
		}	
	}
	
	private static void fillGRlist_transition(PetriNet net, List<Transition> tlist, int index, String s) {
		//assign the name of the transitions and add them to the net
		if(index + 1 < tlist.size()) {
			tlist.set(index, net.addTransition(s + "G")); //left son
			tlist.set(index+1, net.addTransition(s + "R"));  //right son
			if((index + 1) * 2 + 2 < tlist.size()) {
				fillGRlist_transition(net, tlist, (index * 2 + 1), s + "G"); //left subtree
				fillGRlist_transition(net, tlist, ((index + 1) * 2 + 1), s + "R");  //right subtree
			}
		}
	}
}

