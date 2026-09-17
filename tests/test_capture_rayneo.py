import importlib.util
import json
import pathlib
import subprocess
import tempfile
import unittest
from unittest.mock import patch, Mock

spec = importlib.util.spec_from_file_location('capture_rayneo', pathlib.Path(__file__).parents[1] / 'scripts/capture_rayneo.py')
capture = importlib.util.module_from_spec(spec)
spec.loader.exec_module(capture)


class CaptureTest(unittest.TestCase):
    def test_reboot_stops_child_without_relaunch_or_device_writes(self):
        commands = []
        boots = iter(['boot-a', 'boot-b'])

        def run(command, **kwargs):
            commands.append(command)
            if command[-1] == 'get-state':
                output = 'device'
            elif command[-1] == '/proc/sys/kernel/random/boot_id':
                output = next(boots)
            else:
                output = 'read-only-evidence'
            return subprocess.CompletedProcess(command, 0, output, '')

        child = Mock()
        child.poll.return_value = None
        with tempfile.TemporaryDirectory() as temp:
            output = pathlib.Path(temp) / 'capture'
            argv = ['capture', '--serial', 'TEST_SERIAL', '--seconds', '60', '--output', str(output)]
            with patch('sys.argv', argv), patch.object(capture.subprocess, 'run', side_effect=run), \
                 patch.object(capture.subprocess, 'Popen', return_value=child), \
                 patch.object(capture.time, 'sleep'), patch.object(capture.time, 'monotonic', side_effect=range(100)):
                self.assertEqual(2, capture.main())
            child.terminate.assert_called_once()
            records = [json.loads(line) for line in (output / 'transport.jsonl').read_text().splitlines()]
            self.assertTrue(records[-1]['confirmed_reboot'])
            self.assertTrue((output / 'reboot.json').exists())
        forbidden = {'connect', 'tcpip', 'reboot', 'install', 'kill-server', 'am', 'su'}
        self.assertTrue(all(not forbidden.intersection(command) for command in commands))


if __name__ == '__main__':
    unittest.main()
