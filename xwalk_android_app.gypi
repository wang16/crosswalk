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
    {
      'target_name': 'xwalk_runtime_app_template',
      'type': 'none',
      'dependencies': [
        'xwalk_runtime_shared_shell_apk',
      ],
      'copies': [
        {
          'destination': '<(PRODUCT_DIR)/xwalk_app_template/libs/',
          'files': [
            '<(PRODUCT_DIR)/lib.java/xwalk_app_runtime_activity_java.dex.jar',
            '<(PRODUCT_DIR)/lib.java/xwalk_app_runtime_activity_java.jar',
            '<(PRODUCT_DIR)/lib.java/xwalk_app_runtime_client_java.dex.jar',
            '<(PRODUCT_DIR)/lib.java/xwalk_app_runtime_client_java.jar',
          ],
        },
        {
          'destination': '<(PRODUCT_DIR)/xwalk_app_template/scripts/',
          'files': [
            '../build/android/ant/',  
            '../build/android/gyp/',
          ],
        },
        {
          'destination': '<(PRODUCT_DIR)/xwalk_app_template/app_src/',
          'files': [
            'runtime/android/runtimesharedshell/AndroidManifest.xml',
            'runtime/android/runtimesharedshell/res',
            'runtime/android/runtimesharedshell/src',
          ],
        },
        {
          'destination': '<(PRODUCT_DIR)/xwalk_app_template/',
          'files': [
            'build/android/make_apk.sh',
          ],
        },
      ],
    },
  ],
}
