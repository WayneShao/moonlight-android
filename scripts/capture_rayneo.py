"""Read-only, bounded host diagnostics. Never launches, installs, reconnects or reboots.

Example: python scripts/capture_rayneo.py --serial SERIAL --seconds 180 --output out/diagnostics
Run BEFORE manually opening Moonlight. Ctrl+C stops child logcat. A changed boot ID stops capture.
"""
import argparse
import datetime as dt
import json
import pathlib
import subprocess
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--seconds', type=int, default=180)
    parser.add_argument('--output', type=pathlib.Path, required=True)
    args = parser.parse_args()
    if not 1 <= args.seconds <= 3600:
        parser.error('--seconds must be in 1..3600')
    args.output.mkdir(parents=True, exist_ok=False)
    base = [args.adb, '-s', args.serial]
    proc = None
    log = None
    segment = 0
    first_boot = None
    creationflags = getattr(subprocess, 'CREATE_NO_WINDOW', 0)

    def run(command, timeout=8):
        try:
            result = subprocess.run(base + command, capture_output=True, text=True,
                                    encoding='utf-8', errors='replace', timeout=timeout,
                                    creationflags=creationflags)
            return {'returncode': result.returncode, 'output': result.stdout.strip(),
                    'stderr': result.stderr.strip()}
        except subprocess.TimeoutExpired:
            return {'returncode': None, 'error': 'timeout'}

    def stop_log():
        nonlocal proc, log
        if proc is not None:
            proc.terminate()
            try:
                proc.wait(timeout=4)
            except subprocess.TimeoutExpired:
                proc.kill()
                proc.wait(timeout=4)
            proc = None
        if log is not None:
            log.close()
            log = None

    started = time.monotonic()
    try:
        with (args.output / 'transport.jsonl').open('w', encoding='utf-8') as transport:
            while time.monotonic() - started < args.seconds:
                state = run(['get-state'], 4)
                entry = {'time': dt.datetime.now(dt.timezone.utc).isoformat(), 'state': state}
                if state.get('returncode') == 0 and state.get('output') == 'device':
                    boot = run(['shell', 'cat', '/proc/sys/kernel/random/boot_id'], 4)
                    entry['boot'] = boot
                    entry['uptime'] = run(['shell', 'cat', '/proc/uptime'], 4)
                    boot_id = boot.get('output') if boot.get('returncode') == 0 else None
                    if first_boot and boot_id and boot_id != first_boot:
                        entry['confirmed_reboot'] = True
                        transport.write(json.dumps(entry, ensure_ascii=False) + '\n')
                        transport.flush()
                        stop_log()
                        # Read-only post-reboot evidence; do not restart the application.
                        evidence = {
                            'boot_reason': run(['shell', 'getprop', 'sys.boot.reason']),
                            'last_boot_reason': run(['shell', 'getprop', 'ro.boot.bootreason']),
                            'pstore_listing': run(['shell', 'ls', '-l', '/sys/fs/pstore']),
                        }
                        (args.output / 'reboot.json').write_text(json.dumps(evidence, indent=2), encoding='utf-8')
                        print('Boot ID changed. Capture stopped; application was not relaunched.')
                        return 2
                    if boot_id and first_boot is None:
                        first_boot = boot_id
                    if proc is None or proc.poll() is not None:
                        stop_log()
                        segment += 1
                        log = (args.output / f'logcat-{segment:03}.txt').open('wb')
                        proc = subprocess.Popen(base + ['logcat', '-b', 'main', '-b', 'system', '-b', 'crash',
                                '-v', 'threadtime', '-T', '1',
                                'RayNeo:V', 'RayNeoUI:V', 'RayNeoVideo:V', 'RayNeoDecoder:V', 'RayNeoTrace:V',
                                'AndroidRuntime:V', 'DEBUG:V', 'libc:V', 'EGL_emulation:V', 'Adreno:V',
                                'MediaCodec:V', 'CCodec:V', 'ACodec:V', 'SurfaceFlinger:W', '*:S'],
                                stdout=log, stderr=subprocess.STDOUT, creationflags=creationflags)
                else:
                    stop_log()
                transport.write(json.dumps(entry, ensure_ascii=False) + '\n')
                transport.flush()
                time.sleep(min(3, max(0, args.seconds - (time.monotonic() - started))))
    except KeyboardInterrupt:
        print('Stopped by user.')
    finally:
        stop_log()
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
