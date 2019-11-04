from bcc import BPF
import logging

syscall_regex = "^[s]y[s]_"


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
        self._bpf.attach_tracepoint(tp="sched:sched_process_fork", fn_name="on_fork")
        self._bpf.attach_tracepoint(tp="sched:sched_process_exit", fn_name="on_exit")

    def detach_probes(self):
        self._bpf.detach_tracepoint(tp="sched:sched_process_fork")
        self._bpf.detach_tracepoint(tp="sched:sched_process_exit")
        self._bpf.cleanup()

    def filter_pid(self, pid):
        assert isinstance(pid, int)

        logging.info('Filtering events from PID [' + str(pid) + ']')

        self._contents = self._contents.replace(
            '//PID_FILTER//', str(pid))

    def filter_comm(self, comm):
        logging.info('Filtering events from COMM [' + str(comm) + ']')

        if comm is None:
            filter = 'NULL'
        else:
            filter = '"' + comm + '"'

        self._contents = self._contents.replace(
            '//COMM_FILTER//', filter)

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
        socket_probes['kernel_sendpage'] = ('entry__sock_sendmsg', 'exit__sock_sendmsg')
        socket_probes['sock_recvmsg'] = ('entry__sock_recvmsg', 'exit__sock_recvmsg')

        syscall_probes = {}
        # Prefix with 're' to indicate it is a regex.
        syscall_probes['wake_up_new_task'] = ('entry__wake_up_new_task', None)
        syscall_probes['do_wait'] = (None, 'exit__do_wait')
        syscall_probes['re_' + syscall_regex + 'fsync'] = (None, 'exit__sys_fsync')

        self._probes = {'socket': socket_probes, 'process': syscall_probes}

        return self._probes
