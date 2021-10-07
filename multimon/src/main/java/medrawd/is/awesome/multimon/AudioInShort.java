package medrawd.is.awesome.multimon;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.Arrays;

public class AudioInShort extends AudioIn {
    private static final String TAG = AudioInShort.class.getSimpleName();

    private final short[][] buffers = new short[256][8192];
    private boolean stop = false;
    MaxValueListener listener;

    public AudioInShort(AudioBufferProcessor audioBufferProcessor, int bufferSize, int sampleRate) {
        super(audioBufferProcessor, bufferSize);

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
    }

    public void setListener(MaxValueListener listener) {
        this.listener = listener;
    }

    @SuppressLint("NewApi")
    @Override
    public void run() {
        int ix = 0;

        try {
            recorder.startRecording();
            while (!stop) {
                if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
                    Thread.sleep(100);
                    continue;
                }
                int nRead = 0;
                short[] buffer = buffers[ix++ % buffers.length];
                nRead = recorder.read(buffer, 0, buffer.length);
                //Log.d(TAG, "read " + Arrays.toString(buffer));
                audioBufferProcessor.process(buffer);
                if(null != listener){
                    short max = 0;
                    for (short val : buffer) {
                        if(max<val){
                            max = val;
                        }
                    }
                    listener.onMaxValueChanged(max);
                }
                // process(buffer);
            }
        } catch (Throwable x) {
            Log.e(TAG, "Error reading audio", x);
        }
    }

    @Override
    void close() {
        stop = true;
        super.close();
    }

    public interface MaxValueListener{
        void onMaxValueChanged(short currmax);
    }
}
