
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Timer;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import shared.ClientActions;
import shared.Item;

/**
 * JAVAX BROKE FOR ME, I MADE THING IN SWING :(
 * 
 * @author AntonSuka
 *
 */
public class RunnerClient {

	private int Width = 1024;
	private int Height = 800;
	static String login = "";
	static String token = null;
	static boolean loggedin = false;
	static SecretKey key;

	public static void main(String[] args)
			throws NotBoundException, ClassNotFoundException, IOException, InvalidKeyException, SignatureException,
			NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeySpecException {

		ClientActions cl = null;
		try {
			cl = (ClientActions) Naming.lookup("rmi://localhost/Auction");
		} catch (RemoteException e) {
			e.printStackTrace();
		}

//		Timer scheduler = new Timer();
//		scheduler for swing UI.

		String command = "";
		System.out.println(
				"Type 'login <your_login>:<password>' to login into the Auction House\nType 'help' to get started");
		Scanner scan = new Scanner(System.in);

		while (!command.equals("exit")) {
			System.out.print("\n>");
			command = scan.nextLine();
			if (command.toLowerCase().startsWith("login ") && !loggedin) {
				String pass;
				String[] split;
				command = command.replace("login ", "");
				split = command.split(":");
				if (split.length != 2)
					throw new RuntimeException("Format is incorrect");
				login = split[0];
				pass = split[1];
				pass = RunnerClient.getHashed(pass);
				// make key
				key = readKey(login + ".pem");
				Cipher cip = Cipher.getInstance("DES");
				cip.init(Cipher.ENCRYPT_MODE, key);
				String challenge = String.valueOf((int) Math.random() * 10000);
				try {
					// Very sophisticated, me like, me yarrr
					String msg = cl.hello(login);
					System.out.println(msg);
					if (challenge.equals(
							(String) cl.auth(login, new SealedObject(new Object[] { msg, challenge }, cip))
									.getObject(key)))
						token = (String) cl
								.login(login,
										new SealedObject(new Object[] { pass, String.valueOf(Math.random()) }, cip))
								.getObject(key);
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
				if (token != null) {
					System.out.println("Logged in successfully, type 'help' to get started");
					loggedin = true;
				}
			}
			if (command.equals("help")) {
				System.out.println("Available commands:");
				System.out.println("login <username>:<password> - loggs in to the server");
				System.out.println("show - shows all available items with current bids");
				System.out.println("exit - quits the server");
				if (loggedin) {
					System.out.println(
							"place <name>:<description>:<starting_bid>:<rserved_bid> - places the bid on the auction");
					System.out.println("remove <item_id>");
					System.out.println("bid <item_id>:<amount>");
					System.out.println(
							"relog <password> - only available when you have been logged by session timed out method");
					System.out.println("myitems - retrieves list of your items");
					System.out.println("mybids - retrieves a list of items that you currently have bids on");
				}
			}
			if (!login.equals(""))
				if (command.toLowerCase().startsWith("relog ")) {
					command = command.replace("relog ", "");
					String pass = command;
					pass = RunnerClient.getHashed(pass);
//					try {
//						token = cl.login(login, pass);
//					} catch (RemoteException e) {
//						System.out.println(e.getMessage());
//					}
					if (token != null) {
						System.out.println("Logged in successfully, type 'help' to get started");
						loggedin = true;
					}
				}
			if (command.toLowerCase().equals("show")) {
				ArrayList<Item> items = new ArrayList<>();
				try {
					items = cl.getItems();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (!items.isEmpty()) {
					System.out.println("Item ID\tName\tDescription\tBid\tBidder");
					for (Item it : items)
						System.out.println(it.id + "\t" + it.name + "\t" + it.desc + "\t" + it.bid + "\t" + it.bidder);
				} else
					System.out.println(
							"Market is empty, be the first to subbmit a bid, type 'help' to see list of commands");
			}
			if (loggedin) {
				// Session trycatch
				try {
					if (command.toLowerCase().equals("balance")) {
						System.out.println("Current balance is: " + cl.getBalance(login, token));
					}

					if (command.toLowerCase().equals("myitems")) {
						ArrayList<Item> items;
						items = cl.getMyItems(login, token);
						if (!items.isEmpty()) {

							System.out.println("Item ID\tName\tDescription\tBid\tBidder");
							for (Item it : items)
								System.out.println(
										it.id + "\t" + it.name + "\t" + it.desc + "\t" + it.bid + "\t" + it.bidder);
						} else
							System.out.println("You dont have active items on the market");
					}

					if (command.toLowerCase().equals("mybids")) {
						ArrayList<Item> items;
						items = cl.getMyBids(login, token);
						if (!items.isEmpty()) {

							System.out.println("Item ID\tName\tDescription\tBid");
							for (Item it : items)
								System.out.println(it.id + "\t" + it.name + "\t" + it.desc + "\t" + it.bid);
						} else
							System.out.println("You dont have any bids");
					}

					if (command.toLowerCase().startsWith("remove ")) {
						try {
							command = command.replace("remove ", "");
							String status = cl.closeItem(login, token, Integer.parseInt(command));
							if (status != null) {
								String[] split = status.split(":");
								System.out.println("Item removed: winner is " + split[0] + "! (" + split[1]
										+ ") with bid - " + split[3] + "GBP!");
							} else
								System.out.println("Something went wrong!");
						} catch (NumberFormatException e) {
							System.out
									.println("Unsuccessful request, wrong input in command 'remove <integer_number>'");
						}
					}
					if (command.toLowerCase().startsWith("place ")) {
						try {
							command = command.replace("place ", "");
							String[] split = command.split(":");
							if (split.length != 4)
								throw new RuntimeException(
										"Unsuccessful request, wrong input in command 'place <string>:<string>:<float_number>:<float_number>'");
							Item it = new Item(split[0], split[1], Double.parseDouble(split[2]),
									Double.parseDouble(split[3]));
							int id = cl.placeItem(login, token, it);
							if (id != -1)
								System.out.println("Item placed, id: " + id);
						} catch (NumberFormatException e) {
							System.out.println(
									"Unsuccessful request, wrong input in command 'place <string>:<string>:<float_number>:<float_number>'");
						}
					}
					if (command.toLowerCase().startsWith("bid ")) {
						try {
							command = command.replace("bid ", "");
							String[] split = command.split(":");
							if (split.length != 2)
								throw new RuntimeException(
										"Unsuccessful request, wrong input in command 'bid <integer_number>:<float_number>'");
							int status = cl.placeBid(login, token, Integer.parseInt(split[0]),
									Double.parseDouble(split[1]));
							String answer = "";
							switch (status) {
							case 0:
								answer = "Operation successful, item placed";
								break;
							case 1:
								answer = "Operation failed, you have not enough funds";
								break;
							case 2:
								answer = "Operation failed, item no longer exists";
								break;
							case 3:
								answer = "Operation failed, item's current bid is higher";
								break;
							case 4:
								answer = "Operaton failed, cannot bid your own items";
								break;
							default:
								answer = "Operation failed, error code: " + status;
								break;
							}
							System.out.println(answer);
						} catch (NumberFormatException e) {
							System.out.println(
									"Unsuccessful request, wrong input in command 'bid <integer_number>:<float_number>'");
						}
					}
					// Client messaging catch
				} catch (Exception e) {
					if (e.getMessage() == null)
						e.printStackTrace();
					if (e.getMessage().contains("x33SesTerm")) {
						loggedin = false;
						System.out.println("You have been logged out, please enter: 'relog <password>' to reauth");
					} else
						System.out.println(e.getMessage());
				}

			}

		}
		scan.close();

	}

	private static String getHashed(String string) {
		MessageDigest dig = null;
		try {
			dig = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		String token = string;
		return RunnerClient.bytesToHex(dig.digest(token.getBytes()));
	}

	private static String bytesToHex(byte[] hash) {
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if (hex.length() == 1)
				hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}

	private static SecretKey readKey(String keyloc)
			throws InvalidKeySpecException, IOException, NoSuchAlgorithmException {
		byte[] key = Files.readAllBytes(Paths.get(keyloc));
		return new SecretKeySpec(key, 0, key.length, "DES");
	}

}
