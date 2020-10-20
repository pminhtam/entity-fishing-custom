#!/bin/bash 
OUTPUT=/root/output_ner
INPUT=/home/thang.duong/Project/deploy2/data/raw

python async_extract_file.py -d $INPUT/indeed/processed/ -t jobDescription -t jobTitle -o $OUTPUT
python async_extract_file.py -d $INPUT/patent/ -t abstract -t title -o $OUTPUT
#python async_extract_file.py -d $INPUT/register/ -t purpose -o $OUTPUT
