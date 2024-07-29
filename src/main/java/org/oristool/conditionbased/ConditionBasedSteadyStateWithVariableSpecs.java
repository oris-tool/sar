package org.oristool.conditionbased;

import org.oristool.models.stpn.RewardRate;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConditionBasedSteadyStateWithVariableSpecs {
    private static String perCase = "right_linear";
    public static int[] N_SAMPLES = {1, 2, 3, 4, 5, 6, 7, 8};
    public static int BUILD_TIMEBOUND = 1706;
    private static double sensa = 100;
    private static double sensb = 300;
    private static double sensA = 0.8;
    private static double sensB = 0.95;
    private static final String MODEL_PATH = System.getProperty("user.dir") + "/steady_state_" + perCase + "_" + sensA + "_" + sensb + "_specs.csv";
    public static Function<Double, Double> sensitivityAt = (t) -> {
        if(t < sensa) return sensA;
        if(t >= sensb) return sensB;
        return t * (sensA - sensB)/(sensa - sensb) + (sensa * sensB - sensb * sensA)/(sensa - sensb);
    };

    public static void main(String[] args) {
        StringBuilder stringBuilder = new StringBuilder("P&R, TESTS, If(Ko+Detected>0 || Rej>0,1,0), If(Ok>0 && Rej>0,1,0), Ko");
        StringBuilder timeStringBuilder = new StringBuilder("P&R, TESTS, build, steady_state");
        List<String[]> resultsCSV = new ArrayList<>();
        resultsCSV.add(
                new String[]{"P&R", "TESTS", "If(Ko+Detected>0 || Rej>0,1,0)", "If(Ok>0 && Rej>0,1,0)", "Ko"}
        );


        HashMap<String, Integer> deferMap = new HashMap<>();
        for(Integer i: N_SAMPLES){
            PetriNet net = new PetriNet();
            Marking marking = new Marking();

            timeStringBuilder.append("\n").append(perCase).append(", ").append(i).append(", ");
            long time = System.nanoTime();
            //Io qui costruisico la rete ottimizzata
            Rej_n_samples.build(net, marking, i, BUILD_TIMEBOUND, 90, deferMap, sensitivityAt, sensitivityAt);
            timeStringBuilder.append((System.nanoTime() - time)/1e9 + "s,");


            // Questi sono i suoi risultati
            time = System.nanoTime();
            Map<RewardRate, BigDecimal> rSSRewards = Rej_n_samples.rejSteadyStateAnalysis(net, marking, "Ko;If(Ko+Detected>0 || Rej>0,1,0);If(Ok>0 && Rej>0,1,0)");
            timeStringBuilder.append((System.nanoTime() - time)/1e9 + "s");

            stringBuilder.append("\n").append(perCase).append(", ").append(i);
            ArrayList<String> results = new ArrayList<>();
            for(Map.Entry<RewardRate, BigDecimal> e : rSSRewards.entrySet().stream().sorted(
                    (i1, i2)
                            -> i1.getKey().toString().compareTo(
                            i2.getKey().toString())).collect(Collectors.toList())
            ) {
                System.out.println(" - " + e.getKey() + ": " + e.getValue());
                stringBuilder.append(", ").append(e.getValue());
                results.add(e.getValue().toString());
            }

            resultsCSV.add(
                    new String[]{
                            perCase,
                            i.toString(),
                            results.get(0),
                            results.get(1),
                            results.get(2)
                    }
            );

            writeDataToCSV(resultsCSV, MODEL_PATH);
        }



        System.out.println(stringBuilder.toString());
        System.out.println(timeStringBuilder.toString());
    }

    public static void writeDataToCSV(List<String[]> list, String path){
        try (PrintWriter writer = new PrintWriter(new File(path))) {
            for (String[] row : list) {
                writer.println(String.join(",", row));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
