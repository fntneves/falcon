#include <uapi/linux/ptrace.h>
#include <net/sock.h>
#include <linux/if.h>
#include <linux/net.h>
#include <linux/netdevice.h>
#include <linux/sched.h>
#include <linux/socket.h>
#include <linux/types.h>

#define PID_FILTER //PID_FILTER//
#define COMM_FILTER //COMM_FILTER//

enum event_type {
    SOCKET_SEND = 8,
    SOCKET_RECEIVE = 9,
    SOCKET_CONNECT = 11,
    SOCKET_ACCEPT = 12,

    PROCESS_CREATE = 1,
    PROCESS_START = 2,
    PROCESS_END = 3,
    PROCESS_JOIN = 4,

    FSYNC = 13,
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
    u64 ktime;
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

struct pid_info_t
{
    u32 kpid;
    char comm[TASK_COMM_LEN];
};

// The following structures are used to keep data while switching from enter
// to exit syscall points, for instance to keep function variables that
// would be needed later. At the end, this structure should be empty.
BPF_HASH(entry_timestamps, u32, u64);
BPF_HASH(sock_handlers, u32, struct sock *);
BPF_HASH(trace_pids, u32, char);

// The next structure holds information about processes that are being created
// so we can provide full and accuracte information about their parents.
// child_pid -> ktime of fork()
BPF_HASH(forked_pids, u32, u64);
BPF_HASH(exited_pids, u32, u64);

// This structure stores the socket events.
BPF_PERF_OUTPUT(events);

// Helper functions.
struct pid_info_t static pid_info()
{
    struct pid_info_t process_info = {};
    process_info.kpid = bpf_get_current_pid_tgid();
    bpf_get_current_comm(&process_info.comm, sizeof(process_info.comm));

    return process_info;
}

int static comm_equals(const char str1[TASK_COMM_LEN], const char str2[TASK_COMM_LEN])
{
    if (str1[0] != str2[0])
        return 0;
    if (str1[1] != str2[1])
        return 0;
    if (str1[2] != str2[2])
        return 0;
    if (str1[3] != str2[3])
        return 0;
    if (str1[4] != str2[4])
        return 0;
    if (str1[5] != str2[5])
        return 0;
    if (str1[6] != str2[6])
        return 0;
    if (str1[7] != str2[7])
        return 0;
    if (str1[8] != str2[8])
        return 0;
    if (str1[9] != str2[9])
        return 0;
    if (str1[10] != str2[10])
        return 0;
    if (str1[11] != str2[11])
        return 0;
    if (str1[12] != str2[12])
        return 0;
    if (str1[13] != str2[13])
        return 0;
    if (str1[14] != str2[14])
        return 0;
    if (str1[15] != str2[15])
        return 0;

    return 1;
}

int static skip_comm_struct(struct pid_info_t process_info)
{
    char comm[TASK_COMM_LEN];

    if (COMM_FILTER != NULL)
    {
        strcpy(comm, COMM_FILTER);

        return comm_equals(comm, process_info.comm) == 0;
    }

    return 0;
}

int static skip_pid(u32 pid)
{
    if (PID_FILTER == 0 || trace_pids.lookup(&pid) != NULL || pid == PID_FILTER)
    {
        return 0;
    }

    return 1;
}

int static skip_pid_struct(struct pid_info_t process_info)
{
    return skip_pid(process_info.kpid);
}

int static skip(struct pid_info_t process_info)
{
    if (PID_FILTER != NULL && COMM_FILTER != NULL)
    {
        return skip_pid_struct(process_info) && skip_comm_struct(process_info);
    }

    if (PID_FILTER != NULL)
    {
        return skip_pid_struct(process_info);
    }

    if (COMM_FILTER != NULL)
    {
        return skip_comm_struct(process_info);
    }

    return 0;
}

void static emit_socket_connect(struct pt_regs *ctx, u64 timestamp, struct socket_info_t *skp)
{
    struct event_info_t event = {
        .type = SOCKET_CONNECT,
        .pid = bpf_get_current_pid_tgid(),
        .tgid = bpf_get_current_pid_tgid() >> 32
    };
    event.ktime = timestamp;
    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.socket = *skp;

    events.perf_submit(ctx, &event, sizeof(event));
}

void static emit_socket_accept(struct pt_regs *ctx, u64 timestamp, struct socket_info_t *skp)
{
    struct event_info_t event = {
        .type = SOCKET_ACCEPT,
        .pid = bpf_get_current_pid_tgid(),
        .tgid = bpf_get_current_pid_tgid() >> 32
    };
    event.ktime = timestamp;
    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.socket = *skp;

    events.perf_submit(ctx, &event, sizeof(event));
}

void static emit_socket_send(struct pt_regs *ctx, u64 timestamp, struct socket_info_t *skp, int bytes)
{
    struct event_info_t event = {
        .type = SOCKET_SEND,
        .pid = bpf_get_current_pid_tgid(),
        .tgid = bpf_get_current_pid_tgid() >> 32
    };
    event.ktime = timestamp;
    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.socket = *skp;
    event.bytes = bytes;

    events.perf_submit(ctx, &event, sizeof(event));
}

void static emit_socket_receive(struct pt_regs *ctx, u64 timestamp, struct socket_info_t *skp, int bytes)
{
    struct event_info_t event = {
        .type = SOCKET_RECEIVE,
        .pid = bpf_get_current_pid_tgid(),
        .tgid = bpf_get_current_pid_tgid() >> 32
    };
    event.ktime = timestamp;
    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.socket = *skp;
    event.bytes = bytes;

    events.perf_submit(ctx, &event, sizeof(event));
}

void static emit_process_create(struct pt_regs *ctx, u64 timestamp, pid_t child_pid)
{
    char zero = ' ';
    trace_pids.insert(&child_pid, &zero);

    struct event_info_t event = {
        .type = PROCESS_CREATE,
        .pid = bpf_get_current_pid_tgid(),
        .tgid = bpf_get_current_pid_tgid() >> 32
    };
    event.ktime = timestamp;
    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.child_pid = child_pid;

    events.perf_submit(ctx, &event, sizeof(event));
}

void static emit_process_start(struct pt_regs *ctx, u64 timestamp, u32 pid, u32 tgid)
{
    struct event_info_t event = {
        .type = PROCESS_START,
        .pid = pid,
        .tgid = tgid
    };
    event.ktime = timestamp + 1;
    bpf_get_current_comm(&event.comm, sizeof(event.comm));

    events.perf_submit(ctx, &event, sizeof(event));
}

void static emit_process_end(struct pt_regs *ctx, u64 timestamp)
{
    struct event_info_t event = {
        .type = PROCESS_END,
        .pid = bpf_get_current_pid_tgid(),
        .tgid = bpf_get_current_pid_tgid() >> 32
    };
    event.ktime = timestamp;
    bpf_get_current_comm(&event.comm, sizeof(event.comm));

    events.perf_submit(ctx, &event, sizeof(event));
}

void static emit_process_join(struct pt_regs *ctx, u64 timestamp, pid_t child_pid)
{
    struct event_info_t event = {
        .type = PROCESS_JOIN,
        .pid = bpf_get_current_pid_tgid(),
        .tgid = bpf_get_current_pid_tgid() >> 32
    };
    event.ktime = timestamp + 1;
    bpf_get_current_comm(&event.comm, sizeof(event.comm));
    event.child_pid = child_pid;

    trace_pids.delete(&child_pid);

    events.perf_submit(ctx, &event, sizeof(event));
}

void static emit_fsync(struct pt_regs *ctx, u64 timestamp)
{
    struct event_info_t event = {
        .type = FSYNC,
        .pid = bpf_get_current_pid_tgid(),
        .tgid = bpf_get_current_pid_tgid() >> 32
    };
    event.ktime = timestamp;
    bpf_get_current_comm(&event.comm, sizeof(event.comm));

    events.perf_submit(ctx, &event, sizeof(event));
}

struct socket_info_t static socket_info(struct sock * skp) {
    u16 sport = 0;
    u16 dport = 0;
    u16 family = 0;
    short type = 0;
    struct socket_info_t sk = {};

