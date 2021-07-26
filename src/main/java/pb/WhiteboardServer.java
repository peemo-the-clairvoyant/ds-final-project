package pb;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import pb.managers.IOThread;
import pb.managers.ServerManager;
import pb.managers.endpoint.Endpoint;
import pb.utils.Utils;

/**
 * Simple whiteboard server to provide whiteboard peer notifications.
 * @author aaron
 *
 */
public class WhiteboardServer {
	private static Logger log = Logger.getLogger(WhiteboardServer.class.getName());
	
	/**
	 * Emitted by a client to tell the server that a board is being shared. Argument
	 * must have the format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String shareBoard = "SHARE_BOARD";

	/**
	 * Emitted by a client to tell the server that a board is no longer being
	 * shared. Argument must have the format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String unshareBoard = "UNSHARE_BOARD";

	/**
	 * The server emits this event:
	 * <ul>
	 * <li>to all connected clients to tell them that a board is being shared</li>
	 * <li>to a newly connected client, it emits this event several times, for all
	 * boards that are currently known to be being shared</li>
	 * </ul>
	 * Argument has format "host:port:boardid"
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String sharingBoard = "SHARING_BOARD";

	/**
	 * The server emits this event:
	 * <ul>
	 * <li>to all connected clients to tell them that a board is no longer
	 * shared</li>
	 * </ul>
	 * Argument has format "host:port:boardid"
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String unsharingBoard = "UNSHARING_BOARD";

	/**
	 * Emitted by the server to a client to let it know that there was an error in a
	 * received argument to any of the events above. Argument is the error message.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String error = "ERROR";
	
	/**
	 * Default port number.
	 */
	private static int port = Utils.indexServerPort;
	
	/**
	 * Storage of key value index
	 * peerIP:peerport to list of whiteboards that they have shared.
	 */
	public static final Map<String, Set<String>> keyValueMap = new HashMap<>();

	/*
	 * Keeps track of all peers connected to the server
	 * Maps endpoint ID (host:port) to respective endpoint
	 */
	public static final Map<String, Endpoint>  endpointMap = new HashMap<>();
	
	/**
	 * Add the shared board to the hashmap
	 * 
	 * @param peerport
	 * @param boardName
	 */
	private static void whiteboardShare(String peerport, String boardName) {
		synchronized(keyValueMap) {
			if (!keyValueMap.containsKey(peerport)) {
				keyValueMap.put(peerport, new HashSet<String>());
			}

			Set<String> sharedBoards = keyValueMap.get(peerport);
			sharedBoards.add(boardName);
		}
		// after that notify each client by emitting event
	}

	/**
	 * Remove the shared board from the hashmap
	 * 
	 * @param peerport
	 * @param boardName
	 */
	private static void whiteboardUnshare(String peerport, String boardName) {
		synchronized(keyValueMap) {
			if (!keyValueMap.containsKey(peerport)) {
				log.severe("The peer that wants to unshare a board did no share any board before. peerport: " + peerport);
			} else {
				Set<String> sharedBoards = keyValueMap.get(peerport);
				sharedBoards.remove(boardName);
			}
		}
	}

	/**
	 * Add the endpoint to the hashmap in order to broadcast
	 *
	 * @param endpointid
	 * @param endpoint
	 */
	private static void addEndpoint(String endpointid, Endpoint endpoint){
		synchronized(endpointMap) {
			if (!endpointMap.containsKey(endpointid)) {
				endpointMap.put(endpointid, endpoint);
			}
		}
	}

	/**
	 * Remove the endpoint from the hashmap
	 *
	 * @param endpointid
	 * @param endpoint
	 */
	private static void deleteEndpoint(String endpointid, Endpoint endpoint){
		synchronized(endpointMap) {
			if (!endpointMap.containsKey(endpointid)) {
				log.severe("There is not the endpoint!");
			} else {
				endpointMap.remove(endpointid);
			}
		}
	}

	/*
	 * Tell all currently connected boards about this new board, so they can connect to it.
	 */
	private static void broadcast(String boardName, Endpoint endpoint, boolean isShare){
		Iterator<Map.Entry<String, Endpoint>> iter = endpointMap.entrySet().iterator();
		while(iter.hasNext()) {
			Map.Entry<String, Endpoint> entry = (Map.Entry<String, Endpoint>) iter.next();
			String key = (String) entry.getKey();
			Endpoint val = (Endpoint) entry.getValue();
			if(isShare) {
				if (!key.equals(endpoint.getOtherEndpointId())) {
					val.emit(sharingBoard, boardName);
				}
			}
			else{
				if (!key.equals(endpoint.getOtherEndpointId())) {
					val.emit(unsharingBoard, boardName);
				}
			}
		}
	}

