package utils.crypto.sm;

import org.bouncycastle.crypto.digests.SM3Digest;

import utils.crypto.base.HashBaseSecureRandom;

/**
 * 采用基于 SM3 的“哈希法”生成伪随机数；
 * 
 * @author huanghaiquan
 *
 */
public class SM3SecureRandom extends HashBaseSecureRandom {

	private static final long serialVersionUID = -3875122790428609501L;
	
	public static final int DIGEST_LENGTH = 32;

	public SM3SecureRandom(byte[] seed) {
		super(seed);
	}

	@Override
	protected int getHashSize() {
		return DIGEST_LENGTH;
	}

	@Override
	protected void hash(byte[] bytes, byte[] output, int offset) {
		SM3Digest sm3Digest = new SM3Digest();
		sm3Digest.update(bytes, 0, bytes.length);
		sm3Digest.doFinal(output, offset);
	}
}