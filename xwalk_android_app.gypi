{
  'targets': [
    {
      'target_name': 'xwalk_app_runtime_client_java',
      'type': 'none',
      'variables': {
        'java_in_dir': 'app/android/runtime_client',
      },
      'includes': ['../build/java.gypi'],
    },
    {
      'target_name': 'xwalk_app_runtime_activity_java',
      'type': 'none',
      'dependencies': [
        'xwalk_app_runtime_client_java',
      ],
      'variables': {
        'java_in_dir': 'app/android/runtime_activity',
      },
      'includes': ['../build/java.gypi'],
    },
    {
      'target_name': 'xwalk_runtime_shared_shell_apk',
      'type': 'none',
      'dependencies': [
        'xwalk_app_runtime_activity_java',
      ],
      'variables': {
        'apk_name': 'XWalkRuntimeSharedShell',
        'java_in_dir': 'runtime/android/runtimesharedshell',
        'resource_dir': 'runtime/android/runtimesharedshell/res',
        'native_lib_target': 'libxwalkcore',
        'package_native_libs': 0,
      },
      'includes': [ '../build/java_apk.gypi' ],
    },
  ],
}
