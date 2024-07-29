package org.oristool.conditionbased;

import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;

import java.io.FileNotFoundException;
import java.util.HashMap;

public class ConditionBasedTransient {
    public static int[] N_SAMPLES = {5};
    public static int[] PER = {99};
    public static int BUILD_TIMEBOUND = 1706;
    public static void main(String[] args) {
        StringBuilder stringBuilder = new StringBuilder("P&R, TESTS, If(Ko+Detected>0 || Rej>0,1,0), If(Ok>0 && Rej>0,1,0), Ko");
        StringBuilder timeStringBuilder = new StringBuilder("P&R, TESTS, build, unreliability, unavailability, useless");

        for(Integer per: PER){
            HashMap<String, Integer> deferMap = new HashMap<>();
            for(Integer i: N_SAMPLES){
                PetriNet net = new PetriNet();
                Marking marking = new Marking();

                timeStringBuilder.append("\n").append(per).append(", ").append(i).append(", ");
                long time = System.nanoTime();
                //Io qui costruisico la rete ottimizzata
                Rej_n_samples.build(net, marking, i, BUILD_TIMEBOUND, 99, deferMap, t -> {return 99/100.;}, t -> {return 90/100.;});
                timeStringBuilder.append((System.nanoTime() - time)/1e9 + "s,");

                // Questi sono i suoi risultati
                try {
                    time = System.nanoTime();
                    Rej_n_samples.rejTransientAnalysis(net, marking, "Ko", "Ko>0||R>0||GR>0||GGR>0||GGGR>0||GGGGR>0||Rej>0", 4320, 1., per, i, false);
                    timeStringBuilder.append((System.nanoTime() - time)/1e9 + "s,");

                    /*time = System.nanoTime();
                    Rej_n_samples.rejTransientAnalysis(net, marking, "If(Ko+Detected>0 || Rej>0,1,0)", "", 4320, 1., per, i, true);
                    timeStringBuilder.append((System.nanoTime() - time)/1e9 + "s,");

                    time = System.nanoTime();
                    Rej_n_samples.rejTransientAnalysis(net, marking, "If(Ok>0 && Rej>0,1,0)", "", 4320, 1., per, i, true);
                    timeStringBuilder.append((System.nanoTime() - time)/1e9 + "s");*/
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }

            }
        }

    }
}
