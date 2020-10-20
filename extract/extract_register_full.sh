#!/bin/bash 
OUTPUT=/root/output_register_full_comp
INPUT=/home/thang.duong/data/register/

python async_extract_file.py -d $INPUT -t purpose -o $OUTPUT
#python async_extract_file.py -d $INPUT/register/ -t purpose -o $OUTPUT
