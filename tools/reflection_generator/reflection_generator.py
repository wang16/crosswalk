#!/usr/bin/env python

# Copyright (c) 2014 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

import optparse
import os
import re
import sys

from collections import OrderedDict
from string import Template

# Classes list that have to generate bridge and wrap code.
CLASSES_TO_BE_PROCESS = [
  'XWalkViewInternal',
  'XWalkUIClientInternal',
  'XWalkResourceClientInternal',
  'XWalkPreferencesInternal',
  'XWalkNavigationItemInternal',
  'XWalkNavigationHistoryInternal',
  'XWalkJavascriptResultHandlerInternal',
  'XWalkJavascriptResultInternal',
]


def ManagleInternalNameToBridgeName(internal_name):
  if internal_name not in CLASSES_TO_BE_PROCESS:
    return internal_name
  else:
    return internal_name.replace('Internal', 'Bridge')


def MangleInternalNameToWrapperName(internal_name):
  if internal_name not in CLASSES_TO_BE_PROCESS:
    return internal_name
  else:
    return internal_name.replace('Internal', '')

def ManagleInternalNameToFullBridgeName(internal_name):
  if internal_name not in CLASSES_TO_BE_PROCESS:
    return internal_name
  else:
    package_string = 'org.xwalk.core.internal.%s'
    return package_string % (internal_name.replace('Internal', 'Bridge'))


def ManagleInternalNameToFullWrapperName(internal_name):
  if internal_name not in CLASSES_TO_BE_PROCESS:
    return internal_name
  else:
    return "org.xwalk.core.%s" % (internal_name.replace('Internal', ''))


class Method:
  """Internal representaion of a method."""
  ANNOTATION_PRE_WRAPLINE = 'preWrapperLines'
  ANNOTATION_POST_WRAPLINE = 'postWrapLines'

  def __init__(self, is_constructor, is_static, method_name, method_return,
      params, annotation):
    self._is_constructor = is_constructor
    self._is_static = is_static
    self._method_name = method_name
    self._method_return = method_return
    self._params = OrderedDict() # Use OrderedDict to avoid parameter misorder.
    self._method_annotations = {}
    self.ParseMethodParams(params)
    self.ParseMethodAnnotation(annotation)

  @property
  def is_constructor(self):
    return self._is_constructor

  @property
  def is_static(self):
    return self._is_static

  @property
  def method_name(self):
    return self._method_name

  @property
  def method_return(self):
    return self._method_return

  @property
  def params(self):
    return self._params

  @property
  def method_annotations(self):
    return self._method_annotations

  def ParseMethodParams(self, params):
    # TODO(shouqun): Currently, generic parameters are not supported.
    # The support of generic types should be added if such cases happen.
    if not params or params == '':
      return
    for param in params.split(','):
      param = param.strip()
      param_list = param.split()
      param_type = param_list[0]
      param_name = param_list[1]
      self._params[param_name] = param_type

  def ParseMethodAnnotation(self, annotation):
    pre_wrapline_re = re.compile('preWrapperLines\s*=\s*\{'
        '(?P<pre_wrapline>[a-zA-Z0-9%,\s\(\);._"=]*)\}')
    for match in re.finditer(pre_wrapline_re, annotation):
      pre_wrapline = match.group('pre_wrapline')
      self._method_annotations[self.ANNOTATION_PRE_WRAPLINE] = pre_wrapline

    post_wrapline_re = re.compile('postWrapperLines\s*=\s*\{('
        '?P<post_wrapline>[a-zA-Z0-9%,\s\(\);._"=]*)\}')
    for match in re.finditer(post_wrapline_re, annotation):
      post_wrapline = match.group('post_wrapline')
      self._method_annotations[self.ANNOTATION_POST_WRAPLINE] = post_wrapline


class Field:
  """Python class represents static field of a java class"""
  def __init__(self, field_type, name, value):
    self._field_type = field_type
    self._field_name = name
    self._field_value = value

  @property
  def field_type(self):
    return self._field_type

  @property
  def field_name(self):
    return self._field_name

  @property
  def field_value(self):
    return self._field_value


class CodeGenerator(object):
  """Basic class of code generator"""
  def __init__(self, java_data):
    self._java_data = java_data
    self._generated_code = ''
    self._generated_class_name = ''

  def RunTask(self):
    pass

  def GetGeneratedCode(self):
    return self._generated_code

  def GetGeneratedClassFileName(self):
    return self._generated_class_name + '.java'

  def GenerateImportRules(self):
    imports = ''
    for imported in self._java_data.imports:
      import_string = 'import ' + imported + ";\n"
      imports += import_string
    # Add the reflection helper import.
    imports += '\n'
    imports += 'import java.lang.reflect.Constructor;\n'
    imports += 'import java.lang.reflect.Method;\n'
    return imports

  def FormatStaticInitializerConstructorName(self, method):
    constructor = method.method_name
    for param_name in method.params:
      constructor += method.params[param_name]
    constructor += 'Constructor'
    return constructor


