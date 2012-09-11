#!/usr/bin/python
import ldtp
import sys
import SimpleXMLRPCServer
import getopt
import logging
import re
import inspect
import wnck
import gobject
import gtk
import time
import fnmatch
from fnmatch import translate

logger = logging.getLogger("xmlrpcserver.ldtp")
logger.setLevel(logging.INFO)

class LoggingSimpleXMLRPCRequestHandler(SimpleXMLRPCServer.SimpleXMLRPCRequestHandler):
  """Overides the default SimpleXMLRPCRequestHander to support logging.  Logs
  client IP and the XML request and response.
  """

  def do_POST(self):
    clientIP, port = self.client_address
    # Log client IP and Port
    logger.info('Client IP: %s - Port: %s' % (clientIP, port))
    try:
      # get arguments
      data = self.rfile.read(int(self.headers["content-length"]))
      # Log client request
      logger.info('Client request: \n%s\n' % data)

      response = self.server._marshaled_dispatch(data, getattr(self, '_dispatch', None))
      # Log server response
      logger.info('Server response: \n%s\n' % response)

    except:
      # This should only happen if the module is buggy
      # internal error, report as HTTP server error
      self.send_response(500)
      self.end_headers()
    else:
      # got a valid XML RPC response
      self.send_response(200)
      self.send_header("Content-type", "text/xml")
      self.send_header("Content-length", str(len(response)))
      self.end_headers()
      self.wfile.write(response)

      # shut down the connection
      self.wfile.flush()
      self.connection.shutdown(1)

#figure out which methods are in LDTPv2 and only use those
#f = open("/root/bin/ldtp_api2.clj", "r")
#ldtp2commands = []
#line = f.readline().strip()
#while line:
#  command = line.split("\"")[1]
#  ldtp2commands.append(command)
#  line = f.readline()
#ldtp2commands.sort()
#f.close

ldtp2commands = ['activatetext', 'activatewindow', 'appendtext', 'check', 'checkrow', 'click', 'closewindow', 'comboselect', 'comboselectindex', 'copytext', 'cuttext', 'decrease', 'deletetext', 'doesmenuitemexist', 'doesrowexist', 'doubleclick', 'doubleclickrow', 'enterstring', 'expandtablecell', 'generatekeyevent', 'generatemouseevent', 'getallitem', 'getallstates', 'getapplist', 'getcellvalue', 'getcharcount', 'getchild', 'getcursorposition', 'getmax', 'getmaxvalue', 'getminincrement', 'getminvalue', 'getobjectinfo', 'getobjectlist', 'getobjectproperty', 'getobjectsize', 'getrowcount', 'getslidervalue', 'getstatusbartext', 'gettabcount', 'gettablerowindex', 'gettabname', 'gettextvalue', 'getvalue', 'getwindowlist', 'getwindowsize', 'grabfocus', 'guiexist', 'hasstate', 'hidelist', 'imagecapture', 'increase', 'invokemenu', 'isalive', 'ischildindexselected', 'ischildselected', 'istextstateenabled', 'keypress', 'keyrelease', 'launchapp', 'listsubmenus', 'maximizewindow', 'menucheck', 'menuitemenabled', 'menuuncheck', 'minimizewindow', 'mouseleftclick', 'mousemove', 'mouserightclick', 'objectexist', 'onedown', 'oneleft', 'oneright', 'oneup', 'onwindowcreate', 'pastetext', 'poll_events', 'press', 'registerevent', 'remap', 'removecallback', 'removeevent', 'scrolldown', 'scrollleft', 'scrollright', 'scrollup', 'selectall', 'selecteditemcount', 'selectindex', 'selectitem', 'selectlastrow', 'selectmenuitem', 'selectrow', 'selectrowindex', 'selectrowpartialmatch', 'selecttab', 'selecttabindex', 'setcellvalue', 'setcursorposition', 'setlocale', 'setmax', 'setmin', 'settextvalue', 'setvalue', 'showlist', 'simulatemousemove', 'singleclickrow', 'stateenabled', 'uncheck', 'uncheckrow', 'unmaximizewindow', 'unminimizewindow', 'unselectall', 'unselectindex', 'unselectitem', 'verifycheck', 'verifydropdown', 'verifyhidelist', 'verifymenucheck', 'verifymenuuncheck', 'verifypartialmatch', 'verifypartialtablecell', 'verifyscrollbarhorizontal', 'verifyscrollbarvertical', 'verifyselect', 'verifysettext', 'verifysetvalue', 'verifyshowlist', 'verifysliderhorizontal', 'verifyslidervertical', 'verifytablecell', 'verifytabname', 'verifytoggled', 'verifyuncheck', 'wait', 'waittillguiexist', 'waittillguinotexist', 'windowuptime']

