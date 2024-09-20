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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceLoader;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.ssh.components.jce.JCEProvider;

/**
 * Private key format factory used to decode private key files. This factory
 * currently supports SSHTools, OpenSSH and SSH1 encrypted private keys.
 * 
 * @author Lee David Painter
 */
public class SshPrivateKeyFileFactory {

	public static final int OPENSSH_FORMAT = 0;
	public static final int OPENSSL_FORMAT = 4;
	
	/**
	 * Parse formatted data and return a suitable <a
	 * href="SshPrivateKeyFile.html">SshPrivateKeyFile</a> implementation.
	 * 
	 * @param formattedkey
	 * @return SshPrivateKeyFile
	 * @throws IOException
	 */
	public static SshPrivateKeyFile parse(byte[] formattedkey)
			throws IOException {

		if(JCEProvider.hasBCProvider() && JCEProvider.isBCEnabled()) {
			try {
					@SuppressWarnings("unchecked")
					Class<SshPrivateKeyFile> clz = (Class<SshPrivateKeyFile>) Class.forName("com.sshtools.common.publickey.bc.OpenSSHPrivateKeyFile" + JCEProvider.getBCProvider().getName());
					
					Method is = clz.getMethod("isFormatted", byte[].class);
					
					Boolean result = (Boolean) is.invoke(null, formattedkey);
					
					if(result) {
						Constructor<SshPrivateKeyFile> c = clz.getDeclaredConstructor(byte[].class);
						c.setAccessible(true);
						return c.newInstance(formattedkey);
					}
	
			} catch(InvocationTargetException e) { 
				if(Boolean.getBoolean("maverick.verbose")) {
					Log.warn("OpenSSHPrivateKeyFile could not load using Bouncycastle PKIX", e.getTargetException());
				} 
			} catch(Throwable t) {
				if(Boolean.getBoolean("maverick.verbose")) {
					Log.warn("Bouncycastle PKIX not in classpath so falling back to older implementation of OpenSSHPrivateKeyFile.", t);
				} 
			}
		}	
		try {			
			if (OpenSSHPrivateKeyFile.isFormatted(formattedkey)) {
				return new OpenSSHPrivateKeyFile(formattedkey);
			} else if (Base64EncodedFileFormat.isFormatted(formattedkey,
					SshtoolsPrivateKeyFile.BEGIN, SshtoolsPrivateKeyFile.END)) {
				return new SshtoolsPrivateKeyFile(formattedkey);
			} else if (SSHCOMPrivateKeyFile.isFormatted(formattedkey)) {
				return new SSHCOMPrivateKeyFile(formattedkey);
			} else {
				for(var provider : ServiceLoader.load(SshPrivateKeyProvider.class,
						JCEComponentManager.getDefaultInstance().getClassLoader())) {
					if(provider.isFormatted(formattedkey)) {
						return provider.create(formattedkey);
					}
				}
				throw new IOException(
						"A suitable key format could not be found!");
			}
		} catch (OutOfMemoryError ex) {
			throw new IOException(
					"An error occurred parsing a private key file! Is the file corrupt?");
		}

	}

