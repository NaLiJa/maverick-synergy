package com.sshtools.common.policy;

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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.permissions.Permissions;
import com.sshtools.common.sftp.SftpExtension;
import com.sshtools.common.sftp.SftpExtensionFactory;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.IOUtils;
import com.sshtools.common.util.UnsignedInteger32;

public class FileSystemPolicy extends Permissions {

	long connectionUploadQuota = -1;
	FileFactory fileFactory;
	String sftpCharsetEncoding = "UTF-8";
	boolean allowZeroLengthFileUpload = true;
	boolean sftpVersion4Enabled = true;
	int sftpVersion = 4;
	boolean sftpReadWriteEvents = false;
	boolean scpReadWriteEvents = false;
	int maxConcurrentTransfers = 50;
	int maximumSftpRequests = 10;
	String sftpLongnameDateFormat = "MMM dd  yyyy";
	String sftpLongnameDateFormatWithTime = "MMM dd HH:mm";
	List<SftpExtensionFactory> sftpExtensionFactories = new ArrayList<SftpExtensionFactory>();
	Set<String> disabledExtensions = new HashSet<>();
	boolean closeFileBeforeFailedTransferEvents = false;
	boolean mkdirParentMustExist = true;
	
	private int sftpMaxPacketSize = 65536;
	private UnsignedInteger32 sftpMaxWindowSize = new UnsignedInteger32(IOUtils.fromByteSize("16MB").longValue());
	private UnsignedInteger32 sftpMinWindowSize = new UnsignedInteger32(131072);
	
	public FileSystemPolicy() {
	}
	
	public long getConnectionUploadQuota() {
		return connectionUploadQuota;
	}
	
	public void setConnectionUploadQuota(long connectionUploadQuota) {
		this.connectionUploadQuota = connectionUploadQuota;
	}
	
	public boolean hasUploadQuota() {
		return connectionUploadQuota > -1;
	}
	
	/**
	 * Get the current encoding value for filenames in SFTP sessions.
	 * 
	 * @return String
	 */
	public String getSFTPCharsetEncoding() {
		return sftpCharsetEncoding;
	}

	/**
	 * Set the default encoding for filenames in SFTP sessions. The default
	 * encoding for the currently supported SFTP protocol is ISO-8859-1.
	 * 
	 * @param sftpCharsetEncoding
	 *            String
	 */
	public void setSFTPCharsetEncoding(String sftpCharsetEncoding) {
		this.sftpCharsetEncoding = sftpCharsetEncoding;
	}
	
	/**
	 * Set the file factory for this context.
	 * @param fileFactory
	 */
	public void setFileFactory(FileFactory fileFactory) {
		this.fileFactory = new CachingFileFactory(fileFactory);
	}
	
	/**
	 * Get the file factory for this context.
	 * @return
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public FileFactory getFileFactory() {
		return fileFactory;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isAllowZeroLengthFileUpload() {
		return allowZeroLengthFileUpload;
	}

	/**
	 * 
	 * @param allowZeroLengthFileUpload
	 */
	public void setAllowZeroLengthFileUpload(boolean allowZeroLengthFileUpload) {
		this.allowZeroLengthFileUpload = allowZeroLengthFileUpload;
	}
	
	public void setMaxConcurrentTransfers(int maxConcurrentTransfers) {
		this.maxConcurrentTransfers = maxConcurrentTransfers;
	}

	public int getMaxConcurrentTransfers() {
		return maxConcurrentTransfers;
	}
	
	public void setSupportedSFTPVersion(int sftpVersion) {
		if(sftpVersion < 1 || sftpVersion > 4) {
			throw new IllegalArgumentException("SFTP version must be between 1 and 4");
		}
		this.sftpVersion = sftpVersion;
	}
	
	public int getSFTPVersion() {
		return sftpVersion;
	}
	
	public void setSFTPReadWriteEvents(boolean sftpReadWriteEvents) {
		this.sftpReadWriteEvents = sftpReadWriteEvents;
	}

