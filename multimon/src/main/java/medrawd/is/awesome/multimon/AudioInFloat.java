package medrawd.is.awesome.multimon;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.Arrays;

public class AudioInFloat extends AudioIn {
    private static final String TAG = AudioInFloat.class.getSimpleName();
    private final float[][] buffers = new float[256][8192];

    public AudioInFloat(AudioBufferProcessor audioBufferProcessor, int bufferSize) {
        super(audioBufferProcessor, bufferSize);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 22050,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, bufferSize);
    }


    @SuppressLint("NewApi")
    @Override
    public void run() {
        int ix = 0;

        try {
            recorder.startRecording();
            while (true) {
                if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
                    Thread.sleep(100);
                    continue;
                }
                int nRead = 0;
                float[] buffer = buffers[ix++ % buffers.length];
                nRead = recorder.read(buffer, 0, buffers.length, AudioRecord.READ_BLOCKING);
                //Log.d(TAG, "read " + Arrays.toString(buffer));
                audioBufferProcessor.process(buffer);
                // process(buffer);
            }
        } catch (Throwable x) {
            Log.e(TAG, "Error reading audio", x);
        }
    }
}
