# -*- coding: utf-8 -*-

# Learn more: https://github.com/kennethreitz/setup.py

from setuptools import setup, find_packages

EXCLUDE_FROM_PACKAGES = ['falcon.bin']

with open('README.md') as f:
    readme = f.read()

with open('LICENSE') as f:
    license = f.read()

setup(
    name='falcon-tracer',
    version='0.1.0',
    description='',
    author='Francisco Neves',
    author_email='francisco.t.neves@inesctec.pt',
    url='https://github.com/fntneves/falcon',
    license=license,
    packages=find_packages(exclude=EXCLUDE_FROM_PACKAGES),
    install_requires=[
        'ujson',
        'kafka',
        'confluent-kafka',
        'flatbuffers',
        'python-dotenv',
        'sortedcontainers'
    ],
    test_suite='nose.collector',
    tests_require=['nose'],
    include_package_data=True,
    scripts=['falcon/bin/falcon-tracer.py'],
    entry_points={'console_scripts': [
        'falcon-tracer = falcon.core.tracer:main',
    ]}
)

