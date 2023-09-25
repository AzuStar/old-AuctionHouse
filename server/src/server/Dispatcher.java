package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.blocks.cs.Receiver;
import org.jgroups.util.RspList;

import logic.User;
import shared.ClientActions;
import shared.Item;

public class Dispatcher extends UnicastRemoteObject implements ClientActions, Receiver {

	/**
	 * Version 10
	 */
	private static final long serialVersionUID = 10L;

	JChannel channel;
	RpcDispatcher dispatcher;
	RequestOptions options;
	HashMap<String, User> overview;

	public Dispatcher() throws Exception {
		super();
		try {
			LocateRegistry.createRegistry(1099);
			Naming.bind("Auction", this);

			channel = new JChannel();
			channel.setDiscardOwnMessages(true);
			channel.connect("AuctionLogic");
			dispatcher = new RpcDispatcher(channel, this);
			options = new RequestOptions(ResponseMode.GET_ALL, 2000);

			RspList<HashMap<String, User>> rsp = dispatcher.callRemoteMethods(null,
					"getOverview", null, null, options);
			System.out.println(rsp.size());
			System.out.println(rsp.getFirst().size());
			// Not a good solution normally, but would work well if users could register on
			// the server
//			Timer t = new Timer();
//			t.schedule(new TimerTask() {
//
//				@SuppressWarnings("unchecked")
//				@Override
//				public void run() {
//					try {
//						// Normally do a set of strings, to get a new users, then use compare on that set<string>
//						overview = (HashMap<String, User>) dispatcher.callRemoteMethods(null, "getOverview", null, null, options)
//								.getFirst();
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
//			}, 1000, 50000);

			System.out.println("Server in action...");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public int placeItem(String login, String token, Item item) throws Exception {

		return (int) dispatcher.callRemoteMethods(null, "placeItem", new Object[] { login, token, item },
				new Class[] { String.class, String.class, Item.class }, options).getFirst();
	}

	@Override
	public String closeItem(String login, String token, int id) throws Exception {

		return (String) dispatcher.callRemoteMethods(null, "closeItem", new Object[] { login, token, id },
				new Class[] { String.class, String.class, int.class }, options).getFirst();
	}

	@Override
	public ArrayList<Item> getItems() throws Exception {
		RspList<ArrayList<Item>> result = null;
		try {
			result = dispatcher.callRemoteMethods(null, "items", null, null, options);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result.getFirst();
	}

	@Override
	public SealedObject login(String login, SealedObject array) throws Exception {
		System.out.println("login");
		synchronized (overview) {

			if (!overview.containsKey(login))
				throw new RemoteException("Invalid user");
			Cipher cip;
			String hash;
			try {
				hash = (String) ((Object[]) array.getObject(overview.get(login).key))[0];
			} catch (Exception e) {
				return null;
			}

			if (!overview.get(login).checkPass(hash))
				throw new RemoteException("Invalid password");

			// Create token and open session
			try {
				cip = Cipher.getInstance("DES");
				cip.init(Cipher.ENCRYPT_MODE, overview.get(login).key);
			} catch (Exception e) {
				return null;
			}

			String token = Dispatcher.hashedRandom();
			dispatcher.callRemoteMethods(null, "newUser", new Object[] { login, token },
					new Class[] { String.class, String.class }, options);
			// Call market regiter user
			try {
				return new SealedObject(token, cip);
			} catch (Exception e) {
			}
			return null;
		}
	}

	@Override
	public int placeBid(String login, String token, int id, double bid) throws Exception {
		return (int) dispatcher.callRemoteMethods(null, "placeBid", new Object[] { login, token, bid },
				new Class[] { String.class, String.class, double.class }, options).getFirst();
	}

	@Override
	public double getBalance(String login, String token) throws Exception {
		return (double) dispatcher.callRemoteMethods(null, "getBalance", new Object[] { login, token },
				new Class[] { String.class, String.class }, options).getFirst();
	}

	@Override
	public ArrayList<Item> getMyItems(String login, String token) throws Exception {

		return (ArrayList<Item>) dispatcher.callRemoteMethods(null, "getMyItems", new Object[] { login, token },
				new Class[] { String.class, String.class }, options).getFirst();
	}

	@Override
	public ArrayList<Item> getMyBids(String login, String token) throws Exception {

		return (ArrayList<Item>) dispatcher.callRemoteMethods(null, "getMyBids", new Object[] { login, token },
				new Class[] { String.class, String.class }, options).getFirst();
	}

	@Override
	public String hello(String login) throws Exception {

		System.out.println("Hello");
		System.out.println(overview.size());
		if (!overview.containsKey(login))
			throw new RemoteException("Invalid user");
		String msg = hashedRandom();
		System.out.println(msg);
		overview.get(login).messages.add(msg);
		return msg;
	}

	@Override
	public SealedObject auth(String login, SealedObject array) throws Exception {
		System.out.println("auth");
		if (!overview.containsKey(login))
			throw new RemoteException("Invalid user");
		Cipher cip = null;
		Object[] result = null;
		try {
			result = (Object[]) array.getObject(overview.get(login).key);
			cip = Cipher.getInstance("DES");
			cip.init(Cipher.ENCRYPT_MODE, overview.get(login).key);
		} catch (Exception e) {
			e.printStackTrace();
		}

		String message;
		String challenge;
		try {
			message = (String) result[0];
			challenge = (String) result[1];
		} catch (Exception e) {
			throw new RemoteException("Unable to verify sender");
		}

		if (!overview.get(login).messages.contains(message))
			throw new RemoteException("Unable to verify sender");
		overview.get(login).messages.remove(message);
		try {
			return new SealedObject(challenge, cip);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void receive(Address sender, ByteBuffer buf) {
	}

	@Override
	public void receive(Address sender, byte[] buf, int offset, int length) {
	}

	/**
	 * Converts byte array into hexed string
	 * 
	 * @param hash - byte array of hash
	 * @return hexed string
	 */
	private static String bytesToHex(byte[] hash) throws Exception {
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if (hex.length() == 1)
				hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}

	private static String hashedRandom() {
		String string = null;
		try {
			MessageDigest dig = MessageDigest.getInstance("SHA-256");
			string = bytesToHex(dig.digest(String.valueOf(((int) (Math.random() * 900000 + 100000))).getBytes()));
		} catch (Exception e) {
		}
		return string;

	}

	public static void main(String[] args) throws Exception {
		new Dispatcher();
	}

}
