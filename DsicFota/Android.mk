LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := \
	framework \
	dsic.server

LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_USE_AAPT2 := true

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_DEX_PREOPT := false

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PRIVATE_PLATFORM_APIS := true

LOCAL_STATIC_ANDROID_LIBRARIES := \
		androidx.appcompat_appcompat \
		android-support-design \
		android-support-v4 \

LOCAL_STATIC_JAVA_LIBRARIES := \
		DsicFotaLib \
       	android-common \

LOCAL_RESOURCE_DIR := \
        $(LOCAL_PATH)/res \
        frameworks/support/v7/appcompat/res \

LOCAL_AAPT_FLAGS := \
        --auto-add-overlay \
        --extra-packages android.support.design \

LOCAL_PACKAGE_NAME := DsicFota

LOCAL_MODULE_PATH := $(TARGET_OUT_APP_PATH)

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := DsicFotaLib:libs/DsicFotaLib.jar
include $(BUILD_MULTI_PREBUILT)
