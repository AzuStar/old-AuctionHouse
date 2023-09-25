package logic;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.blocks.cs.Receiver;
import org.jgroups.util.RspList;

import shared.Item;

public class Market implements Receiver {

	ArrayList<Item> auctionItems = new ArrayList<>();
	HashMap<String, User> userdb;

	JChannel API = new JChannel();
	RpcDispatcher dispatcher;
	RequestOptions options = new RequestOptions(ResponseMode.GET_ALL, 5000);

	public static void main(String[] args) throws Exception {
		new Market();
	}

	public Market() throws Exception {
		start();
		ResumeState();
	}

	public Market(HashMap<String, User> userdb) throws Exception {
		this.userdb = userdb;
		auctionItems.add(null);
		start();
	}

	public void start() throws Exception {
		API.connect("AuctionLogic");
		API.setDiscardOwnMessages(true);
		dispatcher = new RpcDispatcher(API, this);
		System.out.println("Base in action...");
	}

	public HashMap<String, User> getOverview() {
		System.out.println("Returning "+userdb.size());
			return userdb;
	}

	public void newUser(String login, String token) {
		synchronized (userdb) {
			getUser(login).openSession(token);
		}
	}

	public boolean hasUser(String login) {
		synchronized (userdb) {
			return userdb.containsKey(login);
		}
	}

	public User getUser(String login) {
		synchronized (userdb) {
			return userdb.get(login);
		}
	}

	/**
	 * Returns Item with the specified id
	 * 
	 * @param id - <i>id</i> of the Item
	 * @return Item_server with id - <i>id</i>
	 */
	public Item getItem(int id) throws RemoteException {
		synchronized (auctionItems) {
			if (id <= 0)
				return null;
			Item it = null;
			try {
				it = auctionItems.get(id);
			} catch (Exception e) {
			}
			return it;
		}
	}

	/*
	 * Places item for login given item
	 */
	public synchronized int placeItem(String login, String token, Item item) throws RemoteException {
		verifySession(login, token);
		synchronized (auctionItems) {
			item.id = auctionItems.size();
			auctionItems.add(item);
			getUser(login).myItems.add(item);
			return auctionItems.indexOf(item);
		}
	}

	/*
	 * Removes item from the list, returns item in case of reserved price being
	 * higher
	 */
	public synchronized String removeItem(String login, String token, int id) throws RemoteException {
		verifySession(login, token);
		synchronized (auctionItems) {
			Item it = getItem(id);

			if (getUser(login).myItems.contains(it)) {
				getUser(login).myItems.remove(it);
				getUser(it.bidder).myBids.remove(it);
				auctionItems.remove(it);

				// secret dark magic stuff
				// make it public, so client can read the thing too (which is unsafe) :(
				// Encapsulate into Auction class or smthing, really not in the mood
				double reserved = 0;
				try {
					Field f = it.getClass().getDeclaredField("reserved_price");
					f.setAccessible(true);
					reserved = f.getDouble(it);
					f.setAccessible(false);
				} catch (Exception e) {
				}
				if (reserved <= it.bid) {
					getUser(login).addFunds(getUser(it.bidder), it.bid);
					return getUser(it.bidder).name + ":" + getUser(it.bidder).email + ":" + it.bidder + ":"
							+ Math.round(it.bid * 100) / 100;

				} else
					return null;

			}
			throw new RemoteException("This item does not belong to you!");

		}
	}

	/*
	 * Places a bid on the item
	 */
	public synchronized int placeBid(String login, String token, int id, double bid) throws RemoteException {
		verifySession(login, token);
		synchronized (auctionItems) {

			Item it = getItem(id);
			if (it == null)
				return 2;
			if (getUser(login).myItems.contains(it))
				return 4;
			String bidder = null;
			if (bid > it.bid)
				bidder = it.bidder;
			if (bidder != null)
				if (getUser(login).chargeAccount(bid)) {
					getUser(login).myBids.add(it);
					if (!bidder.equals("")) {
						getUser(bidder).myBids.remove(it);
						getUser(bidder).balance += it.bid;
					}
					it.bidder = login;
					it.bid = bid;

					return 0;
				} else
					return 1;
			return 3;
		}
	}

	/**
	 * Retrieves a list of <i>ALL</i> items on the market
	 * 
	 * @return {@code ArrayList<Item>}
	 */
	public ArrayList<Item> items() throws RemoteException {
		ArrayList<Item> list = new ArrayList<>();
		list.addAll(auctionItems);
		list.remove(0);
		return list;

	}

	public double getBalance(String login, String token) throws RemoteException {
		verifySession(login, token);
		return getUser(login).balance;
	}

	public ArrayList<Item> getMyItems(String login, String token) throws RemoteException {
		verifySession(login, token);
		return getUser(login).myItems;
	}

	public ArrayList<Item> getMyBids(String login, String token) throws RemoteException {
		verifySession(login, token);
		return getUser(login).myBids;
	}

	// Utility or Jgroups stuff

	/**
	 * Verifies the session of this user, throws an error (and stops execution) of
	 * the method if session is not verified
	 * 
	 * @param login - login of a user
	 * @param token - access token
	 * @throws RemoteException
	 */
	private void verifySession(String login, String token) throws RemoteException {
		synchronized (userdb) {
			if (!hasUser(login))
				throw new RemoteException("Invalid user");
			if (!getUser(login).verifySession(token))
				throw new RemoteException("x33SesTerm");
		}
	}

	// Watch it
	/**
	 * Current state of the server (database state)
	 * 
	 * @return State in form of array of Objects
	 * @throws Exception
	 */
	public Object[] state() throws Exception {
		System.out.println("sending...");
		return new Object[] { auctionItems, userdb };
	}

	/**
	 * SHOULD ONLY BE CALLED BY NEW MEMBERS OT OUT-OF-DATE MEMBERS
	 * 
	 * @throws Exception
	 */
	public void ResumeState() throws Exception {
		Object[] state = (Object[]) dispatcher.callRemoteMethods(null, "state", null, null, options).getFirst();
		System.out.println(state.length);
		if (state != null) {
			auctionItems = (ArrayList<Item>) state[0];
			userdb = (HashMap<String, User>) state[1];
		}
	}

//	public <T> T filterNulls(RspList<?> list) {
//		for (Object o : list.values())
//			if (o == null)
//				list.remove(o);
//		return (T) list.getFirst();
//	}

	@Override
	public void receive(Address sender, byte[] buf, int offset, int length) {
	}

	@Override
	public void receive(Address sender, ByteBuffer buf) {
	}

}
