package shared;

import java.rmi.Remote;
import java.util.ArrayList;

import javax.crypto.SealedObject;

/**
 * ClientActions contains callable interfaces for user to use on the server.
 * Thrown remote exceptions' system causes messages begin with 'x33...' Example
 * x33SisTerm stands for session terminated. Below is the table of x33 causes:
 * <ul>
 * <li>x33SysTerm - Session Terminated</li>
 * </ul>
 * 
 * @author AntonSuka
 *
 */
public interface ClientActions extends Remote {

	/**
	 * Initial greeting with the server
	 * 
	 * @param login
	 * @return
	 * @throws Exception
	 */
	public String hello(String login) throws Exception;

	/**
	 * Auths user and gives him a signed token. Read below how the array SHOULD look
	 * like.<br>
	 * <hr>
	 * Please be aware that it is up to you to verify public key using verifyKey,
	 * which is not a mandatory step, but highly suggested.
	 * <hr>
	 * 
	 * @param array - Must be an array of objects in the following format:<br>
	 *              Object[] {String MessageFromHello, String YourChallenge}
	 * @return
	 * @throws Exception
	 */
	public SealedObject auth(String login, SealedObject array) throws Exception;
	

	/**
	 * Creates a session and returns a token for the user
	 * @param array - Must be in the following form:<br>
	 * Object[] {String hashedPassword (with SHA-256), String salt}
	 * @return token of the session
	 */
	public SealedObject login(String login, SealedObject array) throws Exception;

	/**
	 * Sends back user current balance
	 * 
	 * @param login - username used to login to the system
	 * @param token - session token
	 * @return balance of login+token
	 */
	public double getBalance(String login, String token) throws Exception;

	/**
	 * Returns array of items that user have placed on the market for sale
	 * 
	 * @param login - username used to login to the system
	 * @param token - session token
	 * @return list of owned items
	 * @throws Exception
	 */
	public ArrayList<Item> getMyItems(String login, String token) throws Exception;

	/**
	 * Returns array of items on which the user have placed bids
	 * 
	 * @param login - username used to login to the system
	 * @param token - session token
	 * @return list of bids
	 */
	public ArrayList<Item> getMyBids(String login, String token) throws Exception;

	/**
	 * Places an item to the auction house, returns global id of the item
	 * 
	 * @param login - username used to login to the system
	 * @param token - session token
	 * @param item  - item that is to be placed
	 * @return id of the item, and -1 if the item was not placed
	 */
	public int placeItem(String login, String token, Item item) throws Exception;

	/**
	 * Closes the item bidding and removes the item from the market
	 * 
	 * @param login - username used to login to the system
	 * @param token - session token
	 * @param id    - id of the item
	 * @return String in format
	 *         "winner_name/winner_email/winner_username/winner_bid" <br>
	 *         example "Boris/Boris@worship.me/Borisio666/909.09
	 */
	public String closeItem(String login, String token, int id) throws Exception;

	/**
	 * Places a bid on an item with id {@code id}
	 * 
	 * @param login - username used to login to the system
	 * @param token - session token
	 * @param id    - id of the item
	 * @param bid   - amount of funds to bid
	 * @return 0 - operation successful<br>
	 *         1 - fail (not enough funds)<br>
	 *         2 - fail (item does not exist)<br>
	 *         3 - fail (current bid higher)<br>
	 *         4 - cannot bid own items
	 */
	public int placeBid(String login, String token, int id, double bid) throws Exception;

	/**
	 * Returns all existent items in the auction, use for update, retrieval of user
	 * items, user bids etc.
	 * 
	 * @return ArrrayList of all items
	 */
	public ArrayList<Item> getItems() throws Exception;

}
