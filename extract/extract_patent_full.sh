#!/bin/bash 
OUTPUT=/root/output_patent_full_comp
INPUT=/home/thang.duong/data/patent/

python async_extract_file.py -d $INPUT -t abstract -t title -o $OUTPUT
#python async_extract_file.py -d $INPUT/register/ -t purpose -o $OUTPUT
