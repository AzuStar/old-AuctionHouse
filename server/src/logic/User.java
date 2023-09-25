package logic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;

import shared.Item;

public class User implements Serializable {

	/**
	 * Version 10
	 */
	private static final long serialVersionUID = 10L;

	public ArrayList<Item> myItems = new ArrayList<>();
	public ArrayList<Item> myBids = new ArrayList<>();
	double balance;
	public SecretKey key;
	public String name;
	public String email;
	String username;
	public String hash;
	// Session logic
	public ArrayList<String> messages;
	public HashMap<String, Timer> allowedTokens = new HashMap<>();
	int maxSession = 1;
	int TTL = Integer.MAX_VALUE; // TEMP

	public User(SecretKey key, String name, String username, String email, String hash, int balance)
			throws RuntimeException {
		if (!validateEmail(email))
			throw new RuntimeException("Email field is in incorrect format");
		this.balance = balance;
		this.email = email;
		this.name = name;
		this.username = username;
		this.hash = hash;
		this.key = key;
	}

	/**
	 * Validates email of a user
	 * 
	 * @param email
	 * @return
	 */
	public static boolean validateEmail(String email) {
		if (email == null)
			return false;
		// ty stack for regex, their solution is trash tho XD
		return Pattern.compile(
				"^[a-zA-Z0-9_+&*-]+(?:\\." + "[a-zA-Z0-9_+&*-]+)*@" + "(?:[a-zA-Z0-9-]+\\.)+[a-z" + "A-Z]{2,7}$")
				.matcher(email).matches();
	}

	public boolean openSession(String token) {
		if (allowedTokens.size() >= maxSession)
			return false;
		Timer t = new Timer();
		t.schedule(new TimerTask() {

			@Override
			public void run() {
				System.out.println("Session timed out for "+name);
				allowedTokens.remove(token);
			}
		}, TTL);

		allowedTokens.put(token, t);
		return true;
	}

	public boolean checkPass(String hash) {
		return this.hash.equals(hash);
	}

	/**
	 * Verifies the session of the given token for a user.
	 * 
	 * @param token - user token
	 * @return true if session is allowed, false if session not allowed
	 */
	public boolean verifySession(String token) {
		if (allowedTokens.containsKey(token)) {
			allowedTokens.get(token).cancel();
			allowedTokens.remove(token);
			Timer t = new Timer();
			t.schedule(new TimerTask() {

				@Override
				public void run() {
					System.out.println("Session timed out for "+name);
					allowedTokens.remove(token);
				}
			}, TTL);

			allowedTokens.put(token, t);
			return true;
		}
		return false;
	}

	public double getBalance() {
		return balance;
	}

	public synchronized void addFunds(User source, double amount) {
		if (source.chargeAccount(amount))
			balance += amount * 0.99; // 1% commission XD
	}

	public synchronized boolean chargeAccount(double amount) {
		if (balance >= amount) {
			balance -= amount;
			return true;
		}
		return false;
	}

}
