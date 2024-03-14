import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;

import java.io.FileNotFoundException;

public class Main2 {
    public static int[] N_SAMPLES = {4};
    public static int[] PER = {90, 95, 99};
    public static int BUILD_TIMEBOUND = 693;
    public static void main(String[] args) {
        StringBuilder stringBuilder = new StringBuilder("P&R, TESTS, If(Ko+Detected>0 || Rej>0,1,0), If(Ok>0 && Rej>0,1,0), Ko");
        StringBuilder timeStringBuilder = new StringBuilder("P&R, TESTS, build, unreliability, unavailability, useless");

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
                try {
                    time = System.nanoTime();
                    Rej_n_samples.rejTransientAnalysis(net, marking, "Ko", "Ko", 4320, 1., per, i, false);
                    timeStringBuilder.append((System.nanoTime() - time)/1e9 + "s,");

                    time = System.nanoTime();
                    Rej_n_samples.rejTransientAnalysis(net, marking, "If(Ko+Detected>0 || Rej>0,1,0)", "", 4320, 1., per, i, true);
                    timeStringBuilder.append((System.nanoTime() - time)/1e9 + "s,");

                    time = System.nanoTime();
                    Rej_n_samples.rejTransientAnalysis(net, marking, "If(Ok>0 && Rej>0,1,0)", "", 4320, 1., per, i, true);
                    timeStringBuilder.append((System.nanoTime() - time)/1e9 + "s");
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }

            }
        }

    }
}
