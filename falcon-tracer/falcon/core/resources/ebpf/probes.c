#include <uapi/linux/ptrace.h>
#include <net/sock.h>
#include <linux/if.h>
#include <linux/net.h>
#include <linux/netdevice.h>
#include <linux/sched.h>
#include <linux/socket.h>
#include <linux/types.h>

#define PID_FILTER //PID_FILTER//

enum event_type {
    SOCKET_CONNECT = 101,
    SOCKET_ACCEPT = 102,
    SOCKET_SEND = 103,
    SOCKET_RECEIVE = 104,

    PROCESS_CREATE = 203,
    PROCESS_JOIN = 204,
};

struct socket_info_t {
    u16 sport;
    u16 dport;
    u64 saddr[2];
    u64 daddr[2];
    u16 family;
};

struct event_info_t {
    enum event_type type;
    u32 pid;
    u32 tgid;
    char comm[TASK_COMM_LEN];

    // SOCKET_* events
    struct socket_info_t socket;

    union {
        // SOCKET_SEND and SOCKET_RECEIVE events
        u32 bytes;

        // PROCESS_CREATE event
        u32 child_pid;
    };
};

// The following structures are used to keep data while switching from enter
// to exit syscall points, for instance to keep function variables that
// would be needed later. At the end, this structure should be empty.
BPF_HASH(sock_handlers, u32, struct sock *);
BPF_HASH(trace_pids, u32, char);

// This structure stores the socket events.
BPF_PERF_OUTPUT(process_events);
BPF_PERF_OUTPUT(socket_events);

// Helper functions.
int static skip_pid(u32 pid) {
    if (PID_FILTER == 0 || trace_pids.lookup(&pid) != NULL || pid == PID_FILTER) {
        return 0;
    }

    return 1;
}

void static emit_socket_connect(struct pt_regs *ctx, struct socket_info_t * skp) {
    struct event_info_t event = {
        .type = SOCKET_CONNECT,
        .pid = bpf_get_current_pid_tgid(),
        .tgid = bpf_get_current_pid_tgid() >> 32
    };

    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.socket = *skp;

    socket_events.perf_submit(ctx, &event, sizeof(event));
}

void static emit_socket_accept(struct pt_regs *ctx, struct socket_info_t *skp)
{
    struct event_info_t event = {
        .type = SOCKET_ACCEPT,
        .pid = bpf_get_current_pid_tgid(),
        .tgid = bpf_get_current_pid_tgid() >> 32
    };

    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.socket = *skp;

    socket_events.perf_submit(ctx, &event, sizeof(event));
}

void static emit_socket_send(struct pt_regs *ctx, struct socket_info_t *skp, int bytes)
{
    struct event_info_t event = {
        .type = SOCKET_SEND,
        .pid = bpf_get_current_pid_tgid(),
        .tgid = bpf_get_current_pid_tgid() >> 32
    };

    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.socket = *skp;
    event.bytes = bytes;

    socket_events.perf_submit(ctx, &event, sizeof(event));
}

void static emit_socket_receive(struct pt_regs *ctx, struct socket_info_t *skp, int bytes)
{
    struct event_info_t event = {
        .type = SOCKET_RECEIVE,
        .pid = bpf_get_current_pid_tgid(),
        .tgid = bpf_get_current_pid_tgid() >> 32
    };

    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.socket = *skp;
    event.bytes = bytes;

    socket_events.perf_submit(ctx, &event, sizeof(event));
}

void static emit_process_create(struct pt_regs *ctx, pid_t child_pid)
{
    char zero = ' ';
    trace_pids.insert(&child_pid, &zero);

    struct event_info_t event = {
        .type = PROCESS_CREATE,
        .pid = bpf_get_current_pid_tgid(),
        .tgid = bpf_get_current_pid_tgid() >> 32
    };

    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.child_pid = child_pid;

    process_events.perf_submit(ctx, &event, sizeof(event));
}

void static emit_process_join(struct pt_regs *ctx, pid_t child_pid)
{
    struct event_info_t event = {
        .type = PROCESS_JOIN,
        .pid = bpf_get_current_pid_tgid(),
        .tgid = bpf_get_current_pid_tgid() >> 32
    };

    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.child_pid = child_pid;

    trace_pids.delete(&child_pid);

    process_events.perf_submit(ctx, &event, sizeof(event));
}

struct socket_info_t static socket_info(struct sock * skp) {
    u16 sport = 0;
    u16 dport = 0;
    u16 family = 0;
    struct socket_info_t sk = {};

    bpf_probe_read(&family, sizeof(family), &skp->sk_family);

    if (family == AF_INET) {
        u32 saddr = 0;
        u32 daddr = 0;

        bpf_probe_read(&saddr, sizeof(saddr), &skp->sk_rcv_saddr);
        bpf_probe_read(&daddr, sizeof(daddr), &skp->sk_daddr);

        sk.saddr[0] = saddr;
        sk.daddr[0] = daddr;
    } else if (family == AF_INET6) {
        bpf_probe_read(sk.saddr, sizeof(sk.saddr), &skp->sk_v6_rcv_saddr);
        bpf_probe_read(sk.daddr, sizeof(sk.daddr), &skp->sk_v6_daddr);
    }

