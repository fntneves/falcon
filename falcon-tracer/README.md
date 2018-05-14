# Falcon Tracer
Falcon Tracer is an eBPF-based tool for tracing a tree of processes in order to collect events for further processing in the Falcon's pipeline.

This tracer instruments specific kernel functions (using *kprobes*) for gathering the revelant data. It mainly collects events related to socket connections (connect, accept, send, receive) and process events (start, end, fork, join).

## Requirements
- Kernel 4.8+
- [BCC tools](https://github.com/iovisor/bcc/blob/master/INSTALL.md)

## Installation
The installation process requires `sudo` permissions.

- [Install BCC tools](https://github.com/iovisor/bcc/blob/master/INSTALL.md)
- Run `sudo pip install -e .` to install it locally and to make `falcon-tracer` available globally.

## Usage

### **Trace a new process by command.**

To start tracing a new process and the subsequent process tree, we recommend to use this approach.

```bash
sudo falcon-tracer command

# Example
sudo falcon-tracer ls -l
sudo falcon-tracer wget http://google.pt
```

### **Trace a currently running process.**

Sometimes, you want to trace the behavior of a already running process. However, please note that the process tree of the running process will not be traced.

```bash
sudo falcon-tracer --pid pid

# Example
sudo falcon-tracer --pid 123
```

---

See all the available options by running `falcon-tracer --help` in your terminal.
