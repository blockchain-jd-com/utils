package utils.crypto.sm;

import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import utils.security.DecryptionException;
import utils.security.EncryptionException;

public class SM4Utils {

	// SM4 supports 128-bit(16 bytes) secret key
	public static final int KEY_SIZE = 128 / 8;
	// One block contains 16 bytes
	public static final int BLOCK_SIZE = 16;
	// Initial vector's size is 16 bytes
	public static final int IV_SIZE = 16;

	/**
	 * key generation
	 *
	 * @return secret key
	 */
	public static byte[] generateKey() {

		CipherKeyGenerator keyGenerator = new CipherKeyGenerator();

		// To provide secure randomness and key length as input
		// to prepare generate private key
		keyGenerator.init(new KeyGenerationParameters(new SecureRandom(), KEY_SIZE * 8));

		// To generate key
		return keyGenerator.generateKey();
	}

	public static byte[] generateKey(byte[] seed) {
		byte[] hash = SM3Utils.hash(seed);
		return Arrays.copyOf(hash, KEY_SIZE);
	}

	/**
	 * encryption in CBC ;
	 *
	 * @param plainBytes plaintext
	 * @param secretKey  symmetric key
	 * @param iv         initial vector
	 * @return ciphertext
	 */
	public static byte[] encrypt(byte[] plainBytes, byte[] secretKey, byte[] iv) {
		return encrypt(plainBytes, 0, plainBytes.length, secretKey, iv);
	}

	/**
	 * encryption in CBC ;
	 *
	 * @param plainBytes plaintext
	 * @param secretKey  symmetric key
	 * @param iv         initial vector
	 * @return ciphertext
	 */
	public static byte[] encrypt(byte[] plainBytes, int offset, int length, byte[] secretKey, byte[] iv) {

		// To ensure that plaintext is not null
		if (plainBytes == null) {
			throw new IllegalArgumentException("plaintext is null!");
		}

		if (secretKey.length != KEY_SIZE) {
			throw new EncryptionException("secretKey's length is wrong!");
		}

		if (iv.length != IV_SIZE) {
			throw new EncryptionException("iv's length is wrong!");
		}

		// To get the value padded into input
		int padding = 16 - length % BLOCK_SIZE;
		// The plaintext with padding value
		byte[] plainBytesWithPadding = new byte[length + padding];
		System.arraycopy(plainBytes, offset, plainBytesWithPadding, 0, length);
		// The padder adds PKCS7 padding to the input, which makes its length to
		// become an integral multiple of 16 bytes
		PKCS7Padding padder = new PKCS7Padding();
		// To add padding
		padder.addPadding(plainBytesWithPadding, length);

		CBCBlockCipher encryptor = new CBCBlockCipher(new SM4Engine());
		// To provide key and initialisation vector as input
		encryptor.init(true, new ParametersWithIV(new KeyParameter(secretKey), iv));
		byte[] output = new byte[plainBytesWithPadding.length + IV_SIZE];
		// To encrypt the input_p in CBC mode
		int blockCount = plainBytesWithPadding.length / BLOCK_SIZE;
		int outOffset = 0;
		for (int i = 0; i < blockCount; i++) {
			encryptor.processBlock(plainBytesWithPadding, outOffset, output, outOffset + BLOCK_SIZE);
			outOffset += BLOCK_SIZE;
		}

		// The IV locates on the first block of ciphertext
		System.arraycopy(iv, 0, output, 0, BLOCK_SIZE);
		return output;
	}

	public static byte[] encrypt(byte[] plainBytes, int offset, int length, byte[] secretKey) {
		byte[] iv = new byte[IV_SIZE];
		SecureRandom random = new SecureRandom();
		random.nextBytes(iv);
		return encrypt(plainBytes, offset, length, secretKey, iv);
	}

	public static byte[] encrypt(byte[] plainBytes, byte[] secretKey) {
		return encrypt(plainBytes, 0, plainBytes.length, secretKey);
	}

	/**
	 * decryption
	 *
	 * @param cipherBytes ciphertext
	 * @param secretKey   symmetric key
	 * @return plaintext
	 */
	public static byte[] decrypt(byte[] cipherBytes, byte[] secretKey) {
		return decrypt(cipherBytes, 0, cipherBytes.length, secretKey);
	}

	/**
	 * decryption
	 *
	 * @param cipherBytes ciphertext
	 * @param secretKey   symmetric key
	 * @return plaintext
	 */
	public static byte[] decrypt(byte[] cipherBytes, int offset, int length, byte[] secretKey) {

		// To ensure that the ciphertext is not null
		if (cipherBytes == null) {
			throw new IllegalArgumentException("ciphertext is null!");
		}

		// To ensure that the ciphertext's length is integral multiples of 16 bytes
		if (length % BLOCK_SIZE != 0) {
			throw new DecryptionException("ciphertext's length is wrong!");
		}

		if (secretKey.length != KEY_SIZE) {
			throw new DecryptionException("secretKey's length is wrong!");
		}

		byte[] iv = new byte[IV_SIZE];
		System.arraycopy(cipherBytes, offset, iv, 0, BLOCK_SIZE);

		CBCBlockCipher decryptor = new CBCBlockCipher(new SM4Engine());
		// To prepare the decryption
		decryptor.init(false, new ParametersWithIV(new KeyParameter(secretKey), iv));
		byte[] outputWithPadding = new byte[length - BLOCK_SIZE];
		// To decrypt the input in CBC mode
		int blockCount = length / BLOCK_SIZE;
		int outOffset = 0;
		for (int i = 1; i < blockCount; i++) {
			decryptor.processBlock(cipherBytes, outOffset + offset + BLOCK_SIZE, outputWithPadding, outOffset);
			outOffset += BLOCK_SIZE;
		}

		int p = outputWithPadding[outputWithPadding.length - 1];
		// To ensure that the padding of output_p is valid
		if (p > BLOCK_SIZE || p < 0x01) {
			throw new DecryptionException("There no exists such padding!");
		}
		for (int i = 0; i < p; i++) {
			if (outputWithPadding[outputWithPadding.length - i - 1] != p) {
				throw new DecryptionException("Padding is invalid!");
			}
		}

		// To remove the padding from output and obtain plaintext
		byte[] output = new byte[outputWithPadding.length - p];
		System.arraycopy(outputWithPadding, 0, output, 0, output.length);
		return output;
	}
}
