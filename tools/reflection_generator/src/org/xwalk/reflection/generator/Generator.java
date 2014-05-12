package org.xwalk.reflection.generator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import org.xwalk.core.internal.XWalkAPI;
import org.xwalk.core.internal.XWalkJavascriptResultHandlerInternal;
import org.xwalk.core.internal.XWalkNavigationHistoryInternal;
import org.xwalk.core.internal.XWalkPreferencesInternal;
import org.xwalk.core.internal.XWalkUIClientInternal;
import org.xwalk.core.internal.XWalkJavascriptResultInternal;
import org.xwalk.core.internal.XWalkNavigationItemInternal;
import org.xwalk.core.internal.XWalkResourceClientInternal;
import org.xwalk.core.internal.XWalkViewInternal;

public class Generator {
    private final static String INTERNAL_CLASS_SUFFIX = "Internal";
    private final static String BRIDGE_CLASS_SUFFIX = "Bridge";
    private final static String[] PARAM_NAMES = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k"};
    private final static Class<?>[] CLASSES_TO_REFLECTIONIZE = {
        XWalkJavascriptResultHandlerInternal.class,
        XWalkNavigationHistoryInternal.class,
        XWalkPreferencesInternal.class,
        XWalkUIClientInternal.class,
        XWalkJavascriptResultInternal.class,
        XWalkNavigationItemInternal.class,
        XWalkResourceClientInternal.class,
        XWalkViewInternal.class
    };

    enum TargetType {
        REFLECTION_LAYER_INTERNAL,
        REFLECTION_LAYER_BRIDGE,
        REFLECTION_LAYER_WRAPPER
    };
    private static Set<Class<?>> usedClassSet = new HashSet<Class<?>>();

