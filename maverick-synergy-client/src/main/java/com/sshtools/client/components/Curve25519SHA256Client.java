package com.sshtools.client.components;

/*-
 * #%L
 * Client API
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
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.SshKeyExchangeClient;
import com.sshtools.client.SshKeyExchangeClientFactory;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.Curve25519;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.synergy.ssh.SshTransport;

public class Curve25519SHA256Client extends SshKeyExchangeClient {
	
	public static class Curve25519SHA256ClientFactory implements SshKeyExchangeClientFactory<Curve25519SHA256Client> {
		@Override
		public Curve25519SHA256Client create() throws NoSuchAlgorithmException, IOException {
			return new Curve25519SHA256Client();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CURVE25519_SHA2 };
		}
	}

	public static final int SSH_MSG_KEX_ECDH_INIT = 30;
	public static final int SSH_MSG_KEX_ECDH_REPLY = 31;

	public static final String CURVE25519_SHA2 = "curve25519-sha256";
	public final String name;
	byte[] f;
	byte[] privateKey;
	byte[] e;

	String clientId;
	String serverId;
	byte[] clientKexInit;
	byte[] serverKexInit;

	public Curve25519SHA256Client() {
		this(CURVE25519_SHA2);
	}
	
	protected Curve25519SHA256Client(String name) {
		super("SHA-256", SecurityLevel.PARANOID, 5000);
		this.name = name;
	}

	@Override
	public String getAlgorithm() {
		return name;
	}

	private void initCrypto()
			throws InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, SshException {
		e = new byte[32];
		privateKey = new byte[32];
		JCEComponentManager.getSecureRandom().nextBytes(privateKey);
		Curve25519.keygen(e, null, privateKey);
	}

	public void test() {

		try {
			initCrypto();
		} catch (Throwable e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	public void init(SshTransport<SshClientContext> transport, String clientId, String serverId,
			byte[] clientKexInit, byte[] serverKexInit, SshPrivateKey prvkey, SshPublicKey pubkey,
			boolean firstPacketFollows, boolean useFirstPacket) throws IOException, SshException {

		this.transport = transport;
		this.clientId = clientId;
		this.serverId = serverId;
		this.clientKexInit = clientKexInit;
		this.serverKexInit = serverKexInit;

		try {
			initCrypto();

			transport.postMessage(new SshMessage() {
				public boolean writeMessageIntoBuffer(ByteBuffer buf) {

					buf.put((byte) SSH_MSG_KEX_ECDH_INIT);
					buf.putInt(e.length);
					buf.put(e);

					return true;
				}

				public void messageSent(Long sequenceNo) {
					if (Log.isDebugEnabled())
						Log.debug("Sent SSH_MSG_KEX_ECDH_INIT");
				}
			}, true);
		} catch (SshException e) {
			throw new SshIOException(e);
		} catch (Exception e) {
			throw new SshException(e, SshException.KEY_EXCHANGE_FAILED);
		}
	}

	@Override
	public boolean processMessage(byte[] resp) throws SshException, IOException {

		if (resp[0] != SSH_MSG_KEX_ECDH_REPLY) {
			return false;
		}

		if (resp[0] != SSH_MSG_KEX_ECDH_REPLY) {
			throw new SshException("Expected SSH_MSG_KEX_ECDH_REPLY but got message id " + resp[0],
					SshException.KEY_EXCHANGE_FAILED);
		}

		try (ByteArrayReader reply = new ByteArrayReader(resp, 1, resp.length - 1)) {
			hostKey = reply.readBinaryString();
			f = reply.readBinaryString();
			signature = reply.readBinaryString();

			byte[] k = new byte[32];
			Curve25519.curve(k, privateKey, f);
			secret = new BigInteger(1, k);

			calculateExchangeHash();

			transport.sendNewKeys();
		} catch (Exception e) {
			Log.error("Key exchange failed", e);
			throw new SshException("Failed to process key exchange", SshException.INTERNAL_ERROR, e);
		}

		return true;

	}

	protected void calculateExchangeHash() throws SshException {
		Digest hash = (Digest) ComponentManager.getInstance().supportedDigests().getInstance(getHashAlgorithm());

		// The local software version comments
		hash.putString(clientId);

		// The remote software version comments
		hash.putString(serverId);

		// The local kex init payload
		hash.putInt(clientKexInit.length);
		hash.putBytes(clientKexInit);

		// The remote kex init payload
		hash.putInt(serverKexInit.length);
		hash.putBytes(serverKexInit);

		// The host key
		hash.putInt(hostKey.length);
		hash.putBytes(hostKey);

		hash.putInt(e.length);
		hash.putBytes(e);

		hash.putInt(f.length);
		hash.putBytes(f);

		// The diffie hellman k value
		hash.putBigInteger(secret);

		// Do the final output
		exchangeHash = hash.doFinal();
	}

	public String getProvider() {
		return "";
	}
}
