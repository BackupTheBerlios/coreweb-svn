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

public class Logger {

	// Logging Levels
	public static final byte INFO	= 1;
	public static final byte ACCESS	= 2;
	public static final byte ERROR	= 3;
	public static final byte DEBUG	= 4;


	// Log files paths
	private String accessLogPath;
	private String errorLogPath;

	
	private File accessLogFile;
	private File errorLogFile;

	private FileWriter fwAccess;
	private FileWriter fwError;

	
	public Logger(Config config) throws IOException {
		accessLogFile = new File(config.getValue("AccessLog"));
		errorLogFile  = new File(config.getValue("ErrorLog"));

		
		// Check if access log exist, if not then create it
		if ( accessLogFile.exists() )
			try { accessLogFile.createNewFile(); }
			catch (IOException e) {
				System.out.println(e);
				System.exit(1);
			}

		// Check if error log exist, if not then create it
		if ( errorLogFile.exists() )
			try { errorLogFile.createNewFile(); }
			catch (IOException e) {
				System.out.println(e);
				System.exit(1);
			}
	}
	

	public void writeLine(int type, String str) {
		switch ( type )
		{
			case ACCESS: {
				try { 
					fwAccess = new FileWriter(accessLogFile, true);
					fwAccess.write(str + "\n"); 
					fwAccess.close();
					
				}
				catch (IOException e) {
					System.out.println(e);
					System.exit(1);
				}
				break;
			}
			case ERROR: {
			    	try { 
					fwError = new FileWriter(errorLogFile, true);
					fwError.write(str + "\n"); 
					fwError.close();
				}
				catch (IOException e) {
					System.out.println(e);
					System.exit(1);
				}
				break;
			}
		}
	}
	
	
	private void closeAccessLog() throws IOException {
		try { fwAccess.close(); }
		catch (IOException e) {
			throw new IOException("Could not close access log file.");
		}
	}

	private void closeErrorLog() throws IOException  {
		try { fwError.close(); }
			catch (IOException e) {
				throw new IOException("Could not close error log file.");
			}
	}
	
	public void closeLogFiles() throws IOException {
		try {
			closeAccessLog();
			closeErrorLog();
		}
		catch (IOException e) {
				throw new IOException("Could not close log files");
		}
	}
    
}
