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

import java.io.*;
import java.net.*;

public class Server extends Thread {

    private ServerSocket s;
    private Mime mime;
    private Config config;
    private Logger log;

    public Server (Config config) throws IOException {
        if (!new File(config.getValue("DocumentRoot")).isDirectory())
            throw new IOException("documentRoot is not a directory.");
        System.out.println(config.getValue("DocumentRoot"));
        s = new ServerSocket(Integer.parseInt(config.getValue("Listen")));
        this.mime = new Mime(config.getValue("MimeTypesFile"));
        this.config = config;
        this.log = new Logger(this.config);
    }

    public void run () {
        while (true) {
            try {
                Socket socket = s.accept();
                Http http = new Http(socket, this.config, this.mime, this.log);
                http.start();
            }
            catch (IOException e) {
                System.out.println("Cannot Accept from Socket!");
                System.exit(1);
            }
        }
    }

}
