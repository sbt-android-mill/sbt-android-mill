LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := HelloWorldAndroidJNI
LOCAL_SRC_FILES := HelloWorldAndroidJNI.c

include $(BUILD_SHARED_LIBRARY)
