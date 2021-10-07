package medrawd.is.awesome.multimon;

public class DemodConfig {
    final int index;
    int sampleRate;
    boolean useFloat;
    int overlap;

    public DemodConfig(int index) {
        this.index = index;
    }

    private DemodConfig(int index, boolean useFloat, int overlap, int sampleRate) {
        this.index = index;
        this.useFloat = useFloat;
        this.overlap = overlap;
        this.sampleRate = sampleRate;
    }

    public enum Demod {MORSE, AFSK12, AFSK24}
}