class InterfaceGenerator(CodeGenerator):
  """Generator class that generates interfade code in wrapper layer"""
  def __init__(self, java_data):
    super(InterfaceGenerator, self).__init__(java_data)

  def RunTask(self):
    self._generated_code = self.GenerateInterface()
    self._generated_class_name = MangleInternalNameToWrapperName(
        self._java_data.class_name)

  def GenerateInterface(self):
    interface_template = Template("""
${PACKAGE_SECTION}

${IMPORT_SECTION}
public interface ${INTERFACE_NAME} {
${METHOD_SECTION}
}
""")
    package_section = 'package %s;' % \
        (self._java_data.package_name.replace('.internal', ''))
    import_section = self.GenerateImportRules()
    method_section = self.GenerateMethods()
    interface_name = MangleInternalNameToWrapperName(self._java_data.class_name)
    value = {'PACKAGE_SECTION': package_section,
             'IMPORT_SECTION': import_section,
             'INTERFACE_NAME': interface_name,
             'METHOD_SECTION': method_section}
    interface_code = interface_template.substitute(value)
    return interface_code

  def FormatMethodParams(self, params):
    params_string = ''
    for param_name in params:
      param_type = params[param_name]
      if param_type in CLASSES_TO_BE_PROCESS:
        param_type = MangleInternalNameToWrapperName(param_type)
      params_string += '%s %s,' % (param_type, param_name)
    if params_string.endswith(','):
      params_string = params_string[0:-1]
    return params_string

  def FormatMethodString(self, method):
    method_template = Template("""\
    public ${RETURN} ${NAME}(${PARAMS});
""")
    method_return = method.method_return
    if method.method_return in CLASSES_TO_BE_PROCESS:
      method_return = MangleInternalNameToWrapperName(method_return)
    value = {'RETURN': method_return,
             'NAME': method.method_name,
             'PARAMS': self.FormatMethodParams(method.params)}
    return method_template.substitute(value)

  def GenerateMethods(self):
    methods_string = ''
    for method in self._java_data.methods:
      methods_string += self.FormatMethodString(method)
    return methods_string


