import asyncore
import asynchat
import socket
import threading
import json



from threading import Thread

chat_room = {}
 
class ChatHandler(asynchat.async_chat):
    def __init__(self, sock):
        asynchat.async_chat.__init__(self, sock=sock, map=chat_room)
 
        self.set_terminator('\n')
        self.buffer = []
 
    def collect_incoming_data(self, data):
        self.buffer.append(data)
 
    def found_terminator(self):
        msg = ''.join(self.buffer)
        print 'Received:', msg
        #for handler in chat_room.itervalues():
            #if hasattr(handler, 'push'):
                #handler.push(msg + '\n')
        self.buffer = []
 
class ChatServer(asyncore.dispatcher):
    def __init__(self, host, port):
        asyncore.dispatcher.__init__(self, map=chat_room)
        self.create_socket(socket.AF_INET, socket.SOCK_STREAM)
        self.bind((host, port))
        self.listen(5)
 
    def handle_accept(self):
        pair = self.accept()
        if pair is not None:
            sock, addr = pair
            print 'Incoming connection from %s' % repr(addr)
            handler = ChatHandler(sock)
            
class ChatClient(asynchat.async_chat):

     def __init__(self, host, port):
       asynchat.async_chat.__init__(self)
       self.create_socket(socket.AF_INET, socket.SOCK_STREAM)
       self.connect((host, port))

       self.set_terminator('\n')
       self.buffer = []

     def collect_incoming_data(self, data):
        self.buffer.append(data)
 
     def found_terminator(self):
        msg = ''.join(self.buffer)
        print 'Received:', msg
        self.buffer = []


 
def Send_Inv():
    while arroz:
    
        feijao = input("another interaction\n"+
                       "press 0 = AddVehicle"+
                       "press 1 - AddTrafficLights "+
                       "press 2 - Create Object"+
                       "press 3 - Delete Object\n"+
                       "press 4 - InductionLoop"+
                       "press 5 - VehicleUpgrade"+
                       "press 6 -TrafficLightUpgrade")
        if(feijao == 0):
            socket_event = VehicleInstance
        if(feijao == 1):
            socket_event = TrafficlightInstance
        if(feijao ==2):
            socket_event = CreateObject
        if(feijao == 3):
            socket_event = DeleteObject
        if(feijao == 4):
            socket_event = InductionLoop
        if(feijao == 5):
            socket_event = VehicleUpgrade
        if(feijao == 6):
            socket_event = TrafficLightUpgrade
            
        if(feijao>-1 and feijao<7):
            encoded_file = json.dumps(socket_event)
            encoded_file = encoded_file +"\r\n\r\n"
            feijao = 9;
            server.push(encoded_file )
            for handler in chat_room.itervalues():
                if hasattr(handler, 'push'):
                    handler.push(encoded_file)
                    
            #server.push(encoded_file+"\r")
            
VehicleInstance = {
                'evt_type':"VehicleInstance",
                'position':[1.1,2.2,3.3],
                'angle':50.9,
                'velocity':56.8,
                'vname':"zuada",
                'vtype':"massa",
                'id':1
                             
                }




TrafficlightInstance = {
                'evt_type':"TrafficlightInstance",
                'status':"bom,",
                'id':1,
                'position':[1,2.2,3.3]
                
                }



#Socket used to CreateObject

CreateObject = {
                 
                 'evt_type': "CreateObject",
                 'vtype':"loko",
                 'vid':"ViD",
                 'pos':[1.1,2.1,3.1]   
                           
                 }   
#Socket used to CreateObject
DeleteObject = {
                
                'evt_type': "DeleteObject",
                'vid':"yId"
                
                }

#Socket used to InductionLoop

InductionLoop ={ 
               'evt_type':"InductionLoop",
               'id':"myid",
               'count':"wow"
               }       

                
         
VehicleUpgrade = {
                  'evt_type':"VehicleUpgrade",
                  'handle':"3",
                  'velocity':58,
                  'angle':8
                  }


TrafficLightUpgrade = { 
                       'evt_type':"TrafficLightUpgrade",
                       'handle':"3",
                       'status':"green"
                       
                       }




if __name__ == "__main__":
    interface = 'localhost'
    port = 23456
    print 'Serving on localhost:8080'
    server = ChatClient('localhost', 23456)
    arroz = True
    comm = threading.Thread(target=asyncore.loop)
    comm.daemon = True
    comm.start()
    thread = Thread(target = Send_Inv , args = ())
    thread.start()
    asyncore.loop(map=chat_room)
   

