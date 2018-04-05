# -*- coding: utf-8 -*-

# Learn more: https://github.com/kennethreitz/setup.py

from setuptools import setup, find_packages


with open('README.md') as f:
    readme = f.read()

with open('LICENSE') as f:
    license = f.read()

setup(
    name='falcon-tracer',
    version='0.1.0',
    description='',
    long_description=readme,
    author='Francisco Neves',
    author_email='francisco.t.neves@inesctec.pt',
    url='https://github.com/fntneves/falcon',
    license=license,
    packages=find_packages(exclude=('tests', 'docs')),
    include_package_data=True
)

