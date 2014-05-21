#!/usr/bin/env python

# Copyright (c) 2014 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

import os
import re
import shutil


xwalk_path = os.path.dirname(__file__)
core_path = os.path.join(xwalk_path, 'runtime', 'android', 'core')
core_internal_path = os.path.join(xwalk_path, 'runtime', 'android', 'core_internal')
extension_path = os.path.join(xwalk_path, 'extensions', 'android', 'java')

class_list = [
    'XWalkJavascriptResult',
    'XWalkJavascriptResultHandler',
    'XWalkNavigationHistory',
    'XWalkNavigationItem',
    'XWalkPreferences',
    'XWalkResourceClient',
    'XWalkUIClient',
    'XWalkView'
]


def RenamePackage(src_root, orig):
  for d, _, files in os.walk(os.path.join(src_root, 'src')):
    for f in files:
      if os.path.splitext(f)[1] == '.java':
        in_f = open(os.path.join(d, f), 'r')
        content = in_f.readlines()
        in_f.close()
        out_f = open(os.path.join(d, f), 'w')
        for line in content:
          out_f.write(line.replace(orig, '%s.internal' % orig))
        out_f.close()

  orig_dir = os.path.join(src_root, 'src', orig.replace('.', os.path.sep))
  new_dir = os.path.join(src_root, 'src', orig.replace('.', os.path.sep), 'internal')
  temp_dir = orig_dir + 'tmp'

  if os.path.isdir(new_dir):
    shutil.rmtree(new_dir)

  if os.path.isdir(temp_dir):
    shutil.rmtree(temp_dir)

  os.rename(orig_dir, temp_dir)
  os.mkdir(orig_dir)
  os.rename(temp_dir, new_dir)


def RenameClass(src_root, orig):
  for d, _, files in os.walk(os.path.join(src_root, 'src')):
    for f in files:
      if os.path.splitext(f)[1] == '.java':
        in_f = open(os.path.join(d, f), 'r')
        content = in_f.readlines()
        in_f.close()
        if os.path.splitext(f)[0] == orig:
          os.remove(os.path.join(d, f))
          f = '%sInternal.java' % orig
        out_f = open(os.path.join(d, f), 'w')
        for line in content:
          out_f.write(re.sub(r'\b%s\b' % orig, '%sInternal' % orig, line))
        out_f.close()


for clazz in class_list:
  RenameClass(core_path, clazz)

RenamePackage(core_path, 'org.xwalk.core')
RenamePackage(extension_path, 'org.xwalk.core')

if os.path.isdir(core_internal_path):
  shutil.rmtree(core_internal_path)

os.rename(core_path, core_internal_path)
