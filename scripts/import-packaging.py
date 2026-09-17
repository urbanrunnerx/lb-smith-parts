"""Extract factual base/description evidence from Ford's explicit CSV download.

Usage: python scripts/import-packaging.py /path/to/packagingdata.csv
No generated base numbers, range expansion, or service suffix count inflation.
"""
import argparse, csv, hashlib, json, re
from collections import Counter, defaultdict
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(); p.add_argument('csv'); p.add_argument('--date',default='2026-09-17'); args=p.parse_args()
path=Path(args.csv); groups={}; excluded=Counter(); total=0
with path.open(encoding='utf-8-sig',newline='') as f:
 for r in csv.DictReader(f):
  total+=1
  prefix,base,suffix,part,name=(r[k].strip() for k in ['Prefix','Base','Suffix','Service Part Number','Part Description'])
  # Exclude standalone FINIS identifiers and hardware numbers without a conventional prefix.
  if not re.fullmatch(r'[A-Z0-9]{4}',prefix): excluded['nonconventional_prefix']+=1; continue
  if not re.fullmatch(r'[0-9][A-Z0-9]{3,7}',base): excluded['nonconventional_base']+=1; continue
  if not name: excluded['missing_description']+=1; continue
  if re.sub(r'[- ]','',part)!=prefix+base+suffix: excluded['reconstruction_mismatch']+=1; continue
  g=groups.setdefault(base,{'base':base,'descriptions':Counter(),'examples':[],'serviceCount':0})
  g['descriptions'][name]+=1; g['serviceCount']+=1
  if len(g['examples'])<3 and part not in g['examples']: g['examples'].append(part)
entries=[]
for b,g in sorted(groups.items()):
 g['descriptions']=[{'name':k,'count':v} for k,v in g['descriptions'].most_common()]
 entries.append(g)
out={'source':'https://upccrossreference.cdis3.com/downloadfiles/packagingdata.csv','landingPage':'https://upccrossreference.cdis3.com/default.asp?p=8u%24D%21T44bELeW','sourceName':'Ford / Motorcraft packaging cross-reference','retrievedAt':args.date,'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'inputRows':total,'excludedRows':dict(excluded),'distinctBases':len(entries),'entries':entries}
(ROOT/'data/packaging-reference.json').write_text(json.dumps(out,separators=(',',':'))+'\n',encoding='utf-8')
print(json.dumps({k:v for k,v in out.items() if k!='entries'},indent=2))
