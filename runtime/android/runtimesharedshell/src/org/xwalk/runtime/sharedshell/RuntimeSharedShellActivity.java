// Copyright (c) 2013 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.runtime.sharedshell;

import org.xwalk.runtime.wrapper.RuntimeView;

import android.app.Activity;
import android.content.Context;
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

public class RuntimeSharedShellActivity extends Activity {

    private EditText mUrlTextView;
    private FrameLayout mContentContainer;
    private RuntimeView mRuntimeView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.testshell_activity);

        mRuntimeView = new RuntimeView(this);
        mContentContainer = (FrameLayout) findViewById(R.id.content_container);
        if (mRuntimeView.get() != null) {
            mContentContainer.addView(mRuntimeView.get(),
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT));
        }

        initializeUrlField();
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
}
