import os
import logging
import logging.config
from bcc import BPF
from . import util, event

# Configure logger
logging.config.fileConfig(util.get_full_path('../../conf/logging.ini'))

class FalconTracer:
    def __init__(self, disable_process_events=False, disable_socket_events=False, pid_filter=False):
        self.syscall_regex = "^[Ss]y[Ss]_"
        self._bpf = None
        self._socket_events = not disable_socket_events
        self._process_events = not disable_process_events
        self._pid_filter = pid_filter

    def run(self):
        # Load program
        probes_filename = util.get_full_path('resources/ebpf/probes.c')
        with open(probes_filename, 'r') as probe_file:
            # Read and process probes file
            probe_text = self.process_probe_file(probe_file.read())
            self._bpf = BPF(text=probe_text)

            # Prepare handlers
            event_listener = event.listeners.EventListener(self._bpf, FalconTracer.get_handlers())

            # Attach probes
            if self._socket_events:
                self.attach_socket_probes()

            if self._process_events:    
                self.attach_process_probes()

            # Start
            # bpf.trace_print()
            event_listener.run()

        return 0

    def attach_process_probes(self):
        # Trace exit() syscall
        exit_syscall = self.syscall_regex + 'exit'
        self._bpf.attach_kprobe(event_re=exit_syscall, fn_name="entry__sys_exit")

        # Trace wait() syscall
        wait_syscall = self.syscall_regex + 'wait'
        self._bpf.attach_kretprobe(event_re=wait_syscall, fn_name="exit__sys_wait")

        # Trace clone() syscall
        clone_syscall = self.syscall_regex + 'clone'
        self._bpf.attach_kprobe(event_re=clone_syscall, fn_name="entry__sys_clone")
        self._bpf.attach_kretprobe(event_re=clone_syscall, fn_name="exit__sys_clone")

    def attach_socket_probes(self):
        self._bpf.attach_kprobe(event="tcp_connect", fn_name="entry__tcp_connect")
        self._bpf.attach_kretprobe(event="tcp_connect", fn_name="exit__tcp_connect")

        self._bpf.attach_kretprobe(event="inet_csk_accept", fn_name="exit__inet_csk_accept")

        self._bpf.attach_kprobe(event="sock_sendmsg", fn_name="entry__sock_sendmsg")
        self._bpf.attach_kretprobe(event="sock_sendmsg", fn_name="exit__sock_sendmsg")

        self._bpf.attach_kprobe(event="sock_recvmsg", fn_name="entry__sock_recvmsg")
        self._bpf.attach_kretprobe(event="sock_recvmsg", fn_name="exit__sock_recvmsg")

    def process_probe_file(self, contents):
        # Filter pid
        contents = contents.replace('//PID_FILTER//', str(self._pid_filter))

        return contents

    @staticmethod
    def get_handlers():
        handler_instances = []

        # Printer Handler
        handler_instances.append(event.handlers.PrinterSocketEventHandler())

        return handler_instances

