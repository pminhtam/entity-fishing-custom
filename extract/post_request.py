import requests
from requests_toolbelt.multipart.encoder import MultipartEncoder
import json

# json_data = {
#     "input": "/hdd/tam/entity-fishing/data_crawl/processed/20190101T001000/dataset.json/part-r-00000-6f0aa2af-50e7-4f27-9cb8-995dcfa232e0",
#     "output": "/hdd/tam/entity-fishing/data_crawl/processed/20190101T001000/dataset.json/result222_paralell-part-r-00000-6f0aa2af-50e7-4f27-9cb8-995dcfa232e0",
#     "text_field": ["jobDescription","jobTitle"],
# }
# response =requests.post('http://localhost:8090/service/disambiguate', json=json_data)
# print("Status code: ", response.status_code)
# print("Printing Entire Post Request")

query = {"text_field": ["jobDescription","jobTitle"]}

query["input"] = "/root/part-r-00000-6f0aa2af-50e7-4f27-9cb8-995dcfa232e0"
# query["output"] = "/hdd/tam/entity-fishing/data_crawl/processed/20190101T001000/dataset.json/result222_paralell-part-r-00000-6f0aa2af-50e7-4f27-9cb8-995dcfa232e0"
query["output"] = "/root/part-r-00000-6f0aa2af-50e7-4f27-9cb8-995dcfa232e0-extracted.json"
# input_ = "/hdd/tam/entity-fishing/data_crawl/processed/20190101T001000/dataset.json/part-r-00000-6f0aa2af-50e7-4f27-9cb8-995dcfa232e0"
# output = "/hdd/tam/entity-fishing/data_crawl/processed/20190101T001000/dataset.json/result222_paralell-part-r-00000-6f0aa2af-50e7-4f27-9cb8-995dcfa232e0"
# qqq = """{
#                 "input": "/hdd/tam/entity-fishing/data_crawl/processed/20190101T001000/dataset.json/part-r-00000-6f0aa2af-50e7-4f27-9cb8-995dcfa232e0",
#                 "output": "/hdd/tam/entity-fishing/data_crawl/processed/20190101T001000/dataset.json/result222_paralell-part-r-00000-6f0aa2af-50e7-4f27-9cb8-995dcfa232e0",
#                 "text_field": ["jobDescription","jobTitle"],
#             }"""
# multipart_data = MultipartEncoder(
#     fields={
#         'query' :"""{
#                 "input": "/hdd/tam/entity-fishing/data_crawl/processed/20190101T001000/dataset.json/part-r-00000-6f0aa2af-50e7-4f27-9cb8-995dcfa232e0",
#                 "output": "/hdd/tam/entity-fishing/data_crawl/processed/20190101T001000/dataset.json/result222_paralell-part-r-00000-6f0aa2af-50e7-4f27-9cb8-995dcfa232e0",
#                 "text_field": ["jobDescription","jobTitle"],
#             }"""
#            }
#     )
# print(qqq.format(input_,output))
y = json.dumps(query)
# print(y)
multipart_data = MultipartEncoder(
fields={
'query':y
}
)
# print(multipart_data.content_type)
print(str(query))
# print(qqq)

response = requests.post('http://localhost:8090/service/disambiguate', data=multipart_data,
                  headers={'Content-Type': multipart_data.content_type})
print(response)