	private static void help(Options options){
		String header = "PB Whiteboard Server for Unimelb COMP90015\n\n";
		String footer = "\ncontact aharwood@unimelb.edu.au for issues.";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("pb.IndexServer", header, options, footer, true);
		System.exit(-1);
	}

	private static void sendExistingBoards(Endpoint endpoint) {
		Iterator<Entry<String, Set<String>>> it = keyValueMap.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, Set<String>> entry = it.next();
			String peerport = entry.getKey();
			Set<String> boardnameSet = entry.getValue();

			boardnameSet.forEach((boardname) -> {
				log.info("sending SHARING_BOARD event to peer " + endpoint.getOtherEndpointId());
				endpoint.emit(sharingBoard, peerport + ":" + boardname);
			});
		}
	}
	
	public static void main( String[] args ) throws IOException, InterruptedException
    {
    	// set a nice log format
		System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tl:%1$tM:%1$tS:%1$tL] [%4$s] %2$s: %5$s%n");
        
    	// parse command line options
        Options options = new Options();
        options.addOption("port",true,"server port, an integer");
        options.addOption("password",true,"password for server");
        
       
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
			cmd = parser.parse( options, args);
		} catch (ParseException e1) {
			help(options);
		}
        
        if(cmd.hasOption("port")){
        	try{
        		port = Integer.parseInt(cmd.getOptionValue("port"));
			} catch (NumberFormatException e){
				System.out.println("-port requires a port number, parsed: "+cmd.getOptionValue("port"));
				help(options);
			}
        }

        // create a server manager and setup event handlers
        ServerManager serverManager;
        
        if(cmd.hasOption("password")) {
        	serverManager = new ServerManager(port,cmd.getOptionValue("password"));
        } else {
        	serverManager = new ServerManager(port);
        }
        
        /**
         * TODO: Put some server related code here.
         */

		serverManager.on(ServerManager.sessionStarted, (eventArgs) -> {
			Endpoint endpoint = (Endpoint) eventArgs[0];
			log.info("Client session started: " + endpoint.getOtherEndpointId());
			addEndpoint(endpoint.getOtherEndpointId(), endpoint);
			sendExistingBoards(endpoint);

			endpoint.on(shareBoard, (eventArgs2) -> {
				String boardData = (String) eventArgs2[0];

				String[] parts = boardData.split(":");
				if (parts.length != 3) {
					endpoint.emit(error, "wrong sharingBoard format: "+boardData);
					log.warning("Peer tried sharing board using incorrect format: "+boardData);
				} else {
					String peerPort = parts[0] + ":" + parts[1];
					String boardName = parts[2];
					log.info("Received event SHARE_BOARD from peer <" + peerPort + "> for board <" + boardName + ">.");

					whiteboardShare(peerPort, boardName);
					broadcast(boardData, endpoint, true);
				}

			}).on(unshareBoard, (eventArgs3) -> {
				String boardData = (String) eventArgs3[0];

				String[] parts = boardData.split(":");
				if (parts.length != 3) {
					endpoint.emit(error, "wrong sharingBoard format: "+boardData);
					log.warning("Peer tried sharing board using incorrect format: "+boardData);
				} else {
					String peerPort = parts[0] + ":" + parts[1];
					String boardName = parts[2];
					log.info("Received event UNSHARE_BOARD from peer <" + peerPort + "> for board <" + boardName + ">.");

					whiteboardUnshare(peerPort, boardName);
					broadcast(boardData, endpoint, false);
				}
			});
			
		}).on(ServerManager.sessionStopped, (eventArgs) -> {
			Endpoint endpoint = (Endpoint) eventArgs[0];
			log.info("Client session ended: " + endpoint.getOtherEndpointId());
			deleteEndpoint(endpoint.getOtherEndpointId(), endpoint);
		}).on(ServerManager.sessionError, (eventArgs) -> {
			Endpoint endpoint = (Endpoint) eventArgs[0];
			log.warning("Client session ended in error: " + endpoint.getOtherEndpointId());
		}).on(IOThread.ioThread, (eventArgs) -> {
			String peerPort = (String) eventArgs[0];
			log.info("using Internet address: " + peerPort);
		});
		 
        // start up the server
        log.info("Whiteboard Server starting up");
        serverManager.start();
        // nothing more for the main thread to do
        serverManager.join();
        Utils.getInstance().cleanUp();
        
    }

}
