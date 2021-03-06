package utils.crypto.classic;

import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import utils.security.DecryptionException;
import utils.security.EncryptionException;

/**
 * @author zhanglin33
 * @title: AESUtils
 * @description: AES128/CBC/PKCS7Padding symmetric encryption algorithm
 * @date 2019-04-22, 09:37
 */
public class AESUtils {

	// AES128 supports 128-bit(16 bytes) secret key
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
		byte[] hash = SHA256Utils.hash(seed);
		return Arrays.copyOf(hash, KEY_SIZE);
	}

	/**
	 * 采用随机的初始向量 IV 执行 CBC 模式加密；
	 * 
	 * <br>
	 * 
	 * 输出结果为在原始密文块之前扩充了一个块的长度，用于存放初始向量 IV；
	 * 
	 * @param plainBytes
	 * @param secretKey
	 * @return
	 */
	public static byte[] encrypt(byte[] plainBytes, byte[] secretKey) {
		return encrypt(plainBytes, 0, plainBytes.length, secretKey);
	}

	/**
	 * 采用随机的初始向量 IV 执行 CBC 模式加密；
	 * 
	 * <br>
	 * 
	 * 输出结果为在原始密文块之前扩充了一个块的长度，用于存放初始向量 IV；
	 * 
	 * @param plainBytes
	 * @param offset
	 * @param length
	 * @param secretKey
	 * @return
	 */
	public static byte[] encrypt(byte[] plainBytes, int offset, int length, byte[] secretKey) {
		byte[] iv = new byte[IV_SIZE];
		SecureRandom random = new SecureRandom();
		random.nextBytes(iv);
		return encrypt(plainBytes, offset, length, secretKey, iv);
	}

	/**
	 * 采用指定的初始向量 IV 执行 CBC 模式加密；
	 * 
	 * <br>
	 * 
	 * 输出结果为在原始密文块之前扩充了一个块的长度，用于存放初始向量 IV；
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
	 * 采用指定的初始向量 IV 执行 CBC 模式加密；
	 * 
	 * <br>
	 * 
	 * 输出结果为在原始密文块之前扩充了一个块的长度，用于存放初始向量 IV；
	 * 
	 * @param plainBytes
	 * @param offset
	 * @param length
	 * @param secretKey
	 * @param iv
	 * @return
	 */
	public static byte[] encrypt(byte[] plainBytes, int offset, int length, byte[] secretKey, byte[] iv) {

		// To ensure that plaintext is not null
		if (plainBytes == null) {
			throw new EncryptionException("plaintext is null!");
		}

		if (secretKey.length != KEY_SIZE) {
			throw new EncryptionException("secretKey's length is wrong!");
		}

		if (iv.length != IV_SIZE) {
			throw new EncryptionException("iv's length is wrong!");
		}

		// To get the value padded into input
		byte padding = getAESPadding(length);
		int lengthWithPadding = length + padding;

		// 加密的明文输入数组，有 2 部分组成：原始明文 + PKCS7填充；
		byte[] input = new byte[lengthWithPadding];
		System.arraycopy(plainBytes, offset, input, 0, length);
		// PKCS7填充；
		PKCS7PaddingUtils.addPadding(input, length, padding);

		// 加密的密文输出数组，由 2 部分组成：IV + 密文；
		byte[] output = new byte[IV_SIZE + lengthWithPadding];

		// The IV locates on the first block of ciphertext
		System.arraycopy(iv, 0, output, 0, IV_SIZE);

		CBCBlockCipher encryptor = new CBCBlockCipher(new AESEngine());
		// To provide key and initialisation vector as input
		encryptor.init(true, new ParametersWithIV(new KeyParameter(secretKey), iv));

		// To encrypt the input_p in CBC mode
		int blockCount = lengthWithPadding / BLOCK_SIZE;
		int blockOffset;

		for (int i = 0; i < blockCount; i++) {
			blockOffset = i * BLOCK_SIZE;
			encryptor.processBlock(input, blockOffset, output, IV_SIZE + blockOffset);
		}

		return output;
	}

	public static byte getAESPadding(int dataLength) {
		return (byte) (16 - dataLength % BLOCK_SIZE);
	}

	/**
	 * 计算密文长度（包括填充）；<br>
	 * 
	 * @param plaintextSize
	 * @return
	 */
	public static int getCiphertextSizeWithPadding(int plaintextSize) {
		int paddingSize = 16 - plaintextSize % BLOCK_SIZE;
		return plaintextSize + paddingSize;
	}

	/**
	 * 计算密文长度（包括填充）；<br>
	 * 
	 * 包括了插入初始向量 IV 作为首块的长度；
	 * 
	 * @param plaintextSize
	 * @return
	 */
	public static int getCiphertextSizeWithPadding_IV(int plaintextSize) {
		int paddingSize = 16 - plaintextSize % BLOCK_SIZE;
		return IV_SIZE + plaintextSize + paddingSize;
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

	public static byte[] decrypt(byte[] cipherBytes, int offset, int length, byte[] secretKey) {

		// To ensure that the ciphertext is not null
		if (cipherBytes == null) {
			throw new IllegalArgumentException("ciphertext is null!");
		}

		// To ensure that the ciphertext's length is integral multiples of 16 bytes
		if (length % BLOCK_SIZE != 0) {
			throw new DecryptionException(
					"ciphertext's length is wrong! Must be an integer multiple of BLOCK_SIZE[" + BLOCK_SIZE + "]!");
		}

		if (secretKey.length != KEY_SIZE) {
			throw new DecryptionException("secretKey's length is wrong!");
		}

		byte[] iv = new byte[IV_SIZE];
		System.arraycopy(cipherBytes, offset, iv, 0, BLOCK_SIZE);

		CBCBlockCipher decryptor = new CBCBlockCipher(new AESEngine());
		// To prepare the decryption
		decryptor.init(false, new ParametersWithIV(new KeyParameter(secretKey), iv));
		byte[] outputWithPadding = new byte[length - BLOCK_SIZE];
		// To decrypt the input in CBC mode
		int blockcount = length / BLOCK_SIZE;
		for (int i = 1; i < blockcount; i++) {
			decryptor.processBlock(cipherBytes, offset + i * BLOCK_SIZE, outputWithPadding, (i - 1) * BLOCK_SIZE);
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
