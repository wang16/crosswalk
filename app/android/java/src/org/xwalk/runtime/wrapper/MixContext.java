package org.xwalk.runtime.wrapper;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;

public class MixContext extends ContextWrapper {

    private Context mAppCtx;

    public MixContext(Context base, Context app) {
        super(base);
        mAppCtx = app.getApplicationContext();
    }
    
    @Override
    public Context getApplicationContext() {
        return mAppCtx;
    }
    
    @Override
    public boolean bindService(Intent in, ServiceConnection conn, int flags) {
        return mAppCtx.bindService(in, conn, flags);
    }   

    @Override
    public void unbindService(ServiceConnection conn) {
        mAppCtx.unbindService(conn);
    }
}
