import unittest, sqlite3, re
from pathlib import Path
SOURCE=(Path(__file__).parent.parent/'app/src/main/java/com/example/debtledger/data/local/LedgerDao.kt').read_text()
VIEW=re.search(r'@DatabaseView.*?value = """(.*?)"""',SOURCE,re.S).group(1)
TOTAL=re.search(r'@Query\("""(SELECT currency,.*?)"""\)',SOURCE,re.S).group(1)
FILTER=re.search(r'@Query\("""(SELECT \* FROM debt_balances.*?)"""\)',SOURCE,re.S).group(1)
class Tests(unittest.TestCase):
 def setUp(self):
  self.db=sqlite3.connect(':memory:');self.db.execute('PRAGMA foreign_keys=ON')
  self.db.executescript('''CREATE TABLE persons(id TEXT PRIMARY KEY,name TEXT);
CREATE TABLE debts(id TEXT PRIMARY KEY,personId TEXT REFERENCES persons(id) ON DELETE RESTRICT,direction TEXT,currency TEXT,originalAmountMinor INTEGER,debtDate INTEGER,description TEXT,notes TEXT,cancelledAt INTEGER,createdAt INTEGER,updatedAt INTEGER);
CREATE TABLE payments(id TEXT PRIMARY KEY,debtId TEXT REFERENCES debts(id) ON DELETE RESTRICT,operationId TEXT UNIQUE,amountMinor INTEGER,paymentDate INTEGER,notes TEXT,voidedAt INTEGER,createdAt INTEGER,updatedAt INTEGER);''')
  self.db.execute('CREATE VIEW debt_balances AS '+VIEW)
  self.db.execute("INSERT INTO persons VALUES('a','أحمد')")
  self.debt('d',100000);self.db.commit()
 def tearDown(self):self.db.close()
 def debt(self,id,amount,currency='ILS',direction='RECEIVABLE'):
  self.db.execute('INSERT INTO debts VALUES(?,?,?,?,?,10,?,NULL,NULL,1,1)',(id,'a',direction,currency,amount,'دين'))
 def payment(self,id,amount,debt='d',op=None):
  self.db.execute('INSERT INTO payments VALUES(?,?,?,?,10,NULL,NULL,1,1)',(id,debt,op or id,amount))
 def balance(self):return self.db.execute("SELECT paidMinor,remainingMinor FROM debt_balances WHERE id='d'").fetchone()
 def test_partial_and_complete(self):
  self.payment('p1',20000);self.payment('p2',30000);self.assertEqual(self.balance(),(50000,50000))
  self.payment('p3',50000);self.assertEqual(self.balance(),(100000,0))
 def test_edit_and_void(self):
  self.payment('p',20000);self.db.execute("UPDATE payments SET amountMinor=30000 WHERE id='p'");self.assertEqual(self.balance(),(30000,70000))
  self.db.execute("UPDATE payments SET voidedAt=1 WHERE id='p'");self.assertEqual(self.balance(),(0,100000))
 def test_currencies_and_no_join_multiplication(self):
  self.payment('p1',20000);self.payment('p2',30000)
  self.debt('usd',20000,'USD');self.payment('p3',5000,'usd')
  self.debt('pay',15000,'ILS','PAYABLE')
  totals={r[0]:r[1:] for r in self.db.execute(TOTAL,{'personId':None})}
  self.assertEqual(totals['ILS'],(50000,15000,50000,0,1,1,0))
  self.assertEqual(totals['USD'],(15000,0,5000,0,0,1,0))
 def test_cancel_excluded(self):
  self.db.execute("UPDATE debts SET cancelledAt=1 WHERE id='d'")
  self.assertEqual(list(self.db.execute(TOTAL,{'personId':None})),[])
 def test_restrict(self):
  with self.assertRaises(sqlite3.IntegrityError):self.db.execute("DELETE FROM persons WHERE id='a'")
  self.payment('p',100)
  with self.assertRaises(sqlite3.IntegrityError):self.db.execute("DELETE FROM debts WHERE id='d'")
 def test_operation_unique(self):
  self.payment('p1',100,op='same')
  with self.assertRaises(sqlite3.IntegrityError):self.payment('p2',100,op='same')
 def test_rollback(self):
  try:
   with self.db:
    self.payment('p',100)
    self.db.execute("INSERT INTO payments VALUES('bad','missing','op',100,10,NULL,NULL,1,1)")
  except sqlite3.IntegrityError:pass
  self.assertEqual(self.balance(),(0,100000))
 def test_filters(self):
  params=dict(personId=None,currency=None,direction=None,fromDay=None,toDay=None,state='UNPAID')
  self.assertEqual(len(list(self.db.execute(FILTER,params))),1)
  self.payment('p',100);params['state']='PARTIAL'
  self.assertEqual(len(list(self.db.execute(FILTER,params))),1)
  params['currency']='USD';self.assertEqual(len(list(self.db.execute(FILTER,params))),0)
 def test_zero_net_still_open(self):
  self.debt('pay',100000,'ILS','PAYABLE');r=self.db.execute(TOTAL,{'personId':'a'}).fetchone()
  self.assertEqual(r[1]-r[2],0);self.assertEqual(r[5],2)
if __name__=='__main__':unittest.main(verbosity=2)
