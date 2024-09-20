package com.sshtools.server.components.jce;

/*-
 * #%L
 * Server API
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

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshKeyExchangeLegacy;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.Curve25519;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.server.SshServerContext;
import com.sshtools.server.components.SshKeyExchangeServer;
import com.sshtools.server.components.SshKeyExchangeServerFactory;
import com.sshtools.synergy.ssh.SshTransport;
import com.sshtools.synergy.ssh.TransportProtocol;

public class Curve25519SHA256LibSshServer extends SshKeyExchangeServer implements
		SshKeyExchangeLegacy {

	public static final int SSH_MSG_KEX_ECDH_INIT = 30;
	public static final int SSH_MSG_KEX_ECDH_REPLY = 31;

	public static final String CURVE25519_SHA2_AT_LIBSSH_ORG = "curve25519-sha256@libssh.org";
	
	public static class Curve25519SHA256LibSshServerFactory implements SshKeyExchangeServerFactory<Curve25519SHA256LibSshServer> {
		@Override
		public Curve25519SHA256LibSshServer create() throws NoSuchAlgorithmException, IOException {
			return new Curve25519SHA256LibSshServer();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CURVE25519_SHA2_AT_LIBSSH_ORG };
		}
	}
	
	public final String name;
	
	byte[] f;
	byte[] privateKey;
	byte[] e;

	String clientId;
	String serverId;
	byte[] clientKexInit;
	byte[] serverKexInit;

	
	public Curve25519SHA256LibSshServer() {
		this(CURVE25519_SHA2_AT_LIBSSH_ORG);
	}
	protected Curve25519SHA256LibSshServer(String name) {
		super("SHA-256", SecurityLevel.PARANOID, 5000);
		this.name = name;
	}

	@Override
	public String getAlgorithm() {
		return name;
	}
	
	public String getProvider() {
		return "";
	}

	protected void calculateExchangeHash() throws SshException {
		Digest hash = (Digest) ComponentManager.getInstance()
				.supportedDigests().getInstance(getHashAlgorithm());

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

	@Override
	public void init(SshTransport<SshServerContext> transport, String clientId, String serverId,
			byte[] clientKexInit, byte[] serverKexInit, SshPrivateKey prvkey, SshPublicKey pubkey,
			boolean firstPacketFollows, boolean useFirstPacket) throws IOException, SshException {

		try {
			this.transport = transport;
			this.clientId = clientId;
			this.serverId = serverId;
			this.clientKexInit = clientKexInit;
			this.serverKexInit = serverKexInit;
			this.hostKey = pubkey.getEncoded();
			this.prvkey = prvkey;
			this.pubkey = pubkey;
			this.firstPacketFollows = firstPacketFollows;
			this.useFirstPacket = useFirstPacket;
		} catch (SshException e) {
			throw new SshIOException(e);
		}

	}
	
	private void initCrypto() throws InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, SshException {
		f = new byte[32];
		privateKey = new byte[32];
		JCEComponentManager.getSecureRandom().nextBytes(privateKey);
		Curve25519.keygen(f, null, privateKey);
	}
	
	public void test() {
		
		try {
			initCrypto();
		} catch (Throwable e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	public boolean processMessage(byte[] msg) throws SshException, IOException {
 
		if (msg[0] != SSH_MSG_KEX_ECDH_INIT) {
			return false;
		}

		// Discard this message if it was guessed wrong
		if (firstPacketFollows && !useFirstPacket) {
			if(Log.isDebugEnabled()) {
				Log.debug("Client attempted to guess the kex in use but we determined it was wrong so we're waiting for another SSH_MSG_KEX_ECDH_INIT");
			}
			firstPacketFollows = false;
			return true;
		}

		ByteArrayReader reply = new ByteArrayReader(msg, 1, msg.length - 1);

		try {

			initCrypto();
			
			e = reply.readBinaryString();

			byte[] k = new byte[32];
			Curve25519.curve(k, privateKey, e);
			secret = new BigInteger(1, k);
		} catch (Exception e) {
			throw new SshException(SshException.KEY_EXCHANGE_FAILED, e);
		} finally {
			reply.close();
		}

		calculateExchangeHash();

		int count = 0;
		while(true) {
			signature = prvkey.sign(exchangeHash, pubkey.getSigningAlgorithm());
	
			if(Log.isDebugEnabled()) {
				Log.debug("Verifying signature output to mitigate passive SSH key compromise vulnerability");
			}
			
			if(!pubkey.verifySignature(signature, exchangeHash)) {
				if(count++ >= 3) {
					throw new SshException(SshException.HOST_KEY_ERROR, "Detected invalid signautre from private key!");
				}
				if(Log.isDebugEnabled()) {
					Log.debug("Detected invalid signature output from {} implementation", pubkey.getSigningAlgorithm());
				}
			} else {
				break;
			}
		}

		transport.postMessage(new SshMessage() {
			public boolean writeMessageIntoBuffer(ByteBuffer buf) {

				ByteArrayWriter baw = new ByteArrayWriter();
				try {
					buf.put((byte) SSH_MSG_KEX_ECDH_REPLY);
					buf.putInt(hostKey.length);
					buf.put(hostKey);
					byte[] tmp = f;
					buf.putInt(tmp.length);
					buf.put(tmp);

					baw.writeString(pubkey.getSigningAlgorithm());
					baw.writeBinaryString(signature);
					tmp = baw.toByteArray();

					buf.putInt(tmp.length);
					buf.put(tmp);

				} catch (IOException ex) {
					transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED,
							"Could not read host key");
				} finally {
					try {
						baw.close();
					} catch (IOException e) {
					}
				}

				return true;
			}

			public void messageSent(Long sequenceNo) {
				if(Log.isDebugEnabled())
					Log.debug("Sent SSH_MSG_KEX_ECDH_REPLY");
			}
		}, true);

		transport.sendNewKeys();

		return true;
	}
}
