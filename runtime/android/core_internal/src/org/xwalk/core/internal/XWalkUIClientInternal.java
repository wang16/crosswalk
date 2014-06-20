// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.core.internal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.widget.EditText;

/**
 * This class notifies the embedder UI events/callbacks.
 */
public class XWalkUIClientInternal {

    // Strings for displaying Dialog.
    private static String mJSAlertTitle;
    private static String mJSConfirmTitle;
    private static String mJSPromptTitle;
    private static String mOKButton;
    private static String mCancelButton;

    private Context mContext;
    private AlertDialog mDialog;
    private EditText mPromptText;
    private int mSystemUiFlag;
    private View mDecorView;
    private XWalkViewInternal mXWalkView;
    private boolean mOriginalFullscreen;

    /**
     * Constructor.
     * @param view the owner XWalkViewInternal instance.
     */
    public XWalkUIClientInternal(XWalkViewInternal view) {
        mContext = view.getContext();
        mDecorView = view.getActivity().getWindow().getDecorView();
        if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            mSystemUiFlag = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        }
        mXWalkView = view;
        initResources();
    }

    private void initResources() {
        if (mJSAlertTitle != null) return;
        mJSAlertTitle = mContext.getString(R.string.js_alert_title);
        mJSConfirmTitle = mContext.getString(R.string.js_confirm_title);
        mJSPromptTitle = mContext.getString(R.string.js_prompt_title);
        mOKButton = mContext.getString(android.R.string.ok);
        mCancelButton = mContext.getString(android.R.string.cancel);
    }

    /**
     * Request display and focus for this XWalkViewInternal.
     * @param view the owner XWalkViewInternal instance.
     */
    public void onRequestFocus(XWalkViewInternal view) {
    }

    /**
     * Notify the client to close the given XWalkViewInternal.
     * @param view the owner XWalkViewInternal instance.
     */
    public void onJavascriptCloseWindow(XWalkViewInternal view) {
        if (view != null && view.getActivity() != null) {
            view.getActivity().finish();
        }
    }

    /**
     * The type of JavaScript modal dialog.
     */
    public enum JavascriptMessageType {
        /** JavaScript alert dialog. */
        JAVASCRIPT_ALERT,
        /** JavaScript confirm dialog. */
        JAVASCRIPT_CONFIRM,
        /** JavaScript prompt dialog. */
        JAVASCRIPT_PROMPT,
        /** JavaScript dialog for a window-before-unload notification. */
        JAVASCRIPT_BEFOREUNLOAD
    }

    /**
     * Tell the client to display a prompt dialog to the user.
     * @param view the owner XWalkViewInternal instance.
     * @param type the type of JavaScript modal dialog.
     * @param url the url of the web page which wants to show this dialog.
     * @param message the message to be shown.
     * @param defaultValue the default value string. Only valid for Prompt dialog.
     * @param result the callback to handle the result from caller.
     */
    public boolean onJavascriptModalDialog(XWalkViewInternal view, JavascriptMessageType type, String url,
            String message, String defaultValue, XWalkJavascriptResultInternal result) {
        switch(type) {
            case JAVASCRIPT_ALERT:
                return onJsAlert(view, url, message, result);
            case JAVASCRIPT_CONFIRM:
                return onJsConfirm(view, url, message, result);
            case JAVASCRIPT_PROMPT:
                return onJsPrompt(view, url, message, defaultValue, result);
            case JAVASCRIPT_BEFOREUNLOAD:
                // Reuse onJsConfirm to show the dialog.
                return onJsConfirm(view, url, message, result);
            default:
                break;
        }
        assert(false);
        return false;
    }

    /**
     * Tell the client to toggle fullscreen mode.
     * @param view the owner XWalkViewInternal instance.
     * @param enterFullscreen true if it has entered fullscreen mode.
     */
    public void onFullscreenToggled(XWalkViewInternal view, boolean enterFullscreen) {
        Activity activity = view.getActivity();
        if (enterFullscreen) {
            if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                mSystemUiFlag = mDecorView.getSystemUiVisibility();
                mDecorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            } else {
                if ((activity.getWindow().getAttributes().flags &
                        WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0) {
                    mOriginalFullscreen = true;
                } else {
                    mOriginalFullscreen = false;
                }
                if (!mOriginalFullscreen) {
                    activity.getWindow().setFlags(
                            WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
            }
        } else {
            if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                mDecorView.setSystemUiVisibility(mSystemUiFlag);
            } else {
                // Clear the activity fullscreen flag.
                if (!mOriginalFullscreen) {
                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
            }
        }
    }

    /**
     * Tell the client to open a file chooser.
     * @param view the owner XWalkViewInternal instance.
     * @param uploadFile the callback class to handle the result from caller. It MUST
     *        be invoked in all cases. Leave it not invoked will block all following
     *        requests to open file chooser.
     * @param acceptType value of the 'accept' attribute of the input tag associated
     *        with this file picker.
     * @param capture value of the 'capture' attribute of the input tag associated
     *        with this file picker
     */
    public void openFileChooser(XWalkViewInternal view, ValueCallback<Uri> uploadFile,
            String acceptType, String capture) {
        uploadFile.onReceiveValue(null);
    }

    /**
     * Notify the client that the scale applied to the XWalkViewInternal has changed.
     * @param view the owner XWalkViewInternal instance.
     * @param oldScale the old scale before scaling.
     * @param newScale the current scale factor after scaling.
     */
    public void onScaleChanged(XWalkViewInternal view, float oldScale, float newScale) {
    }

    private boolean onJsAlert(XWalkViewInternal view, String url, String message,
            XWalkJavascriptResultInternal result) {
        final XWalkJavascriptResultInternal fResult = result;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
        dialogBuilder.setTitle(mJSAlertTitle)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(mOKButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fResult.confirm();
                        dialog.dismiss();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        fResult.cancel();
                    }
                });
        mDialog = dialogBuilder.create();
        mDialog.show();
        return false;
    }

    private boolean onJsConfirm(XWalkViewInternal view, String url, String message,
            XWalkJavascriptResultInternal result) {
        final XWalkJavascriptResultInternal fResult = result;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
        dialogBuilder.setTitle(mJSConfirmTitle)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(mOKButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fResult.confirm();
                        dialog.dismiss();
                    }
                })
                // Need to implement 'onClick' and call the dialog.cancel. Otherwise, the
                // UI will be locked.
                .setNegativeButton(mCancelButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // This will call OnCancelLisitener.onCancel().
                        dialog.cancel();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        fResult.cancel();
                    }
                });
        mDialog = dialogBuilder.create();
        mDialog.show();
        return false;
    }

    private boolean onJsPrompt(XWalkViewInternal view, String url, String message,
            String defaultValue, XWalkJavascriptResultInternal result) {
        final XWalkJavascriptResultInternal fResult = result;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
        dialogBuilder.setTitle(mJSPromptTitle)
                .setMessage(message)
                .setPositiveButton(mOKButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fResult.confirmWithResult(mPromptText.getText().toString());
                        dialog.dismiss();
                    }
                })
                // Need to implement 'onClick' and call the dialog.cancel. Otherwise, the
                // UI will be locked.
                .setNegativeButton(mCancelButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // This will call OnCancelLisitener.onCancel().
                        dialog.cancel();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        fResult.cancel();
                    }
                });
        mPromptText = new EditText(mContext);
        mPromptText.setVisibility(View.VISIBLE);
        mPromptText.setText(defaultValue);
        mPromptText.selectAll();

        dialogBuilder.setView(mPromptText);
        mDialog = dialogBuilder.create();
        mDialog.show();
        return false;
    }
}
