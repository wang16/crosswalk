// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.app.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import org.xwalk.app.runtime.CrossPackageWrapper;
import org.xwalk.app.runtime.CrossPackageWrapperExceptionHandler;
import org.xwalk.app.runtime.RuntimeClient;

public abstract class RuntimeActivityBase extends Activity implements CrossPackageWrapperExceptionHandler {

    private static final String g_default_library_apk_url = null;

    private RuntimeClient mRuntimeView;

    private boolean ShownNotFoundDialog = false;
    private boolean GotLibrary = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LoadLibrary();
        mRuntimeView.onCreate();
    }

    @Override
    public void onStart() {
        super.onStart();
        LoadLibrary();
    }

    @Override
    public void onPause() {
        super.onPause();
        mRuntimeView.onPause();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mRuntimeView.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRuntimeView.onDestroy();
    }
    
    private String getLibraryApkDownloadUrl() {
        int resId = getResources().getIdentifier("xwalk_library_apk_download_url", "string", getPackageName());
        if (resId == 0)
            return g_default_library_apk_url;
        return getString(resId);
    }

    public String getString(String label) {
        int resId = getResources().getIdentifier(label, "string", getPackageName());
        if (resId == 0)
            return label;
        return getString(resId);
    }

    private void LoadLibrary() {
        if (!GotLibrary) {
            mRuntimeView = new RuntimeClient(this, null, this);
            if (mRuntimeView.get() != null) {
                GotLibrary = true;
                ShownNotFoundDialog = false;
            }
            DidTryLoadRuntimeView(mRuntimeView.get());
        }
    }
    
    public RuntimeClient GetRuntimeView() {
        return mRuntimeView;
    }

    @Override
    public void OnException(Exception e) {
        ShowLibraryNotFoundDialog();
    }

    @Override
    public void OnException(String msg) {
        ShowLibraryNotFoundDialog();        
    }

    private void ShowLibraryNotFoundDialog() {
        if (!ShownNotFoundDialog) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            final String downloadUrl = getLibraryApkDownloadUrl();
            if (downloadUrl != null && downloadUrl.length() > 0) {
                builder.setNeutralButton(getString("download_from_url"),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent goDownload = new Intent(Intent.ACTION_VIEW);
                                goDownload.setData(Uri.parse(downloadUrl));
                                startActivity(goDownload);
                            }
                        });
            }
            builder.setPositiveButton(getString("download_from_store"),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent goToMarket = new Intent(Intent.ACTION_VIEW);
                            goToMarket.setData(Uri.parse("market://details?id="+CrossPackageWrapper.g_library_apk_name));
                            startActivity(goToMarket);
                        }
                    });
            builder.setTitle(getString("download_dialog_title")).setMessage(getString("download_dialog_msg"));
    
            // Create the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.show();
            ShownNotFoundDialog = true;
        }
    }

    /*
     * Called each time trying to load runtime view from library apk.
     * Descendant should handle both succeeded and failed to load
     * library apk.
     * 
     * @param, The WebView loaded, it can be null for failed to load webview.
     */
    abstract protected void DidTryLoadRuntimeView(View webView);
}