    bpf_probe_read(&family, sizeof(family), &skp->sk_family);

    if (family == AF_INET) {
        u32 saddr = 0;
        u32 daddr = 0;

        bpf_probe_read(&saddr, sizeof(saddr), &skp->sk_rcv_saddr);
        bpf_probe_read(&daddr, sizeof(daddr), &skp->sk_daddr);

        sk.saddr[1] = saddr;
        sk.daddr[1] = daddr;
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
    struct pid_info_t proc_info = pid_info();

    if (skip(proc_info)) {
        return 1;
    }

    u64 timestamp = bpf_ktime_get_ns();
    entry_timestamps.update(&kpid, &timestamp);

    // Stash the current sock for the exit call.
    if (sk->sk_family == AF_INET || sk->sk_family == AF_INET6)
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
    u64 *timestamp = entry_timestamps.lookup(&kpid);

    if (error >= 0 && skpp != NULL && timestamp != NULL) {
        struct sock * skp = *skpp;
        struct socket_info_t sk = socket_info(skp);
        emit_socket_connect(ctx, *timestamp, &sk);
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
    struct pid_info_t proc_info = pid_info();

    if (skip(proc_info)) {
        return 1;
    }

    struct sock * skp = (struct sock *) PT_REGS_RC(ctx);

    if (skp != NULL)
    {
        struct socket_info_t sk = socket_info(skp);
        emit_socket_accept(ctx, bpf_ktime_get_ns(), &sk);
    }

    return 0;
}

/**
 * Probe "sock_sendmsg" at the entry point.
 */
int entry__sock_sendmsg(struct pt_regs *ctx, struct socket * sock) {
    u32 kpid = bpf_get_current_pid_tgid();
    struct pid_info_t proc_info = pid_info();

    if (skip(proc_info)) {
        return 1;
    }

    u64 timestamp = bpf_ktime_get_ns();
    entry_timestamps.update(&kpid, &timestamp);

    // Stash the current sock for the exit call.
    struct sock * sk = sock->sk;

    if (sk->sk_family == AF_INET || sk->sk_family == AF_INET6)
        sock_handlers.update(&kpid, &sk);

    return 0;
}

/**
 * Probe "sock_sendmsg" at the exit point.
 */
int exit__sock_sendmsg(struct pt_regs *ctx) {
    u32 kpid = bpf_get_current_pid_tgid();
    int sent_bytes = PT_REGS_RC(ctx);
    struct sock ** skpp = sock_handlers.lookup(&kpid);
    u64 *timestamp = entry_timestamps.lookup(&kpid);

    if (sent_bytes > 0 && skpp != NULL && timestamp != NULL) {
        struct sock *skp = *skpp;
        struct socket_info_t sk = socket_info(skp);
        emit_socket_send(ctx, *timestamp, &sk, sent_bytes);
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
    struct pid_info_t proc_info = pid_info();

    if (skip(proc_info)) {
        return 1;
    }

    u64 timestamp = bpf_ktime_get_ns();
    entry_timestamps.update(&kpid, &timestamp);

    // Stash the current sock for the exit call.
    struct sock *sk = sock->sk;

    if (sk->sk_family == AF_INET || sk->sk_family == AF_INET6)
        sock_handlers.update(&kpid, &sk);

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
    u64 *timestamp = entry_timestamps.lookup(&kpid);

    if (read_bytes > 0 && skpp != NULL && timestamp != NULL)
    {
        struct sock *skp = *skpp;
        struct socket_info_t sk = socket_info(skp);
        emit_socket_receive(ctx, *timestamp, &sk, read_bytes);
    }
    else if (read_bytes <= 0)
    {
        // Something went wrong.
    }

    sock_handlers.delete(&kpid);

    return 0;
}

/**
 * Handle forks.
 */
struct sched_process_fork
{
    u64 __unused__;
    char parent_comm[16];
    pid_t parent_pid;
    char child_comm[16];
    pid_t child_pid;
};

int on_fork(struct sched_process_fork * args)
{
    struct pid_info_t process_info = {};
    process_info.kpid = args->parent_pid;
    bpf_probe_read(&process_info.comm, sizeof(process_info.comm), &args->parent_comm);

    if (skip(process_info))
    {
        return 1;
    }

    u32 child_kpid = args->child_pid;
    u64 fork_ktime = bpf_ktime_get_ns();
    forked_pids.insert(&child_kpid, &fork_ktime);

    emit_process_create((struct pt_regs *)args, fork_ktime, child_kpid);

    return 0;
}

/**
 * Probe "wake_up_new_task" at the entry point.
 */
int entry__wake_up_new_task(struct pt_regs *ctx, struct task_struct *p)
{
    u32 kpid = p->pid;

    struct pid_info_t process_info = {};
    process_info.kpid = p->pid;
    bpf_probe_read(&process_info.comm, sizeof(process_info.comm), &p->comm);

    if (skip(process_info))
    {
        return 1;
    }

    u64 *fork_ktime = forked_pids.lookup(&kpid);
    u64 fork_actual_ktime = 0;

    if (fork_ktime == NULL) {
        fork_actual_ktime = bpf_ktime_get_ns();
    } else {
        fork_actual_ktime = *fork_ktime;
        forked_pids.delete(&kpid);
    }

    emit_process_start(ctx, fork_actual_ktime, kpid, p->tgid);

    return 0;
}

/**
 * Handle process termination.
 */
struct sched_process_exit
{
    u64 __unused__;
    char comm[16];
    pid_t pid;
};

int on_exit(struct sched_process_exit *args)
{
    struct pid_info_t process_info = {};
    process_info.kpid = args->pid;
    bpf_probe_read(&process_info.comm, sizeof(process_info.comm), &args->comm);

    if (skip(process_info))
    {
        return 1;
    }

    u32 child_kpid = args->pid;
    u64 exit_ktime = bpf_ktime_get_ns();
    exited_pids.insert(&child_kpid, &exit_ktime);

    emit_process_end((struct pt_regs *)args, exit_ktime);

    return 0;
}

/**
 * Probe "do_wait" at the exit point.
 */
int exit__do_wait(struct pt_regs *ctx)
{
    u32 kpid = bpf_get_current_pid_tgid();
    struct pid_info_t proc_info = pid_info();

    if (skip(proc_info)) {
        return 1;
    }

    pid_t exited_pid = PT_REGS_RC(ctx);

    u64 *exit_ktime = exited_pids.lookup(&exited_pid);

    if (exit_ktime != NULL)
    {
        emit_process_join(ctx, *exit_ktime, exited_pid);
        exited_pids.delete(&exited_pid);
    }

    return 0;
}

/**
 * Handle fsyncs.
 */
int exit__sys_fsync(struct pt_regs *ctx)
{
    u32 kpid = bpf_get_current_pid_tgid();
    int return_value = PT_REGS_RC(ctx);
    struct pid_info_t proc_info = pid_info();

    if (skip(proc_info) || return_value < 0)
    {
        return 1;
    }

    u64 fsync_time = bpf_ktime_get_ns();
    emit_fsync(ctx, fsync_time);

    return 0;
}
