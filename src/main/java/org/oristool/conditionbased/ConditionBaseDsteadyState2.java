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
import java.util.stream.Collectors;

public class ConditionBaseDsteadyState2 {


    public static int[] N_SAMPLES = {1,2,3,4,5,6,7,8};
    public static int v1 = 99;
    public static int v2 = 90;
    public static int BUILD_TIMEBOUND = 1706;

    public static String caso = "_" + v1 + "_vs_" + v2;
    private static final String MODEL_PATH = System.getProperty("user.dir") + "/steady_state_fixed" + caso + ".csv";

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

                timeStringBuilder.append("\nCASO").append(i).append(", ");
                long time = System.nanoTime();
                //Io qui costruisico la rete ottimizzata
                Rej_n_samples.build(net, marking, i, BUILD_TIMEBOUND, v1, deferMap, t -> {return v1/100.;}, t -> {return v2/100.;});
                timeStringBuilder.append((System.nanoTime() - time)/1e9 + "s,");


                // Questi sono i suoi risultati
                time = System.nanoTime();
                Map<RewardRate, BigDecimal> rSSRewards = Rej_n_samples.rejSteadyStateAnalysis(net, marking, "Ko;If(Ko+Detected>0 || Rej>0,1,0);If(Ok>0 && Rej>0,1,0)");
                timeStringBuilder.append((System.nanoTime() - time)/1e9 + "s");

                stringBuilder.append("\n").append(", ").append(i);
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
