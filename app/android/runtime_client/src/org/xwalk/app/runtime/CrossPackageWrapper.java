package org.xwalk.app.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

public abstract class CrossPackageWrapper {
    public final static String g_library_apk_name = "org.xwalk.runtime.lib";
    private Context mLibCtx;
    private Class<?> mTargetClass;
    private Constructor<?> mCreator;
    private CrossPackageWrapperExceptionHandler mExceptionHandler;

    public CrossPackageWrapper(Context ctx, String className, 
            CrossPackageWrapperExceptionHandler handler, Class<?>... parameters) {
        mExceptionHandler = handler;
        try {
            mLibCtx = ctx.createPackageContext(
                    g_library_apk_name,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            mTargetClass =
                    mLibCtx.getClassLoader().loadClass(className);
            mCreator = mTargetClass.getConstructor(parameters);
        } catch (NameNotFoundException e) {
            HandleException(e);
        } catch (ClassNotFoundException e) {
            HandleException(e);
        } catch (NoSuchMethodException e) {
            HandleException(e);
        }
    }
    
    public Object CreateInstance(Object... parameters) {
        Object ret = null;
        if (mCreator != null) {
            try {
                ret = mCreator.newInstance(parameters);
            } catch (IllegalArgumentException e) {
                HandleException(e);
            } catch (InstantiationException e) {
                HandleException(e);
            } catch (IllegalAccessException e) {
                HandleException(e);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                HandleException(e);
            }
        } else {
            HandleException("No matched constructor found");
        }
        return ret;
    }
    
    public void HandleException(Exception e) {
        if (mExceptionHandler != null)
            mExceptionHandler.OnException(e);
    }
    
    public void HandleException(String e) {
        if (mExceptionHandler != null)
            mExceptionHandler.OnException(e);
    }

    public Class<?> getTargetClass() {
        return mTargetClass;
    }

    public Context getLibCtx() {
        return mLibCtx;
    }
    
    public Method LookupMethod(String method, Class<?>... parameters) {
        if (mTargetClass == null)
            return null;
        try {
            return mTargetClass.getMethod(method, parameters);
        } catch (NoSuchMethodException e) {
            HandleException(e);
        }
        HandleException("No match method found");
        return null;
    }
    
    public Object InvokeMethod(Method m, Object instance, Object... parameters) {
        Object ret = null;
        if (m != null) {
            try {
                ret = m.invoke(instance, parameters);
            } catch (IllegalArgumentException e) {
                HandleException(e);
            } catch (IllegalAccessException e) {
                HandleException(e);
            } catch (InvocationTargetException e) {
                HandleException(e);
            } catch (NullPointerException e) {
                HandleException(e);
            }
        }
        return ret;
    }
}
