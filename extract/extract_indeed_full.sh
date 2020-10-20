#!/bin/bash 
OUTPUT=/root/output_indeed_full_comp
INPUT=/home/thang.duong/data/indeed/extracted/

python async_extract_file.py -d $INPUT -t jobDescription -t jobTitle -o $OUTPUT
#python async_extract_file.py -d $INPUT/register/ -t purpose -o $OUTPUT
