

#!/usr/bin/env python
 
import socket
import json
 
HOST = "localhost"
PORT = 8082


sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM) #Responsavel pela criacao do socket.
sock.connect((HOST, PORT))



'''
Socket used to AddTrafficLight object
Attention! 
   
  
''' 
addVehicle = {
                'evt_type':"AddVehicle",
                'VihicleName' : "RobertVeihicle",
                'VihicleType':"Fast",
                'DestinationName': "Market",
                'Source': "Home123",            
                }




VehicleArrived = {
                 'evt_type':"VehicleArrived",
               'ID':"myID1234",
               'VehicleType':"Slow"
                }



#Socket used to CreateObject

CreateObject = {
                 
                 'evt_type':"CreateObject",
                 'ID':"myID1234",
                 'VehicleType':"Fast",
                 'Position':[1.1,2.1,3.1]   
                           
                 }   
#Socket used to CreateObject
DeleteObject = {
                
                'evt_type': "DeleteObject",
                'ID':"v12433523"
                
                }

#Socket used to InductionLoop

InductionLoop ={ 
               'evt_type':"InductionLoop",
               'id':"myid",
               'count':"wow"
               }       

                
         
VehicleDeparted = {
                  'evt_type':"VehicleDeparted",
                  'ID':"myID123",
                  'Position':58,
                  'VehicleType': "Fast"
                  }




#encoded_file = json.dumps({'id': 1, 'message': "Hello"})
arroz = True
while arroz:
    feijao = input("press 1another interaction\n"+
                   "press 0 = AddVehicle"+
                   "press 1 - AddTrafficLights "+
                   "press 2 - Create Object"+
                   "press 3 - Delete Object\n"+
                   "press 4 - InductionLoop"+
                   "press 5 - VehicleUpgrade"+
                   "press 6 -TrafficLightUpgrade")
    if(feijao == 0):
        socket_event = addVehicle
    if(feijao == 1):
        socket_event = VehicleArrived
    if(feijao ==2):
        socket_event = CreateObject
    if(feijao == 3):
        socket_event = DeleteObject
    if(feijao == 4):
        socket_event = InductionLoop
    if(feijao == 5):
        socket_event = VehicleDeparted
    
        
    if(feijao>-1 and feijao<7):
        encoded_file = json.dumps(socket_event)
        feijao = 9;
        encoded_file = encoded_file + "\r\n\r\n"
#result = json.loads(sock.recv(1024))
#print result;
        sock.sendall(encoded_file )

#sock.sendall("Hello\r\n")
'''

data = sock.recv(1024)
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







'''#!/usr/bin/env python
 
import socket
import json
 
HOST = "localhost"
PORT = 8080



sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM) #Responsavel pela criacao do socket.
sock.connect((HOST, PORT))

my_name = "Bruno Murakami"
my_age = 23
my_email = "murakami.bruno@gmail.com"

myself ={
        'name' : (my_name),
        'age' : (my_age),
        'email' : (my_email)
    }


pname = "Honda Accord"
tripid = 1
sname = "San Francisco"
dname = "San Diego"

socket_event = {
            'evt_type': "AddVehicle",
            'vname':"Ferrari",
            'vtype':"Fancy Car",
            'sname':"San Francisco",
            'dname':"San Diego",           
        }


#encoded_file = json.dumps({'id': 1, 'message': "Hello"})
encoded_file = json.dumps(socket_event)

#result = json.loads(sock.recv(1024))
#print result;
sock.sendall(encoded_file + "\r")

#sock.sendall("Hello\r\n")

''''''data = sock.recv(1024)
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
