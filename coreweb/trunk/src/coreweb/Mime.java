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

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Hashtable;

public class Mime {

    private Hashtable mimeTypes = new Hashtable();

    public Mime(String mimeFile) {
        readMimeTypes(mimeFile);
    }

    private void readMimeTypes(String mimeFile) {
        try {
            BufferedReader bReader = new BufferedReader(new FileReader(mimeFile));
            while(bReader.ready()) {
                String line=bReader.readLine();
                if (line.length()>0 && !line.substring(0,1).equals("#")) {
                    int firstSpaceIndex=line.indexOf(" ");
                    if (firstSpaceIndex>=0) {
                        String fileExtension = line.substring(0,firstSpaceIndex);
                        String contentType = line.substring(firstSpaceIndex+1);
                        mimeTypes.put(fileExtension,contentType);
                    }
                }
            }
            bReader.close();
        } catch(IOException e) {
            System.out.println(e.toString()+" in readMimeTypes");
        }
    }

    public String getContentType(String fileExtension) {
        if (mimeTypes.containsKey(fileExtension))
            return (String) mimeTypes.get(fileExtension);
        else
            return "text/html";
    }

}
