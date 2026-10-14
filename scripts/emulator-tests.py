#!/usr/bin/env python3
"""Run real AndroidJUnitRunner tests via an emulator's authenticated ADB transport.

Useful when the native adb client insists on a read-only home directory. No auth
or TLS checks are disabled. An ADB RSA key is generated in the ignored work dir.
Normal Android Studio users should run connectedDebugAndroidTest instead.
"""
import argparse
from pathlib import Path
from adb_shell.adb_device import AdbDeviceTcp
from adb_shell.auth.keygen import keygen
from adb_shell.auth.sign_pythonrsa import PythonRSASigner

parser=argparse.ArgumentParser()
parser.add_argument('--host',default='127.0.0.1')
parser.add_argument('--port',type=int,default=5555)
parser.add_argument('--class',dest='test_class')
parser.add_argument('--skip-install',action='store_true')
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
work=root/'work';work.mkdir(exist_ok=True)
key=work/'android-user/python-adb-key';key.parent.mkdir(exist_ok=True)
if not key.exists(): keygen(str(key))
device=AdbDeviceTcp(args.host,args.port,default_transport_timeout_s=180)
device.connect(rsa_keys=[PythonRSASigner.FromRSAKeyPath(str(key))],auth_timeout_s=30)
try:
    boot=device.shell('getprop sys.boot_completed',read_timeout_s=180).strip()
    if boot!='1': raise RuntimeError(f'Wait for the emulator to finish booting (sys.boot_completed={boot!r})')
    if not args.skip_install:
        for path,remote in [('app/build/outputs/apk/debug/app-debug.apk','app.apk'),('app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk','tests.apk')]:
            print(f'Installing {path}',flush=True)
            device.push(str(root/path),f'/data/local/tmp/{remote}',read_timeout_s=180)
            result=device.shell(f'pm install -r -t /data/local/tmp/{remote}',read_timeout_s=180,timeout_s=600)
            print(result,flush=True)
            if 'Success' not in result: raise RuntimeError(result)
    # Reduce software-rendering load while retaining enough space for 300dp fixtures.
    device.shell('wm size 480x800; wm density 160; input keyevent 82',read_timeout_s=180)
    command='am instrument -w -r'
    if args.test_class:
        import shlex
        command+=' -e class '+shlex.quote(args.test_class)
    command+=' com.arrowescape.test/androidx.test.runner.AndroidJUnitRunner'
    chunks=[]
    with (work/'instrumentation.log').open('w') as report:
        for chunk in device.streaming_shell(command,read_timeout_s=600):
            print(chunk,end='',flush=True);report.write(chunk);report.flush();chunks.append(chunk)
    output=''.join(chunks)
    if 'FAILURES!!!' in output or 'INSTRUMENTATION_FAILED' in output or 'OK (' not in output:
        raise RuntimeError('Instrumentation failed; see work/instrumentation.log')
finally:
    device.close()
