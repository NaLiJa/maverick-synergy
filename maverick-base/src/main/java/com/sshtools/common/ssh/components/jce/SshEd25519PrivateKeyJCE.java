package com.sshtools.common.ssh.components.jce;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import com.sshtools.common.util.Arrays;
import com.sshtools.common.util.Utils;


public class SshEd25519PrivateKeyJCE implements SshEd25519PrivateKey {

	public static final byte[] ASN_HEADER = { 0x30, 0x2E, 0x02, 0x01, 0x00, 0x30, 0x05, 0x06,
			0x03, 0x2B, 0x65, 0x70, 0x04, 0x22, 0x04, 0x20};
	
	PrivateKey key;
	public SshEd25519PrivateKeyJCE(byte[] sk, byte[] pk) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, NoSuchProviderException {
		loadPrivateKey(sk, pk);
	}
	
	private void loadPrivateKey(byte[] sk, byte[] pk) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		KeyFactory keyFactory = KeyFactory.getInstance(JCEAlgorithms.ED25519);
		byte[] seed = Arrays.copy(sk, 32);
		byte[] encoded = Arrays.cat(ASN_HEADER, seed);
		PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(encoded);
		key = keyFactory.generatePrivate(pkcs8KeySpec);
	}
	
	public SshEd25519PrivateKeyJCE(PrivateKey prv) {
		key = prv;
	}

	@Override
	public byte[] sign(byte[] data) throws IOException {
		return sign(data, getAlgorithm());
	}
	
	@Override
	public byte[] sign(byte[] data, String signingAlgorithm) throws IOException {
		try {
			Signature sgr = Signature.getInstance(JCEAlgorithms.ED25519);
			sgr.initSign(key);
			sgr.update(data);
			return sgr.sign();
		} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	@Override
	public String getAlgorithm() {
		return SshEd25519PublicKeyJCE.ALGORITHM_NAME;
	}

	@Override
	public PrivateKey getJCEPrivateKey() {
		return key;
	}

	public byte[] getSeed() {
		byte[] encoded = key.getEncoded();
		byte[] seed = Arrays.copy(encoded, ASN_HEADER.length, 32);
		return seed;
	}

	@Override
	public int hashCode() {
		return new String(Utils.bytesToHex(getSeed())).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof SshEd25519PrivateKeyJCE)) {
			return false;
		}
		return Arrays.areEqual(getSeed(), ((SshEd25519PrivateKeyJCE)obj).getSeed());
	}
}
