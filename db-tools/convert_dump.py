#!/usr/bin/env python3
"""Convert a known v1 MySQL dump offline. Does not connect to a database or print personal data."""
from pathlib import Path
import argparse,base64,hashlib,hmac,json,re,secrets
from cryptography.hazmat.primitives.ciphers import Cipher,algorithms,modes
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

def decode(token):
    if token=='NULL':return None
    if not token.startswith("'"):return token
    token=token[1:-1];out=[];i=0;esc={'0':'\0','n':'\n','r':'\r','t':'\t','b':'\b','Z':'\x1a'}
    while i<len(token):
        if token[i]=='\\' and i+1<len(token):i+=1;out.append(esc.get(token[i],token[i]))
        elif token[i:i+2]=="''":out.append("'");i+=1
        else:out.append(token[i])
        i+=1
    return ''.join(out)
def literal(value):return "'"+value.replace('\\','\\\\').replace("'","\\'").replace('\n','\\n').replace('\r','\\r')+"'"
def rows(text,start):
    data=[];i=start
    while True:
        while text[i].isspace() or text[i]==',':i+=1
        if text[i]==';':return data,i+1
        if text[i]!='(':raise ValueError('Invalid INSERT row')
        i+=1;fields=[];begin=i;quoted=False
        while True:
            c=text[i]
            if quoted:
                if c=='\\':i+=2;continue
                if c=="'":
                    if text[i:i+2]=="''":i+=2;continue
                    quoted=False
            elif c=="'":quoted=True
            elif c in ',)':
                fields.append(text[begin:i].strip());begin=i+1
                if c==')':i+=1;break
            i+=1
        data.append(fields)
def legacy_plain(value):
    if re.fullmatch(r'\d{3}-?\d{2}-?\d{5}',value):return value.replace('-','')
    key=('bb5'+'bb5'+'1860'+'17a74'+'213f')[:16].encode()
    decryptor=Cipher(algorithms.AES(key),modes.ECB()).decryptor()
    plain=decryptor.update(bytes.fromhex(value))+decryptor.finalize()
    pad=plain[-1]
    if not 1<=pad<=16 or plain[-pad:]!=bytes([pad])*pad:raise ValueError('Invalid legacy padding')
    number=plain[:-pad].decode()
    if not re.fullmatch(r'\d{10}',number):raise ValueError('Invalid company number')
    return number

