package pb.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import pb.WhiteboardServer;
import pb.managers.ClientManager;
import pb.managers.IOThread;
import pb.managers.PeerManager;
import pb.managers.ServerManager;
import pb.managers.endpoint.Endpoint;
import pb.utils.Utils;

/**
 * Initial code obtained from:
 * https://www.ssaurel.com/blog/learn-how-to-make-a-swing-painting-and-drawing-application/
 */
public class WhiteboardApp {
	private static Logger log = Logger.getLogger(WhiteboardApp.class.getName());

	/**
	 * Emitted to another peer to subscribe to updates for the given board. Argument
	 * must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String listenBoard = "BOARD_LISTEN";

	/**
	 * Emitted to another peer to unsubscribe to updates for the given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String unlistenBoard = "BOARD_UNLISTEN";

	/**
	 * Emitted to another peer to get the entire board data for a given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String getBoardData = "GET_BOARD_DATA";

	/**
	 * Emitted to another peer to give the entire board data for a given board.
	 * Argument must have format "host:port:boardid%version%PATHS".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardData = "BOARD_DATA";

	/**
	 * Emitted to another peer to add a path to a board managed by that peer.
	 * Argument must have format "host:port:boardid%version%PATH". The numeric value
	 * of version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathUpdate = "BOARD_PATH_UPDATE";

	/**
	 * Emitted to another peer to indicate a new path has been accepted. Argument
	 * must have format "host:port:boardid%version%PATH". The numeric value of
	 * version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathAccepted = "BOARD_PATH_ACCEPTED";

	/**
	 * Emitted to another peer to remove the last path on a board managed by that
	 * peer. Argument must have format "host:port:boardid%version%". The numeric
	 * value of version must be equal to the version of the board without the undo
	 * applied, i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoUpdate = "BOARD_UNDO_UPDATE";

	/**
	 * Emitted to another peer to indicate an undo has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the undo applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoAccepted = "BOARD_UNDO_ACCEPTED";

	/**
	 * Emitted to another peer to clear a board managed by that peer. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearUpdate = "BOARD_CLEAR_UPDATE";

	/**
	 * Emitted to another peer to indicate an clear has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearAccepted = "BOARD_CLEAR_ACCEPTED";

	/**
	 * Emitted to another peer to indicate a board no longer exists and should be
	 * deleted. Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardDeleted = "BOARD_DELETED";

	/**
	 * Emitted to another peer to indicate an error has occurred.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardError = "BOARD_ERROR";

	/**
	 * White board map from board name to board object
	 */
	Map<String, Whiteboard> whiteboards;

	/**
	 * The currently selected white board
	 */
	Whiteboard selectedBoard = null;

	/**
	 * The peer:port string of the peer. This is synonomous with IP:port, host:port,
	 * etc. where it may appear in comments.
	 */
	String peerport = "standalone"; // default value for non-distributed version
	private static String whiteboardServerHost = Utils.serverHost;
	private static int whiteboardServerPort = Utils.serverPort;

	/*
	 * GUI objects, you probably don't need to modify these things... you don't need
	 * to modify these things... don't modify these things [LOTR reference?].
	 */
	JButton clearBtn, blackBtn, redBtn, createBoardBtn, deleteBoardBtn, undoBtn;
	JCheckBox sharedCheckbox;
	DrawArea drawArea;
	JComboBox<String> boardComboBox;
	boolean modifyingComboBox = false;
	boolean modifyingCheckBox = false;

	ServerManager serverManager = null;
	ClientManager clientManager = null;
	PeerManager peerManager = null;

	// client's connection to the whiteboard server
	Endpoint clientEndpoint = null;

	// A HashMap of <boardname, Set<endpointID>>, shared boards mapping to the peers listening to it.
	Map<String, Map<String, Endpoint>> sharedBoardMap = new HashMap<>();

	// A HashMap of host:port to all the clientManagers in this whiteboardApp that connects to another peer.
	Map<String, Map<ClientManager, Endpoint>> clientManagerMap = new HashMap<>();

	// A HashMap of <ClientManager, Set<boardName>>, clientmanager mapping to the boards that it connects to.
	Map<ClientManager, Set<String>> remoteBoardMap = new HashMap<>();

