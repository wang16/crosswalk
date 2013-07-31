// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.runtime.sharedshell;

import org.xwalk.runtime.wrapper.CrossPackageWrapper;
import org.xwalk.runtime.wrapper.CrossPackageWrapperExceptionHandler;
import org.xwalk.runtime.wrapper.RuntimeView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class RuntimeSharedShellActivity extends Activity implements CrossPackageWrapperExceptionHandler {

    private static final String g_library_apk_url = "http://powerbuilder.sh.intel.com/wang16/projects/android/android_bin/XWalkRuntimeLib.apk";

    private EditText mUrlTextView;
    private FrameLayout mContentContainer;
    private RuntimeView mRuntimeView;

    private boolean ShownNotFoundDialog = false;
    private boolean GotLibrary = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.testshell_activity);
        mContentContainer = (FrameLayout) findViewById(R.id.content_container);
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

    private void LoadLibrary() {
        if (!GotLibrary) {
            mRuntimeView = new RuntimeView(this, this);
            mContentContainer.removeAllViews();
            if (mRuntimeView.get() != null) {
                mContentContainer.addView(mRuntimeView.get(),
                        new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT));
                initializeUrlField();
                GotLibrary = true;
                ShownNotFoundDialog = false;
                mRuntimeView.loadAppFromUrl("http://www.baidu.com");
            } else {
                TextView msgText = new TextView(this);
                msgText.setText(R.string.download_dialog_msg);
                msgText.setTextSize(36);
                msgText.setTextColor(Color.BLACK);
                mContentContainer.addView(msgText);
            }
        }
    }

    private static String sanitizeUrl(String url) {
        if (url == null) return url;
        if (url.startsWith("www.") || url.indexOf(":") == -1) url = "http://" + url;
        return url;
    }

    private void initializeUrlField() {
        mUrlTextView = (EditText) findViewById(R.id.url);
        mUrlTextView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId != EditorInfo.IME_ACTION_GO) && (event == null ||
                        event.getKeyCode() != KeyEvent.KEYCODE_ENTER ||
                        event.getKeyCode() != KeyEvent.ACTION_DOWN)) {
                    return false;
                }

                mRuntimeView.loadAppFromUrl(sanitizeUrl(mUrlTextView.getText().toString()));
                mUrlTextView.clearFocus();
                setKeyboardVisibilityForUrl(false);
                return true;
            }
        });
        mUrlTextView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                setKeyboardVisibilityForUrl(hasFocus);
                if (!hasFocus) {
                    // TODO(yongsheng): Fix this.
                    // mUrlTextView.setText(mRuntimeView.getUrl());
                }
            }
        });
    }

    private void setKeyboardVisibilityForUrl(boolean visible) {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (visible) {
            imm.showSoftInput(mUrlTextView, InputMethodManager.SHOW_IMPLICIT);
        } else {
            imm.hideSoftInputFromWindow(mUrlTextView.getWindowToken(), 0);
        }
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
            builder.setNeutralButton(R.string.download_from_url,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent goDownload = new Intent(Intent.ACTION_VIEW);
                            goDownload.setData(Uri.parse(g_library_apk_url));
                            startActivity(goDownload);
                        }
                    });
            builder.setPositiveButton(R.string.download_from_store,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent goToMarket = new Intent(Intent.ACTION_VIEW);
                            goToMarket.setData(Uri.parse("market://details?id="+CrossPackageWrapper.g_library_apk_name));
                            startActivity(goToMarket);
                        }
                    });
            builder.setTitle(R.string.download_dialog_title).setMessage(R.string.download_dialog_msg);
    
            // Create the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.show();
            ShownNotFoundDialog = true;
        }
    }
}
