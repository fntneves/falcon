import os

def check_pid(pid):
    """ Check For the existence of a unix pid. """
    if pid is None:
        return False

    try:
        os.kill(pid, 0)
    except OSError:
        return False
    else:
        return True

def clean_pid(file):
    try:
        os.remove(file)
    except:
        pass

def write_pid(file):
    with open(file, 'w') as f:
        pid = str(os.getpid())
        f.write(pid)

def read_pid(file):
    try:
        with open(file, 'r') as f:
            pid = f.readline()
            return int(pid)
    except:
        return None
