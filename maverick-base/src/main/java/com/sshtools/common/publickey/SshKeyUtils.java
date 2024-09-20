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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sshtools.common.ssh.SshKeyFingerprint;
import com.sshtools.common.ssh.components.SshCertificate;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKeySHA256;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKeySHA512;
import com.sshtools.common.util.IOUtils;

public class SshKeyUtils {

	public static String getOpenSSHFormattedKey(SshPublicKey key) throws IOException {
		return getFormattedKey(key, "", SshPublicKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public static String getOpenSSHFormattedKey(SshPublicKey key, String comment) throws IOException {
		return getFormattedKey(key, comment, SshPublicKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public static String getFormattedKey(SshPublicKey key, String comment, int format) throws IOException {	
		SshPublicKeyFile file = SshPublicKeyFileFactory.create(key, comment, format);
		return new String(file.getFormattedKey(), "UTF-8");
	}
	
	public static String getFormattedKey(SshPublicKey key, String comment) throws IOException {	
		SshPublicKeyFile file = SshPublicKeyFileFactory.create(key, comment, SshPublicKeyFileFactory.OPENSSH_FORMAT);
		return new String(file.getFormattedKey(), "UTF-8");
	}
	
	public static String getFormattedKey(SshKeyPair pair, String passphrase) throws IOException {	
		SshPrivateKeyFile kf = SshPrivateKeyFileFactory.create(pair, passphrase);
		return new String(kf.getFormattedKey(), "UTF-8");
	}
	
	public static SshPublicKey getPublicKey(File key) throws IOException {
		return getPublicKey(IOUtils.readUTF8StringFromStream(new FileInputStream(key)));
	}
	
	public static SshPublicKey getPublicKey(InputStream key) throws IOException {
		return getPublicKey(IOUtils.readUTF8StringFromStream(key));
	}
	
	public static SshPublicKey getPublicKey(String formattedKey) throws IOException {
		SshPublicKeyFile file = SshPublicKeyFileFactory.parse(formattedKey.getBytes("UTF-8"));
		return file.toPublicKey();
	}
	
	public static SshPublicKey getPublicKey(Path path) throws IOException {
		SshPublicKeyFile file = SshPublicKeyFileFactory.parse(Files.newInputStream(path));
		return file.toPublicKey();
	}
	
	public static String getPublicKeyComment(String formattedKey) throws IOException {
		SshPublicKeyFile file = SshPublicKeyFileFactory.parse(formattedKey.getBytes("UTF-8"));
		return file.getComment();
	}
	
	public static SshKeyPair getPrivateKey(File key, String passphrase) throws IOException, InvalidPassphraseException {
		return getPrivateKey(IOUtils.readUTF8StringFromStream(new FileInputStream(key)), passphrase);
	}
	
	public static SshKeyPair getPrivateKey(File key) throws IOException, InvalidPassphraseException {
		return getPrivateKey(IOUtils.readUTF8StringFromStream(new FileInputStream(key)), "");
	}
	
	public static SshKeyPair getPrivateKey(File key, PassphrasePrompt prompt) throws IOException, InvalidPassphraseException {
		SshPrivateKeyFile file = SshPrivateKeyFileFactory.parse(key);
		if(file.isPassphraseProtected()) {
			return file.toKeyPair(prompt.getPasshrase(key.getName()));
		} else {
			return file.toKeyPair(null);
		}
	}
	
	public static SshKeyPair getPrivateKey(InputStream key, String passphrase) throws IOException, InvalidPassphraseException {
		return getPrivateKey(IOUtils.readUTF8StringFromStream(key), passphrase);
	}
	
	public static SshKeyPair getPrivateKey(String formattedKey, String passphrase) throws IOException, InvalidPassphraseException {
		SshPrivateKeyFile file = SshPrivateKeyFileFactory.parse(formattedKey.getBytes("UTF-8"));
		return file.toKeyPair(passphrase);
	}
	
	public static SshKeyPair getPrivateKey(String formattedKey, PassphrasePrompt prompt) throws IOException, InvalidPassphraseException {
		SshPrivateKeyFile file = SshPrivateKeyFileFactory.parse(formattedKey.getBytes("UTF-8"));
		if(file.isPassphraseProtected()) {
			return file.toKeyPair(prompt.getPasshrase(file.getType()));
		} else {
			return file.toKeyPair(null);
		}
	}
	
	public static SshCertificate getCertificate(File privateKey, String passphrase) throws IOException, InvalidPassphraseException {
		return getCertificate(privateKey, passphrase, new File(privateKey.getAbsolutePath() + "-cert.pub"));
	}
	
	public static SshCertificate getCertificate(File privateKey, String passphrase, File certFile) throws IOException, InvalidPassphraseException {
		if(!certFile.exists()) {
			throw new IOException(String.format("No certificate file %s to match private key file %s", certFile.getName(), privateKey.getName()));
		}
		SshKeyPair pair = getPrivateKey(privateKey, passphrase);
		SshPublicKey publicKey = getPublicKey(certFile);
		if(!(publicKey instanceof OpenSshCertificate)) {
			throw new IOException(String.format("%s is not a certificate file", certFile.getName()));
		}
		return new SshCertificate(pair, (OpenSshCertificate)publicKey);
	}
	
	public static SshCertificate getCertificate(InputStream privateKey, String passphrase, InputStream certFile) throws IOException, InvalidPassphraseException {
		SshKeyPair pair = getPrivateKey(privateKey, passphrase);
		SshPublicKey publicKey = getPublicKey(certFile);
		if(!(publicKey instanceof OpenSshCertificate)) {
			throw new IOException("Stream input is not a certificate file");
		}
		return new SshCertificate(pair, (OpenSshCertificate)publicKey);
	}
	
	public static SshCertificate getCertificate(String privateKey, String passphrase, String certFile) throws IOException, InvalidPassphraseException {
		SshKeyPair pair = getPrivateKey(privateKey, passphrase);
		SshPublicKey publicKey = getPublicKey(certFile);
		if(!(publicKey instanceof OpenSshCertificate)) {
			throw new IOException("String input is not a certificate file");
		}
		return new SshCertificate(pair, (OpenSshCertificate)publicKey);
	}
	
	public static SshKeyPair makeRSAWithSHA256Signature(SshKeyPair pair) {
		SshKeyPair n = new SshKeyPair();
		n.setPrivateKey(pair.getPrivateKey());
		n.setPublicKey(new Ssh2RsaPublicKeySHA256((SshRsaPublicKey)pair.getPublicKey()));
		return n;
	}
	
	public static SshKeyPair getRSAPrivateKeyWithSHA256Signature(String formattedKey, String passphrase) throws UnsupportedEncodingException, IOException, InvalidPassphraseException {
		SshPrivateKeyFile file = SshPrivateKeyFileFactory.parse(formattedKey.getBytes("UTF-8"));
		return makeRSAWithSHA256Signature(file.toKeyPair(passphrase));
	}

	public static SshKeyPair getRSAPrivateKeyWithSHA256Signature(InputStream key, String passphrase) throws IOException, InvalidPassphraseException {
		return makeRSAWithSHA256Signature(getPrivateKey(key, passphrase));
	}
	
	public static SshKeyPair getRSAPrivateKeyWithSHA256Signature(File key, String passphrase) throws IOException, InvalidPassphraseException {
		return makeRSAWithSHA256Signature(getPrivateKey(key, passphrase));
	}
	
	public static SshKeyPair makeRSAWithSHA512Signature(SshKeyPair pair) {
		SshKeyPair n = new SshKeyPair();
		n.setPrivateKey(pair.getPrivateKey());
		n.setPublicKey(new Ssh2RsaPublicKeySHA512((SshRsaPublicKey)pair.getPublicKey()));
		return n;
	}
	
	public static SshKeyPair getRSAPrivateKeyWithSHA512Signature(String formattedKey, String passphrase) throws UnsupportedEncodingException, IOException, InvalidPassphraseException {
		SshPrivateKeyFile file = SshPrivateKeyFileFactory.parse(formattedKey.getBytes("UTF-8"));
		return makeRSAWithSHA512Signature(file.toKeyPair(passphrase));
	}

	public static SshKeyPair getRSAPrivateKeyWithSHA512Signature(InputStream key, String passphrase) throws IOException, InvalidPassphraseException {
		return makeRSAWithSHA512Signature(getPrivateKey(key, passphrase));
	}
	
	public static SshKeyPair getRSAPrivateKeyWithSHA512Signature(File key, String passphrase) throws IOException, InvalidPassphraseException {
		return makeRSAWithSHA512Signature(getPrivateKey(key, passphrase));
	}
	
	public static String getFingerprint(File key) throws IOException {
		return SshKeyFingerprint.getFingerprint(getPublicKey(key));
	}
	
	public static String getFingerprint(String key) throws IOException {
		return SshKeyFingerprint.getFingerprint(getPublicKey(key));
	}
	
	public static String getFingerprint(InputStream key) throws IOException {
		return SshKeyFingerprint.getFingerprint(getPublicKey(key));
	}
	
	public static String getFingerprint(SshPublicKey key) {
		return SshKeyFingerprint.getFingerprint(key);
	}
	
	public static String getBubbleBabble(SshPublicKey pub) {
		return SshKeyFingerprint.getBubbleBabble(pub);
	}
		
	public static void createPublicKeyFile(SshPublicKey publicKey, String comment, File file) throws IOException {
		createPublicKeyFile(publicKey, comment, file, SshPublicKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public static void createPublicKeyFile(SshPublicKey publicKey, String comment, File file, int format) throws IOException {
		
		SshPublicKeyFile kf = SshPublicKeyFileFactory.create(publicKey, comment, format);
		IOUtils.writeUTF8StringToFile(file, new String(kf.getFormattedKey(), "UTF-8"));
		
	}

	public static void createPrivateKeyFile(SshKeyPair pair, String passphrase, File file) throws IOException {
		createPrivateKeyFile(pair, passphrase, file, SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public static void createPrivateKeyFile(SshKeyPair pair, String passphrase, File file, int format) throws IOException {
		
		SshPrivateKeyFile kf = SshPrivateKeyFileFactory.create(pair, passphrase, format);
		IOUtils.writeUTF8StringToFile(file, new String(kf.getFormattedKey(), "UTF-8"));
	}

	public static boolean isPrivateKeyFile(File file) {
		
		try(InputStream in = new FileInputStream(file)) {
			SshPrivateKeyFileFactory.parse(in);
			return true;
		} catch (IOException e) {

		}
		return false;
	}
	
	public static SshKeyPair getCertificateAndKey(File privateKey, String passphrase) throws IOException, InvalidPassphraseException {
		File certFile = new File(privateKey.getAbsolutePath() + "-cert.pub");
		if(!certFile.exists()) {
			throw new IOException(String.format("No certificate file %s to match private key file %s", certFile.getName(), privateKey.getName()));
		}
		SshKeyPair pair = getPrivateKey(privateKey, passphrase);
		pair.setPublicKey(getPublicKey(certFile));
		return pair;
	}
	
	public static void savePrivateKey(SshKeyPair pair, String passphrase, String comment, File privateKeyFile) throws IOException {
		
		SshPrivateKeyFile privateFile = SshPrivateKeyFileFactory.create(pair, passphrase, comment, SshPrivateKeyFileFactory.OPENSSH_FORMAT);
		
		IOUtils.writeBytesToFile(privateFile.getFormattedKey(), privateKeyFile);
		
		savePublicKey(pair.getPublicKey(), comment, new File(privateKeyFile.getParent(), privateKeyFile.getName() + ".pub"));
	}
	
	public static void saveCertificate(SshCertificate pair, String passphrase, String comment, File privateKeyFile) throws IOException {
		
		SshPrivateKeyFile privateFile = SshPrivateKeyFileFactory.create(pair, passphrase, comment, SshPrivateKeyFileFactory.OPENSSH_FORMAT);
		
		IOUtils.writeBytesToFile(privateFile.getFormattedKey(), privateKeyFile);
		
		savePublicKey(pair.getPublicKey(), comment, new File(privateKeyFile.getParent(), privateKeyFile.getName() + ".pub"));
		savePublicKey(pair.getCertificate(), comment, new File(privateKeyFile.getParent(), privateKeyFile.getName() + "-cert.pub"));
	}

	private static void savePublicKey(SshPublicKey publicKey, String comment, File publicKeyFile) throws IOException {
		
		SshPublicKeyFile publicFile = SshPublicKeyFileFactory.create(publicKey, comment, SshPublicKeyFileFactory.OPENSSH_FORMAT);
		
		IOUtils.writeBytesToFile(publicFile.getFormattedKey(), publicKeyFile);
	}

}
