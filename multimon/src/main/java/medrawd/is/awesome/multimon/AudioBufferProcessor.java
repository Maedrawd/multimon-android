package medrawd.is.awesome.multimon;

import android.media.AudioRecord;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioBufferProcessor extends Thread {
    public static final int BUFFER_SIZE = 16384;
    public static String TAG = AudioBufferProcessor.class.getSimpleName();
    private final AudioInShort.MaxValueListener changeListener;

    private AudioInShort audioIn;
    private final PacketCallback callback;
    private final DemodConfig.Demod selctedDemod;

    DemodConfig demodConfig;

    private boolean inited = false;

    float[] fbuf = new float[BUFFER_SIZE];
    short[] sbuf = new short[BUFFER_SIZE];
    private int fbuf_cnt = 0;
    private final boolean writeAudioBuffer = false; // for debug

    int _dumpCount = 1024;
    private boolean stop = false;


    native DemodConfig init(int demodIndex);

    native void processBufferFloat(float[] fbuf, int length);

    native void processBufferShort(short[] sbuf, int length);

    // for debugging the caputured samples
    // sox -e signed -r 22050 -b 16 sambombo.raw output2.wav
    FileOutputStream _fos;
    File _f = new File("/sdcard/PacketDroidSamples.raw");

    private final LinkedBlockingQueue<short[]> queueShort;
    private final LinkedBlockingQueue<float[]> queueFloat;

    static {
        System.loadLibrary("multimon");
    }

    public AudioBufferProcessor(PacketCallback cb, AudioInShort.MaxValueListener changeListener, DemodConfig.Demod selectedDemod) {
        super(TAG);
        this.changeListener = changeListener;
        queueShort = new LinkedBlockingQueue<>();
        queueFloat = new LinkedBlockingQueue<>();

        //audioIn = demodConfig.useFloat? new AudioInFloat(this, BUFFER_SIZE) : new AudioInShort(this, BUFFER_SIZE);
        callback = cb;

        selctedDemod = selectedDemod;

        if (writeAudioBuffer) {
            try {
                _fos = new FileOutputStream(_f);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

//	public void read() {
//		if (!inited) { inited = true; init(); } // init native demodulators
//		if (!audioIn.isAlive()) audioIn.start();
//
//		while (true) {
//			try {
//				decode(queue.take());
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}

    @Override
    public void run() {
        if (!inited) {
            inited = true;
            demodConfig = init(selctedDemod.ordinal());

            audioIn = new AudioInShort(this, BUFFER_SIZE, demodConfig.sampleRate);
            audioIn.setListener(changeListener);
        } // init native demodulators
        if (!audioIn.isAlive()) audioIn.start();

        while (!stop) {
            try {
                //if(demodConfig.useFloat){
                //    decode(queueFloat.take());
                //} else {
                    decode(queueShort.take());
                //}
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void startRecording() {
        if (audioIn.recorder.getState() == AudioRecord.STATE_INITIALIZED) {
            audioIn.recorder.startRecording();
        } else {
            throw new RuntimeException("unable to start recording");
        }
    }

    public void stopRecording() {
        audioIn.close();
        queueShort.clear();
        queueFloat.clear();
        stop = true;
    }


    void decode(short[] s) {
        //Log.d(TAG, "sending short array for processing " + Arrays.toString(s));
        for (int i = 0; i < s.length; i++, fbuf_cnt++) {
            if (writeAudioBuffer) {
                try {
                    _fos.write(s[i] & 0xFF);
                    _fos.write((s[i] >> 8) & 0xFF);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            sbuf[fbuf_cnt] = s[i];
            fbuf[fbuf_cnt] = s[i] * (1.0f / 32768.0f);
        }


        if (fbuf_cnt > demodConfig.overlap) {
            int length = fbuf_cnt - demodConfig.overlap;
            if(demodConfig.useFloat){
                //Log.d(TAG, "sending float array for processing " + Arrays.toString(fbuf));
                //Log.d(TAG, "sending float array for processing " + length);
                processBufferFloat(fbuf, length);
                if(demodConfig.overlap >0) {
                    System.arraycopy(fbuf, length, fbuf, 0, demodConfig.overlap);
                }
            } else {
                //Log.d(TAG, "sending short array for processing " + Arrays.toString(sbuf));
                //Log.d(TAG, "sending short array for processing " + length);
                processBufferShort(sbuf, length);
                if(demodConfig.overlap >0) {
                    System.arraycopy(sbuf, length, sbuf, 0, demodConfig.overlap);
                }
            }

            fbuf_cnt = demodConfig.overlap;
        }
    }

    void decode(float[] s) {
        for (int i = 0; i < s.length; i++) {
            fbuf[fbuf_cnt++] = s[i];
            //fbuf[fbuf_cnt++] = s[i] * (1.0f / 32768.0f);
        }

        Log.d(TAG, "sending float array for processing " + Arrays.toString(fbuf));
        Log.d(TAG, "sending float array for processing " + fbuf.length);
        if (fbuf_cnt > demodConfig.overlap) {
            int length = fbuf_cnt - demodConfig.overlap;
            Log.d(TAG, "sending short array for processing " + length);
            processBufferFloat(fbuf, length);
            Log.d(TAG, "sending short array for processing " + length);
            System.arraycopy(fbuf, length, fbuf, 0, demodConfig.overlap);
            //processBufferFloat(fbuf, fbuf_cnt - demodConfig.overlap);
            //System.arraycopy(fbuf, fbuf_cnt - demodConfig.overlap, fbuf, 0, demodConfig.overlap);
            fbuf_cnt = demodConfig.overlap;
        }
    }

    public void callback(byte[] data) {
        Log.d(TAG, "called callback: " + new String(data));
        callback.received(data);
    }

    public void callback(char data) {
        Log.d(TAG, "called callback: " + data);
        callback.received(data);
    }

    public void process(short[] buffer) throws InterruptedException {
        //if(demodConfig.useFloat){
        //    throw new RuntimeException("expecting float");
        //}
        queueShort.put(buffer);
    }

    public void process(float[] buffer) throws InterruptedException {
        //if(!demodConfig.useFloat){
        //    throw new RuntimeException("expecting short");
        //}
        queueFloat.put(buffer);
    }

    public String callback(String message){
        Log.d(TAG, "callback "+message);
        return message;
    }

}