def convert(source,keys,target='seinfomationa_secure'):
    if not re.fullmatch('[A-Za-z0-9_]+',target):raise ValueError('Invalid schema name')
    data_key=base64.b64decode(keys['SEINFO_DATA_KEY'],validate=True);lookup_key=base64.b64decode(keys['SEINFO_LOOKUP_KEY'],validate=True)
    if len(data_key)!=32 or len(lookup_key)!=32 or data_key==lookup_key:raise ValueError('Independent 32-byte keys required')
    schemas={m.group(1):re.findall(r'^  `([^`]+)`',m.group(2),re.M) for m in re.finditer(r'CREATE TABLE `([^`]+)` \((.*?)\) ENGINE',source,re.S)}
    inserts=[];table_rows={t:[] for t in schemas}
    for match in re.finditer(r'INSERT INTO `([^`]+)` VALUES ',source):
        table=match.group(1);data,end=rows(source,match.end());inserts.append((match.start(),end,table,data));table_rows[table].extend(data)
        if any(len(r)!=len(schemas[table]) for r in data):raise ValueError('Column count mismatch: '+table)
    count_before={t:len(r) for t,r in table_rows.items()}
    mapping={};plain_by_hash={}
    for table,columns in schemas.items():
        if 'company_no' not in columns:continue
        index=columns.index('company_no')
        for row in table_rows[table]:
            old=decode(row[index])
            if not old:continue
            number=legacy_plain(old)
            hashed='h2_'+hmac.new(lookup_key,('company:v2:'+number).encode(),hashlib.sha256).hexdigest()
            mapping[old]=hashed;plain_by_hash[hashed]=number
    vendor_enc={}
    for row in table_rows['seinfo_vendor']:
        old=decode(row[schemas['seinfo_vendor'].index('company_no')]);hashed=mapping[old];nonce=secrets.token_bytes(12)
        vendor_enc[old]='v2:'+base64.urlsafe_b64encode(nonce+AESGCM(data_key).encrypt(nonce,plain_by_hash[hashed].encode(),b'seinfo:company:v2')).decode().rstrip('=')
    if len(vendor_enc)!=len(table_rows['seinfo_vendor']):raise ValueError('Duplicate vendor key')
    parent_keys=set(vendor_enc)
    member_company=schemas['seinfo_vendor_manager'].index('company_no')
    if any(decode(r[member_company]) not in parent_keys for r in table_rows['seinfo_vendor_manager']):raise ValueError('Orphan company/member relation; manual review required')
    role_index=schemas['seinfo_vendor_manager'].index('user_role')
    if any(decode(r[role_index]) not in ('USER','ADMIN','EMPLOYEE',None) for r in table_rows['seinfo_vendor_manager']):raise ValueError('Unexpected account role; manual review required')
    ephemeral={'zzz_token_access','zzz_token_denied','seinfo_mfile_down'}
    result=source
    for begin,end,table,data in reversed(inserts):
        if table in ephemeral:replacement='-- Security migration: transient credentials/permits require reauthentication.'
        else:
            transformed=[];columns=schemas[table]
            for row in data:
                new=row.copy()
                if 'company_no' in columns:
                    idx=columns.index('company_no');old=decode(row[idx])
                    if old:new[idx]=literal(mapping[old])
                    if table=='seinfo_vendor':new.append(literal(vendor_enc[old]))
                if table=='seinfo_vendor_manager' and decode(new[role_index]) is None:new[role_index]=literal('USER')
                transformed.append('('+','.join(new)+')')
            replacement='INSERT INTO `'+table+'` VALUES '+','.join(transformed)+';'
        result=result[:begin]+replacement+result[end:]
    def schema(match):
        table,body=match.group(1),match.group(2)
        additions=[]
        if table=='seinfo_vendor':
            body=body.replace('  PRIMARY KEY',"  `company_no_enc` varchar(512) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'AES-256-GCM v2 business number',\n  PRIMARY KEY")
        if table=='seinfo_access_log':body=re.sub(r'(`company_no` )varchar\(50\)',r'\1varchar(100)',body)
        if table in ('seinfo_freeboard_comments','seinfo_freeboard_posts'):body=re.sub(r'(`(?:user_id|mod_user_id)` )varchar\(30\)',r'\1varchar(40)',body)
        if table=='seinfo_request_cont':body=re.sub(r'(`question_content` )varchar\(100\)(.*?) DEFAULT NULL',r'\1text\2',body)
        if table=='seinfo_vendor_manager':
            body=re.sub(r"(`user_role` varchar\(10\)[^\n]*?)DEFAULT NULL",r"\1NOT NULL DEFAULT 'USER'",body)
            additions.append("CONSTRAINT chk_user_role_v2 CHECK (user_role IN ('USER','ADMIN','EMPLOYEE'))")
        if table in ('zzz_token_access','zzz_token_denied'):
            body=re.sub(r'`token_key` varchar\(400\)[^\n]*', '`token_key` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,',body)
            body=body.replace('  PRIMARY KEY', '  `expires_at` datetime NOT NULL,\n  PRIMARY KEY')
            additions+=['KEY idx_token_expiry_v2 (`expires_at`)',"CONSTRAINT chk_token_hash_"+table+" CHECK (token_key REGEXP '^[0-9a-f]{64}$')"]
            if table=='zzz_token_denied':body=body.replace('PRIMARY KEY (`token_usr_id`)','PRIMARY KEY (`token_key`)');additions.append('KEY idx_denied_user_v2 (`token_usr_id`)')
        index_map={
          'seinfo_mfile_list':['KEY idx_file_group_owner_v2 (`mfile_group_id`,`reg_user_id`)'],
          'seinfo_mfile_group':['KEY idx_file_group_owner_v2 (`reg_user_id`)'],
          'seinfo_request':['KEY idx_request_owner_v2 (`user_uid`,`request_no`)'],
          'seinfo_request_cont':['KEY idx_cont_files_v2 (`mfile_group_id`)'],
          'seinfo_notice':['KEY idx_notice_files_v2 (`mfile_group_id`,`notice_type`)'],
          'seinfo_work':['KEY idx_work_owner_v2 (`user_email`,`deleted`,`work_date`)'],
          'seinfo_freeboard_comments':['KEY idx_comment_parent_v2 (`freeboard_no`,`parent_no`)'],
          'seinfo_freeboard_posts':['KEY idx_post_owner_v2 (`user_id`)'],
          'seinfo_vendor_manager':['KEY idx_member_company_v2 (`company_no`)'],
          'seinfo_mfile_down':['PRIMARY KEY (`mfile_down_id`)','KEY idx_download_expiry_v2 (`reg_dt`,`mfile_down_yn`)']}
        additions+=index_map.get(table,[])
        if additions:body=body.rstrip()+',\n  '+',\n  '.join(additions)+'\n'
        return 'CREATE TABLE `'+table+'` ('+body+') ENGINE'
    result=re.sub(r'CREATE TABLE `([^`]+)` \((.*?)\) ENGINE',schema,result,flags=re.S)
    result=result.replace('`seinfomationa`','`'+target+'`').replace('Database: seinfomationa','Database: '+target).replace('SET NAMES utf8 */','SET NAMES utf8mb4 */')
    result=result.replace('CREATE DATABASE  IF NOT EXISTS','CREATE DATABASE ',1)
    marker={ 'schema_version':'2','data_key_id':hashlib.sha256(data_key).hexdigest(),'lookup_key_id':hashlib.sha256(lookup_key).hexdigest() }
    result+='\nCREATE TABLE seinfo_security_meta (meta_key varchar(64) PRIMARY KEY, meta_value varchar(128) NOT NULL) ENGINE=InnoDB;\n'
    result+='INSERT INTO seinfo_security_meta VALUES '+','.join('('+literal(k)+','+literal(v)+')' for k,v in marker.items())+';\n'
    audit={'schema':target,'business_rows_before':count_before,'business_rows_after':{t:(0 if t in ephemeral else n) for t,n in count_before.items()},'companies_migrated':len(vendor_enc),'password_hashes_preserved':True,'company_member_links_preserved':True,'ephemeral_tables_cleared':sorted(ephemeral),'null_roles_defaulted_to_USER':sum(decode(r[role_index]) is None for r in table_rows['seinfo_vendor_manager']),'source_sha256':hashlib.sha256(source.encode()).hexdigest()}
    return result,audit

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--input',required=True);parser.add_argument('--output',required=True);parser.add_argument('--keys',required=True);parser.add_argument('--schema',default='seinfomationa_secure');args=parser.parse_args()
    keys=dict(line.strip().split('=',1) for line in Path(args.keys).read_text().splitlines() if line.strip() and not line.startswith('#'))
    output,audit=convert(Path(args.input).read_text(),keys,args.schema)
    destination=Path(args.output);destination.write_text(output,encoding='utf-8');destination.with_suffix('.audit.json').write_text(json.dumps(audit,ensure_ascii=False,indent=2))
    print('Converted successfully; see audit JSON for counts. No live DB connection was made.')
if __name__=='__main__':main()
