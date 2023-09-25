import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import logic.Market;
import logic.User;
import server.Dispatcher;

public class RunnerServer {

	public static void main(String[] args) throws Exception {

		System.out.println("Main server starting...");
		// db
		HashMap<String, User> users = new HashMap<>();
		users.put("Admin", new User(readKey("Admin.pem"), "Anton", "Admin", "Anton@worship.me",
				"e7cf3ef4f17c3999a94f2c6f612e8a888e5b1026878e4e19398b23bd38ec221a", 90000)); // Password
		users.put("Boriso666", new User(readKey("Boriso666.pem"), "Boris", "Boriso666", "Boris@worship.me",
				"daaad6e5604e8e17bd9f108d91e26afe6281dac8fda0091040a7a6d7bd9b43b5", 2500)); // qwerty123
		users.put("Sintax", new User(readKey("Sintax.pem"), "Sirius", "Sintax", "Sintax@sinus.com",
				"226af793b8853fe0c56a398f59ba963c901a53b4a7fd2a6da8ac7d07d8c46ce7", 1200)); // nv09q32j
		Market marketplace = new Market(users);

	}

	private static SecretKey readKey(String keyloc)
			throws InvalidKeySpecException, IOException, NoSuchAlgorithmException {
		byte[] key = Files.readAllBytes(Paths.get(keyloc));
		return new SecretKeySpec(key, 0, key.length, "DES");
	}

}