	/**
	 * Initialize the white board app.
	 */
	public WhiteboardApp(int peerPort, String whiteboardServerHost, int whiteboardServerPort) {
		whiteboards = new HashMap<>();

		WhiteboardApp.whiteboardServerHost = whiteboardServerHost;
		WhiteboardApp.whiteboardServerPort = whiteboardServerPort;

		try {
			shareBoards(peerPort);
			Thread.sleep(1000); // allow for peerport update
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}

		show(this.peerport);
	}

	/******
	 * Utility methods to extract fields from argument strings.
	 ******/

	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer:port:boardid
	 */
	public static String getBoardName(String data) {
		String[] parts = data.split("%", 2);
		return parts[0];
	}

	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return boardid%version%PATHS
	 */
	public static String getBoardIdAndData(String data) {
		String[] parts = data.split(":");
		return parts[2];
	}

	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version%PATHS
	 */
	public static String getBoardData(String data) {
		String[] parts = data.split("%", 2);
		return parts[1];
	}

	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version
	 */
	public static long getBoardVersion(String data) {
		String[] parts = data.split("%", 3);
		return Long.parseLong(parts[1]);
	}

	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return PATHS
	 */
	public static String getBoardPaths(String data) {
		String[] parts = data.split("%", 3);
		return parts[2];
	}

	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer
	 */
	public static String getIP(String data) {
		String[] parts = data.split(":");
		return parts[0];
	}

	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return port
	 */
	public static int getPort(String data) {
		String[] parts = data.split(":");
		return Integer.parseInt(parts[1]);
	}

	/**
	 * 
	 * @param data = peer:port:boardid
	 * @return
	 */
	public static String getOtherPeerId(String data) {
		String[] parts = data.split(":");
		return parts[0] + (parts[1]);
	}

	/******
	 * 
	 * Methods called from events.
	 * 
	 ******/

	/*
	 * Subscribe to remote boards the server has told us about
	 * Listen for boardUpdates from those remote boards once connected
	 * Store references to those remote boards in <clientManagerMap> so they can be accessed and communicated with
	 */
	private void subscribeToBoard(PeerManager peerManager, String peerport, String boardname)
			throws InterruptedException, UnknownHostException {

		System.out.println(boardname);
		String[] parts = boardname.split(":");
		ClientManager peerClientManager;
		String theOtherPeerId = parts[0] + parts[1];

		// if we have not connected to the peer.
		if (!clientManagerMap.containsKey(theOtherPeerId)) {

			try {
				peerClientManager = peerManager.connect(Integer.parseInt(parts[1]), parts[0]);
				log.info("Connected to the board: " + boardname);
			} catch (NumberFormatException e) {
				System.out.println("Information about the board is bad, port is not a number: " + parts[1]);
				return;
			} catch (UnknownHostException e) {
				System.out.println("Could not find the peer IP address: " + parts[0]);
				return;
			}

			peerClientManager.on(PeerManager.peerStarted, (args) -> {
				Endpoint endpoint = (Endpoint) args[0];

				// add the new clientManager and endpoint to the map for later reference.
				Map<ClientManager, Endpoint> clientManagerKeyValue = new HashMap<>();
				clientManagerKeyValue.put(peerClientManager, endpoint);
				clientManagerMap.put(theOtherPeerId, clientManagerKeyValue);
				
				// register the clientmanager and the boardname to the map.
				Set<String> boardnameSet = new HashSet<>();
				boardnameSet.add(boardname);
				remoteBoardMap.put(peerClientManager, boardnameSet);

				endpoint.on(boardData, (args2) -> {
					String data = (String) args2[0];
					String boardName = getBoardName(data);
					String boardInfo = getBoardData(data);
					log.info("Received board data of " + boardName);
					Whiteboard remoteWhiteboard = new Whiteboard(boardName, true);
					remoteWhiteboard.whiteboardFromString(boardName, boardInfo);
					addBoard(remoteWhiteboard, false);

				}).on(boardPathAccepted, (args3) -> {
					log.info("Board path has been accepted");
				}).on(boardUndoAccepted, (args4) -> {
					log.info("Board undo has been accepted");
				}).on(boardClearAccepted, (args5) -> {
					log.info("Board clear has been accepted");
				}).on(boardDeleted, (args6) -> {
					String name = (String) args6[0];

					log.info("Received BOARD_DELETE event for board " + name);
					deleteBoard(name);

				}).on(boardError, (args7) -> {
					String error = (String)args7[0];
					log.warning("Received boardError from remoteBoard peer: "+error);
				});

				System.out.println("Subscribing to board " + boardname);
				endpoint.emit(listenBoard, boardname);
				endpoint.emit(getBoardData, boardname);

			}).on(PeerManager.peerStopped, (args) -> {
				Endpoint endpoint = (Endpoint) args[0];
				System.out.println("Disconnected from peer: "+endpoint.getOtherEndpointId());
			}).on(PeerManager.peerError, (args) -> {
				Endpoint endpoint = (Endpoint) args[0];
				System.out.println("Error while communicating with peer: "+endpoint.getOtherEndpointId());
			});

			peerClientManager.start();
		} else {
			peerClientManager = clientManagerMap.get(theOtherPeerId).entrySet().iterator().next().getKey();
			Endpoint endpoint = clientManagerMap.get(theOtherPeerId).entrySet().iterator().next().getValue();
			endpoint.emit(listenBoard, boardname);
			endpoint.emit(getBoardData, boardname);

			Set<String> boardnameSet = remoteBoardMap.get(peerClientManager);
			boardnameSet.add(boardname);
		}		
	}

