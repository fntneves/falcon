import os
PATH = os.path.dirname(os.path.abspath(__file__))

def get_full_path(*path):
    return os.path.join(PATH, '..', *path)
