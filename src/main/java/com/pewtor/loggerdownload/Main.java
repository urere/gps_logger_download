package com.pewtor.loggerdownload;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import jssc.SerialPort;
import jssc.SerialPortList;

public class Main {

	public static void main(String[] args) throws Exception {

		Main main = new Main();
		main.start( args );
	}
	
	private SerialPort serialPort = null;	

	private Main() {
	}

	private void start( String[] args ) throws Exception {
		
		System.out.println( "Logger Download");
		System.out.println( "===============");
		
		String port = "COM8";
		List<String> commands = new ArrayList<String>();

		// Extract commands from arguments
		for ( String arg : args ) {
			if ( arg.startsWith( "PORT=" ) ) {
			
				// Extract port number
				port = arg.substring(5);
				
			} else {
				commands.add( arg );
			}
		}	
		if ( commands.isEmpty() ) {
			commands.add( "HELP" );
		}

		// Process each command
		for ( String command: commands ) {
		
			switch ( command ) {
				case "HELP":
					 commandHelp();
					break;
				case "PORTS":
					commandPorts();
					break;
				default:
					executeSerialCommand( port, command );
					break;
			}
		}
	}
	
	private void executeSerialCommand( String port, String command ) throws Exception {
		
		System.out.println( command );
		System.out.println( "  Using Port: [" + port + "]" );
		serialPort = new SerialPort( port ); 

        try {
            serialPort.openPort();
            serialPort.setParams(115200, 8, 1, 0);
            	
        	switch ( command ) {
            	case "LIST":
        			commandList();
            		break;
        		case "DOWNLOADALLGPX":
        			commandDownloadAllGpx();
        			break;
        		case "DELETEALL":
        			commandDeleteAll();
        			break;
        		default:
                    System.out.println( "  Command: [" + command + "] not recognised" );
        			break;	
        	}
            
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
        finally {
        	if ( serialPort != null ) {
        		serialPort.closePort();
        	}
        }
		
	}

	private void commandHelp() {
        System.out.println( "HELP" );
        System.out.println( "  HELP ------------- List available commands" );
        System.out.println( "  PORT=<port>------- Set com port to use" );
        System.out.println( "  PORTS ------------ List available com ports" );
        System.out.println( "  LIST ------------- List available logs" );
        System.out.println( "  DOWNLOADALLGPX --- Downloads all logs in GPX format" );
        System.out.println( "  DELETEALL -------- Delete all logs from logger" );
	}
	private void commandPorts() throws Exception {
		
        System.out.println( "PORTS" );
        List<String> availablePorts = getAvailablePorts();
        if ( !availablePorts.isEmpty() ) {
	        for ( String portName : availablePorts ) {
	        	System.out.println( "  " + portName );
	        }
        } else {
        	System.out.println( "  No ports available" );
        }
	}
	
	private void commandList() throws Exception {
		
        List<String> availableLogs = getAvailableLogs();
        if ( !availableLogs.isEmpty() ) {
	        for ( String logName : availableLogs ) {
	        	System.out.println( "  " + logName );
	        }
        } else {
        	System.out.println( "  No logs available" );
        }
	}

	private void commandDeleteAll() throws Exception {
		
        List<String> deletedLogs = deleteAllLogs();
        if ( !deletedLogs.isEmpty() ) {
	        for ( String logName : deletedLogs ) {
	        	System.out.println( "  Deleted: [" + logName + "]" );
	        }
        } else {
        	System.out.println( "  No logs deleted" );
        }
	}
	
	private void commandDownloadAllGpx()  throws Exception {
		
        List<String> availableLogs = getAvailableLogs();
       	downloadAllLogsGpx( availableLogs );
	}
	
	private void downloadAllLogsGpx( List<String> availableLogs ) throws Exception  {
		
		if ( !availableLogs.isEmpty() ) {
	        for ( String logName : availableLogs ) {
	        	System.out.println( "  Log: [" + logName + "]" );
	        	List<String> logLines = getLog( logName );
	    		System.out.println( "    " + logLines.size() + " lines" );
	    		String localLogFile = "sailing-" + logName.toLowerCase().replace("-", "") + ".gpx";
	    		writeLogToFile( logLines, localLogFile );
	    		System.out.println( "    Log file written to: " + localLogFile );
	        }
		} else {
        	System.out.println( "  No logs available to download" );
		}
        
	}

	private List<String> getAvailableLogs() throws Exception {
		
        serialPort.writeString( "LIST\r\n" );
        List<String> availableLogs = readResponse();
        
        return availableLogs;
	}

	private List<String> deleteAllLogs() throws Exception {
		
        serialPort.writeString( "DELETEALL\r\n" );
        List<String> deletedLogs = readResponse();
        
        return deletedLogs;
	}
	
	private List<String> getLog( String logName ) throws Exception {
		
        serialPort.writeString( "GETGPX=" + logName + "\r\n" );
        List<String> logContent = readResponse();
        
        return logContent;
	}
	
	private void writeLogToFile( List<String> logLines, String logName ) throws Exception {
		
		try ( FileWriter writer = new FileWriter(logName) ) { 
    		for(String line: logLines) {
    			writer.write(line + "\r\n");
    		}
		}
	}
	
	private List<String> readResponse( ) throws Exception {
	
        List<String> respLines = new ArrayList<String>();
        
        boolean reading = true;
        StringBuffer lineBuffer = new StringBuffer();
        while ( reading ) {
        	String data = serialPort.readString(1, 2000);
        	if ( data != null ) {
            	if ( data.equals( "\n" ) ) {
            		// End of line
            		String line = lineBuffer.toString();
            		if ( line.startsWith( "<<<" ) ) {
            			// Ignore
            		} else if ( line.equals( ">>>" ) ) {
            			// End of response
            			reading = false;
            		} else {
            			// Store the line
	            		respLines.add( line );
            		}
            		// New buffer for the next line
                    lineBuffer = new StringBuffer();
            	} else if ( !data.equals( "\r" ) ) {
            		// Store it, ignore CR
            		lineBuffer.append( data );
            	}
        	}
        }
		
        return respLines;
	}

	private List<String> getAvailablePorts() {
		
		List<String> portList = new ArrayList<String>();
		String[] portNames = SerialPortList.getPortNames();
		for ( String pn : portNames ) {
			portList.add(pn);
		}
		
		return portList;
	}	
	
}