class BrigdeGenerator(CodeGenerator):
  """ Generator class that generates bridge layer code."""
  def __init__(self, java_data):
    super(BrigdeGenerator, self).__init__(java_data)

  def RunTask(self):
    self._generated_code = self.GenerateBrigdeClass()
    self._generated_class_name = ManagleInternalNameToBridgeName(
        self._java_data.class_name)

  def GenerateBrigdeClass(self):
    bridge_class_template = Template("""\
${PACKAGE_SECTION}

${IMPORT_SECTION}
public class ${CLASS_NAME} extends ${PARENT_CLASS} {
    private final static String WRAPPER_CLASS = "org.xwalk.core.Object";
    private Object wrapper;

    public Object getWrapper() {
        return wrapper;
    }
${CREATE_INTERNALLY_CONSTRUCTOR}
${STATIC_METHOD_SECTION}
${METHODS_SECTION}

${REFLECTION_INIT_SECTION}
${STATIC_INITIALIZER}
}
""")
    package_name = ''
    if self._java_data.package_name != '':
      package_name = 'package ' + self._java_data.package_name + ";"
    imports_string = self.GenerateImportRules()
    internal_class_name = self._java_data.class_name
    bridge_class_name = ManagleInternalNameToBridgeName(internal_class_name)
    create_internally_constructor = self.GenerateCreateInternallyConstructor()
    static_methods = self.GenerateStaticMethods()
    bridge_methods = self.GenerateBridgeMethods()
    reflection_init = self.GenerateReflectionInitString()
    static_initializer = self.GenerateStaticInitializerString()
    value = {'PACKAGE_SECTION': package_name,
             'IMPORT_SECTION': imports_string,
             'CLASS_NAME': bridge_class_name,
             'PARENT_CLASS': internal_class_name,
             'STATIC_METHOD_SECTION': static_methods,
             'METHODS_SECTION': bridge_methods,
             'REFLECTION_INIT_SECTION': reflection_init,
             'CREATE_INTERNALLY_CONSTRUCTOR': create_internally_constructor,
             'STATIC_INITIALIZER': static_initializer}
    class_content = bridge_class_template.substitute(value)
    return class_content

  def GenerateCreateInternallyConstructor(self):
    if not self._java_data.class_annotations.has_key(
        InternalJavaFileData.ANNOTATION_CREATE_INTERNALLY):
      return ''
    constructor_template = Template("""\
    private ${INTERNAL_CLASS_NAME} internal = null;
    ${BRIDGE_CLASS_NAME}(${INTERNAL_CLASS_NAME} internal) {
        this.internal = internal;
        this.wrapper = ReflectionHelper.createInstance(\
"${STATIC_CONSTRUCTOR_NAME}", this);
        try {
          reflectionInit();
        } catch (Exception e) {
          ReflectionHelper.handleException(e);
        }
    }
""")
    internal_class_name = self._java_data.class_name
    bridge_class_name = ManagleInternalNameToBridgeName(internal_class_name)
    constructor_method = Method(True, False, bridge_class_name, '', '', '')
    static_constructor_name = \
        self.FormatStaticInitializerConstructorName(constructor_method)
    value = {'INTERNAL_CLASS_NAME': internal_class_name,
             'BRIDGE_CLASS_NAME': bridge_class_name,
             'STATIC_CONSTRUCTOR_NAME': static_constructor_name}
    return constructor_template.substitute(value)


  def FormatStaticMethodString(self, class_name, method):
    method_template = Template("""\
    public static ${RETURN} ${NAME}($PARAMS) {
        ${RETURN_STATE}${CLASS_NAME}.${NAME}(${CALL_PARAMS});
    }
""")
    return_string = method.method_return
    name = method.method_name
    params = self.FormatMethodParamsString(method.params)
    return_state = ''
    if return_string != 'void':
      return_state = 'return '
    call_params = self.FormatMethodCallParamsString(method.params)[1]
    value = {'RETURN': return_string,
             'NAME': name,
             'PARAMS': params,
             'RETURN_STATE': return_state,
             'CLASS_NAME': class_name,
             'CALL_PARAMS': call_params}
    return method_template.substitute(value)

  def GenerateStaticMethods(self):
    static_methods = ''
    for method in self._java_data.methods:
      static_methods += self.FormatStaticMethodString(
          self._java_data.class_name, method)
    return static_methods

  def GenerateBridgeMethods(self):
    methods_string = ''
    for method in self._java_data.methods:
      if method.is_constructor:
        methods_string += self.GenerateBridgeConstructorString(method)
      elif not method.is_static:
        methods_string += self.GenerateBridgeMethodString(method)
    method_field_template = Template("""\
    private Method ${METHOD}Method;
""")
    for method in self._java_data.methods:
      if not method.is_constructor and not method.is_static:
        value = {'METHOD': method.method_name}
        methods_string += method_field_template.substitute(value)
    return methods_string

  def FormatMethodParamsString(self, params):
    param_string = ''
    for param_name in params:
      param_type = params[param_name]
      if param_type in CLASSES_TO_BE_PROCESS:
        # Mangle class name if the param type is in internal class list.
        param_type = ManagleInternalNameToBridgeName(param_type)
      param_string += '%s %s' % (param_type, param_name)
      param_string += ','
    # Trim the last comma for the method parameter list.
    if param_string.endswith(','):
      param_string = param_string[0:-1]
    return param_string

  def FormatMethodCallParamsString(self, params):
    param_string = ''
    for param_name in params:
      param_type = params[param_name]
      if param_type in CLASSES_TO_BE_PROCESS:
        # Mangle parameter string if it is in internal class list.
        param_type = ManagleInternalNameToBridgeName(param_type)
        param_name = '((%s) a).getWrapper()' % (param_type)
      param_string += ', '
      param_string += param_name
    super_param_string = param_string
    # Trim the first comma.
    if super_param_string.startswith(','):
      super_param_string = super_param_string[1:]
    return [param_string, super_param_string]

  def FormatConstructorParamsString(self, params):
    param_string = ''
    for param_name in params:
      param_type = params[param_name]
      param_string += '%s %s' % (param_type, param_name)
      param_string += ','
    return param_string

  def FormatConstructorCallParamsString(self, params):
    param_string = ''
    for param_name in params:
      param_string += param_name
      param_string += ','
    if param_string.endswith(','):
      param_string = param_string[0:-1]
    return param_string

  def GenerateBridgeConstructorString(self, method):
    constructor_template = Template("""\
    public ${NAME}(${PARAMS} Object wrapper) {
        super(${CALL_PARAMS});
        this.wrapper = wrapper;
        try {
            reflectionInit();
        } catch (Exception e) {
            ReflectionHelper.handleException(e);
        }
    }
""")
    params = self.FormatConstructorParamsString(method.params)
    call_params = self.FormatConstructorCallParamsString(method.params)
    value = {'NAME': method.method_name,
             'PARAMS': params,
             'CALL_PARAMS': call_params}
    return constructor_template.substitute(value)

  def FormatMethodReturnStatement(self, method):
    if method.method_return == 'void':
      return ''
    else:
      return 'return (%s)' % (method.method_return)

  def FormatSuperMethodReturnStatemt(self, method):
    if method.method_return == 'void':
      return ''
    else:
      return 'return '

  def FormatPreCreateInternallyReturn(self):
    return 'if (internal == null) {'

  def FormatPostCreateInternallyReturn(self, method, call_params):
    return_template = Template("""\
} else {
            internal.${METHOD_NAME}(${SUPER_CALL_PARAMS});
        }
""")
    method_name = method.method_name
    value = {'METHOD_NAME': method_name,
             'SUPER_CALL_PARAMS': call_params}
    return return_template.substitute(value)

  def GenerateBridgeMethodString(self, method):
    method_template = Template("""\
    public ${RETURN} ${NAME}(${PARAMS}) {
        ${RETURN_STATEMENT}ReflectionHelper.invokeMethod(${NAME}Method, \
wrapper${CALL_PARAMS});
    }

    public ${RETURN} ${NAME}Super(${PARAMS}) {
        ${PRE_CREATE_INTERNALLY_RETURN}
        ${SUPER_RETURN_STATEMENT}super.${NAME}(${SUPER_CALL_PARAMS});
        ${POST_CREATE_INTERNALLY_RETURN}
    }

""")
    param_string = self.FormatMethodParamsString(method.params)
    call_param_strings = self.FormatMethodCallParamsString(method.params)
    return_statement = self.FormatMethodReturnStatement(method)
    super_return_statement = self.FormatSuperMethodReturnStatemt(method)
    pre_create_internally_return = self.FormatPreCreateInternallyReturn()
    post_create_internally_return = \
        self.FormatPostCreateInternallyReturn(method, call_param_strings[1])
    values = {'RETURN': method.method_return,
              'NAME': method.method_name,
              'PARAMS': param_string,
              'RETURN_STATEMENT': return_statement,
              'SUPER_RETURN_STATEMENT': super_return_statement,
              'CALL_PARAMS': call_param_strings[0],
              'SUPER_CALL_PARAMS': call_param_strings[1],
              'PRE_CREATE_INTERNALLY_RETURN': pre_create_internally_return,
              'POST_CREATE_INTERNALLY_RETURN': post_create_internally_return}
    return method_template.substitute(values)

  def FormatReflectionParamString(self, params):
    params_string = ''
    for param_name in params:
      params_string += ', '
      param_type = params[param_name]
      if param_type in CLASSES_TO_BE_PROCESS:
        mangle = ManagleInternalNameToFullWrapperName(param_type)
        param_type = 'clazz.getClassLoader().loadClass(\"%s\")' % (mangle)
        params_string += param_type
      else:
        params_string += "%s.class"  % (params[param_name])
    return params_string

  def GenerateReflectionInitString(self):
    ref_methods_string = ''
    ref_method_template = Template("""\
        ${METHOD}Method = clazz.getMethod(\"${METHOD}\"${PARAMS});
""")
    for method in self._java_data.methods:
      if method.is_constructor or method.is_static:
        continue
      params_string = self.FormatReflectionParamString(method.params)
      value = { 'METHOD': method.method_name,
                'PARAMS': params_string}
      ref_methods_string += ref_method_template.substitute(value)

    ref_init_template = Template("""\
    private void reflectionInit() throws NoSuchMethodException,
            ClassNotFoundException {
        Class<?> clazz = wrapper.getClass();
${REF_METHODS}
    }
""")
    value = {'REF_METHODS': ref_methods_string}
    ref_init_string = ref_init_template.substitute(value)
    return ref_init_string

  def GenerateStaticInitializerString(self):
    if not self._java_data.class_annotations.has_key(
        InternalJavaFileData.ANNOTATION_CREATE_INTERNALLY):
      return ''
    static_initializer_template = Template("""\
    static {
        ReflectionHelper.registerConstructor("${STATIC_CONSTRUCTOR_NAME}", \
"${FULL_CLASS_NAME}", Object.class);
    }
""")
    bridge_class_name = \
        ManagleInternalNameToBridgeName(self._java_data.class_name)
    constructor_method = Method(True, False, bridge_class_name, '', '', '')
    static_constructor_name = \
        self.FormatStaticInitializerConstructorName(constructor_method)
    full_class_name = \
        ManagleInternalNameToFullWrapperName(self._java_data.class_name)
    value = {'STATIC_CONSTRUCTOR_NAME': static_constructor_name,
             'FULL_CLASS_NAME': full_class_name}
    return static_initializer_template.substitute(value)


