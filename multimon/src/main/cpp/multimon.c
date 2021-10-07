
#include <stdio.h>
#include <stdarg.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/wait.h>
#include <stdlib.h>

#include <jni.h>

#include "multimon.h"


static const struct demod_param *dem[] = {&demod_morse, &demod_afsk1200, &demod_afsk2400};

#define NUMDEMOD (sizeof(dem)/sizeof(dem[0]))

static struct demod_state dem_st[NUMDEMOD];
//static unsigned int dem_mask[(NUMDEMOD + 31) / 32];

//#define MASK_SET(n) dem_mask[(n)>>5] |= 1<<((n)&0x1f)
//#define MASK_RESET(n) dem_mask[(n)>>5] &= ~(1<<((n)&0x1f))
//#define MASK_ISSET(n) (dem_mask[(n)>>5] & 1<<((n)&0x1f))


int selectedDemodIndex = 0;
static int verbose_level = 0;

//memset(&dem_afsk1200_st, 0, sizeof(dem_afsk1200_st));
//dem_st[
//dem_afsk1200_st.dem_par = ALL_DEMOD[3]; //&dem_afsk1200;
//&dem_afsk1200->init(dem_afsk1200_st);

void verbprintf(int verb_level, char *fmt, ...) {
    if (verb_level <= verbose_level) {
        if (strstr(fmt, "%") != NULL) {
            va_list args;
            va_start(args, fmt);
            char s[10 * sizeof(fmt)];
            __builtin___vsprintf_chk(s, 0, __bos(s), fmt, args);
            LOGI("%s", s);
            va_end(args);
        } else {
            LOGI("%s", fmt);
        }
    }
}


static void process_buffer(float *float_buf, short *short_buf, unsigned int len) {
    buffer_t buffer = {short_buf, float_buf};
    dem[selectedDemodIndex]->demod(dem_st + selectedDemodIndex, buffer, len);
}

static unsigned int fbuf_cnt = 0;
static int overlap = 18;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    LOGD("called JNI_OnLoad");

    return JNI_VERSION_1_6;
}

jobject
Java_medrawd_is_awesome_multimon_AudioBufferProcessor_init(JNIEnv *env, jobject object, jint i) {
    static int sample_rate = -1;
    unsigned int overlap = 0;

    LOGD("YESYESYES I'm on init");

    LOGD("NUMDEMOD: %d", NUMDEMOD);

    memset(dem_st + i, 0, sizeof(dem_st[i]));
    dem_st[i].dem_par = dem[i];
    if (dem[i]->init)
        dem[i]->init(dem_st + i);
    if (sample_rate == -1)
        sample_rate = dem[i]->samplerate;
    else if (sample_rate != dem[i]->samplerate) {
        fprintf(stdout, "\n");
        LOGE("Error: Current sampling rate %d, demodulator \"%s\" requires %d\n", sample_rate,
             dem[i]->name, dem[i]->samplerate);
        exit(3);
    }
    if (dem[i]->overlap > overlap)
        overlap = dem[i]->overlap;

    selectedDemodIndex = i;
    selectedDemodIndex = i;

    // create named pipe for sending data to Java side
    unlink(NAMED_PIPE);
    int res = mkfifo(NAMED_PIPE, S_IRUSR | S_IWUSR);

    fbuf_cnt = 0;

    jclass jcls = (*env)->FindClass(env, "medrawd/is/awesome/multimon/DemodConfig");
    jmethodID constructor = (*env)->GetMethodID(env, jcls, "<init>", "(IZII)V");
    LOGD("created demod %s from index %d using float: %s overlap: %d samplerate: %d" , dem[i]->name, i, ( dem[i]->float_samples ? "true" : "false"), (int)dem[i]->overlap, (int)dem[i]->samplerate);
    return (*env)->NewObject(env, jcls, constructor, i, (dem[i]->float_samples), (int)dem[i]->overlap, (int)dem[i]->samplerate);
}

JNIEnv *env_global;
jobject *abp_global;

JNIEXPORT void JNICALL
Java_medrawd_is_awesome_multimon_AudioBufferProcessor_processBufferFloat(JNIEnv *env, jobject object, jfloatArray fbuf, jint length) {
    env_global = env;
    abp_global = object;
    verbprintf(1, "ProcessBufferFloat");
    jfloat *jfbuf = (*env)->GetFloatArrayElements(env, fbuf, NULL);
    short sbuf[] = {};
    process_buffer(jfbuf, sbuf, length);
    (*env)->ReleaseFloatArrayElements(env, fbuf, jfbuf, 0);
}

JNIEXPORT void JNICALL
Java_medrawd_is_awesome_multimon_AudioBufferProcessor_processBufferShort(JNIEnv *env, jobject object, jshortArray sbuf, jint length) {
    env_global = env;
    abp_global = object;
    verbprintf(1, "ProcessBufferShort");
    jshort *jsbuf = (*env)->GetShortArrayElements(env, sbuf, NULL);
    float fbuf[] = {};
    process_buffer(fbuf, jsbuf, length);
    (*env)->ReleaseShortArrayElements(env, sbuf, jsbuf, 0);
}

void send_frame_to_java(unsigned char *bp, unsigned int len) {
    LOGD("send_frame_to_java NATIVE %s", bp);

    // prepare data array to pass to callback
    jbyteArray data = (*env_global)->NewByteArray(env_global, len);
    if (data == NULL) {
        LOGD("OOM on allocating data buffer");
        return;
    }
    (*env_global)->SetByteArrayRegion(env_global, data, 0, len, (jbyte *) bp);

    // get callback function
    jclass cls = (*env_global)->GetObjectClass(env_global, abp_global);
    jmethodID callback = (*env_global)->GetMethodID(env_global, cls, "callback", "([B)V");
    if (callback == 0) {
        LOGE("could not find callback");
        return;
    }
    (*env_global)->CallVoidMethod(env_global, abp_global, callback, data);
    //(*env_global)->ReleaseByteArrayElements(env_global, data);
}

void send_char_to_java(unsigned char sign) {
    //LOGD("send_char_to_java NATIVE %s", sign);

    // get callback function
    jclass cls = (*env_global)->GetObjectClass(env_global, abp_global);
    jmethodID callback = (*env_global)->GetMethodID(env_global, cls, "callback", "(C)V");
    if (callback == 0) {
        LOGE("could not find callback");
        return;
    }
    (*env_global)->CallVoidMethod(env_global, abp_global, callback, sign);
    //(*env_global)->ReleaseByteArrayElements(env_global, data);
}