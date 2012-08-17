#include <jni.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

jint JNICALL add(JNIEnv *, jobject, jint, jint);

jstring JNICALL hello(JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
