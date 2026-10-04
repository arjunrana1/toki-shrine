from pathlib import Path
import json,sqlite3,re,xml.etree.ElementTree as ET,subprocess
schemas=Path('app/schemas/com.arjunrana.tokishrine.data.db.TokiDatabase')
v3=json.loads((schemas/'3.json').read_text())['database'];v4=json.loads((schemas/'4.json').read_text())['database']
# SQL-level migration comparison with Room's exported schema; not Android execution.
a=sqlite3.connect(':memory:');b=sqlite3.connect(':memory:')
for db, schema in [(a,v3),(b,v4)]:
 for entity in schema['entities']:
  db.execute(entity['createSql'].replace('${TABLE_NAME}',entity['tableName']))
  for index in entity['indices']: db.execute(index['createSql'].replace('${TABLE_NAME}',entity['tableName']))
src=Path('app/src/main/java/com/arjunrana/tokishrine/data/db/TokiDatabase.kt').read_text()
for sql in re.findall(r'db.execSQL\("([^"\n]+)"\)',src):a.execute(sql)
for entity in v4['entities']:
 table=entity['tableName']
 for pragma in ['table_info','index_list','foreign_key_list']:
  assert a.execute(f'pragma {pragma}("{table}")').fetchall()==b.execute(f'pragma {pragma}("{table}")').fetchall(),(table,pragma)
assert v3['entities']==v4['entities'][:len(v3['entities'])], 'v3 entity definition changed'
print('PASS: migration SQL matches exported v4 table/index/FK metadata; all exported v3 entities unchanged. Host SQLite only; Android migration not executed.')
