// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.runtime.wrapper;

import java.lang.reflect.Method;

import android.content.Context;
import android.widget.FrameLayout;

/**
 * This class is to wrap RuntimeView class in library APK.
 *
 * A web application APK should use this class in its Activity.
 */

public class RuntimeView extends CrossPackageWrapper {
    private final static String class_name = "org.xwalk.runtime.RuntimeView";
    private Object mInstance;
    private Method mLoadAppFromUrl;
    private Method mLoadAppFromManifest;
    private Method mOnCreate;
    private Method mOnResume;
    private Method mOnPause;
    private Method mOnDestroy;
    private Method mEnableRemoteDebugging;
    private Method mDisableRemoteDebugging;

    public RuntimeView(Context context, CrossPackageWrapperExceptionHandler exception_handler) {
        super(context, class_name, exception_handler, Context.class);
        Context libCtx = getLibCtx();
        mInstance = this.CreateInstance(new MixContext(libCtx, context));
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