	/*
	 * Manage connections to the server
	 * Listen for sharingBoard & unsharingBoard events
	 */
	private void connectToServer(PeerManager peerManager, String peerport)
			throws UnknownHostException, InterruptedException {
		// connect to the Whiteboard server and tell it we are sharing a board.
		clientManager = peerManager.connect(whiteboardServerPort, whiteboardServerHost);

		clientManager.on(PeerManager.peerStarted, (args) -> {
			clientEndpoint = (Endpoint) args[0];

			System.out.println("Connected to whiteboard server: " + clientEndpoint.getOtherEndpointId());
			this.peerport = peerport;
			clientEndpoint.on(WhiteboardServer.sharingBoard, (args2) -> {

				// register the board to the whiteboards, and connect to the remote peer
				log.info("Received event SHARING_BOARD from the whiteboard server");
				String boardname = (String) args2[0];
				try {
					subscribeToBoard(peerManager, peerport, boardname);
				} catch (Exception e) {
					log.warning("couldn't subscribe to board: "+e.getMessage());
				}

			}).on(WhiteboardServer.unsharingBoard, (args3) -> {
				String boardname = (String) args3[0];

				// unregister board from whiteboards, and if no other whiteboards from that peer, disconnect.
				deleteBoard(boardname);
				log.info("Delete a remote board:" + boardname);

			}).on(WhiteboardServer.error, (args4) -> {
				String errorEventName = (String) args4[0];
				log.severe("The event " + errorEventName + " has missing arguments");
			});

		}).on(PeerManager.peerStopped, (args) -> {
			Endpoint endpoint = (Endpoint) args[0];
			System.out.println("Disconnected from the Whiteboard server: " + endpoint.getOtherEndpointId());
		}).on(PeerManager.peerError, (args) -> {
			Endpoint endpoint = (Endpoint) args[0];
			System.out.println("Error communicating with the whiteboard server: " + endpoint.getOtherEndpointId());
		});

		clientManager.start();
	}

