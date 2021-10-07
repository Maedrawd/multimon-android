package medrawd.is.awesome.multimon;

import static android.media.AudioRecord.STATE_INITIALIZED;

import android.media.AudioRecord;
import android.util.Log;

// taken from: http://stackoverflow.com/questions/4525206/android-audiorecord-class-process-live-mic-audio-quickly-set-up-callback-func
public abstract class AudioIn extends Thread {
    private static final String TAG = AudioIn.class.getSimpleName();
    protected final AudioBufferProcessor audioBufferProcessor;
    protected AudioRecord recorder;

    public AudioIn(AudioBufferProcessor audioBufferProcessor, int bufferSize) {
        super("AudioIn");
        this.audioBufferProcessor = audioBufferProcessor;
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
    }


    void close() {
        if (recorder != null && recorder.getState() == STATE_INITIALIZED) recorder.stop();
        Log.d(TAG, "AudioIn: close");
    }
}
