package org.oristool.conditionbased;

import org.oristool.models.stpn.RewardRate;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

public class ConditionBasedSteadyState {
    public static int[] N_SAMPLES = {1,2,3,4,5,6,7,8};
    public static int[] PER = {90, 95, 99};
    public static int BUILD_TIMEBOUND = 693;
    public static void main(String[] args) {
        StringBuilder stringBuilder = new StringBuilder("P&R, TESTS, If(Ko+Detected>0 || Rej>0,1,0), If(Ok>0 && Rej>0,1,0), Ko");
        StringBuilder timeStringBuilder = new StringBuilder("P&R, TESTS, build, steady_state");

        for(Integer per: PER){
            for(Integer i: N_SAMPLES){
                PetriNet net = new PetriNet();
                Marking marking = new Marking();

                timeStringBuilder.append("\n").append(per).append(", ").append(i).append(", ");
                long time = System.nanoTime();
                //Io qui costruisico la rete ottimizzata
                Rej_n_samples.build(net, marking, i, BUILD_TIMEBOUND, per);
                timeStringBuilder.append((System.nanoTime() - time)/1e9 + "s,");


                // Questi sono i suoi risultati
                time = System.nanoTime();
                Map<RewardRate, BigDecimal> rSSRewards = Rej_n_samples.rejSteadyStateAnalysis(net, marking, "Ko;If(Ko+Detected>0 || Rej>0,1,0);If(Ok>0 && Rej>0,1,0)");
                timeStringBuilder.append((System.nanoTime() - time)/1e9 + "s");

                stringBuilder.append("\n").append(per).append(", ").append(i);
                for(Map.Entry<RewardRate, BigDecimal> e : rSSRewards.entrySet().stream().sorted(
                        (i1, i2)
                                -> i1.getKey().toString().compareTo(
                                i2.getKey().toString())).collect(Collectors.toList())
                ) {
                    System.out.println(" - " + e.getKey() + ": " + e.getValue());
                    stringBuilder.append(", ").append(e.getValue());
                }
            }
        }


        System.out.println(stringBuilder.toString());
        System.out.println(timeStringBuilder.toString());
    }
}
