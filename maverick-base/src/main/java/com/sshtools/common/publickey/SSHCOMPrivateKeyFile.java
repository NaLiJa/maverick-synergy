package com.sshtools.common.publickey;

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
import java.math.BigInteger;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshCipher;
import com.sshtools.common.ssh.components.SshDsaPublicKey;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

class SSHCOMPrivateKeyFile extends Base64EncodedFileFormat implements
		SshPrivateKeyFile {

	static String BEGIN = "---- BEGIN SSH2 ENCRYPTED PRIVATE KEY ----";
	static String END = "---- END SSH2 ENCRYPTED PRIVATE KEY ----";

	byte[] formattedkey;

	SSHCOMPrivateKeyFile(byte[] formattedkey) throws IOException {
		super(BEGIN, END);

		if (!isFormatted(formattedkey)) {
			throw new IOException("Key is not formatted in the ssh.com format");
		}

		this.formattedkey = formattedkey;
	}

	public String getType() {
		return "SSH Communications Security";
	}

	public static boolean isFormatted(byte[] formattedkey) {
		return isFormatted(formattedkey, BEGIN, END);
	}

	public boolean supportsPassphraseChange() {
		return false;
	}

	public boolean isPassphraseProtected() {

		try {
			byte[] keyblob = getKeyBlob(formattedkey);
			ByteArrayReader bar = new ByteArrayReader(keyblob);
			try {
				long magic = bar.readInt();
				if (magic != 0x3f6ff9eb) {
					throw new IOException(
							"Invalid ssh.com key! Magic number not found");
				}
				// long size =
				bar.readInt();
				// String type =
				bar.readString();
				String cipher = bar.readString();

				return cipher.equals("3des-cbc");
			} finally {
				bar.close();
			}
		} catch (IOException ex) {
		}

		return false;

	}

	public SshKeyPair toKeyPair(String passphrase) throws IOException,
			InvalidPassphraseException {

		byte[] keyblob = getKeyBlob(formattedkey);
		boolean wasEncrypted = false;
		ByteArrayReader bar = new ByteArrayReader(keyblob);

		try {
			long magic = bar.readInt();

			if (magic != 0x3f6ff9eb)
				throw new IOException(
						"Invalid ssh.com key! Magic number not found");

			// long size =
			bar.readInt();
			String type = bar.readString();
			String cipher = bar.readString();
			byte[] blob = bar.readBinaryString();

			try {
				if (!cipher.equals("none")) {
					if (!cipher.equals("3des-cbc")) {
						throw new IOException("Unsupported cipher type "
								+ cipher + " in ssh.com private key");
					}

					SshCipher c = (SshCipher) ComponentManager.getInstance()
							.supportedSsh2CiphersCS().getInstance("3des-cbc");

					byte[] iv = new byte[32];
					byte[] keydata = makePassphraseKey(passphrase);

					c.init(SshCipher.DECRYPT_MODE, iv, keydata);

					c.transform(blob);
					wasEncrypted = true;

				}
			} catch (SshException e1) {
				throw new SshIOException(e1);
			}

			ByteArrayReader data = new ByteArrayReader(blob, 4,
					blob.length - 4);
			
			try {
				

				if (type.startsWith("if-modn{sign{rsa")) {

					BigInteger e = data.readMPINT32();
					BigInteger d = data.readMPINT32();
					BigInteger n = data.readMPINT32();
					
					/**
					 * Removing collection of unused parameters
					 * as we have encountered issues where we 
					 * could not read final parameter due to incorrect
					 * encoding. This blindly fixes the issue as we 
					 * don't use these parameters.
					 */
//					@SuppressWarnings("unused")
//					BigInteger u = data.readMPINT32();
//					@SuppressWarnings("unused")
//					BigInteger p = data.readMPINT32();
//					@SuppressWarnings("unused")
//					BigInteger q = data.readMPINT32();

					SshKeyPair pair = new SshKeyPair();

					pair.setPublicKey(ComponentManager.getInstance()
							.createRsaPublicKey(n, e));
					pair.setPrivateKey(ComponentManager.getInstance()
							.createRsaPrivateKey(n, d));

					return pair;

				} else if (type.startsWith("dl-modp{sign{dsa")) {

					long predefined = data.readInt();

					if (predefined != 0)
						throw new IOException(
								"Unexpected value in DSA key; this is an unsupported feature of ssh.com private keys");

					BigInteger p = data.readMPINT32();
					BigInteger g = data.readMPINT32();
					BigInteger q = data.readMPINT32();
					BigInteger y = data.readMPINT32();
					BigInteger x = data.readMPINT32();

					SshKeyPair pair = new SshKeyPair();

					SshDsaPublicKey pub = ComponentManager.getInstance()
							.createDsaPublicKey(p, q, g, y);
					pair.setPublicKey(pub);
					pair.setPrivateKey(ComponentManager.getInstance()
							.createDsaPrivateKey(p, q, g, x, pub.getY()));

					return pair;
				} else
					throw new IOException("Unsupported ssh.com key type " + type);
			} catch (Throwable t) {
				Log.error("Parsing of ssh.com key failed", t);
				if (wasEncrypted)
					throw new InvalidPassphraseException();
				throw new IOException("Bad SSH.com private key format!");
			} finally {
				data.close();
			}

		} finally {
			bar.close();
		}
	}

	private byte[] makePassphraseKey(String passphrase) throws IOException {
		ByteArrayWriter baw = new ByteArrayWriter();

		try {
			Digest hash = (Digest) ComponentManager.getInstance()
					.supportedDigests().getInstance("MD5");

			hash.putBytes(passphrase.getBytes());
			byte[] tmp = hash.doFinal();
			hash.reset();
			hash.putBytes(passphrase.getBytes());
			hash.putBytes(tmp);

			baw.write(tmp);
			baw.write(hash.doFinal());

			return baw.toByteArray();
		} catch (SshException e) {
			throw new SshIOException(e);
		} finally {
			baw.close();
		}
	}

	public void changePassphrase(String oldpassphrase, String newpassprase)
			throws IOException {
		/** @todo Implement this com.sshtools.publickey.SshPrivateKeyFile method */
		throw new IOException(
				"Changing passphrase is not supported by the ssh.com key format engine");
	}

	public byte[] getFormattedKey() throws IOException {
		return formattedkey;
	}

	@Override
	public String getComment() {
		return "";
	}

}
