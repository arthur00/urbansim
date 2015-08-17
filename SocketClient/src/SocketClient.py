#!/usr/bin/env python
 
import socket
import json

HOST = "localhost"
PORT = 8080


sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM) #Responsavel pela criacao do socket.
sock.connect((HOST, PORT))

pname = "Honda Accord"
tripid = 1
sname = "San Francisco"
dname = "San Diego"

socket_event = {
            'evt_type': "AddVehicle",
            'vname':"Ferrari ",
            'vtype':"Fancy Car",
            'sname':"San Francisco",
            'dname':"San Diego",
            'id': 1,
            'float_number' : 0.3,
            'array': ([1,2,3]),
            'array_list':["Irvine", "Santa Ana", "Santa Barbara"],           
        }

socket_event2 = {
        
            'id': "2",         
        }

#encoded_file = json.dumps({'id': 1, 'message': "Hello"})
encoded_file = json.dumps(socket_event)
encoded_file2 = json.dumps(socket_event2)

#result = json.loads(sock.recv(1024))
#print result;
sock.sendall(encoded_file + "\r")

#sock.sendall("Hello\r\n")

'''data = sock.recv(1024)
print "1)", data
 
if ( data == "olleH\r\n" ):
    encoded_file = json.dumps({'id': 2, 'message': "Bye"})
    sock.sendall(encoded_file + "\r")
    data = sock.recv(1024)
    print "2)", data
 
    if (data == "eyB}\n\r"):
        sock.close()
        print "Socket closed"
'''
