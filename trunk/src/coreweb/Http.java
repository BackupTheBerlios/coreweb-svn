/*
 *   CoreWeb - a tiny web server written in java
 *   Copyright (C) 2005, Ioannis Nikiforakis <I.Nikiforakis@gmail.com>
 *                       Ioannis Apostolidis <pantsos@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software Foundation,
 *   Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package coreweb;

import java.net.Socket;
import java.net.URLDecoder;
import java.io.*;
import java.util.Date;
import java.util.Hashtable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.SimpleDateFormat;

public class Http extends Thread {

    private Socket s;
    private String documentRoot;
    private BufferedReader in;
    private DataOutputStream out;
    private Hashtable httpRequest = new Hashtable();
    private Mime mime;
    private Config config;
    private Logger log;


    public Http(Socket s, Config config, Mime mime, Logger log) {
        this.s = s;
        this.config = config;
        this.documentRoot = config.getValue("DocumentRoot");
        this.mime = mime;
        this.log = log;
    }

    private boolean parseRequest(BufferedReader input) {
        String line="input";
        while(true) {
            try {
                line=in.readLine();
            }
            catch(IOException e) {
                System.out.println("Client disconnected");
            }
            if(line==null || line.equals(""))
                break;
            if(line.substring(0,3).equals("GET")) {
                if(check501(line) || check400(line))
                    return false;
                String file = line.substring(line.indexOf(" ")+1,line.lastIndexOf(" "));
                String http = line.substring(line.lastIndexOf(" ")+1);
                httpRequest.put("GET", file);
                httpRequest.put("HTTP", http);
            } else {
                int space = line.indexOf(" ");
                String attribute=line.substring(0,space-1);
                String value =line.substring(space+1);
                httpRequest.put(attribute, value);
            }
        }
        return true;
    }

    public void run () {
        try {
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            out = new DataOutputStream(s.getOutputStream());
            String clientAddress = getClientAddress(s);

            if(!parseRequest(in)) {
                logRequest(500, clientAddress);
                out.close();
                in.close();
                s.close();
                return;
            }

            if(!(s==null)) {
                String path = (String) httpRequest.get("GET");
                File file = new File(documentRoot, URLDecoder.decode(path, "UTF-8")).getCanonicalFile();
                String absPath=file.toString();

                if(file.isDirectory())
                    absPath = file.toString()+"/";

                if(!file.exists()) {
                    sendHttpError(404);
                    logRequest(404, clientAddress);
                } else if(!absPath.startsWith(documentRoot)) {
                    sendHttpError(403);
                    logRequest(403, clientAddress);
                } else if (file.isDirectory()) {
                    File index = new File(file, "index.html");
                    if (index.exists() && !index.isDirectory()) {
                        file = index;
                        sendFile(file);
                    } else {
                        sendDirectoryIndex(path, file);
                    }
                    logRequest(200, clientAddress);
                } else {
                    sendFile(file);
                    logRequest(200, clientAddress);
                }
            } else {
                sendHttpError(500);
                logRequest(500, clientAddress);
            }
            out.close();
            s.close();
        }
        catch(IOException e) {
            System.exit(1);
        }
    }

    private void logRequest(int code, String clientAddress) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss");
        String currentDate = dateFormat.format(new Date());
        String logLine = clientAddress +" - - ["+ currentDate + "] "+ "\"GET " + (String) httpRequest.get("GET") +" "+ (String) httpRequest.get("HTTP") +"\" "+code+" - \"-\" \"" + (String) httpRequest.get("User-Agent") +"\"";
        log.writeLine(Logger.ACCESS, logLine);
    }

    private String getClientAddress(Socket socket) {
        String clientIP = ""+ socket.getInetAddress();
        return clientIP.substring(clientIP.indexOf("/")+1);
    }

    private void sendHttpError(int errorCode) throws IOException {
        sendHttpHeader(errorCode, "text/html", -1, ""+System.currentTimeMillis());
        sendHttp("<font face=\"Lucida Grande\" size=\"large\">"+errorCode+" "+getStatusCode(errorCode)+"</font>");
    }

    private String getStatusCode(int httpCode) {
        switch(httpCode) {
            // Most Used
            case 200: return "OK";
            case 404: return "Not Found";
            // Informational
            case 100: return "Continue";
            case 101: return "Switching Protocols";
            // Successful
            case 201: return "Created";
            case 202: return "Accepted";
            case 203: return "Non-Authoritative Information";
            case 204: return "No Content";
            case 205: return "Reset Content";
            case 206: return "Partial Content";
            // Redirection
            case 300: return "Multiple Choices";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 303: return "See Other";
            case 304: return "Not Modified";
            case 305: return "Use Proxy";
            case 307: return "Temporary Redirect";
            // Client Errors
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 402: return "Payment Required";
            case 403: return "Forbidden";
            case 405: return "Method Not Allowed";
            case 406: return "Not Acceptable";
            case 407: return "Proxy Authentication Required";
            case 408: return "Request Timeout";
            case 409: return "Conflict";
            case 410: return "Gone";
            case 411: return "Length Required";
            case 412: return "Precondition Failed";
            case 413: return "Request Entity Too Large";
            case 414: return "Request-URI Too Long";
            case 415: return "Unsupported Media Type";
            case 416: return "Requested Range Not Satisfiable";
            case 417: return "Expectation Failed";
            // Server Errors
            case 500: return "Internal Server Error";
            case 501: return "Not Implemented";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            case 505: return "HTTP Version Not Supported";
            default : return "Unknown Status Code";
        }
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".")+1, fileName.length());
    }

    private void sendHttp(String output) {
        try {
            out.writeBytes(output);
        } catch(IOException e) {
            System.out.println("sendHttp: Exception!");
        }
    }

    private void sendHttpHeader(int code, String mime, long len, String lastModified) {
        try {
            out.writeBytes("HTTP/1.1 " + code + " "+getStatusCode(code)+" \r\n" +
                   "Date: " + new Date().toString() + "\r\n" +
                   "Content-Type: " + mime + "\r\n" +
                   "Accept-Ranges: bytes\r\n" +
                   "Content-Length: " + len + "\r\n" +
                   "Connection: close\r\n" +
                   //"Last-modified: " + new Date(lastModified).toString() + "\r\n" +
                   "\r\n");
        } catch(IOException e) {
            System.out.println("sendHttp: Exception!");
        }
    }

    private boolean check501(String httpRequest) {
        Pattern pattern;
        Matcher matcher;
        pattern=Pattern.compile("^GET .*$");
        matcher=pattern.matcher(httpRequest);
        if (!matcher.matches()) {
            try {
                sendHttpError(501);
            } catch(IOException e) {
                System.exit(1);
            }
            return true;
        }
        return false;
    }

    private boolean check400(String httpRequest) {
        Pattern pattern;
        Matcher matcher;
        pattern=Pattern.compile("^GET (.*) HTTP/(.*)$");
        matcher=pattern.matcher(httpRequest);
        if (!matcher.matches()) {
            try {
                sendHttpError(400);
            } catch(IOException e) {
                System.exit(1);
            }
            return true;
        }
        return false;
    }

    private void sendFile(File file) {
        byte[] buffer=new byte[16000];
        try {
            String contentType = mime.getContentType(getFileExtension(file.getName()));
            sendHttpHeader(200, contentType, file.length(), ""+System.currentTimeMillis());
            FileInputStream is=new FileInputStream(file);
            int readBytes=0;
            while(readBytes>=0) {
                readBytes=is.read(buffer);
                if(readBytes>=0)
                    out.write(buffer,0,readBytes);
            }
        } catch(IOException e) {
            try{
                sendHttpError(500);
            } catch(IOException z) {
                System.exit(1);
            }
        }
    }

    private void sendDirectoryIndex(String path, File file) {
        File[] files = file.listFiles();
        sendHttpHeader(200, "text/html", -1, ""+System.currentTimeMillis());
            sendHttp("<HTML><HEAD><TITLE>Index of "+path+"</TITLE>\n");
            sendHttp("<STYLE TYPE=\"text/css\"><!--\n" +
                           "BODY { FONT-FAMILY: Verdana, Arial, Helvetica, sans-serif; FONT-SIZE: 10pt; COLOR: #000000;}\n"+
                           ".TableText { FONT-FAMILY: Verdana, Arial, Helvetica, sans-serif; FONT-SIZE: 10pt; COLOR: #000000;}\n"+
                           "A:LINK { FONT-FAMILY: Verdana, Arial, Helvetica, sans-serif; FONT-SIZE: 10pt; COLOR: #0000FF; TEXT-DECORATION: underline;}\n"+
                           "A:VISITED { FONT-FAMILY: Verdana, Arial, Helvetica, sans-serif; FONT-SIZE: 10pt; COLOR: #333333; TEXT-DECORATION: underline;}\n"+
                           "--></STYLE>\n");
            sendHttp("</HEAD><BODY><H2>Index of "+path+"</H2><BR>\n");
            sendHttp("<TABLE CLASS=\"TableText\">\n");
            sendHttp("<TR><TD WIDTH=200>Filename</TD><TD WIDTH=150>Last Modified</TD><TD WIDTH=80>Size</TD><TD WIDTH=50>Description</TD>\n");
            for (int i=0; i<files.length; i++) {
                file = files[i];
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
                String fileType = "";
                String fileSize = "";
                String fileName = "";
                if (file.getName().substring(0,1).equals("."))
                    continue;
                if (file.isDirectory()) {
                    fileType = "Directory";
                    fileSize = "-";
                    fileName = file.getName()+"/";
                } else {
                    fileType = "File";
                    fileSize = getFileSize(file.length());
                    fileName = file.getName();
                }
                sendHttp("<TR><TD><A HREF=\""+path+fileName+"\">"+file.getName()+"</A></TD><TD>"+dateFormat.format(new Date(file.lastModified()))+"</TD><TD>"+fileSize+"</TD><TD>"+fileType+"</TD>\n");
            }
            sendHttp("</TABLE><HR>CoreWeb "+CoreWeb.VERSION+"</BODY><HTML>");
    }

    private String getFileSize(long size) {
        String fileSize;
        if((size/1073741824)>1)
            fileSize = Double.toString((double) (size/1073741824)) + "GiB";
        else if((size/1048576)>1)
            fileSize = Double.toString((double) (size/1048576)) + "MiB";
        else if((size/1024)>1)
            fileSize = (size/1024) + "KiB";
        else
            fileSize = size + "B";
       return fileSize;
    }

}
