import json  
#m = {'id': 2, 'name': 'hussain\r\n'}  
#n = json.dumps(m)  
#o = json.loads(n)  
#print o['id'], o['name']

myself ={
         'name' : "Bruno",
        'age' : 23,
        'email' : "murakami.bruno@gmail.com"
    }


#encoded_file = json.dumps({'id': 1, 'message': "Hello"})
encoded_file = json.dumps(myself)
#load_encoded_file = json.loads(encoded_file)
#print load_encoded_file
