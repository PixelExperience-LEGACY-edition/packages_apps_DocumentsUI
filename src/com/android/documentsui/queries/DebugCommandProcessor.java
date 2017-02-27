/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.documentsui.queries;

import android.content.Context;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.base.DebugFlags;
import com.android.documentsui.base.EventHandler;
import com.android.documentsui.base.Shared;

import java.util.ArrayList;
import java.util.List;

public final class DebugCommandProcessor implements EventHandler<String> {

    @VisibleForTesting
    static final String COMMAND_PREFIX = "dbg:";

    private static final String TAG = "DebugCommandProcessor";

    private final List<EventHandler<String[]>> mCommands = new ArrayList<>();

    public DebugCommandProcessor() {
        if (Build.IS_DEBUGGABLE) {
            mCommands.add(DebugCommandProcessor::quickViewer);
            mCommands.add(DebugCommandProcessor::gestureScale);
            mCommands.add(DebugCommandProcessor::docDetails);
            mCommands.add(DebugCommandProcessor::forcePaging);
        }
    }

    public void add(EventHandler<String[]> handler) {
        if (Build.IS_DEBUGGABLE) {
            mCommands.add(handler);
        }
    }

    @Override
    public boolean accept(String query) {
        if (query.length() > COMMAND_PREFIX.length() && query.startsWith(COMMAND_PREFIX)) {
            String[] tokens = query.substring(COMMAND_PREFIX.length()).split("\\s+");
            for (EventHandler<String[]> command : mCommands) {
                if (command.accept(tokens)) {
                    return true;
                }
            }
            Log.d(TAG, "Unrecognized debug command: " + query);
        }
        return false;
    }

    private static boolean quickViewer(String[] tokens) {
        if ("qv".equals(tokens[0])) {
            if (tokens.length == 2 && !TextUtils.isEmpty(tokens[1])) {
                DebugFlags.setQuickViewer(tokens[1]);
                Log.i(TAG, "Set quick viewer to: " + tokens[1]);
                return true;
            } else {
                Log.w(TAG, "Invalid command structure: " + TextUtils.join(" ", tokens));
            }
        } else if ("deqv".equals(tokens[0])) {
            Log.i(TAG, "Unset quick viewer");
            DebugFlags.setQuickViewer(null);
            return true;
        }
        return false;
    }

    private static boolean gestureScale(String[] tokens) {
        if ("gs".equals(tokens[0])) {
            if (tokens.length == 2 && !TextUtils.isEmpty(tokens[1])) {
                boolean enabled = asBool(tokens[1]);
                DebugFlags.setGestureScaleEnabled(enabled);
                Log.i(TAG, "Set gesture scale enabled to: " + enabled);
                return true;
            }
            Log.w(TAG, "Invalid command structure: " + TextUtils.join(" ", tokens));
        }
        return false;
    }

    private static boolean docDetails(String[] tokens) {
        if ("docinfo".equals(tokens[0])) {
            if (tokens.length == 2 && !TextUtils.isEmpty(tokens[1])) {
                boolean enabled = asBool(tokens[1]);
                DebugFlags.setDocumentDetailsEnabled(enabled);
                Log.i(TAG, "Set gesture scale enabled to: " + enabled);
                return true;
            }
            Log.w(TAG, "Invalid command structure: " + TextUtils.join(" ", tokens));
        }
        return false;
    }

    private static boolean forcePaging(String[] tokens) {
        if (!Shared.ENABLE_OMC_API_FEATURES) {
            Log.i(TAG, "Paging is disabled.");
            return false;
        }

        if ("page".equals(tokens[0])) {
            if (tokens.length >= 2) {
                try {
                    int offset = Integer.parseInt(tokens[1]);
                    int limit = (tokens.length == 3) ? Integer.parseInt(tokens[2]) : -1;
                    DebugFlags.setForcedPaging(offset, limit);
                    Log.i(TAG, "Set forced paging to offset: " + offset + ", limit: " + limit);
                    return true;
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Command input does not contain valid numbers: "
                            + TextUtils.join(" ", tokens));
                    return false;
                }
            } else {
                Log.w(TAG, "Invalid command structure: " + TextUtils.join(" ", tokens));
            }
        } else if ("deqv".equals(tokens[0])) {
            Log.i(TAG, "Unset quick viewer");
            DebugFlags.setQuickViewer(null);
            return true;
        }
        return false;
    }

    private static final boolean asBool(String val) {
        if (val == null || val.equals("0")) {
            return false;
        }
        if (val.equals("1")) {
            return true;
        }
        return Boolean.valueOf(val);
    }

    public static final class DumpRootsCacheHandler implements EventHandler<String[]> {
        private final Context mContext;

        public DumpRootsCacheHandler(Context context) {
            mContext = context;
        }

        @Override
        public boolean accept(String[] tokens) {
            if ("dumpCache".equals(tokens[0])) {
                DocumentsApplication.getRootsCache(mContext).logCache();
                return true;
            }
            return false;
        }
    }
}