_ldtp_methods = filter(lambda fn: inspect.isfunction(getattr(ldtp,fn)),  dir(ldtp))
_supported_methods = filter(lambda x: x in ldtp2commands, _ldtp_methods)
_additional_methods = ['closewindow', 'maximizewindow']
for item in _additional_methods: _supported_methods.append(item)
_supported_methods.sort()


#create a class with all ldtp methods as attributes
class AllMethods:
  #states class variable
  #states enum from /usr/include/at-spi-1.0/cspi/spi-statetypes.h as part of at-spi-devel
  #hint: state = $line_number - 80
  states = ['INVALID',
            'ACTIVE',
            'ARMED',
            'BUSY',
            'CHECKED',
            'COLLAPSED',
            'DEFUNCT',
            'EDITABLE',
            'ENABLED',
            'EXPANDABLE',
            'EXPANDED',
            'FOCUSABLE',
            'FOCUSED',
            'HORIZONTAL',
            'ICONIFIED',
            'MODAL',
            'MULTI_LINE',
            'MULTISELECTABLE',
            'OPAQUE',
            'PRESSED',
            'RESIZABLE',
            'SELECTABLE',
            'SELECTED',
            'SENSITIVE',
            'SHOWING',
            'SINGLE_LINE',
            'STALE',
            'TRANSIENT',
            'VERTICAL',
            'VISIBLE',
            'MANAGES_DESCENDANTS',
            'INDETERMINATE',
            'TRUNCATED',
            'REQUIRED',
            'INVALID_ENTRY',
            'SUPPORTS_AUTOCOMPLETION',
            'SELECTABLE_TEXT',
            'IS_DEFAULT',
            'VISITED',
            'LAST_DEFINED']

  def _translate_state(self,value):
    if value in self.states:
      return self.states.index(value)
    else:
      return value

  def _translate_number(self,num):
    if num in xrange(len(self.states)):
      return self.states[num]
    else:
      return num

  def _getobjectproperty(self, window, object):
    getobjectlist = getattr(ldtp,"getobjectlist")
    objects = getobjectlist(window)
    for item in objects:
      if re.search(object,str(item)):
        return str(item)
    return object

  def _matches(self, pattern, item):
    return bool(re.match(fnmatch.translate(pattern), item, re.M | re.U | re.L))

  #this replicates the origional algorithm
  def _gettablerowindex(self, window, table, target):
    numrows = ldtp.getrowcount(window, table)
    numcols = len(ldtp.getobjectproperty(window, table, 'children').split())
    for i in range(0,numrows):
      for j in range(0,numcols):
        try:
          value = ldtp.getcellvalue(window, table, i, j)
          if self._matches(target,value):
            ldtp.selectrowindex(window, table, i)
            return i
        except:
          continue
    raise Exception("Item not found in table!")

  #this only searches the first column and is much quicker.
  def _quickgettablerowindex(self, window, table, target):
    numrows = ldtp.getrowcount(window, table)
    for i in range(0,numrows):
      try:
        value = ldtp.getcellvalue(window, table, i, 0)
        if self._matches(target,value):
          ldtp.selectrowindex(window, table, i)
          return i
      except:
        continue
    raise Exception("Item not found in table!")

  def _window_search(self,match,term):
    if re.search(fnmatch.translate(term),
                   match,
                   re.U | re.M | re.L) \
          or re.search(fnmatch.translate(re.sub("(^frm|^dlg)", "", term)),
                       re.sub(" *(\t*)|(\n*)", "", match),
                       re.U | re.M | re.L):
      return True
    else:
      return False

  def _closewindow(self,window_name):
    screen = wnck.screen_get_default()
    while gtk.events_pending():
      gtk.main_iteration()

    windows = screen.get_windows()
    success = 0
    for w in windows:
      current_window = w.get_name()
      if self._window_search(current_window,window_name):
        w.close(int(time.time()))
        success = 1
        break

    gobject.idle_add(gtk.main_quit)
    gtk.main()
    return success

  def _maximizewindow(self,window_name):
    screen = wnck.screen_get_default()
    while gtk.events_pending():
      gtk.main_iteration()

    windows = screen.get_windows()
    success = 0
    for w in windows:
      current_window = w.get_name()
      if self._window_search(current_window,window_name):
        w.maximize()
        success = 1
        break

    gobject.idle_add(gtk.main_quit)
    gtk.main()
    return success

  def _dispatch(self,method,params):
    if method in _supported_methods:
      paramslist = list(params)
      if method == "hasstate":
        paramslist[2]=self._translate_state(paramslist[2])
        params = tuple(paramslist)
      elif method == "closewindow":
        return self._closewindow(paramslist[0])
      elif method == "maximizewindow":
        return self._maximizewindow(paramslist[0])
      elif method == "getobjectproperty":
        paramslist[1] = self._getobjectproperty(paramslist[0],paramslist[1])
        params = tuple(paramslist)

      function = getattr(ldtp,method)
      retval = function(*params)

      if (method == "gettextvalue") and not (isinstance(retval, str) or
                                             isinstance(retval, unicode)):
        retval = ""
      elif (retval == -1) and (method == "gettablerowindex"):
        paramslist = list(params)
        #use quick method for now
        retval = self._quickgettablerowindex(paramslist[0],
                                             paramslist[1],
                                             paramslist[2])
      elif method == "getallstates":
        retval = [self._translate_number(state) for state in retval]

      if retval == None:
        retval = 0

      return retval
  pass

