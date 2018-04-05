# Falcon Tracer
Falcon Tracer is a eBPF-based tool that trace processes in order to collect events to be processed in Falcon's pipeline.

This tracer mainly collects events related to socket connections (connect, accept, send, receive) and process events (start, end, fork, join).
It uses *kprobes* in order to instrument specific kernel functions and gather the revelant data.

## Requirements
- Kernel 4.8+
  - *Ubuntu:* `apt-get -y install linux-image-4.8.0-41-generic linux-image-extra-4.8.0-41-generic`

## Installation

The installation process requires `sudo` permissions.

- [Install BCC tools](https://github.com/iovisor/bcc/blob/master/INSTALL.md)
- Run `make`

## Usage
- Run `sudo ./bin/falcon-tracer` on your terminal.
