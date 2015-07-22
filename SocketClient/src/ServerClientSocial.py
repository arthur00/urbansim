import asynchat
import asyncore
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
                       "press 0 = AddVehicle")
        if(feijao == 0):
            socket_event = VehicleInstance

            
        if(feijao>-1 and feijao<1):
            encoded_file = json.dumps(socket_event)
            encoded_file = encoded_file +"\r\n\r\n"
            feijao = 9;
            server.push(encoded_file )
            for handler in chat_room.itervalues():
                if hasattr(handler, 'push'):
                    handler.push(encoded_file)
                    
            #server.push(encoded_file+"\r")
            
VehicleInstance = {
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


if __name__ == "__main__":
    interface = 'localhost'
    port = 8080
    server = ChatClient('localhost', 8081)
    print 'Serving on localhost:8081'
    arroz = True
    comm = threading.Thread(target=asyncore.loop)
    comm.daemon = True
    comm.start()
    thread = Thread(target = Send_Inv , args = ())
    thread.start()
    asyncore.loop(map=chat_room)
   

