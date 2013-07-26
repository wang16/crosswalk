{
  'targets': [
    {
      'target_name': 'xwalk_runtime_wrapper_java',
      'type': 'none',
      'dependencies': [
        'xwalk_runtime_lib_apk',
      ],
      'variables': {
        'java_in_dir': 'app/android/java',
      },
      'includes': ['../build/java.gypi'],
    },
    {
      'target_name': 'xwalk_runtime_shared_shell_apk',
      'type': 'none',
      'dependencies': [
        'xwalk_runtime_wrapper_java',
      ],
      'variables': {
        'apk_name': 'XWalkRuntimeSharedShell',
        'java_in_dir': 'runtime/android/runtimesharedshell',
        'resource_dir': 'runtime/android/runtimesharedshell/res',
        'native_lib_target': 'libxwalkcore',
        'package_native_libs': 0,
        'additional_input_paths': [
          '<(PRODUCT_DIR)/xwalk_runtime/assets/xwalk.pak',
        ],
        'asset_location': '<(ant_build_out)/xwalk_runtime/assets',
      },
      'includes': [ '../build/java_apk.gypi' ],
    },
  ],
}
