# Falcon Tracer
Falcon Tracer is an eBPF-based tool for tracing a tree of processes in order to collect events for further processing in the Falcon's pipeline.

This tracer instruments specific kernel functions (using *kprobes*) for gathering the revelant data. It mainly collects events related to socket connections (connect, accept, send, receive) and process events (start, end, fork, join).

## Requirements
- Kernel 4.8+
  - *Ubuntu:* `sudo apt install linux-image-4.8.0-41-generic linux-image-extra-4.8.0-41-generic`

## Installation
The installation process requires `sudo` permissions.

- [Install BCC tools](https://github.com/iovisor/bcc/blob/master/INSTALL.md)
- Run `sudo pip install -e .` to install it locally.

## Usage
- Run `sudo falcon-tracer --help` in your terminal to see all the available options.
