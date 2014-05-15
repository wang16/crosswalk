import optparse
import os
import sys
import shutil
import subprocess


def Touch(path):
  if not os.path.isdir(os.path.dirname(path)):
    os.makedirs(os.path.dirname(path))
  with open(path, 'a'):
    os.utime(path, None)


def GetCommandOutput(command, cwd=None):
  proc = subprocess.Popen(command, stdout=subprocess.PIPE,
                          stderr=subprocess.STDOUT, bufsize=1,
                          cwd=cwd)
  output = proc.communicate()[0]
  result = proc.returncode
  if result:
    raise Exception('%s: %s' % (subprocess.list2cmdline(command), output))
  return output


def DoGenerate(options):
  mainClass = 'org.xwalk.reflection.generator.Generator'
  jars = []
  for jar in options.classpath.split(' '):
    try:
      jarPath = eval(jar)
    except:
      jarPath = jar
    jars.append(jarPath)
  classpath = ':'.join(jars)
  if os.path.isdir(options.output):
    shutil.rmtree(options.output)
  os.makedirs(options.output)
  cmd = ['java', '-classpath', classpath, mainClass,
         str(options.reflection).lower(), options.source, options.output]
  GetCommandOutput(cmd)


def DoCopyHelperJava(options):
  if options.helper_class is None:
    return
  f = open(options.helper_class, 'r')
  output = os.path.join(options.output, 'wrapper', 'src', 'org', 'xwalk', 'core',
                        os.path.basename(options.helper_class))
  if not os.path.isdir(os.path.dirname(output)):
    os.makedirs(os.path.dirname(output))
  fo = open(output, 'w')
  for line in f.read().split('\n'):
    if line.startswith('package '):
      fo.write('package org.xwalk.core;\n')
    else:
      fo.write(line + '\n')
  fo.close()
  f.close()


def main():
  parser = optparse.OptionParser()
  info = ('list of jars needed when running generator')
  parser.add_option('--classpath', help=info)
  info = ('path or internal java files')
  parser.add_option('--source', help=info)
  info = ('intermediate folder to place generated java files')
  parser.add_option('--output', help=info)
  info = ('the file to touch on success.')
  parser.add_option('--stamp', help=info)
  info = ('set to true to create real reflection, otherwise '
          'only bridge and wrapper will be created')
  parser.add_option('--reflection', default=False, action='store_true', help=info)
  info = ('the path of the ReflectionHelper java source, '
          'will copy it to output folder')
  parser.add_option('--helper-class', help=info)
  options, _ = parser.parse_args()

  DoGenerate(options)
  DoCopyHelperJava(options)
  if options.stamp:
    Touch(options.stamp)

if __name__ == '__main__':
  sys.exit(main())