class WrapperGenerator(CodeGenerator):
  """ Generator class thar generates wrapper layer code."""
  def __init__(self, java_data):
    super(WrapperGenerator, self).__init__(java_data)

  def RunTask(self):
    self._generated_code = self.GenerateWrapperClass()
    self._generated_class_name = MangleInternalNameToWrapperName(
        self._java_data.class_name)

  def GenerateWrapperClass(self):
    wrapper_template = Template("""\
${PACKAGE_SECTION}

${IMPORT_SECTION}

public class ${CLASS_NAME} ${CLASS_EXTENDS} ${CLASS_IMPLEMENTS}{

${FIELD_SECTION}

    private final static String BRIDGE_CLASS = "${BRIDGE_CLASS_FULL_NAME}";
    private Object bridge;

    public Object getBridge() {
        return bridge;
    }
${CREATE_INTERNALLY_CONSTRUCORS}
${CONSTRUCTORS_SECTION}
${STATIC_METHOD_SECTION}
${METHODS_SECTION}

${REFLECTION_SECTION}

${STATIC_INITIALIZER}
}
""")
    package_string = self.GeneratePackageString()
    imports = self.GenerateImportRules()
    class_name = self.GenerateWrapperClassName()
    class_extends = self.GenerateClassExtends()
    class_implements = self.GenerateClassImplements()
    fields = self.GenerateClassFields()
    bridge_full_class_name = self._java_data.package_name + \
        '.' + self._java_data.class_name
    create_internally_constructor = self.GenerateCreateInternallyConstructor()
    constructors = self.GenerateConstructors()
    methods = self.GenerateMethods()
    static_methods = self.GenerateStaticMethods()
    reflections = self.GenerateReflectionInitString()
    static_initializer = self.GenerateStaticInitializerString()
    if self._java_data.class_annotations.has_key(
        InternalJavaFileData.ANNOTATION_NO_INSTANCE):
      create_internally_constructor = ''
      constructors = ''
      static_initializer = ''
      reflections = ''
    value = {'PACKAGE_SECTION': package_string,
             'IMPORT_SECTION': imports,
             'CLASS_NAME': class_name,
             'CLASS_EXTENDS': class_extends,
             'CLASS_IMPLEMENTS': class_implements,
             'FIELD_SECTION': fields,
             'BRIDGE_CLASS_FULL_NAME': bridge_full_class_name,
             'CREATE_INTERNALLY_CONSTRUCORS': create_internally_constructor,
             'CONSTRUCTORS_SECTION': constructors,
             'STATIC_METHOD_SECTION': static_methods,
             'METHODS_SECTION': methods,
             'REFLECTION_SECTION': reflections,
             'STATIC_INITIALIZER': static_initializer}
    return wrapper_template.substitute(value)

  def GeneratePackageString(self):
    # Remove the 'internal' folder from internal package.
    package_name = self._java_data.package_name.replace('.internal', '')
    return 'package %s;' % (package_name)

  def GenerateWrapperClassName(self):
    internal_class_name = self._java_data.class_name
    return internal_class_name.replace('Internal', '')

  def GenerateClassExtends(self):
    annotations = self._java_data.class_annotations
    if annotations.has_key(InternalJavaFileData.ANNOTATION_EXTEND_CLASS):
      to_extend = annotations[InternalJavaFileData.ANNOTATION_EXTEND_CLASS]
      return ' extends %s ' % (to_extend.replace('.class', ''))
    return ''

  def GenerateClassImplements(self):
    annotations = self._java_data.class_annotations
    if annotations.has_key(InternalJavaFileData.ANNOTATION_IMPL):
      to_implement = annotations[InternalJavaFileData.ANNOTATION_IMPL]
      impl_interface = to_implement.replace('.class', '')
      if impl_interface in CLASSES_TO_BE_PROCESS:
        impl_interface = MangleInternalNameToWrapperName(impl_interface)
      return ' implements %s ' % (impl_interface)
    return ''

  def GenerateClassFields(self):
    fields_string = ''
    field_template = Template("""\
    public final static ${TYPE} ${NAME} = ${VALUE};
""")
    for field in self._java_data.fields:
      value = {'TYPE': field.field_type,
               'NAME': field.field_name,
               'VALUE': field.field_value}
      fields_string += field_template.substitute(value)
    return fields_string

  def FormatConstructorParamsString(self, method):
    params_string = ''
    for param_name in method.params:
      param_type = method.params[param_name]
      if param_type in CLASSES_TO_BE_PROCESS:
        param_type = MangleInternalNameToWrapperName(param_type)
      params_string += '%s %s' % (param_type, param_name)
      params_string += ','
    if params_string.endswith(','):
      params_string = params_string[0:-1]
    # For internally created class, additional bridge parameter is needed.
    if self._java_data.class_annotations.has_key(
        InternalJavaFileData.ANNOTATION_CREATE_INTERNALLY):
      if len(params_string) > 0:
        params_string += ','
      params_string += 'Object bridge'
    return params_string

  def FormatInstanceParams(self, method):
    params_string = ''
    for param_name in method.params:
      param_type = method.params[param_name]
      if param_type in CLASSES_TO_BE_PROCESS:
        params_string += ','
        params_string += '(%s).getBridge()' % (param_name)
      else:
        params_string += ',' + param_name
    return params_string

  def FormatCreateInstanceString(self, method):
    if self._java_data.class_annotations.has_key(
        InternalJavaFileData.ANNOTATION_CREATE_EXTERNALLY):
      create_instance_template = Template("""\
bridge = ReflectionHelper.createInstance("${STATIC_CONSTRUCTOR_NAME}" \
${INSTANCE_PARAMS},this)
""")
      static_constructor_name = \
          self.FormatStaticInitializerConstructorName(method)
      instance_params = self.FormatInstanceParams(method)
      value = {'STATIC_CONSTRUCTOR_NAME': static_constructor_name,
               'INSTANCE_PARAMS': instance_params}
      return create_instance_template.substitute(value)
    return ''

  def UnStringifyWrapLines(self, wraplines):
    # Un-stringify the wrap lines, convert it from string to statement.
    result_lines = ''
    lines = wraplines.split('\n')
    for line in lines:
      line = line.strip()
      if line.endswith(','):
        line = line[0:-1]
      if not line.startswith('"') and not line.endswith('"'):
        # TODO: Should be an Error here.
        continue
      line = line[1:-1]
      result_lines += line + '\n'
    return result_lines

  def ExpandStatementArguments(self, lines, method):
    # Expand arguments, we assume there are at most 10 arguments.
    # For example if the '%1' and '%2' in 'super(%1, %2);' should be
    # substitute by the actual parameter.
    for arg in range(1, 9):
      arg_string = "%%%d" % (arg)
      if arg_string in lines and arg <= len(method.params.keys()):
        lines = lines.replace(arg_string, method.params.keys()[arg-1])
    return lines

  def FormatPreWrapLinesString(self, method):
    annotations = method.method_annotations
    if not annotations.has_key(Method.ANNOTATION_PRE_WRAPLINE):
      return ''
    pre_wrapline = annotations[Method.ANNOTATION_PRE_WRAPLINE]
    lines = self.UnStringifyWrapLines(pre_wrapline)
    # Expand the code statement by replacing arguments by parameter.
    lines = self.ExpandStatementArguments(lines, method)
    return lines

  def FormatPostWrapLinesString(self, method):
    annotations = method.method_annotations
    if not annotations.has_key(Method.ANNOTATION_POST_WRAPLINE):
      return ''
    post_wrapline = annotations[Method.ANNOTATION_POST_WRAPLINE]
    lines = self.UnStringifyWrapLines(post_wrapline)
    # Expand the code statement.
    lines = self.ExpandStatementArguments(lines, method)
    return lines

  def GenerateConstructors(self):
    constructor_template = Template("""\
    public ${CLASS_NAME}($CONSTRUCTOR_PARAMS) {
        ${PRE_WRAP_LINES}

        ${CREATE_INSTANCE_STATEMENT}
        try {
            reflectionInit();
        } catch(Exception e) {
            ReflectionHelper.handleException(e);
        }

        ${POST_WRAP_LINES}
    }
""")
    constructors_string = ''
    for method in self._java_data.methods:
      if not method.is_constructor:
        continue
      constructor = ''
      class_name = self.GenerateWrapperClassName()
      constructor_params = self.FormatConstructorParamsString(method)
      create_instance = self.FormatCreateInstanceString(method)
      pre_wrap_line = self.FormatPreWrapLinesString(method)
      post_wrap_line = self.FormatPostWrapLinesString(method)
      value = {'CLASS_NAME': class_name,
               'CONSTRUCTOR_PARAMS': constructor_params,
               'CREATE_INSTANCE_STATEMENT': create_instance,
               'PRE_WRAP_LINES': pre_wrap_line,
               'POST_WRAP_LINES': post_wrap_line }
      constructor = constructor_template.substitute(value)
      constructors_string += constructor
    return constructors_string

  def GenerateCreateInternallyConstructor(self):
    if not self._java_data.class_annotations.has_key(
        InternalJavaFileData.ANNOTATION_CREATE_INTERNALLY):
      return ''
    constructor_template = Template("""\
    public ${CLASS_NAME}(Object bridge) {
        this.bridge = bridge;
        try {
            reflectionInit();
        } catch (Exception e) {
            ReflectionHelper.handleException(e);
        }
    }  """)
    class_name = MangleInternalNameToWrapperName(self._java_data.class_name)
    return constructor_template.substitute({'CLASS_NAME': class_name})

  def FormatMethodParamsString(self, params):
    param_string = ''
    for param_name in params:
      param_type = params[param_name]
      if param_type in CLASSES_TO_BE_PROCESS:
        # Mangle class name if the param type is in internal class list.
        param_type = MangleInternalNameToWrapperName(param_type)
      param_string += '%s %s' % (param_type, param_name)
      param_string += ','
    # Trim the last comma for the method parameter list.
    if param_string.endswith(','):
      param_string = param_string[0:-1]
    return param_string

  def FormatMethodCallParamsString(self, params):
    param_string = ''
    for param_name in params:
      param_type = params[param_name]
      if param_type in CLASSES_TO_BE_PROCESS:
        # Mangle parameter string if it is in internal class list.
        param_name = '(%s).getBridge()' % (param_name)
      param_string += ', '
      param_string += param_name
    return param_string

  def FormatRefectionMethodName(self, method):
    # FIXME: Update the reflection method name with param types.
    return '%sMethod' % (method.method_name)

  def FormatReturnStatement(self, method):
    reflection_method = self.FormatRefectionMethodName(method)
    call_params = self.FormatMethodCallParamsString(method.params)
    return_string = ''
    if method.method_return in CLASSES_TO_BE_PROCESS:
      return_template = Template("""\
${RETURN_STRING}ReflectionHelper.getBridgeOrWrapper(\
ReflectionHelper.invokeMethod(${REFLECTION}, bridge ${CALL_PARAMS}))""")
      return_string = MangleInternalNameToWrapperName(method.method_return)
    else:
      return_template = Template("""\
${RETURN_STRING}ReflectionHelper.invokeMethod(${REFLECTION},\
 bridge ${CALL_PARAMS})""")
      if method.method_return == 'void':
        return_string = ''
      else:
        return_string = 'return (%s)' % (method.method_return)
    value = {'RETURN_STRING': return_string,
             'REFLECTION': reflection_method,
             'CALL_PARAMS': call_params}
    return return_template.substitute(value)

  def FormatMethodString(self, method):
    method_string = ''
    method_template = Template("""\
    public ${RETURN} ${METHOD_NAME}(${METHOD_PARAMS}) {
       ${RETURN_STATEMENT};
    }

""")
    method_return = method.method_return
    if method_return in CLASSES_TO_BE_PROCESS:
      method_return = MangleInternalNameToWrapperName(method_return)
    method_name = method.method_name
    method_params = self.FormatMethodParamsString(method.params)
    return_statement = self.FormatReturnStatement(method)
    value = {'RETURN': method_return,
             'METHOD_NAME': method_name,
             'METHOD_PARAMS': method_params,
             'RETURN_STATEMENT': return_statement}
    method_string = method_template.substitute(value)
    return method_string

  def GenerateMethods(self):
    methods_string = ''
    # Generate method definitions.
    for method in self._java_data.methods:
      if method.is_constructor or method.is_static:
        continue
      method_content = self.FormatMethodString(method)
      methods_string += method_content
    # Generate Method field lists.
    method_field_template = Template("""\
    private Method ${METHOD}Method;
""")
    for method in self._java_data.methods:
      if not method.is_constructor and not method.is_static:
        value = {'METHOD': method.method_name}
        methods_string += method_field_template.substitute(value)
    return methods_string

  def FormatStaticMethodCallParamsType(self, params):
    params_string = ''
    for param_name in params:
      params_string += ','
      params_string += '%s.class' % (params[param_name])
    return params_string

  def FormatStaticMethodCallParams(self, params):
    param_string = ''
    for param_name in params:
      param_string += ','
      param_string += param_name
    return param_string

  def FormatStaticMethodTemplate(self, class_name, method):
    method_template = Template("""\
    public static ${RETURN} ${NAME}(${PARAMS}) {
       Class<?> clazz = ReflectionHelper.loadClass(\"${FULL_CLASS_NAME}\");
       Method method = ReflectionHelper.loadMethod(clazz, \"${NAME}\"\
${CALL_PARAMS_TYPE});
       ${RETURN_STATE}ReflectionHelper.invokeMethod(method${CALL_PARAMS});
    }
""")
    return_string = method.method_return
    name = method.method_name
    full_class_name = ManagleInternalNameToFullBridgeName(class_name)
    call_params_type = self.FormatStaticMethodCallParamsType(method.params)
    call_params = self.FormatStaticMethodCallParams(method.params)
    params = self.FormatMethodParamsString(method.params)
    return_state = ''
    if method.method_return != 'void':
      return_state = 'return (%s)' % (method.method_return)
    value = {'RETURN': return_string,
             'NAME': name,
             'FULL_CLASS_NAME': full_class_name,
             'CALL_PARAMS_TYPE': call_params_type,
             'CALL_PARAMS': call_params,
             'PARAMS': params,
             'RETURN_STATE': return_state}
    return method_template.substitute(value)

  def GenerateStaticMethods(self):
    methods_string = ''
    for method in self._java_data.methods:
      if method.is_static:
        methods_string += self.FormatStaticMethodTemplate(
            self._java_data.class_name, method)
    return methods_string

  def FormatReflectionParamString(self, params):
    params_string = ''
    for param_name in params:
      params_string += ', '
      param_type = params[param_name]
      if param_type in CLASSES_TO_BE_PROCESS:
        mangle = ManagleInternalNameToFullBridgeName(param_type)
        param_type = 'clazz.getClassLoader().loadClass(\"%s\")' % (mangle)
        params_string += param_type
      else:
        params_string += "%s.class"  % (params[param_name])
    return params_string

  def GenerateReflectionInitString(self):
    ref_methods_string = ''
    ref_method_template = Template("""\
        ${METHOD}Method = clazz.getMethod(\"${METHOD}Super\"${PARAMS});
""")
    for method in self._java_data.methods:
      if method.is_constructor or method.is_static:
        continue
      params_string = self.FormatReflectionParamString(method.params)
      value = { 'METHOD': method.method_name,
                'PARAMS': params_string}
      ref_methods_string += ref_method_template.substitute(value)

    ref_init_template = Template("""\
    private void reflectionInit() throws NoSuchMethodException,
            ClassNotFoundException {
        Class<?> clazz = wrapper.getClass();
${REF_METHODS}
    }
""")
    value = {'REF_METHODS': ref_methods_string}
    ref_init_string = ref_init_template.substitute(value)
    return ref_init_string

  def FormatStaticInitializerParamsList(self, method):
    params_string = ''
    for param_name in method.params:
      params_string += method.params[param_name] + '.class'
      params_string += ','
    return params_string

  def FormatStaticInitializer(self, method):
    static_initializer_template = Template("""\
        ReflectionHelper.registerConstructor(\"${CONSTRUCTOR_NAME}\", \
\"${FULL_CLASS_NAME}\", ${PARAM_LIST} Object.class};
""")
    constructor_name = self.FormatStaticInitializerConstructorName(method)
    full_class_name = ManagleInternalNameToFullBridgeName(
        self._java_data.class_name)
    params_list = self.FormatStaticInitializerParamsList(method)
    value = {"CONSTRUCTOR_NAME": constructor_name,
             "FULL_CLASS_NAME": full_class_name,
             "PARAM_LIST": params_list}
    return static_initializer_template.substitute(value)

  def GenerateStaticInitializerString(self):
    if not self._java_data.class_annotations.has_key(
        InternalJavaFileData.ANNOTATION_CREATE_EXTERNALLY):
      return ''
    static_initializer_template = Template("""\
    static {
${STATIC_INITIALIZER_LIST}
    }
""")
    static_initializer_list = ''
    for method in self._java_data.methods:
      if method.is_constructor:
        static_initializer_list += self.FormatStaticInitializer(method)
    value = {'STATIC_INITIALIZER_LIST': static_initializer_list}
    return static_initializer_template.substitute(value)


