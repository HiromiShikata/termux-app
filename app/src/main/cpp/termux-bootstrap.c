#include <jni.h>

extern jbyte blob[];
extern int blob_size;

JNIEXPORT jobject JNICALL Java_com_termux_app_TermuxInstaller_getZipBuffer(JNIEnv *env, __attribute__((__unused__)) jobject This)
{
    return (*env)->NewDirectByteBuffer(env, blob, blob_size);
}
