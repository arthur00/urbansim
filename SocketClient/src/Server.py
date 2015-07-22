import SocketServer
import json

class Server(SocketServer.ThreadingTCPServer):
    allow_reuse_address = True

class ServerHandler(SocketServer.BaseRequestHandler):
    def handle(self):
        try:
            data = json.loads(self.request.recv(1024).strip())
            # process the data, i.e. print it:

            print data
            # send some 'ok' back
            self.request.sendall(json.dumps({'return':'ok'}))
        except Exception, e:
            print "Exception while receiving message: ", e


server = Server(('localhost',8080), ServerHandler)
print 'Connection open'
server.serve_forever()