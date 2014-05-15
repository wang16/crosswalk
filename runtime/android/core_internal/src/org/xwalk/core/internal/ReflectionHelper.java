// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.core.internal;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;

public class ReflectionHelper {
    static class ConstructorHelper {
        private String fullClassName;
        private Object[] paramTypes;

        Constructor<?> loadConstructor() {
            Class<?> clazz = loadClass(fullClassName);
            Class<?>[] params = new Class<?>[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                Object type = paramTypes[i];
                if (type instanceof Class<?>) {
                    params[i] = (Class<?>) type;
                } else if (type instanceof String) {
                    params[i] = loadClass((String) type);
                }
            }
            try {
                return clazz.getConstructor(params);
            } catch (NoSuchMethodException e) {
                ReflectionHelper.handleException(e);
                return null;
            }
        }

        ConstructorHelper(String className, Object... paramTypes) {
            this.fullClassName = className;
            this.paramTypes = paramTypes;
        }
    }

    private static Map<Class<?>, Method> sBridgeWrapperMap = new HashMap<Class<?>, Method>();
    private static Map<String, Constructor<?>> sConstructorMap = new HashMap<String, Constructor<?>>();
    private static Map<String, ConstructorHelper> sConstructorHelperMap =
            new HashMap<String, ConstructorHelper>();
    private static ClassLoader sBridgeOrWrapperLoader = null;
    private static boolean sIsWrapper;
    private final static String INTERNAL_PACKAGE = "org.xwalk.core.internal";
    private static boolean sClassLoaderUpdated = true;

    public static void init(boolean crossPackage) {
        assert isWrapper();
        if (!crossPackage) {
            initClassLoader(ReflectionHelper.class.getClassLoader());
        }
    }

    public static void initClassLoader(ClassLoader loader) {
        sBridgeOrWrapperLoader = loader;
        sBridgeWrapperMap.clear();
        sConstructorMap.clear();
        try {
            for (String name : sConstructorHelperMap.keySet()) {
                ConstructorHelper helper = sConstructorHelperMap.get(name);
                if (helper != null) sConstructorMap.put(name, helper.loadConstructor());
            }
            if (sIsWrapper) {
                Class<?> helperInBridge =
                        sBridgeOrWrapperLoader.loadClass(INTERNAL_PACKAGE + "." + "ReflectionHelper");
                Method initInBridge = helperInBridge.getMethod("initClassLoader", ClassLoader.class);
                initInBridge.invoke(null, ReflectionHelper.class.getClassLoader());
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    public static void registerConstructor(String name, String clazz, Object... params) {
        sConstructorHelperMap.put(name, new ConstructorHelper(clazz, params));
    }

    public static Class<?> loadClass(String clazz) {
        if (sBridgeOrWrapperLoader == null) init(false);
        try {
            return sBridgeOrWrapperLoader.loadClass(clazz);
        } catch (ClassNotFoundException e) {
            handleException(e);
            return null;
        }
    }

    public static Method loadMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            handleException(e);
            return null;
        }
    }

    public static void handleException(Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
    }

    public static void handleException(String e) {
        handleException(new RuntimeException(e));
    }

    public static Object createInstance(String name, Object... parameters) {
        Object ret = null;
        Constructor<?> creator = sConstructorMap.get(name);
        if (creator == null) {
            ConstructorHelper helper = sConstructorHelperMap.get(name);
            if (helper != null) {
                creator = helper.loadConstructor();
                sConstructorMap.put(name, creator);
            }
        }
        if (creator != null) {
            try {
                ret = creator.newInstance(parameters);
            } catch (IllegalArgumentException e) {
                handleException(e);
            } catch (InstantiationException e) {
                handleException(e);
            } catch (IllegalAccessException e) {
                handleException(e);
            } catch (InvocationTargetException e) {
                handleException(e);
            }
        }
        return ret;
    }

    public static Object invokeMethod(Method m, Object instance, Object... parameters) {
        Object ret = null;
        if (m != null) {
            try {
                ret = m.invoke(instance, parameters);
            } catch (IllegalArgumentException e) {
                handleException(e);
            } catch (IllegalAccessException e) {
                handleException(e);
            } catch (InvocationTargetException e) {
                handleException(e);
            } catch (NullPointerException e) {
                handleException(e);
            }
        }
        return ret;
    }

    public static Object getBridgeOrWrapper(Object instance) {
        Class<?> clazz = instance.getClass();
        Method method = sBridgeWrapperMap.get(clazz);
        if (method == null) {
            String methodName = "getBridge";
            if (sIsWrapper) {
                methodName = "getWrapper";
            }
            try {
                method = clazz.getMethod(methodName);
            } catch (NoSuchMethodException e) {
                handleException(e);
            }
            if (method != null) sBridgeWrapperMap.put(clazz, method);
        }
        return invokeMethod(method, instance);
    }

    private static boolean isWrapper() {
        return !ReflectionHelper.class.getPackage().getName().equals(INTERNAL_PACKAGE);
    }

    static {
        sIsWrapper = isWrapper();
    }
}