    /**
     * @param args
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] argv) {
        if (argv.length < 2) {
            throw new RuntimeException("Invalid parameters");
        }
        String wrapperPackage = "org.xwalk.core";;
        String internalPackagePath = argv[0];
        String intermediatePath = argv[1];
        File output = new File(intermediatePath);
        File input = new File(internalPackagePath);
        if (!output.isDirectory()) {
            throw new RuntimeException("Intermediate folder not exist");
        }
        for (int i = 0; i < CLASSES_TO_REFLECTIONIZE.length; i++) {
            Class<?> clazz = CLASSES_TO_REFLECTIONIZE[i];
            String internalPackage = clazz.getPackage().getName();
            String className = clazz.getSimpleName();
            XWalkAPI annotation = clazz.getAnnotation(XWalkAPI.class);
            if (annotation == null) {
                throw new RuntimeException("Class to be reflectionized must has XWalkAPI annotation :" + className);
            }
            File internalPackageDir = new File(input, internalPackage.replace('.', '/'));
            if (!internalPackageDir.isDirectory()) {
                throw new RuntimeException("internal package folder not exist");
            }
            File wrapperPackageDir = new File(output, "wrapper" + "/src/" + wrapperPackage.replace('.', '/'));
            File bridgePackageDir = new File(output, "bridge" + "/src/" + internalPackage.replace('.', '/'));
            if (!wrapperPackageDir.isDirectory()) wrapperPackageDir.mkdirs();
            if (!bridgePackageDir.isDirectory()) bridgePackageDir.mkdirs();
            if (!className.endsWith(INTERNAL_CLASS_SUFFIX)) {
                throw new RuntimeException("Class to be reflectionized must be named end with Internal :" + className);
            }
            String baseClassName = className.substring(0, className.length() - INTERNAL_CLASS_SUFFIX.length());
            File wrapperFile = new File(wrapperPackageDir, baseClassName + ".java");
            File internalFile = new File(internalPackageDir, className + ".java");
            BufferedWriter wrapperWriter = null;
            BufferedWriter bridgeWriter = null;
            BufferedReader internalReader = null;
            try {
                usedClassSet.clear();
                InputStreamReader isr = new InputStreamReader(new FileInputStream(internalFile));
                internalReader = new BufferedReader(isr);
                List<String> imports = getImportsInInternal(internalReader);
                wrapperWriter = new BufferedWriter(new FileWriter(wrapperFile));
                if (clazz.isInterface()) {
                    parseInterface(clazz, annotation, wrapperPackage, imports, wrapperWriter);
                } else {
                    File bridgeFile = new File(bridgePackageDir, baseClassName + "Bridge.java");
                    bridgeWriter = new BufferedWriter(new FileWriter(bridgeFile));
                    parseClass(clazz, annotation, wrapperPackage, imports, wrapperWriter, bridgeWriter);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (internalReader != null) internalReader.close();
                    if (wrapperWriter != null) wrapperWriter.close();
                    if (bridgeWriter != null) bridgeWriter.close();
                } catch (IOException e) {}
            }
        }
        System.exit(0);
    }

    private static List<String> getImportsInInternal(BufferedReader internalReader) throws IOException {
        String line = internalReader.readLine();
        List<String> ret = new ArrayList<String>();
        while(line != null) {
            if (line.startsWith("import ")) {
                ret.add(line.trim());
            }
            line = internalReader.readLine();
        }
        return ret;
    }

    public static void parseClass(Class<?> clazz, XWalkAPI annotation, String wrapperPackage,
            List<String> imports, Writer wrapperWriter, Writer bridgeWriter) throws IOException {
        String className = clazz.getSimpleName();
        if (!className.endsWith(INTERNAL_CLASS_SUFFIX)) {
            throw new RuntimeException("Class to be reflectionized must end with Internal");
        }
        className = className.substring(0, className.length() - INTERNAL_CLASS_SUFFIX.length());
        StringBuilder bridge = new StringBuilder();
        StringBuilder wrapper = new StringBuilder();
        generateHeader(className, annotation, false, bridge, TargetType.REFLECTION_LAYER_BRIDGE);
        generateHeader(className, annotation, false, wrapper, TargetType.REFLECTION_LAYER_WRAPPER);
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        boolean hasConstructor = false;
        // If the class declares noInstance or createInternally, it doesn't need constructor besides
        // the ones with bridge or wrapper.
        if (!annotation.noInstance() && !annotation.createInternally()) {
            for (Constructor<?> constructor : constructors) {
                if (constructor.getAnnotation(XWalkAPI.class) != null) {
                    appendConstrucor(className, constructor, bridge, TargetType.REFLECTION_LAYER_BRIDGE);
                    appendConstrucor(className, constructor, wrapper, TargetType.REFLECTION_LAYER_WRAPPER);
                    hasConstructor = true;
                }
            }
            if (!hasConstructor) {
                appendConstrucor(className, null, bridge, TargetType.REFLECTION_LAYER_BRIDGE);
                appendConstrucor(className, null, wrapper, TargetType.REFLECTION_LAYER_WRAPPER);
            }
        }
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getAnnotation(XWalkAPI.class) != null) {
                appendConst(field, wrapper);
            }
        }
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getAnnotation(XWalkAPI.class) != null) {
                appendMethod(method, annotation, false, bridge, TargetType.REFLECTION_LAYER_BRIDGE);
                appendMethod(method, annotation, false, wrapper, TargetType.REFLECTION_LAYER_WRAPPER);
            }
        }
        generateFooter(className, bridge, TargetType.REFLECTION_LAYER_BRIDGE);
        generateFooter(className, wrapper, TargetType.REFLECTION_LAYER_WRAPPER);
        InsertPackageImport(clazz.getPackage().getName(), wrapperPackage, imports,
                bridge, TargetType.REFLECTION_LAYER_BRIDGE);
        InsertPackageImport(wrapperPackage, clazz.getPackage().getName(), imports,
                wrapper, TargetType.REFLECTION_LAYER_WRAPPER);
        wrapperWriter.write(wrapper.toString());
        bridgeWriter.write(bridge.toString());
    }

    public static void parseInterface(Class<?> clazz, XWalkAPI annotation, String wrapperPackage,
            List<String> imports, Writer wrapperWriter) throws IOException {
        String className = clazz.getSimpleName();
        if (!className.endsWith(INTERNAL_CLASS_SUFFIX)) {
            throw new RuntimeException("Class to be reflectionized must end with Internal");
        }
        className = className.substring(0, className.length() - INTERNAL_CLASS_SUFFIX.length());
        StringBuilder wrapper = new StringBuilder();
        generateHeader(className, annotation, true, wrapper, TargetType.REFLECTION_LAYER_WRAPPER);
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getAnnotation(XWalkAPI.class) != null) {
                appendMethod(method, annotation, true, wrapper, TargetType.REFLECTION_LAYER_WRAPPER);
            }
        }
        generateFooter(className, wrapper, TargetType.REFLECTION_LAYER_WRAPPER);
        InsertPackageImport(wrapperPackage, clazz.getPackage().getName(), imports,
                wrapper, TargetType.REFLECTION_LAYER_WRAPPER);
        wrapperWriter.write(wrapper.toString());
    }

    private static void InsertPackageImport(String packageName, String reflectionPackage,
            List<String> imports, StringBuilder builder, TargetType type) {
        for (String importString : imports) {
            String[] segs = importString.split(" ");
            if (segs.length == 2) {
                try {
                    Class<?> clazz = Class.forName(segs[1].replace(";", ""));
                    if (usedClassSet.contains(clazz)) {
                        builder.insert(0, importString + "\n");
                    }
                } catch (ClassNotFoundException e) {
                }
            }
        }
        String suffix = "";
        if (type == TargetType.REFLECTION_LAYER_WRAPPER) {
            suffix = BRIDGE_CLASS_SUFFIX;
        }
        for (int i = 0 ; i < CLASSES_TO_REFLECTIONIZE.length; i++) {
            Class<?> clazz = CLASSES_TO_REFLECTIONIZE[i];
            if (!clazz.isInterface()) {
                String className =
                        clazz.getSimpleName().replace(INTERNAL_CLASS_SUFFIX, suffix);
                builder.insert(0, "import " + reflectionPackage + "." + className + ";\n");
            }
        }
        builder.insert(0, "package " + packageName + ";\n");
    }

    private static void appendConstrucor(String className, Constructor<?> constructor,
            StringBuilder builder, TargetType type) {
        String bridge = className + BRIDGE_CLASS_SUFFIX;
        String declare = "";
        String use = "";
        String comma = "";
        String preWrapper = "";
        String postWrapper = "";
        XWalkAPI methodAnnotation = constructor.getAnnotation(XWalkAPI.class);
        for (String line : methodAnnotation.preWrapperLines()) {
            preWrapper += "        " + line + "\n";
        }
        for (String line : methodAnnotation.postWrapperLines()) {
            postWrapper += "        " + line + "\n";
        }
        for (int i = 1; i <= PARAM_NAMES.length; i++) {
            preWrapper = preWrapper.replace("%" + i, PARAM_NAMES[i-1]);
            postWrapper = postWrapper.replace("%" + i, PARAM_NAMES[i-1]);
        }
        if (constructor != null) {
            String[] paramsStrings =
                    getMethodParamsStrings(constructor.getGenericParameterTypes(), type, false, PARAM_NAMES);
            declare = paramsStrings[0];
            use = paramsStrings[1];
            if (!use.isEmpty()) comma = ", ";
        }
        if (type == TargetType.REFLECTION_LAYER_BRIDGE) {
            /*
                public clazzBridge(int a, clazz wrapper) {
                    super(a);
                    this.wrapper = wrapper;
                }
            */
            builder.append(String.format(
                    "    public %2$s(%3$s%5$s%1$s wrapper) {\n" +
                    "        super(%4$s);\n" +
                    "        this.wrapper = wrapper;\n" +
                    "    }\n",
                    className, bridge, declare, use, comma));
        } else if (type == TargetType.REFLECTION_LAYER_WRAPPER) {
            /*
                XWalkClient(int a) {
                    bridge = new XWalkClientBridge(a, this);
                }
            */
            builder.append(String.format(
                    "    public %1$s(%3$s) {\n" +
                    "%6$s" +
                    "        bridge = new %2$s(%4$s%5$sthis);\n" +
                    "%7$s" +
                    "    }\n",
                    className, bridge, declare, use, comma,
                    preWrapper, postWrapper));
        }
    }

    private static void generateHeader(
            String className, XWalkAPI annotation, boolean isInterface,
            StringBuilder builder, TargetType type) {
        String bridge = className + BRIDGE_CLASS_SUFFIX;
        String internal = className + INTERNAL_CLASS_SUFFIX;
        boolean isStatic = annotation.noInstance();
        if (type == TargetType.REFLECTION_LAYER_BRIDGE) {
            if (isStatic) {
                /*
                public class clazzBridge extends clazzInternal {
                */
                builder.append(String.format(
                        "public class %1$s extends %2$s {\n",
                        bridge, internal));
            } else {
                /*
                public class clazzBridge extends clazzInternal {
                    private clazz wrapper;
                    clazz getWrapper() {
                        return wrapper;
                    }
                */
                builder.append(String.format(
                        "public class %2$s extends %3$s {\n" +
                        "    private %1$s wrapper;\n" +
                        "    public %1$s getWrapper() {\n" +
                        "        return wrapper;\n" +
                        "    }\n",
                        className, bridge, internal));
                if (annotation.createInternally()) {
                    /*
                    private clazzInternal internal = null;
                    clazzBridge(clazzInternal internal) {
                        this.internal = internal;
                        this.wrapper = new clazz(this);
                    }
                    */
                    builder.append(String.format(
                            "    private %3$s internal = null;\n" +
                            "    %2$s(%3$s internal) {\n" +
                            "        this.internal = internal;\n" +
                            "        this.wrapper = new %1$s(this);\n" +
                            "    }\n",
                            className, bridge, internal));
                }
            }
        } else if (type == TargetType.REFLECTION_LAYER_WRAPPER) {
            if (isInterface) {
                /*
                public interface XWalkInterface {
                */
                builder.append(String.format("public interface %1$s {\n", className));
            } else {
                /*
                public class XWalkClient {
                    private XWalkClientBridge bridge;
                    public XWalkClient(XWalkClientBridge bridge) {
                        this.bridge = bridge;
                    }
                    public XWalkClientBridge getBridge() {
                        return bridge;
                    }
                */
                String extAndImpl = "";
                Class<?> ext = annotation.extendClass();
                if (ext != Object.class) {
                    usedClassSet.add(ext);
                    extAndImpl = "extends " + getTypeString(null, ext, true) + " ";
                }
                Class<?> impl = annotation.impl();
                if (impl != Object.class) {
                    extAndImpl += "implements " +
                            getTypeString(null, impl, true).replace(INTERNAL_CLASS_SUFFIX, "") + " ";
                }
                builder.append(String.format(
                        "public class %1$s %3$s{\n" +
                        "    private %2$s bridge;\n" +
                        "    public %2$s getBridge() {\n" +
                        "        return bridge;\n" +
                        "    }\n",
                        className, bridge, extAndImpl));
                if (!annotation.createExternally()) {
                    builder.append(String.format(
                            "    public %1$s(%2$s bridge) {\n" +
                            "        this.bridge = bridge;\n" +
                            "    }\n",
                            className, bridge, extAndImpl));
                }
            }
        }
    }

    private static void appendMethod(
            Method method, XWalkAPI annotation, boolean isInterface, StringBuilder builder, TargetType target) {
        String name = method.getName();
        Type retType = method.getGenericReturnType();
        Class<?> retClazz = method.getReturnType();
        String modifierString = "public";
        String retTypeString = getTypeString(retType, retClazz, true);
        retTypeString = transferType(retTypeString, retClazz, target);
        String returnTerm = retTypeString.equals("void") ? "" : "return ";
        String preWrapper = "";
        String postWrapper = "";
        XWalkAPI methodAnnotation = method.getAnnotation(XWalkAPI.class);
        for (String line : methodAnnotation.preWrapperLines()) {
            preWrapper += "        " + line + "\n";
        }
        for (String line : methodAnnotation.postWrapperLines()) {
            postWrapper += "        " + line + "\n";
        }
        for (int i = 1; i <= PARAM_NAMES.length; i++) {
            preWrapper.replace("%" + i, PARAM_NAMES[i-1]);
            postWrapper.replace("%" + i, PARAM_NAMES[i-1]);
        }
        String[] paramsStrings = getMethodParamsStrings(method.getGenericParameterTypes(), target, false, PARAM_NAMES);
        boolean isStatic = (method.getModifiers() & Modifier.STATIC) != 0;
        String retValue = "";
        if (isStatic) modifierString += " static";
        if (target == TargetType.REFLECTION_LAYER_BRIDGE) {
            /*
                public void work(int a) {
                    wrapper.work(a);
                }

                public void workSuper(int a) {
                    super.work(a);
                }
            */
            /* In case the class has XWalkAPI annotation with createInternally, it will be
                public void work(int a) {
                    wrapper.work(a);
                }
                public void workSuper(int a) {
                    if (internal == null) {
                        super.work(a);
                    } else {
                        internal.work(a);
                    }
                }
            */
            /* In case the method is static, it will be
                public static void work(int a) {
                    clazzInternal.work(a);
                }
            */
            String[] paramsStringsWrapper = getMethodParamsStrings(method.getGenericParameterTypes(), target, true, PARAM_NAMES);
            if (isStatic) {
                String internalClass = method.getDeclaringClass().getSimpleName();
                String v = String.format("%1$s.%2$s(%3$s)", internalClass, name, paramsStringsWrapper[1]);
                retValue = transferValueExpress(retType, v, true, true, target);
            } else {
                String v = String.format("wrapper.%1$s(%2$s)", name, paramsStringsWrapper[1]);
                retValue = transferValueExpress(retType, v, true, true, target);
            }
            // BridgeCall
            builder.append(String.format(
                    "    %5$s %2$s %1$s(%4$s) {\n" +
                    "        %3$s%6$s;\n" +
                    "    }\n",
                    name, retTypeString, returnTerm,
                    paramsStringsWrapper[0], modifierString, retValue
            ));
            if (!isStatic) {
                // SuperCall
                String v = String.format("super.%1$s(%2$s)", name, paramsStrings[1]);
                retValue = transferValueExpress(retType, v, false, true, target);
                if (annotation.createInternally()) {
                    String internalV = String.format("internal.%1$s(%2$s)", name, paramsStrings[1]);
                    String internalRetValue = transferValueExpress(retType, internalV, false, true, target);
                    builder.append(String.format(
                            "    %5$s %2$s %1$sSuper(%4$s) {\n" +
                            "        if (internal == null) {\n" +
                            "            %3$s%6$s;\n" +
                            "        } else {\n" +
                            "            %3$s%7$s;\n" +
                            "        }\n" +
                            "    }\n",
                            name, retTypeString, returnTerm,
                            paramsStrings[0], modifierString,
                            retValue, internalRetValue
                    ));
                } else {
                    builder.append(String.format(
                            "    %5$s %2$s %1$sSuper(%4$s) {\n" +
                            "        %3$s%6$s;\n" +
                            "    }\n",
                            name, retTypeString, returnTerm,
                            paramsStrings[0], modifierString, retValue
                    ));
                }
            }
        } else if (target == TargetType.REFLECTION_LAYER_WRAPPER) {
            /*
                public void work(int a) {
                    bridge.workSuper(a);
                }
            */
            if (isInterface) {
                builder.append(String.format(
                        "    %4$s %2$s %1$s(%3$s);\n",
                        name, retTypeString, paramsStrings[0], modifierString
                ));
            } else {
                if (isStatic) {
                    String internalClass = method.getDeclaringClass().getSimpleName();
                    String bridgeClass = internalClass.replace(INTERNAL_CLASS_SUFFIX, BRIDGE_CLASS_SUFFIX);
                    String v = String.format("%1$s.%2$s(%3$s)", bridgeClass, name, paramsStrings[1]);
                    retValue = transferValueExpress(retType, v, false, true, target);
                } else {
                    String v = String.format("bridge.%1$sSuper(%2$s)", name, paramsStrings[1]);
                    retValue = transferValueExpress(retType, v, false, true, target);
                }
                builder.append(String.format(
                        "    %5$s %2$s %1$s(%4$s) {\n" +
                        "%7$s" +
                        "        %3$s%6$s;\n" +
                        "%8$s" +
                        "    }\n",
                        name, retTypeString, returnTerm, paramsStrings[0], modifierString, retValue,
                        preWrapper, postWrapper
                ));
            }
        }
    }

    private static void appendConst(Field field, StringBuilder builder) {
        String modifier = "public final static";
        // The assumption is that const always are "public final static" and always are simple types.
        try {
            String type = field.getType().getSimpleName();
            String value = field.get(null).toString();
            if (field.getType() == String.class) value = "\"" + value + "\"";
            builder.append(String.format(
                    "    %1$s %2$s %3$s = %4$s;\n",
                    modifier, type, field.getName(), value));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static String[] getMethodParamsStrings(
            Type[] gpTypes, TargetType target, boolean forWrapperCallOfBridge, String[] paramNames) {
        String declare = "";
        String use = "";
        int i = 0;
        for (Type gpType : gpTypes) {
            String v = paramNames[i];
            i++;
            String value = transferValueExpress(gpType, v, forWrapperCallOfBridge, false, target);
            Class<?> clazz = extractClassFromType(gpType);
            String typeString = getTypeString(gpType, null, true);
            typeString = transferType(typeString, clazz, target);
            declare += typeString + " " + v + ", ";
            use += value + ", ";
        }
        if (!declare.isEmpty()) {
            // To get rid of the tailing ", "
            declare = declare.substring(0, declare.length() - 2);
            use = use.substring(0, use.length() - 2);
        }
        return new String[] {declare, use};
    }

    /*
     * Transfer an internal type string into bridge/wrapper type.
     */
    private static String transferType(String type, Class<?> clazz, TargetType target) {
        XWalkAPI annotation = clazz.getAnnotation(XWalkAPI.class);
        if (annotation == null) return type;

        if (target == TargetType.REFLECTION_LAYER_BRIDGE) {
            Class<?> instance = annotation.instance();
            if (instance != Object.class) {
                return transferType(instance.getSimpleName(), instance, target);
            }
            return type.replace(INTERNAL_CLASS_SUFFIX, BRIDGE_CLASS_SUFFIX);
        } else if (target == TargetType.REFLECTION_LAYER_WRAPPER) {
            return type.replace(INTERNAL_CLASS_SUFFIX, "");
        }
        return type;
    }

    /*
     * Transfer an internal type value into bridge/wrapper for function params/return value.
     */
    private static String transferValueExpress(
            Type valueType, String v,
            boolean bridgeWrapperCall, boolean forReturnValue, TargetType target) {
        String type = getTypeString(valueType, null, true);
        String value = v;
        // In case it's void return type.
        if (forReturnValue && type.equals("void")) return value;

        Class<?> clazz = extractClassFromType(valueType);
        XWalkAPI annotation = clazz.getAnnotation(XWalkAPI.class);
        if (annotation == null) return value;

        /* Handle Internal class */

        // If annotation declares instance class, cast value to it.
        Class<?> instance = annotation.instance();
        String instanceType = type;
        if (instance != Object.class) {
            instanceType = getTypeString(null, instance, true);
            // If annotation has instance, cast value to instance type.
            // For wrapper, cast to the wrapper class of instance type.
            if (target == TargetType.REFLECTION_LAYER_WRAPPER) {
                value = "(" + instanceType.replace(INTERNAL_CLASS_SUFFIX, "") + ")" + value;
            } else if (target == TargetType.REFLECTION_LAYER_BRIDGE) {
                value = "(" + instanceType + ")" + value;
            }
        }
        String instanceBridgeType = instanceType.replace(INTERNAL_CLASS_SUFFIX, BRIDGE_CLASS_SUFFIX);
        String instanceWrapperType = instanceType.replace(INTERNAL_CLASS_SUFFIX, "");
        // Get whether this class is only created internally or externally.
        boolean createInternally = annotation.createInternally();
        boolean createExternally = annotation.createExternally();

        if (target == TargetType.REFLECTION_LAYER_WRAPPER) {
            if (forReturnValue) {
                // The return of bridge's function is always bridge.
                // So here transfers bridge value to wrapper, add ".getWrapper()"
                value = String.format("(%s).getWrapper()", value);
            } else {
                // The input of wrapper's function is always wrapper.
                // So here transfers wrapper value to bridge value, add ".getBridge()"
                value = String.format("(%s).getBridge()", value);
            }
        // Bridge class has two function for each internal function: wrapperCall and superCall.
        // They needs to be treaded differently.
        } else if (target == TargetType.REFLECTION_LAYER_BRIDGE && bridgeWrapperCall) {
            if (forReturnValue) {
                // The return of wrapper's function is always wrapper.
                // So here transfers wrapper to bridge.
                value = String.format("(%s).getBridge()", value);
            } else {
                // The input of bridge's function is always internal.
                // So here transfers internal to wrapper.
                String castToBridge = String.format("(%1$s) %2$s", instanceBridgeType, v);
                if (!createExternally) {
                    // If not create externally, the internal value might not able to be cast to bridge type.
                    // If not, need to create bridge via internal value.
                    value = String.format(
                            "%1$s instanceof %2$s ? (%3$s) : new %2$s(%4$s)",
                            v, instanceBridgeType, castToBridge, value);
                } else {
                    value = castToBridge;
                }
                value = String.format("(%s).getWrapper()", value);
            }
        } else if (target == TargetType.REFLECTION_LAYER_BRIDGE && !bridgeWrapperCall) {
            // superCall of bridge function
            if (forReturnValue) {
                // The return value of superCall is always internal.
                // So here needs to transfer internal to bridge.
                String castToBridge = String.format("(%1$s) %2$s", instanceBridgeType, v);
                if (!createExternally) {
                    // If not create externally, the internal value might not able to be cast to bridge type.
                    // If not, need to create bridge via internal value.
                    value = String.format(
                            "%1$s instanceof %2$s ? (%3$s) : new %2$s(%4$s)",
                            v, instanceBridgeType, castToBridge, value);
                } else {
                    value = castToBridge;
                }
            } else {
                // The input of superCall is always internal, so directly return the original value is ok.
                value = v;
            }
        }
        return value;
    }

    /*
     * Get the string of a given type. The main reason to have this is that Class<?> doesn't
     * contain GenericTypes. e.g. HashSet<String> is only HashSet in Class<?>.
     */
    private static String getTypeString(Type type, Class<?> clazz, boolean isRootCall) {
        try {
            if (clazz == null) {
                clazz = extractClassFromType(type);
            }
            if (!clazz.isAnnotationPresent(XWalkAPI.class)) usedClassSet.add(clazz);
            if (type instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) type;
                Type[] argus = pType.getActualTypeArguments();
                String arguList = "";
                for (Type argu : argus) {
                    arguList += getTypeString(argu, null, false) + ", ";
                }
                arguList = arguList.substring(0, arguList.length() - 2);
                return clazz.getSimpleName() + "<" + arguList + ">";
            } else if (type instanceof GenericArrayType) {
                GenericArrayType pType = (GenericArrayType) type;
                Type cType = pType.getGenericComponentType();
                return getTypeString(cType, null, false) + "[]";
            }
        } catch (ClassCastException e) {
            // If this is a recursive call, just throw the exception.
            // when the exception thrown to the top level, return the
            // clazz's simple name as fallback.
            if (!isRootCall) throw e;
        }
        return clazz.getSimpleName();
    }

    private static Class<?> extractClassFromType(Type t) throws ClassCastException {
        if (t instanceof Class<?>) {
            return (Class<?>)t;
        }
        return (Class<?>)((ParameterizedType)t).getRawType();
    }

    private static void generateFooter(String className, StringBuilder builder, TargetType type) {
        builder.append("}\n");
    }
}