for name in _supported_methods:
  if not item in _additional_methods:
    setattr(AllMethods, name, getattr(ldtp, name))

def usage():
  print "Usage:"
  print "[-p, --port=] Port to listen on"
  print "[-l --logfile=] file to write logging to"
  print "[-h] This help message"

def start_server(port,logfile):
  if logfile:
    hdlr = logging.FileHandler(logfile)
    formatter = logging.Formatter("%(asctime)s  %(levelname)s  %(message)s")
    hdlr.setFormatter(formatter)
    logger.addHandler(hdlr)
    server = SimpleXMLRPCServer.SimpleXMLRPCServer(("",int(port)),
                                                    LoggingSimpleXMLRPCRequestHandler)
  else:
    server = SimpleXMLRPCServer.SimpleXMLRPCServer(('',int(port)),
                                                    logRequests=True)

  server.register_introspection_functions()
  server.register_instance(AllMethods())

  try:
    print("Listening on port %s" % port)
    server.serve_forever()
  except KeyboardInterrupt:
    print 'Exiting'

def main():
  try:
    opts, args = getopt.getopt(sys.argv[1:], "hpl:v", ["help", "port=", "logfile="])
    print(opts)
  except getopt.GetoptError, err:
    # print help information and exit:
    print str(err) # will print something like "option -a not recognized"
    usage()
    sys.exit(2)

  port = 4118 #default port
  logfile = None

  for o, a in opts:
    if o in ("-p", "--port"):
      port = a
    elif o in ("-l", "--logfile"):
      logfile = a
    elif o in ("-h", "--help"):
        usage()
        sys.exit()
    else:
        assert False, "unhandled option"

  start_server(port,logfile)

if __name__ == "__main__":
    main()
