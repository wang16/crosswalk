// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.app.runtime;

import java.lang.reflect.Method;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * This class is to wrap RuntimeView class in library APK.
 *
 * A web application APK should use this class in its Activity.
 */

public class RuntimeClient extends CrossPackageWrapper {
    private final static String class_name = "org.xwalk.runtime.XWalkRuntimeView";
    private Object mInstance;
    private Method mLoadAppFromUrl;
    private Method mLoadAppFromManifest;
    private Method mOnCreate;
    private Method mOnResume;
    private Method mOnPause;
    private Method mOnDestroy;
    private Method mEnableRemoteDebugging;
    private Method mDisableRemoteDebugging;

    public RuntimeClient(Context context, AttributeSet attrs,CrossPackageWrapperExceptionHandler exception_handler) {
        super(context, class_name, exception_handler, Context.class, AttributeSet.class);
        Context libCtx = getLibCtx();
        Method _getVersion = LookupMethod("getVersion");
        String _lib_version = (String) InvokeMethod(_getVersion, null);
        if (_lib_version == null || !CompareVersion(_lib_version, getVersion())) {
            HandleException("Library apk is not up to date");
            return;
        }
        mInstance = this.CreateInstance(new MixContext(libCtx, context), attrs);
        // TODO(wang16): Add version check
        mLoadAppFromUrl = LookupMethod("loadAppFromUrl", String.class);
        mLoadAppFromManifest = LookupMethod("loadAppFromManifest", String.class);
        mOnCreate = LookupMethod("onCreate");
        mOnResume = LookupMethod("onResume");
        mOnPause = LookupMethod("onPause");
        mOnDestroy = LookupMethod("onDestroy");
        mEnableRemoteDebugging = LookupMethod("enableRemoteDebugging", String.class, String.class);
        mDisableRemoteDebugging = LookupMethod("disableRemoteDebugging");
    }

    /**
     * Compare the given versions.
     * @param lib_version
     * version of library apk
     * @param client_version
     * version of client
     * @return
     * true if library is not older than client, false otherwise or either of the version string
     * is invalid. Valid string should be \d[\d\.]*
     */
    private static boolean CompareVersion(String lib_version, String client_version) {
        if (lib_version.equals(client_version)) {
            return true;
        }
        Pattern version = Pattern.compile("\\d+(\\.\\d+)*");
        Matcher lib = version.matcher(lib_version);
        Matcher client = version.matcher(client_version);
        if (lib.matches() && client.matches()) {
            StringTokenizer lib_tokens = new StringTokenizer(lib_version, ".");
            StringTokenizer client_tokens = new StringTokenizer(client_version, ".");
            int lib_token_count = lib_tokens.countTokens();
            int client_token_count = client_tokens.countTokens();
            if (lib_token_count == client_token_count) {
                while (lib_tokens.hasMoreTokens()) {
                    int lib_value = 0;
                    int client_value = 0;
                    try {
                        lib_value = Integer.parseInt(lib_tokens.nextToken());
                        client_value = Integer.parseInt(client_tokens.nextToken());
                    } catch (NumberFormatException e) {
                        return false;
                    }
                    if (lib_value == client_value)
                        continue;
                    return lib_value > client_value;
                }
                return true;
            } else {
                return lib_token_count > client_token_count;
            }
        }
        return false;
    }

    public FrameLayout get() {
        return (FrameLayout) mInstance;
    }

    /**
     * Get the version information of current runtime library.
     *
     * @return the string containing the version information.
     */
    public static String getVersion() {
        return "0.1";
    }

    /**
     * Load a web application through the entry url. It may be
     * a file from assets or a url from network.
     *
     * @param url the url of loaded html resource.
     */
    public void loadAppFromUrl(String url) {
        InvokeMethod(mLoadAppFromUrl, mInstance, url);
    }

    /**
     * Load a web application through the url of the manifest file.
     * The manifest file typically is placed in android assets. Now it is
     * compliant to W3C SysApps spec.
     *
     * @param manifestUrl the url of the manifest file
     */
    public void loadAppFromManifest(String manifestUrl) {
        InvokeMethod(mLoadAppFromManifest, mInstance, manifestUrl);
    }

    /**
     * Tell runtime that the application is on creating. This can make runtime
     * be aware of application life cycle.
     */
    public void onCreate() {
        InvokeMethod(mOnCreate, mInstance);
    }

    /**
     * Tell runtime that the application is on resuming. This can make runtime
     * be aware of application life cycle.
     */
    public void onResume() {
        InvokeMethod(mOnResume, mInstance);
    }

    /**
     * Tell runtime that the application is on pausing. This can make runtime
     * be aware of application life cycle.
     */
    public void onPause() {
        InvokeMethod(mOnPause, mInstance);
    }

    /**
     * Tell runtime that the application is on destroying. This can make runtime
     * be aware of application life cycle.
     */
    public void onDestroy() {
        InvokeMethod(mOnDestroy, mInstance);
    }

    /**
     * Enable remote debugging for the loaded web application. The caller
     * can set the url of debugging url. Besides, the socket name for remote
     * debugging has to be unique so typically the string can be appended
     * with the package name of the application.
     *
     * @param frontEndUrl the url of debugging url. If it's empty, then a
     *                    default url will be used.
     * @param socketName the unique socket name for setting up socket for
     *                   remote debugging.
     */
    public void enableRemoteDebugging(String frontEndUrl, String socketName) {
        InvokeMethod(mEnableRemoteDebugging, mInstance, frontEndUrl, socketName);
    }

    /**
     * Disable remote debugging so runtime can close related stuff for
     * this feature.
     */
    public void disableRemoteDebugging() {
        InvokeMethod(mDisableRemoteDebugging, mInstance);
    }
}