	/**
	 * Parse an InputStream and return a suitable <a
	 * href="SshPrivateKeyFile.html">SshPrivateKeyFile</a> implementation.
	 * 
	 * @param in
	 * @return SshPrivateKeyFile
	 * @throws IOException
	 */
	public static SshPrivateKeyFile parse(InputStream in) throws IOException {

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int read;
			while ((read = in.read()) > -1) {
				out.write(read);
			}
			return parse(out.toByteArray());
		} finally {
			try {
				in.close();
			} catch (IOException ex) {
			}
		}

	}

	public static SshPrivateKeyFile create(SshKeyPair pair) throws IOException {
		return create(pair, null, SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public static SshPrivateKeyFile create(SshKeyPair pair, String passphrase) throws IOException {
		return create(pair, passphrase, SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public static SshPrivateKeyFile create(SshKeyPair pair, String passphrase, int format) throws IOException {
		return create(pair, passphrase, "", format);
	}
	
	public static SshPrivateKeyFile create(SshKeyPair pair, String passphrase, String comment) throws IOException {
		return create(pair, passphrase, comment, SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public static SshPrivateKeyFile create(SshKeyPair pair, String passphrase, String comment, int format) throws IOException {

		switch (format) {
		case OPENSSL_FORMAT:
			if(!JCEProvider.isBCEnabled()) {
				throw new IOException("OpenSSL format requires maverick-bc dependency and BouncyCastle JCE and PKIX dependencies");
			}
			
			try {
				return tryBC(pair, passphrase);
			} catch(UnsupportedOperationException e) {
					throw new IOException(e.getMessage(), e);
			}
			
		case OPENSSH_FORMAT:
			if(JCEProvider.isBCEnabled()) {
				try {
					return tryBC(pair, passphrase);
				} catch(UnsupportedOperationException t) {
				}
			}
			return new OpenSSHPrivateKeyFile(pair, passphrase, comment);
		default:
			throw new IOException("Invalid key format!");
		}

	}

	private static SshPrivateKeyFile tryBC(SshKeyPair pair, String passphrase) throws UnsupportedOperationException {
		
		try {
			/**
			 * Try BouncyCastle based PEM / OpenSSH else failover to
			 * previous implementation
			 */
			@SuppressWarnings("unchecked")
			Class<SshPrivateKeyFile> clz = (Class<SshPrivateKeyFile>) Class.forName("com.sshtools.common.publickey.bc.OpenSSHPrivateKeyFile" + JCEProvider.getBCProvider().getName());
			
			Constructor<SshPrivateKeyFile> c = clz.getDeclaredConstructor(SshKeyPair.class, String.class);
			c.setAccessible(true);
			SshPrivateKeyFile f = c.newInstance(pair, passphrase);
			f.toKeyPair(passphrase);
			return f;
		} catch(Throwable t) {
			throw new UnsupportedOperationException(t);
		}
	}

	public static void createFile(SshKeyPair key, String passphrase,
			File toFile) throws IOException {
		createFile(key, passphrase, OPENSSH_FORMAT, toFile);
	}
	
	/**
	 * Take a <a href="SshPrivateKey.html">SshPrivateKey</a> and write it to a
	 * file.
	 * 
	 * @param key
	 * @param comment
	 * @param format
	 * @param toFile
	 * @throws IOException
	 */
	public static void createFile(SshKeyPair key, String passphrase,
			int format, File toFile) throws IOException {

		SshPrivateKeyFile pub = create(key, passphrase, format);

		FileOutputStream out = new FileOutputStream(toFile);

		try {
			out.write(pub.getFormattedKey());
			out.flush();
		} finally {
			out.close();
		}
	}

	/**
	 * Take a file in any of the supported public key formats and convert to the
	 * requested format.
	 * 
	 * @param keyFile
	 * @param toFormat
	 * @param toFile
	 * @throws IOException
	 * @throws InvalidPassphraseException
	 */
	public static void convertFile(File keyFile, String passphrase, int toFormat, File toFile) throws IOException,
			InvalidPassphraseException {

		SshPrivateKeyFile pub = parse(new FileInputStream(keyFile));

		createFile(pub.toKeyPair(passphrase), passphrase, toFormat,
				toFile);
	}
	
	/**
	 * Take a file in any of the supported public key formats and convert to the
	 * requested format.
	 * 
	 * @param keyFile
	 * @param toFormat
	 * @param toFile
	 * @throws IOException
	 * @throws InvalidPassphraseException
	 */
	public static void changePassphrase(File keyFile, String passphrase,
			String newPassphrase) throws IOException,
			InvalidPassphraseException {

		SshPrivateKeyFile pub = parse(new FileInputStream(keyFile));

		pub.changePassphrase(passphrase, newPassphrase);
		
		FileOutputStream out = new FileOutputStream(keyFile);

		try {
			out.write(pub.getFormattedKey());
			out.flush();
		} finally {
			out.close();
		}
	}

	public static SshPrivateKeyFile parse(Path path) throws IOException {
		return parse(Files.newInputStream(path));
	}

	public static SshPrivateKeyFile parse(File identityFile) throws IOException {
		return parse(identityFile.toPath());
	}
}
