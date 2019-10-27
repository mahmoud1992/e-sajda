# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

# coder.c
#
include $(CLEAR_VARS)

LOCAL_MODULE    := coder
LOCAL_SRC_FILES := coder.c

include $(BUILD_STATIC_LIBRARY)

# decod.c
#
include $(CLEAR_VARS)

LOCAL_MODULE    := decod
LOCAL_SRC_FILES := decod.c

include $(BUILD_STATIC_LIBRARY)

# vad.c
#
include $(CLEAR_VARS)

LOCAL_MODULE    := vad
LOCAL_SRC_FILES := vad.c

include $(BUILD_STATIC_LIBRARY)

# cod_cng.c
#
include $(CLEAR_VARS)

LOCAL_MODULE    := cod_cng
LOCAL_SRC_FILES := cod_cng.c

include $(BUILD_STATIC_LIBRARY)

# dec_cng.c
#
include $(CLEAR_VARS)

LOCAL_MODULE    := dec_cng
LOCAL_SRC_FILES := dec_cng.c

include $(BUILD_STATIC_LIBRARY)

# lsp.c
#
include $(CLEAR_VARS)

LOCAL_MODULE    := lsp
LOCAL_SRC_FILES := lsp.c

include $(BUILD_STATIC_LIBRARY)

# tab_lbc.c
#
include $(CLEAR_VARS)

LOCAL_MODULE    := tab_lbc
LOCAL_SRC_FILES := tab_lbc.c

include $(BUILD_STATIC_LIBRARY)

# util_lbc.c
#
include $(CLEAR_VARS)

LOCAL_MODULE    := util_lbc
LOCAL_SRC_FILES := util_lbc.c

include $(BUILD_STATIC_LIBRARY)

# basop.c
#
include $(CLEAR_VARS)

LOCAL_MODULE    := basop
LOCAL_SRC_FILES := basop.c

include $(BUILD_STATIC_LIBRARY)

# lpc.c
#
include $(CLEAR_VARS)

LOCAL_MODULE    := lpc
LOCAL_SRC_FILES := lpc.c

include $(BUILD_STATIC_LIBRARY)

# exc_lbc.c
#
include $(CLEAR_VARS)

LOCAL_MODULE    := exc_lbc
LOCAL_SRC_FILES := exc_lbc.c

include $(BUILD_STATIC_LIBRARY)

# tame.c
#
include $(CLEAR_VARS)

LOCAL_MODULE    := tame
LOCAL_SRC_FILES := tame.c

include $(BUILD_STATIC_LIBRARY)

# util_cng.c
#
include $(CLEAR_VARS)

LOCAL_MODULE    := util_cng
LOCAL_SRC_FILES := util_cng.c

include $(BUILD_STATIC_LIBRARY)

# native.c (main.c)
#
include $(CLEAR_VARS)

LOCAL_MODULE    := native
LOCAL_SRC_FILES := native.c

LOCAL_STATIC_LIBRARIES := \
coder \
decod \
vad \
cod_cng \
dec_cng \
lsp \
tab_lbc \
util_lbc \
basop \
lpc \
exc_lbc \
tame \
util_cng

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)