    bpf_probe_read(&sport, sizeof(sport), &skp->sk_num);
    bpf_probe_read(&dport, sizeof(dport), &skp->sk_dport);
    bpf_probe_read(&dport, sizeof(dport), &skp->sk_dport);

    sk.sport = sport;
    sk.dport = ntohs(dport);
    sk.family = family;

    return sk;
}

/**
 * Probe "tcp_connect" at the entry point.
 */
int entry__tcp_connect(struct pt_regs *ctx, struct sock * sk) {
    u32 kpid = bpf_get_current_pid_tgid();

    if (skip_pid(kpid)) {
        return 1;
    }

    sock_handlers.update(&kpid, &sk);

    return 0;
}

/**
 * Probe "tcp_connect" at the exit point.
 */
int exit__tcp_connect(struct pt_regs *ctx) {
    u32 kpid = bpf_get_current_pid_tgid();
    int error = PT_REGS_RC(ctx);
    struct sock ** skpp = sock_handlers.lookup(&kpid);

    if (error >= 0 && skpp != NULL) {
        struct sock * skp = *skpp;
        struct socket_info_t sk = socket_info(skp);
        emit_socket_connect(ctx, &sk);
    } else if (error < 0 ) {
        // Something went wrong, so do not register socket.
    }

    sock_handlers.delete(&kpid);

    return 0;
}

/**
 * Probe "inet_csk_accept" at the exit point.
 */
int exit__inet_csk_accept(struct pt_regs *ctx)
{
    u32 kpid = bpf_get_current_pid_tgid();

    if (skip_pid(kpid)) {
        return 1;
    }

    struct sock * skp = (struct sock *) PT_REGS_RC(ctx);

    if (skp != NULL)
    {
        struct socket_info_t sk = socket_info(skp);
        emit_socket_accept(ctx, &sk);
    }

    return 0;
}

/**
 * Probe "sock_sendmsg" at the entry point.
 */
int entry__sock_sendmsg(struct pt_regs *ctx, struct socket * sock, struct msghdr * msg) {
    u32 kpid = bpf_get_current_pid_tgid();

    if (skip_pid(kpid)) {
        return 1;
    }

    // Stash the current sock for the exit call.
    struct sock * skp = sock->sk;

    sock_handlers.update(&kpid, &skp);

    return 0;
}

/**
 * Probe "sock_sendmsg" at the exit point.
 */
int exit__sock_sendmsg(struct pt_regs *ctx) {
    u32 kpid = bpf_get_current_pid_tgid();
    int sent_bytes = PT_REGS_RC(ctx);
    struct sock ** skpp = sock_handlers.lookup(&kpid);

    if (sent_bytes > 0 && skpp != NULL) {
        struct sock *skp = *skpp;
        struct socket_info_t sk = socket_info(skp);
        emit_socket_send(ctx, &sk, sent_bytes);
    } else if (sent_bytes <= 0 ) {
        // Something went wrong.
    }

    sock_handlers.delete(&kpid);

    return 0;
}

/**
 * Probe "sock_recvmsg" at the entry point.
 */
int entry__sock_recvmsg(struct pt_regs *ctx, struct socket *sock, struct msghdr *msg)
{
    u32 kpid = bpf_get_current_pid_tgid();

    if (skip_pid(kpid)) {
        return 1;
    }

    // Stash the current sock for the exit call.
    struct sock *skp = sock->sk;

    sock_handlers.update(&kpid, &skp);

    return 0;
}

/**
 * Probe "sock_recvmsg" at the exit point.
 */
int exit__sock_recvmsg(struct pt_regs *ctx)
{
    u32 kpid = bpf_get_current_pid_tgid();
    int read_bytes = PT_REGS_RC(ctx);
    struct sock **skpp = sock_handlers.lookup(&kpid);

    if (read_bytes > 0 && skpp != NULL)
    {
        struct sock *skp = *skpp;
        struct socket_info_t sk = socket_info(skp);
        emit_socket_receive(ctx, &sk, read_bytes);
    }
    else if (read_bytes <= 0)
    {
        // Something went wrong.
    }

    sock_handlers.delete(&kpid);

    return 0;
}

/**
 * Probe "sys_clone" at the exit point.
 */
int exit__sys_clone(struct pt_regs *ctx)
{
    u32 kpid = bpf_get_current_pid_tgid();

    if (skip_pid(kpid)) {
        return 1;
    }

    pid_t new_tid = PT_REGS_RC(ctx);
    if (new_tid > 0) {
        emit_process_create(ctx, new_tid);
    }

    return 0;
}

/**
 * Probe "sys_wait" at the exit point.
 */
int exit__sys_wait(struct pt_regs *ctx)
{
    u32 kpid = bpf_get_current_pid_tgid();

    if (skip_pid(kpid)) {
        return 1;
    }

    pid_t exited_pid = PT_REGS_RC(ctx);

    if (exited_pid > 0) {
        emit_process_join(ctx, exited_pid);
    }

    return 0;
}
