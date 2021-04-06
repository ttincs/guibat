import os
from subprocess import call

from pygator.utils import make_temp_dir

JAR = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                   '..', 'tools', 'apktool_2.4.0.jar')


def decode_res_from_apk(apk_path, dir=''):
    return decode_apk(apk_path, ['--no-src'], dir)


def decode_src_from_apk(apk_path, dir=''):
    return decode_apk(apk_path, ['--no-res'], dir)


def decode_apk(apk_path, opt=None, dir=''):
    tmp_dir = make_temp_dir(prefix='gator-', dir=dir)
    cmd = ['java', '-jar', JAR, 'decode', '-f']
    if opt is not None:
        cmd.extend(opt)
    cmd.extend(['--output', tmp_dir, '--frame-path', tmp_dir, apk_path])
    print('...... ' + ' '.join(cmd))
    call(cmd)
    return tmp_dir