	public boolean isSFTPReadWriteEvents() {
		return sftpReadWriteEvents;
	}

	public void setSCPReadWriteEvents(boolean scpReadWriteEvents) {
		this.scpReadWriteEvents = scpReadWriteEvents;
	}

	public boolean isSCPReadWriteEvents() {
		return scpReadWriteEvents;
	}
	
	public int getMaximumNumberOfAsyncSFTPRequests() {
		return maximumSftpRequests;
	}
	
	public void setMaximumNumberofAsyncSFTPRequests(int maximumSftpRequests) {
		this.maximumSftpRequests = maximumSftpRequests;
	}

	public String getSFTPLongnameDateFormat() {
		return sftpLongnameDateFormat; //"MMM dd yyyy";
	}

	public String getSFTPLongnameDateFormatWithTime() {
		return sftpLongnameDateFormatWithTime; //"MMM dd HH:mm";
	}
	
	public void disableSFTPExtension(String requestName) {
		disabledExtensions.add(requestName);
	}
	
	public void enableSFTPExtension(String requestName) {
		disabledExtensions.remove(requestName);
	}

	public SftpExtension getSFTPExtension(String requestName) {
		if(disabledExtensions.contains(requestName)) {
			return null;
		}
		for(SftpExtensionFactory factory : sftpExtensionFactories) {
			if(factory.getSupportedExtensions().contains(requestName)) {
				return factory.getExtension(requestName);
			}
		}
		return null;
	}

	public Collection<SftpExtensionFactory> getSFTPExtensionFactories() {
		return sftpExtensionFactories;
	}

	public boolean isSFTPCloseFileBeforeFailedTransferEvents() {
		return closeFileBeforeFailedTransferEvents;
	}
	
	public void setSFTPCloseFileBeforeFailedTransferEvents(boolean closeFileBeforeFailedTransferEvents) {
		this.closeFileBeforeFailedTransferEvents = closeFileBeforeFailedTransferEvents;
	}
	
	public int getSftpMaxPacketSize() {
		return sftpMaxPacketSize;
	}
	
	public void setSftpMaxPacketSize(int sftpMaxPacketSize) {
		this.sftpMaxPacketSize = sftpMaxPacketSize;
	}
	
	public UnsignedInteger32 getSftpMaxWindowSize() {
		return sftpMaxWindowSize;
	}
	
	public void setSftpMaxWindowSize(UnsignedInteger32 sftpMaxWindowSize) {
		this.sftpMaxWindowSize = sftpMaxWindowSize;
	}
	
	public UnsignedInteger32 getSftpMinWindowSize() {
		return sftpMinWindowSize;
	}
	
	public void setSftpMinWindowSize(UnsignedInteger32 sftpMinWindowSize) {
		this.sftpMinWindowSize = sftpMinWindowSize;
	}
	
	class CachingFileFactory implements FileFactory {

		private static final String CACHED_FILE_FACTORY = "cachedFileFactory";
		
		FileFactory fileFactory;
		
		CachingFileFactory(FileFactory fileFactory) {
			this.fileFactory = fileFactory;
		}
		
		@Override
		public AbstractFileFactory<?> getFileFactory(SshConnection con) 
				throws IOException, PermissionDeniedException {
			AbstractFileFactory<?> ff = (AbstractFileFactory<?>) con.getProperty(CACHED_FILE_FACTORY);
			if(Objects.isNull(ff)) {
				if(Objects.isNull(fileFactory)) {
					throw new PermissionDeniedException("Invalid file system configuration");
				}
				ff = fileFactory.getFileFactory(con);
				con.setProperty(CACHED_FILE_FACTORY, ff);
			}
			return ff;
		}
		
	}

	public void setMkdirParentMustExist(boolean mkdirParentMustExist) {
		this.mkdirParentMustExist = mkdirParentMustExist;
	}
	
	public boolean isMkdirParentMustExist() {
		return mkdirParentMustExist;
	}
}
