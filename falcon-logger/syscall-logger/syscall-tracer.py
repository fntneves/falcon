#!/usr/bin/python2
from __future__ import print_function
from ptrace import PtraceError
from ptrace.debugger import (PtraceDebugger, Application,
                             ProcessExit, ProcessSignal, NewProcessEvent, ProcessExecution)
from ptrace.func_call import FunctionCallOptions
from sys import stderr, exit
from optparse import OptionParser
from logging import getLogger, error
from ptrace.error import PTRACE_ERRORS, writeError
from time import time
import psutil
import json
from collections import OrderedDict


def time_ms():
        return time() * 1000

class NetTracer(Application):

    def __init__(self):
        Application.__init__(self)

        # Parse self.options
        self.parseOptions()

        # Setup output (log)
        self.setupLog()

        # Setup registry
        self.sockfd = {}
        self.log = []
        self.exit_syscalls = set(['read', 'accept', 'recvfrom', 'recvmsg',])

    def setupLog(self):
        if self.options.output:
            fd = open(self.options.output, 'w')
            self._output = fd
        else:
            fd = stderr
            self._output = None
        self._setupLog(fd)

    def parseOptions(self):
        parser = OptionParser(
            usage="%prog [options] -- program [arg1 arg2 ...]")
        self.createCommonOptions(parser)
        parser.add_option("--output", "-o", help="Write output to specified log file",
                          type="str")

        self.createLogOptions(parser)

        self.options, self.program = parser.parse_args()

        if self.options.pid is None and not self.program:
            parser.print_help()
            exit(1)

        # Create "only" filter
        self.only = set(['read', 'accept', 'connect', 'close', 'recvfrom', 'write', 'recvmsg', 'sendto', 'sendmsg'])

        self.processOptions()

    def ignoreSyscall(self, syscall):
        name = syscall.name
        if self.only and (name not in self.only):
            return True
        return False

    def find_parent_pid(self, syscall):
        if syscall.process.is_thread:
            parent = syscall.process.parent
            while parent.is_thread and parent.parent is not None:
                # Follow hierarchy of pid processes
                parent = parent.parent
            return parent.pid
        else:
            return syscall.process.pid

    def displaySyscall(self, syscall):
        # Save the file descriptor associated with the socket.
        if syscall.name == 'accept':
            fd = syscall.result
        else:
            if syscall.name == 'read' or syscall.name == 'write' or syscall.name == 'close' or syscall.name == 'recvmsg':
                name = 'fd'
            else:
                name = 'sockfd'

            fd = syscall[name].value

        # Find connection associated with the socket descriptor.
        p = psutil.Process(syscall.process.pid)
        pc = None
        for c in p.connections():
            if c.fd == fd:
                pc = c

        pid = self.find_parent_pid(syscall)

        # Update the list of socketfds
        if syscall.name == 'accept' or syscall.name == 'connect':
            open_fds = self.sockfd.get(pid, {})
            open_fds[fd] = pc
            self.sockfd[pid] = open_fds

        data = OrderedDict()
        data['data'] = {}
        if syscall.name not in self.exit_syscalls:
            data['timestamp'] = syscall.__enter_timestamp
        else:
            data['timestamp'] = syscall.__exit_timestamp
        data['pid'] = pid
        data['thread'] = syscall.process.pid
        data['type'] = syscall.name.upper()
        data['data']['syscall'] = syscall.name
        data['data']['enter_timestamp'] = syscall.__enter_timestamp
        data['data']['exit_timestamp'] = syscall.__exit_timestamp
        data['data']['syscall_exit'] = syscall.result
        data['data']['fd'] = fd

        # Register a close event, when socket is closed.
        if not pc:
            if syscall.name == 'close' and \
                syscall.process.pid in self.sockfd and \
                fd in self.sockfd[pid]:
                pc = self.sockfd[pid][fd]
                del self.sockfd[pid][fd]
            else:
                return

        if pc.type == 2:
            # Ignore UDP messages
            return

        socket_id = self._socket_id(pc)
        data['socket'] = socket_id
        data['data']['socket_type'] = pc.type

        # Register a write or send event.
        if syscall.name in set(['write', 'sendmsg', 'sendto']):
            data['type'] = 'SND'
            data['src'] = pc.laddr[0]
            data['src_port'] = pc.laddr[1]
            data['dst'] = pc.raddr[0]
            data['dst_port'] = pc.raddr[1]
            data['size'] = syscall.result
        elif syscall.name in set(['read', 'recvmsg', 'recvfrom']):
            data['src'] = pc.raddr[0]
            data['src_port'] = pc.raddr[1]
            data['dst'] = pc.laddr[0]
            data['dst_port'] = pc.laddr[1]

            if syscall.result > 0:
                data['type'] = 'RCV'
                data['size'] = syscall.result
            else:
                return

        self.log.append(data)

    def _socket_id(self, pc):
        if len(pc.raddr) == 0:
            return "%s:%d-" % (pc.laddr[0],pc.laddr[1])

        src_ip = map(int, pc.laddr[0].split('.'))
        dst_ip = map(int, pc.raddr[0].split('.'))

        inverse = pc.laddr[1] > pc.raddr[1] if src_ip == dst_ip else src_ip > dst_ip

        return "%s:%d-%s:%d" % (pc.laddr[0],pc.laddr[1],pc.raddr[0],pc.raddr[1]) if not inverse \
            else "%s:%d-%s:%d" % (pc.raddr[0],pc.raddr[1],pc.laddr[0],pc.laddr[1])

    def syscallTrace(self, process):
        # First query to break at next syscall
        self.prepareProcess(process)

        while True:
            # No more process? Exit
            if not self.debugger:
                break

            # Wait until next syscall enter
            try:
                event = self.debugger.waitSyscall()
            except ProcessExit as event:
                self.processExited(event)
                continue
            except ProcessSignal as event:
                event.process.syscall(event.signum)
                continue
            except NewProcessEvent as event:
                self.newProcess(event)
                continue
            except ProcessExecution as event:
                self.processExecution(event)
                continue

            # Process syscall enter or exit
            self.syscall(event.process)

    def syscall(self, process):
        state = process.syscall_state
        syscall = state.event(self.syscall_options)
        if syscall and state.next_event == "exit":
            syscall.__enter_timestamp = time_ms()
        if syscall and (syscall.result is not None):
            syscall.__exit_timestamp = time_ms()
            self.displaySyscall(syscall)

        # Break at next syscall
        process.syscall()



    def processExited(self, event):
        # Display syscall which has not exited
        state = event.process.syscall_state
        if (state.next_event == "exit") \
                and state.syscall:
            self.displaySyscall(state.syscall)

        # Display exit message
        self.log.append({
            'pid': self.find_parent_pid(event),
            'thread': event.process.pid,
            'timestamp': time_ms(),
            'type': 'END',
            'data': {
                'event': "%s" % event
            }
        })
        #error("%f *** %s ***" % (time_ms(), event))

    def prepareProcess(self, process):
        process.syscall()
        process.syscall_state.ignore_callback = self.ignoreSyscall

    def newProcess(self, event):
        process = event.process
        #error("%f *** New process %s ***" % (time_ms(), process.pid))
        self.log.append({
            'pid': self.find_parent_pid(event),
            'thread': process.pid,
            'type': 'START',
            'timestamp': time_ms()
        })
        self.prepareProcess(process)
        process.parent.syscall()

    def processExecution(self, event):
        process = event.process
        error("%f *** Process %s execution ***" % (time_ms(),process.pid))
        process.syscall()

    def runDebugger(self):
        # Create debugger and traced process
        self.setupDebugger()
        process = self.createProcess()
        if not process:
            return

        self.syscall_options = FunctionCallOptions()

        self.syscallTrace(process)

    def main(self):
        self._main()
        if self._output is not None:
            self._output.close()
        self.log.sort(key=lambda x: x['timestamp'])
        for p in self.log: error(json.dumps(p))

    def _main(self):
        self.debugger = PtraceDebugger()
        self.debugger.traceClone()
        try:
            self.runDebugger()
        except ProcessExit as event:
            self.processExited(event)
        except PtraceError as err:
            error("ptrace() error: %s" % err)
        except KeyboardInterrupt:
            error("Interrupted.")
        except PTRACE_ERRORS as err:
            writeError(getLogger(), err, "Debugger error")
        self.debugger.quit()

    def createChild(self, program):
        pid = Application.createChild(self, program)
        #error("%f *** Process %s ***" % (time_ms(), pid))
        return pid


if __name__ == "__main__":
    NetTracer().main()