class InternalJavaFileData(object):
  """Data class stores the generator information of internal class."""
  ANNOTATION_CREATE_INTERNALLY = 'createInternally'
  ANNOTATION_CREATE_EXTERNALLY = 'createExternally'
  ANNOTATION_EXTEND_CLASS = 'extendClass'
  ANNOTATION_NO_INSTANCE = 'noInstance'
  ANNOTATION_INSTANCE = 'instance'
  ANNOTATION_IMPL = 'impl'

  def __init__(self):
    self._class_name = ''
    self._class_type = ''  # class or interface
    self._class_annotations = {}
    self._methods = []
    self._fields = []
    self._imports = []
    self._package_name = ''

  @property
  def class_name(self):
    return self._class_name

  @property
  def class_type(self):
    return self._class_type

  @property
  def class_annotations(self):
    return self._class_annotations

  @property
  def methods(self):
    return self._methods

  @property
  def fields(self):
    return self._fields

  @property
  def imports(self):
    return self._imports

  @property
  def package_name(self):
    return self._package_name

  def SetClassContent(self, content):
    self.ExtractPackageName(content)
    self.ExtractImports(content)
    self.ExtractClassProperties(content)
    self.ExtractMethods(content)
    self.ExtractFields(content)

  def ExtractPackageName(self, java_content):
    package_re = re.compile('\s*package\s+(?P<package>[a-zA-Z0-9._]+)\s*;')
    for match in re.finditer(package_re, java_content):
      self._package_name = match.group('package')

  def ShouldIgnoreImport(self, imported):
    # Determine whether the import rule should be ignored for generated code.
    # TODO: Currently we only use a blacklist to filter the import rule.
    if imported.startswith('org.xwalk.core.internal'):
      return True
    if imported.startswith('org.chromium'):
      return True

  def ExtractImports(self, java_content):
    imports_re = re.compile('\s*import\s+(?P<imported>[a-zA-Z0-9._*]+)\s*;')
    for match in re.finditer(imports_re, java_content):
      imported = match.group('imported')
      if not self.ShouldIgnoreImport(imported):
        self._imports.append(imported)

  def ExtractClassProperties(self, java_content):
    class_re = re.compile('@XWalkAPI\(?'
                          '(?P<annotation_content>[a-zA-Z0-9.,=\s]*)\)?'
                          '\s*public\s+(?P<type>(class|interface))\s+'
                          '(?P<class_name>[a-zA-Z0-9]*)')
    for match in re.finditer(class_re, java_content):
      annotation_content = match.group('annotation_content')
      self._class_name = match.group('class_name')
      self._class_type = match.group('type')
      self.ParseClassAnnotations(annotation_content)

  def ParseClassAnnotations(self, annotation):
    """Class annotation contains the following optional attributes:
        'extendClass' - The class have to extend
        'createExternally' - boolean
        'craeteInternally' - boolean
        'noInstance' - boolean
        'isConst' - boolean
        'impl' - Class to impl
        'instance - instance'"""
    extend_class_re = re.compile('extendClass\s*=\s*'
        '(?P<extend_class>[a-zA-Z0-9.]+)')
    for match in re.finditer(extend_class_re, annotation):
      extend_class = match.group('extend_class')
      self._class_annotations['extendClass'] = extend_class

    create_internally_re = re.compile('createInternally\s*=\s*'
        '(?P<create_internally>(true|false))')
    for match in re.finditer(create_internally_re, annotation):
      create_internally = match.group('create_internally')
      if create_internally == 'true':
        self._class_annotations['createInternally'] = True
      elif create_internally == 'false':
        self._class_annotations['createInternally'] = False

    create_externally_re = re.compile('createExternally\s*=\s*'
        '(?P<create_externally>(true|false))')
    for match in re.finditer(create_externally_re, annotation):
      create_externally = match.group('create_externally')
      if create_externally == 'true':
        self._class_annotations['createExternally'] = True
      elif create_externally == 'false':
        self._class_annotations['createExternally'] = False

    no_instance_re = re.compile('noInstance\s*=\s*'
        '(?P<no_instance>(true|false))')
    for match in re.finditer(no_instance_re, annotation):
      no_instance = match.group('no_instance')
      if no_instance == 'true':
        self._class_annotations['noInstance'] = True
      elif no_instance == 'false':
        self._class_annotations['noInstance'] = False

    is_const_re = re.compile('isConst\s*=\s*'
        '(?P<is_const>(true|false))')
    for match in re.finditer(is_const_re, annotation):
      is_const = match.group('is_const')
      if is_const == 'true':
        self._class_annotations['isConst'] = True
      elif is_const == 'false':
        self._class_annotations['isConst'] = False

    impl_re = re.compile('impl\s*=\s*'
        '(?P<impl>[a-zA-Z0-9.]+)')
    for match in re.finditer(impl_re, annotation):
      impl = match.group('impl')
      self._class_annotations['impl'] = impl

    instance_re = re.compile('instance\s*=\s*'
        '(?P<instance>[a-zA-Z0-9.]+)')
    for match in re.finditer(instance_re, annotation):
      instance = match.group('instance')
      self._class_annotations['instance'] = instance

  def ExtractMethods(self, java_content):
    constructor_re = re.compile('@XWalkAPI\(?'
        '(?P<method_annotation>[a-zA-Z0-9%,\s\(\)\{\};._"=]*)\)?'
        '\s*public\s(?P<method_name>[a-zA-Z0-9]+)\('
        '(?P<method_params>[a-zA-Z0-9\s,]*)\)')
    for match in re.finditer(constructor_re, java_content):
      method_annotation = match.group('method_annotation')
      method_name = match.group('method_name')
      method_params = match.group('method_params')
      method = Method(True, False, method_name, None,
          method_params, method_annotation)
      self._methods.append(method)

    method_re = re.compile('@XWalkAPI\(?'
        '(?P<method_annotation>[a-zA-Z0-9%,\s\(\)\{\};._"=]*)\)?'
        '\s*public\s+(?P<method_return>[a-zA-Z0-9]+)\s+'
        '(?P<method_name>[a-zA-Z0-9]+)\('
        '(?P<method_params>[a-zA-Z0-9\s,\<\>]*)\)')
    for match in re.finditer(method_re, java_content):
      method_annotation = match.group('method_annotation')
      method_name = match.group('method_name')
      method_params = match.group('method_params')
      method_return = match.group('method_return')
      method = Method(False, False, method_name, method_return, method_params,
          method_annotation)
      self._methods.append(method)

    method_re = re.compile('@XWalkAPI\(?'
        '(?P<method_annotation>[a-zA-Z0-9%,\s\(\)\{\};._"=]*)\)?'
        '\s*public\s+static\s+(synchronized\s+)*'
        '(?P<method_return>[a-zA-Z0-9]+)\s+'
        '(?P<method_name>[a-zA-Z0-9]+)\('
        '(?P<method_params>[a-zA-Z0-9\s,\<\>]*)\)')
    for match in re.finditer(method_re, java_content):
      method_annotation = match.group('method_annotation')
      method_name = match.group('method_name')
      method_params = match.group('method_params')
      method_return = match.group('method_return')
      method = Method(False, True, method_name, method_return, method_params,
          method_annotation)
      self._methods.append(method)



  def ExtractFields(self, java_content):
    field_re = re.compile('@XWalkAPI\s*public\s+static\s+final\s+'
        '(?P<field_type>[a-zA-Z0-9_]+)\s+'
        '(?P<field_name>[a-zA-Z0-9_]+)\s*=\s*'
        '(?P<field_value>[a-zA-Z0-9-_"]+)\s*;')
    for match in re.finditer(field_re, java_content):
      field_type = match.group('field_type')
      field_name = match.group('field_name')
      field_value = match.group('field_value')
      field_object = Field(field_type, field_name, field_value)
      self._fields.append(field_object)


