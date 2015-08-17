
from multiprocessing import Manager, Process, Pool,Queue
from Queue import Empty


import asyncore
import asynchat
import socket
import threading
import json
import multiprocessing


#from threading import Thread

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



        '''
                       #element = self.q.get() 
                        print "woo332323oow"
                        encoded_file = json.dumps(element)
                        encoded_file = encoded_file +"\r\n\r\n"
                        server.push(encoded_file )
                        for handler in chat_room.itervalues():
                                 if hasattr(handler, 'push'):
                                         handler.push(encoded_file)
                                         self.q.task_done()
	'''

from threading import Thread
import time
import random
from Queue import Queue

queue = Queue(maxsize=0)

class ConsumerThread(Thread):

    def run(self):
        global queue
        while True:
            num = queue.get()
            queue.task_done()
            print "Consumed", num
            time.sleep(random.random())

class Connection(Thread):

        def __init__(self):
                #self.q = Queue.Queue(maxsize=0)
                self.server = ChatClient('localhost', 23456)
                self.comm =  multiprocessing.Process(target=asyncore.loop)
                self.comm.daemon = True
                self.comm.start()
                #self.thread = Process(target = self.Send_Inv , args = (self.q,))
                #self.thread.daemon = True
                #self.thread.start()

		ConsumerThread().start()
                asyncore.loop(map=chat_room)
                #self.q.join()
        def PushQueue(self,element):


                queue.put("wooow")

                        #self.q.task_done()
                #self.q.join()
                #self.Send_Inv(self.q)

        def PopQueue(self):

                print "wow"
                return self.q.get()

        def run(self):
                while True:
                        #print "wow"
                        time.sleep(2)
                        print self.q.empty()
                        # queue.task_done()     
                        '''
                        #element = self.q.get() 
                        print "woo332323oow"
                        encoded_file = json.dumps(element)
                        encoded_file = encoded_file +"\r\n\r\n"
                        server.push(encoded_file )
                        for handler in chat_room.itervalues():
                                 if hasattr(handler, 'push'):
                                         handler.push(encoded_file)
                                         self.q.task_done()

                        '''



class wow:

	def __init__(self):
		#ProducerThread().start()
		connec = Connection()
		time.sleep(10)
		connec.PushQueue("woooow")
		connec.PushQueue("woooow")
		connec.PushQueue("woooow")
		connec.PushQueue("woooow")
		connec.PushQueue("woooow")
		connec.PushQueue("woooow")
		connec.PushQueue("woooow")
		connec.PushQueue("woooow")
		connec.PushQueue("woooow")
		connec.PushQueue("woooow")

wow1 = wow()
