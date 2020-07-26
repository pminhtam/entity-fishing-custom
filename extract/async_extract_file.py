import argparse
from glob import glob
import os
import json
import aiohttp
import asyncio
from itertools import islice
# import uvloop
from requests_toolbelt.multipart.encoder import MultipartEncoder
from aiohttp import FormData

async def entity_fishing(input_path, output_path,text_field, company_field):
    url = 'http://localhost:8090/service/disambiguate'
    query = {"text_field": text_field, "company_field": company_field}

    query["input"] = input_path
    query["output"] = output_path

    wikidata_ids = []
    query_json = json.dumps(query)
    multipart_data = FormData()
    # multipart_data = MultipartEncoder(
    #     fields={
    #         'query': query_json
    #     }
    # )
    # multipart_data.add_field("query",query_json)
    # multipart_data.add_field("output",output_path)
    # multipart_data.add_field("text_field",text_field)
    multipart_data.add_field('query', query_json, content_type='multipart/form-data')
    async with aiohttp.ClientSession(timeout=aiohttp.ClientTimeout(total=12000)) as session:
        async with session.post(url, data=multipart_data) as resp:
            try:
                resp.raise_for_status()
                resp_json = await resp.json()
            except aiohttp.client_exceptions.ClientResponseError as e:
                print(e.__class__, e.status, e.message, query)

    return wikidata_ids

async def async_extract_all(datapath, outputpath,textfield, companyfield):
    output_dir = os.path.join(datapath, outputpath)
    raw_dir = os.path.join(datapath, '**')
    log_dir = 'log/'


    entry_list = []
    # task_list = []

    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    if not os.path.exists(log_dir):
        os.makedirs(log_dir)

    for filepath in sorted(glob(raw_dir, recursive=True), reverse=True):
        print(filepath)
        if os.path.isdir(filepath): continue
        current_ds = filepath.split('/')[-2]
        filename = os.path.basename(filepath)
        output_file_base = os.path.join(output_dir,current_ds)
        if not os.path.exists(output_file_base):
            os.makedirs(output_file_base)
        if filename == '_SUCCESS': continue
        await entity_fishing(input_path = filepath,output_path = os.path.join(output_dir,current_ds,filename+'-extracted.json'),text_field=textfield, company_field=companyfield)


import time
if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-d', '--datapath', 
                        default='../data',
                        type=str,
                        help='path to data directory (default is "../data")')
    parser.add_argument('-o', '--outputpath',
                        default='extracted/',
                        type=str,
                        help='path to output directory (default is "extracted/")')

    parser.add_argument('-t', '--textfield',
                        action='append',
                        help='list of text field will extract',
                        required=True)
    #parser.add_argument('-c', '--companyfield', default='companyName', type=str, help='company text field')

    args = parser.parse_args()

    # uvloop.install()
    start = time.perf_counter()
    current_ds = args.datapath.split('/')[-2]
    companyfield = ""
    if current_ds == "patent":
        companyfield = "company_name"
    else:
        companyfield = "companyName"
    print(current_ds)
    asyncio.run(async_extract_all(args.datapath,args.outputpath,args.textfield, companyfield))
    stop = time.perf_counter()
    print(stop-start)
    print('Done extracting!')

# python async_extract_file.py -t jobDescription -t jobTitle
