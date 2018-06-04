import os
import pkg_resources
from dotenv import load_dotenv

env_path = pkg_resources.resource_filename('falcon', '../conf/tracer.conf')
load_dotenv(dotenv_path=env_path)
