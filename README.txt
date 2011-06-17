### About ###
SuUpServer is a http server written all in the Java Runtime Environment (JRE).
Don't know when this is supposed to be useful, but it was a good excersise.

Some the limitations/bugs with the current version:
- Can only delivers documents with Content-Type:text/html
- Occasionally the server fails to retrieve the header. Haven't been able to reproduce this with consistency.
- Curently there's no easy way to turn of the log messages printet to console.

### Links ###
web: https://github.com/sorisos/SuSuServer
read only checkout: git://github.com/sorisos/SuSuServer.git
   
### Usage ###
cd to the SuUpServer directory
$ java com/HttpServer <port number> <html directory>

note that the <html directory> is also where any upload would end up.

to run the server on port 80 you could use iptablses as:
iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 3001
where 3001 would be the port number the server is running on. this works for linux only.