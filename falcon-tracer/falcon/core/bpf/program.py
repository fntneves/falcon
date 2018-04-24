from bcc import BPF

syscall_regex = "^[Ss]y[Ss]_"


class BpfProgram():
    def __init__(self, text):
        self._contents = text
        self._bpf = None

    def bpf_instance(self):
        return self._bpf

    def prepare(self):
        assert self._bpf is None

        self._bpf = BPF(text=self._contents)

    def attach_probes(self):
        # self._attach_socket_probes()
        self._attach_process_probes()

    def detach_probes(self):
        self._bpf.cleanup()

    def filter_pid(self, pid):
        assert isinstance(pid, int)

        print 'PID filter added: ' + str(pid)

        self._contents = self._contents.replace(
            '//PID_FILTER//', str(pid))

    def open_event_buffer(self, name, handler):
        self._bpf[name].open_perf_buffer(handler)

    def _attach_process_probes(self):
        # Trace exit() syscall
        exit_syscall = syscall_regex + 'exit'
        self._bpf.attach_kprobe(event_re=exit_syscall,
                                fn_name="entry__sys_exit")

        # Trace wait() syscall
        wait_syscall = syscall_regex + 'wait'
        self._bpf.attach_kretprobe(
            event_re=wait_syscall, fn_name="exit__sys_wait")

        # Trace clone() syscall
        clone_syscall = syscall_regex + 'clone'
        self._bpf.attach_kprobe(event_re=clone_syscall,
                                fn_name="entry__sys_clone")
        self._bpf.attach_kretprobe(
            event_re=clone_syscall, fn_name="exit__sys_clone")

    def _attach_socket_probes(self):

        self._bpf.attach_kprobe(event="tcp_connect",
                                fn_name="entry__tcp_connect")
        self._bpf.attach_kretprobe(
            event="tcp_connect", fn_name="exit__tcp_connect")

        self._bpf.attach_kretprobe(
            event="inet_csk_accept", fn_name="exit__inet_csk_accept")

        self._bpf.attach_kprobe(event="sock_sendmsg",
                                fn_name="entry__sock_sendmsg")
        self._bpf.attach_kretprobe(
            event="sock_sendmsg", fn_name="exit__sock_sendmsg")

        self._bpf.attach_kprobe(event="sock_recvmsg",
                                fn_name="entry__sock_recvmsg")
        self._bpf.attach_kretprobe(
            event="sock_recvmsg", fn_name="exit__sock_recvmsg")

    @staticmethod
    def get_probes():
        socket_probes = {}
        socket_probes['tcp_connect'] = ('entry__tcp_connect', 'exit__tcp_connect')
        socket_probes['inet_csk_accept'] = (None, 'exit__inet_csk_accept')
        socket_probes['sock_sendmsg'] = ('entry__sock_sendmsg', 'exit__sock_sendmsg')
        socket_probes['sock_recvmsg'] = ('entry__sock_recvmsg', 'exit__sock_recvmsg')

        syscall_probes = {}
        syscall_probes[syscall_regex + 'exit'] = ('entry__sys_exit', None)
        syscall_probes[syscall_regex + 'wait'] = (None, 'exit__sys_wait')
        syscall_probes[syscall_regex + 'clone'] = ('entry__sys_clone', 'exit__sys_clone')

        return {'socket': socket_probes, 'process': syscall_probes}
