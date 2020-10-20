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

async def ner_text(text):
    url = 'http://localhost:8090/service/grobid'

    headers = {'content-type': 'application/json', 'accept': 'application/json'}
    async with aiohttp.ClientSession(timeout=aiohttp.ClientTimeout(total=12000)) as session:
        async with session.post(url, data=text, headers=headers) as resp:
            try:
                resp.raise_for_status()
                resp_json = await resp.json()
                print(resp_json)
            except aiohttp.client_exceptions.ClientResponseError as e:
                print(e.__class__, e.status, e.message)

async def ner_file(path, dtype):
    if dtype == "indeed":
        data = []
        with open(path) as f:
            for line in f:
                js = json.loads(line)
                text = js['jobTitle'] + js["jobDescription"]
                await ner_text(text)


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

    #parser.add_argument('-t', '--textfield',
    #                    action='append',
    #                    help='list of text field will extract',
    #                    required=True)
    #parser.add_argument('-c', '--companyfield', default='companyName', type=str, help='company text field')

    args = parser.parse_args()

    # uvloop.install()
    asyncio.run(ner_file(args.datapath, "indeed"))
    print('Done extracting!')

# python async_extract_file.py -t jobDescription -t jobTitle
