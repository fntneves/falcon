from bcc import BPF

syscall_regex = "^[Ss]y[Ss]_"


class BpfProgram():
    def __init__(self, text):
        self._contents = text
        self._bpf = None
        self._probes = None
        self._perf_buffer_size = 64 * 1024

    def bpf_instance(self):
        return self._bpf

    def prepare(self):
        assert self._bpf is None

        self._bpf = BPF(text=self._contents)

    def attach_probes(self):
        self._attach_socket_probes()
        self._attach_process_probes()

    def detach_probes(self):
        self._bpf.cleanup()

    def filter_pid(self, pid):
        assert isinstance(pid, int)

        print 'Filtering events from PID: ' + str(pid)

        self._contents = self._contents.replace(
            '//PID_FILTER//', str(pid))

    def open_event_buffer(self, name, handler):
        self._bpf[name].open_perf_buffer(handler, page_cnt=self._perf_buffer_size)

    def _attach_probes_set(self, probes):
        for event, (entry, exit) in probes.items():
            if event.startswith('re_'):
                event = event[3:]
                entry is not None and self._bpf.attach_kprobe(event_re=event, fn_name=entry)
                exit is not None and self._bpf.attach_kretprobe(event_re=event, fn_name=exit)
            else:
                entry is not None and self._bpf.attach_kprobe(event=event, fn_name=entry)
                exit is not None and self._bpf.attach_kretprobe(event=event, fn_name=exit)


    def _attach_process_probes(self):
        self._attach_probes_set(self.get_probes()['process'])

    def _attach_socket_probes(self):
        self._attach_probes_set(self.get_probes()['socket'])

    def get_probes(self):
        if self._probes is not None:
            return self._probes

        socket_probes = {}
        socket_probes['tcp_connect'] = ('entry__tcp_connect', 'exit__tcp_connect')
        socket_probes['inet_csk_accept'] = (None, 'exit__inet_csk_accept')
        socket_probes['sock_sendmsg'] = ('entry__sock_sendmsg', 'exit__sock_sendmsg')
        socket_probes['sock_recvmsg'] = ('entry__sock_recvmsg', 'exit__sock_recvmsg')

        syscall_probes = {}
        # Prefix with 're' to indicate it is a regex.
        syscall_probes['re_' + syscall_regex + 'exit'] = ('entry__sys_exit', None)
        syscall_probes['re_' + syscall_regex + 'wait'] = (None, 'exit__sys_wait')
        syscall_probes['re_' + syscall_regex + 'clone'] = ('entry__sys_clone', 'exit__sys_clone')

        self._probes = {'socket': socket_probes, 'process': syscall_probes}

        return self._probes