def PerformSerialize(output_path, generator):
  # Serialize the code.
  file_name = os.path.join(output_path, generator.GetGeneratedClassFileName())
  file_handle = open(file_name, 'w')
  file_handle.write(generator.GetGeneratedCode())
  file_handle.close()
  print '%s has been generated!' % (file_name)


def GenerateBindingForJavaFile(file_name, bridge_output, wrap_output):
  try:
    file_handle = open(file_name, 'r')
    file_content = file_handle.read()
    file_handle.close()
  except Exception:
    print 'Error reading input Java file, please check.'
    return

  java_data = InternalJavaFileData()
  java_data.SetClassContent(file_content)

  if java_data.class_type == 'interface':
    interface_generator = InterfaceGenerator(java_data)
    interface_generator.RunTask()
    PerformSerialize(wrap_output, interface_generator)
  else:
    # Generate Bridge code.
    bridge_generator = BrigdeGenerator(java_data)
    bridge_generator.RunTask()
    # Serialize.
    PerformSerialize(bridge_output, bridge_generator)
    # Generate Wrapper code.
    wrapper_generator = WrapperGenerator(java_data)
    wrapper_generator.RunTask()
    PerformSerialize(wrap_output, wrapper_generator)


def GenerateBindingForJavaDirectory(input_dir, bridge_output, wrap_output):
  for input_file in os.listdir(input_dir):
    input_class_name = input_file.replace('.java', '')
    if input_class_name in CLASSES_TO_BE_PROCESS:
      input_file = os.path.join(input_dir, input_file)
      print 'Generate bridge and wrapper code for %s' % (input_file)
      GenerateBindingForJavaFile(input_file, bridge_output, wrap_output)


def main(argv):
  usage = """Usage: %prog [OPTIONS]
This script can generate bridge and wrap source files for given file or
directory. Either \'input_java\' is provided as single source file or
\'input_dir\' is provided as directory containing source files.
  """
  option_parser = optparse.OptionParser(usage=usage)
  option_parser.add_option('--input_java',
                           help='Input source file.')
  option_parser.add_option('--input_dir',
                           help= ('Input source file directory which contains'
                                  'input files'))
  option_parser.add_option('--bridge_output',
                           help=('Output directory where the bridge code'
                                 'is placed.'))
  option_parser.add_option('--wrap_output',
                           help=('Output directory where the wrap code'
                                'is placed.'))
  options, _ = option_parser.parse_args(argv)
  if not options.input_java and not options.input_dir:
    print('Error: Must specify input.')
    return 1
  if options.input_java:
    GenerateBindingForJavaFile(options.input_java,
        options.bridge_output, options.wrap_output)
  elif options.input_dir:
    GenerateBindingForJavaDirectory(options.input_dir,
        options.bridge_output, options.wrap_output)


if __name__ == '__main__':
  sys.exit(main(sys.argv))