	/*
	 * Start listening for connections from peers who want to listen to a board we have shared
	 * Connect to the whiteboardServer once our serverManager is ready
	 */
	private void shareBoards(int peerPort) throws InterruptedException, IOException {
		peerManager = new PeerManager(peerPort);

		peerManager.on(PeerManager.peerStarted, (args) -> {
			Endpoint endpoint = (Endpoint) args[0];
			System.out.println("Connection from peer: " + endpoint.getOtherEndpointId());

			endpoint.on(listenBoard, (args2) -> {
				String boardname = (String) args2[0];

				// have a HashMap of <String, Set<String>>, boardname mapping to a list of peers listening to this board.
				log.info("Received LISTEN_BOARD event from peer: " + endpoint.getOtherEndpointId());
				if (!sharedBoardMap.containsKey(boardname)) {
					// if it is not already set up, create an empty hash set first.
					sharedBoardMap.put(boardname, new HashMap<>());
				}
				Map<String, Endpoint> sharedPeers = sharedBoardMap.get(boardname);
				sharedPeers.put(endpoint.getOtherEndpointId(), endpoint);

			}).on(unlistenBoard, (args3) -> {
				String boardname = (String) args3[0];

				// deletes the peer from the HashMap
				log.info("Received UNLISTEN_BOARD event for board " + boardname + " from peer " + endpoint.getOtherEndpointId());
				Map<String, Endpoint> sharedPeers = sharedBoardMap.get(boardname);
				sharedPeers.remove(endpoint.getOtherEndpointId());

			}).on(getBoardData, (args4) -> {
				String boardname = (String) args4[0];

				// send the specifed board with its data (stored in the hashmap)
				String data = whiteboards.get(boardname).toString();
				endpoint.emit(boardData, data);

			}).on(boardPathUpdate, (args5) -> {
				String data = (String) args5[0];

				String boardname = getBoardName(data);
				String paths = getBoardPaths(data);
				log.info("Received BOARD_PATH_UPDATE event for board "+boardname+" from peer "+endpoint.getOtherEndpointId());
				Whiteboard whiteboard = whiteboards.get(boardname);

				whiteboard.addPath(new WhiteboardPath(paths), getBoardVersion(data));
				// look for the endpoints that are communicating with other peers.
				// only share update to other peers not the one that sends the update
				// check if the update is ok, if it is then send accept event to the peer that sends the update.
				Endpoint peerEndpoint;
				Iterator<Map.Entry<String, Endpoint>> it = sharedBoardMap.get(boardname).entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, Endpoint> pair = (Map.Entry<String, Endpoint>) it.next();
					peerEndpoint = (Endpoint) pair.getValue();
					if (peerEndpoint.equals(endpoint)) {
						// there might be some check for acceptance
						log.info("emitting BOARD_PATH_ACCEPTED to peer " + peerEndpoint.getOtherEndpointId() + " for board " + boardname);
					} else {
						log.info("emitting BOARD_PATH_UPDATE to peer " + peerEndpoint.getOtherEndpointId()
								+ "for board " + boardname);
						peerEndpoint.emit(boardData, whiteboard.toString());
					}
				}

				drawSelectedWhiteboard();

			}).on(boardUndoUpdate, (args6) -> {
				String data = (String) args6[0];

				String boardname = getBoardName(data);
				long versionBeingUpdated = getBoardVersion(data);
				log.info("Received BOARD_UNDO_UPDATE event for board " + boardname);
				Whiteboard whiteboard = whiteboards.get(boardname);
				
				whiteboard.undo(versionBeingUpdated);
				// look for the endpoints that are communicating with other peers.
				// only share update to other peers not the one that sends the update
				// check if the update is ok, if it is then send accept event to the peer that
				// sends the update.

				Endpoint peerEndpoint;
				Iterator<Map.Entry<String, Endpoint>> it = sharedBoardMap.get(boardname).entrySet().iterator();

				while (it.hasNext()) {
					Map.Entry<String, Endpoint> pair = (Map.Entry<String, Endpoint>) it.next();
					peerEndpoint = (Endpoint) pair.getValue();
					if (peerEndpoint.equals(endpoint)) {
						// there might be some check for acceptance
						log.info("emitting BOARD_UNDO_ACCEPTED to peer " + peerEndpoint.getOtherEndpointId()
								+ " for board " + boardname);
					} else {
						log.info("emitting BOARD_UNDO_UPDATE to peer " + peerEndpoint.getOtherEndpointId()
								+ "for board " + boardname);
						peerEndpoint.emit(boardData, whiteboard.toString());
					}
				}

				drawSelectedWhiteboard();
			}).on(boardClearUpdate, (args7) -> {
				String data = (String) args7[0];

				String boardname = getBoardName(data);
				long versionBeingUpdated = getBoardVersion(data);
				log.info("Received BOARD_CLEAR_UPDATE event for board " + boardname);
				Whiteboard whiteboard = whiteboards.get(boardname);

				whiteboard.clear(versionBeingUpdated);
				// look for the endpoints that are communicating with other peers.
				// only share update to other peers not the one that sends the update
				// check if the update is ok, if it is then send accept event to the peer that
				// sends the update.
				Endpoint peerEndpoint;
				Iterator<Map.Entry<String, Endpoint>> it = sharedBoardMap.get(boardname).entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, Endpoint> pair = (Map.Entry<String, Endpoint>) it.next();
					peerEndpoint = (Endpoint) pair.getValue();
					if (peerEndpoint.equals(endpoint)) {
						// there might be some check for acceptance
						log.info("emitting BOARD_CLEAR_ACCEPTED to peer " + peerEndpoint.getOtherEndpointId()
								+ " for board " + boardname);
					} else {
						log.info("emitting BOARD_CLEAR_UPDATE to peer " + peerEndpoint.getOtherEndpointId()
								+ "for board " + boardname);
						peerEndpoint.emit(boardData, whiteboard.toString());
					}
				}
				drawSelectedWhiteboard();

			}).on(boardError, (args8) -> {
				String eventName = (String) args8[0];
				log.info("Encountered an error for event " + eventName);
			});
		}).on(PeerManager.peerStopped, (args) -> {
			Endpoint endpoint = (Endpoint) args[0];
			System.out.println("Disconnected from peer: " + endpoint.getOtherEndpointId());
		}).on(PeerManager.peerError, (args) -> {
			Endpoint endpoint = (Endpoint) args[0];
			System.out.println("There was an error communicating with the peer: " + endpoint.getOtherEndpointId());
		}).on(PeerManager.peerServerManager, (args) -> {
			serverManager = (ServerManager) args[0];

			serverManager.on(IOThread.ioThread, (args2) -> {
				String peerport = (String) args2[0];

				try {
					connectToServer(peerManager, peerport);
				} catch (UnknownHostException e) {
					System.out.println("The whiteboard server host could not be found: " + whiteboardServerHost);
				} catch (InterruptedException e) {
					System.out.println("Interrupted while trying to send updates to the whiteboard server");
				}
			});
		});

		peerManager.start();
	}

	/******
	 * 
	 * Methods to manipulate data locally. Distributed systems related code has been
	 * cut from these methods.
	 * 
	 ******/

	/**
	 * Wait for the peer manager to finish all threads.
	 */
	public void waitToFinish() {
		try {
			clientManager.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (clientEndpoint != null) {
			try {
				clientEndpoint.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Add a board to the list that the user can select from. If select is true then
	 * also select this board.
	 * 
	 * @param whiteboard
	 * @param select
	 */
	public void addBoard(Whiteboard whiteboard, boolean select) {
		synchronized (whiteboards) {
			whiteboards.put(whiteboard.getName(), whiteboard);
		}
		updateComboBox(select ? whiteboard.getName() : null);
	}

	/**
	 * Delete a board from the list.
	 * 
	 * @param boardname must have the form peer:port:boardid
	 */
	public void deleteBoard(String boardname) {
		synchronized (whiteboards) {
			Whiteboard whiteboard = whiteboards.get(boardname);
			if (whiteboard != null) {
				// if it is local, delete and notify all the peers that listens to it,
				// if it is remote, unlisten from the remote peer.
				if (whiteboard.isRemote()) {
					Map<ClientManager, Endpoint> clientManagerKeyValue = clientManagerMap.get(getOtherPeerId(boardname));

					ClientManager peerClientManager = clientManagerKeyValue.entrySet().iterator().next().getKey();
					Endpoint endpoint = clientManagerKeyValue.entrySet().iterator().next().getValue();
					log.info("Sending UNLISTEN_BOARD event for board " + boardname);
					endpoint.emit(unlistenBoard, boardname);

					Set<String> boardnameSet = remoteBoardMap.get(peerClientManager);
					boardnameSet.remove(boardname);
					if (boardnameSet.isEmpty()) {
						// if the connection to the other peer is no longer required.
						peerClientManager.shutdown();
						clientManagerMap.remove(getOtherPeerId(boardname));
					}
				} else {
					if (sharedBoardMap.get(boardname) != null) {
						Iterator<Map.Entry<String, Endpoint>> it = sharedBoardMap.get(boardname).entrySet().iterator();
						while (it.hasNext()) {
							Map.Entry<String, Endpoint> pair = (Map.Entry<String, Endpoint>) it.next();
							Endpoint endpoint = (Endpoint) pair.getValue();
							log.info("emitting BOARD_DELETED event to peer " + endpoint.getOtherEndpointId()
									+ "for board " + boardname);
							endpoint.emit(boardDeleted, boardname);
						}
					}

					// unshare it before deleting.
					if (whiteboard.isShared()) {
						clientManager.emit(WhiteboardServer.unshareBoard, boardname);
					}
				}
				whiteboards.remove(boardname);
			}
		}
		updateComboBox(null);
	}

	/**
	 * Create a new local board with name peer:port:boardid. The boardid includes
	 * the time stamp that the board was created at.
	 */
	public void createBoard() {
		String name = peerport + ":board" + Instant.now().toEpochMilli();
		Whiteboard whiteboard = new Whiteboard(name, false);
		addBoard(whiteboard, true);
	}

	/**
	 * Add a path to the selected board. The path has already been drawn on the draw
	 * area; so if it can't be accepted then the board needs to be redrawn without
	 * it.
	 * 
	 * @param currentPath
	 */
	public void pathCreatedLocally(WhiteboardPath currentPath) {
		if (selectedBoard != null) {
			String nameAndVersion = selectedBoard.getNameAndVersion();

			if (!selectedBoard.addPath(currentPath, selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard(); // just redraw the screen without the path
			} else {
				// was accepted locally, so do remote stuff if needed
				Endpoint endpoint;
				String boardname = selectedBoard.getName();
				String data = nameAndVersion + "%" + currentPath.toString();

				if (selectedBoard.isRemote()) {
					endpoint = clientManagerMap.get(getOtherPeerId(boardname)).entrySet().iterator().next().getValue();
					endpoint.emit(boardPathUpdate, data);
				} else if (selectedBoard.isShared()){
					broadcastUpdate(boardname);
				}

				// drawSelectedWhiteboard();
			}
		} else {
			log.severe("path created without a selected board: " + currentPath);
		}
	}

	/**
	 * Clear the selected whiteboard.
	 */
	public void clearedLocally() {
		if (selectedBoard != null) {
			String nameAndVersion = selectedBoard.getNameAndVersion();
			if (!selectedBoard.clear(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				// was accepted locally, so do remote stuff if needed
				Endpoint endpoint;
				String boardname = selectedBoard.getName();
				String data = nameAndVersion;

				if (selectedBoard.isRemote()) {
					endpoint = clientManagerMap.get(getOtherPeerId(boardname)).entrySet().iterator().next().getValue();
					log.info("emitting the update to the peer sharing the board");
					endpoint.emit(boardClearUpdate, data);
				} else if (selectedBoard.isShared()) {
					broadcastUpdate(boardname);
				}
				drawSelectedWhiteboard();
			}
		} else {
			log.severe("cleared without a selected board");
		}
	}

	/**
	 * Undo the last path of the selected whiteboard.
	 */
	public void undoLocally() {
		if (selectedBoard != null) {
			String nameAndVersion = selectedBoard.getNameAndVersion();
			if (!selectedBoard.undo(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				Endpoint endpoint;
				String boardname = selectedBoard.getName();
				String data = nameAndVersion;
				if (selectedBoard.isRemote()) {
					endpoint = clientManagerMap.get(getOtherPeerId(boardname)).entrySet().iterator().next().getValue();
					endpoint.emit(boardUndoUpdate, data);
				} else if (selectedBoard.isShared()) {
					broadcastUpdate(boardname);
				}

				drawSelectedWhiteboard();
			}
		} else {
			log.severe("undo without a selected board");
		}
	}

	public void broadcastUpdate(String boardname) {
		Endpoint endpoint;
		Iterator<Map.Entry<String, Endpoint>> it = sharedBoardMap.get(boardname).entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry<String, Endpoint> pair = (Map.Entry<String, Endpoint>) it.next();
			endpoint = (Endpoint) pair.getValue();
			log.info("emitting BOARD_DATA to peer " + endpoint.getOtherEndpointId() + "for board " + boardname);
			endpoint.emit(boardData, selectedBoard.toString());
		}
	}

	/**
	 * The variable selectedBoard has been set.
	 */
	public void selectedABoard() {
		drawSelectedWhiteboard();

		log.info("selected board: " + selectedBoard.getName());
	}

	/**
	 * Set the share status on the selected board.
	 */
	public void setShare(boolean share) {
		if (selectedBoard != null) {
			selectedBoard.setShared(share);

			if (clientEndpoint != null) {
				if (share) {
					clientEndpoint.emit(WhiteboardServer.shareBoard, selectedBoard.getName());
				} else {
					clientEndpoint.emit(WhiteboardServer.unshareBoard, selectedBoard.getName());
				}
			} else {
				log.severe("the peer is not connected to a server, unable to share.");
			}
		} else {
			log.severe("there is no selected board");
		}
	}

	/**
	 * Called by the gui when the user closes the app.
	 */
	public void guiShutdown() {
		// do some final cleanup
		HashSet<Whiteboard> existingBoards = new HashSet<>(whiteboards.values());
		existingBoards.forEach((board) -> {
			deleteBoard(board.getName());
		});
		// whiteboards.values().forEach((whiteboard) -> {

		// });

		
		Iterator<Map<ClientManager, Endpoint>> it = clientManagerMap.values().iterator();
		while (it.hasNext()) {
			Map<ClientManager, Endpoint> clientMap = (Map<ClientManager, Endpoint>) it.next();
			ClientManager cm = clientMap.keySet().iterator().next();
			cm.shutdown();
		}

		if (clientManager != null && peerport != "standalone") {
			clientManager.shutdown();
		} else {

		}

		if (peerManager != null && peerport != "standalone") {
			peerManager.shutdown();
		}		
	}

	/******
	 * 
	 * GUI methods and callbacks from GUI for user actions. You probably do not need
	 * to modify anything below here.
	 * 
	 ******/

	/**
	 * Redraw the screen with the selected board
	 */
	public void drawSelectedWhiteboard() {
		drawArea.clear();
		if (selectedBoard != null) {
			selectedBoard.draw(drawArea);
		}
	}

	/**
	 * Setup the Swing components and start the Swing thread, given the peer's
	 * specific information, i.e. peer:port string.
	 */
	public void show(String peerport) {
		// create main frame
		JFrame frame = new JFrame("Whiteboard Peer: " + peerport);
		Container content = frame.getContentPane();
		// set layout on content pane
		content.setLayout(new BorderLayout());
		// create draw area
		drawArea = new DrawArea(this);

		// add to content pane
		content.add(drawArea, BorderLayout.CENTER);

		// create controls to apply colors and call clear feature
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

		/**
		 * Action listener is called by the GUI thread.
		 */
		ActionListener actionListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == clearBtn) {
					clearedLocally();
				} else if (e.getSource() == blackBtn) {
					drawArea.setColor(Color.black);
				} else if (e.getSource() == redBtn) {
					drawArea.setColor(Color.red);
				} else if (e.getSource() == boardComboBox) {
					if (modifyingComboBox)
						return;
					if (boardComboBox.getSelectedIndex() == -1)
						return;
					String selectedBoardName = (String) boardComboBox.getSelectedItem();
					if (whiteboards.get(selectedBoardName) == null) {
						log.severe("selected a board that does not exist: " + selectedBoardName);
						return;
					}

					selectedBoard = whiteboards.get(selectedBoardName);
					// remote boards can't have their shared status modified
					if (selectedBoard.isRemote()) {
						sharedCheckbox.setEnabled(false);
						sharedCheckbox.setVisible(false);
					} else {
						modifyingCheckBox = true;
						sharedCheckbox.setSelected(selectedBoard.isShared());
						modifyingCheckBox = false;
						sharedCheckbox.setEnabled(true);
						sharedCheckbox.setVisible(true);
					}
					selectedABoard();
				} else if (e.getSource() == createBoardBtn) {
					createBoard();
				} else if (e.getSource() == undoBtn) {
					if (selectedBoard == null) {
						log.severe("there is no selected board to undo");
						return;
					}
					undoLocally();
				} else if (e.getSource() == deleteBoardBtn) {
					if (selectedBoard == null) {
						log.severe("there is no selected board to delete");
						return;
					}
					deleteBoard(selectedBoard.getName());
				}
			}
		};

		clearBtn = new JButton("Clear Board");
		clearBtn.addActionListener(actionListener);
		clearBtn.setToolTipText("Clear the current board - clears remote copies as well");
		clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		blackBtn = new JButton("Black");
		blackBtn.addActionListener(actionListener);
		blackBtn.setToolTipText("Draw with black pen");
		blackBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		redBtn = new JButton("Red");
		redBtn.addActionListener(actionListener);
		redBtn.setToolTipText("Draw with red pen");
		redBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		deleteBoardBtn = new JButton("Delete Board");
		deleteBoardBtn.addActionListener(actionListener);
		deleteBoardBtn.setToolTipText("Delete the current board - only deletes the board locally");
		deleteBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		createBoardBtn = new JButton("New Board");
		createBoardBtn.addActionListener(actionListener);
		createBoardBtn.setToolTipText("Create a new board - creates it locally and not shared by default");
		createBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		undoBtn = new JButton("Undo");
		undoBtn.addActionListener(actionListener);
		undoBtn.setToolTipText("Remove the last path drawn on the board - triggers an undo on remote copies as well");
		undoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		sharedCheckbox = new JCheckBox("Shared");
		sharedCheckbox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (!modifyingCheckBox)
					setShare(e.getStateChange() == 1);
			}
		});
		sharedCheckbox.setToolTipText("Toggle whether the board is shared or not - tells the whiteboard server");
		sharedCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);

		// create a drop list for boards to select from
		JPanel controlsNorth = new JPanel();
		boardComboBox = new JComboBox<String>();
		boardComboBox.addActionListener(actionListener);

		// add to panel
		controlsNorth.add(boardComboBox);
		controls.add(sharedCheckbox);
		controls.add(createBoardBtn);
		controls.add(deleteBoardBtn);
		controls.add(blackBtn);
		controls.add(redBtn);
		controls.add(undoBtn);
		controls.add(clearBtn);

		// add to content pane
		content.add(controls, BorderLayout.WEST);
		content.add(controlsNorth, BorderLayout.NORTH);

		frame.setSize(600, 600);

		// create an initial board
		createBoard();

		// closing the application
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent windowEvent) {
				if (JOptionPane.showConfirmDialog(frame, "Are you sure you want to close this window?", "Close Window?",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					guiShutdown();
					frame.dispose();
				}
			}
		});

		// show the swing paint result
		frame.setVisible(true);

	}

	/**
	 * Update the GUI's list of boards. Note that this method needs to update data
	 * that the GUI is using, which should only be done on the GUI's thread, which
	 * is why invoke later is used.
	 * 
	 * @param select, board to select when list is modified or null for default
	 *                selection
	 */
	private void updateComboBox(String select) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				modifyingComboBox = true;
				if (boardComboBox != null) {
					boardComboBox.removeAllItems();

					int anIndex = -1;
					synchronized (whiteboards) {
						ArrayList<String> boards = new ArrayList<String>(whiteboards.keySet());
						Collections.sort(boards);
						for (int i = 0; i < boards.size(); i++) {
							String boardname = boards.get(i);
							boardComboBox.addItem(boardname);
							if (select != null && select.equals(boardname)) {
								anIndex = i;
							} else if (anIndex == -1 && selectedBoard != null
									&& selectedBoard.getName().equals(boardname)) {
								anIndex = i;
							}
						}
					}
					modifyingComboBox = false;
					if (anIndex != -1) {
						boardComboBox.setSelectedIndex(anIndex);
					} else {
						if (whiteboards.size() > 0) {
							boardComboBox.setSelectedIndex(0);
						} else {
							drawArea.clear();
							createBoard();
						}
					}
				}
			}
		});
	}

}
