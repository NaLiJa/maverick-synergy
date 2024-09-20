package com.sshtools.client.sftp;

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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

/**
 * <p>
 * This class provides a list of operations that have been/or will be completed
 * by the SftpClient's copyRemoteDirectory/copyLocalDirectory methods.
 * </p>
 * <p>
 * The objects returned could either be {@link com.sshtools.client.sftp.maverick.sftp.SftpFile} or
 * <em>java.io.File</em> depending upon the commit state and whether
 * syncronization is required. Any code using the values returned should be able
 * to handle both types of file object.
 * </p>
 * 
 * 
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class DirectoryOperation {

	Vector unchangedFiles = new Vector();
	Vector newFiles = new Vector();
	Vector updatedFiles = new Vector();
	Vector deletedFiles = new Vector();
	Vector recursedDirectories = new Vector();
	Hashtable failedTransfers = new Hashtable();

	/**
	 * Construct a new directory operation object
	 */
	public DirectoryOperation() {
	}

	
	void addNewFile(AbstractFile f) {
		newFiles.addElement(f);
	}

	void addFailedTransfer(AbstractFile f, SftpStatusException ex) {
		failedTransfers.put(f, ex);
	}

	void addUpdatedFile(AbstractFile f) {
		updatedFiles.addElement(f);
	}

	void addDeletedFile(AbstractFile f) {
		deletedFiles.addElement(f);
	}

	void addUnchangedFile(AbstractFile f) {
		unchangedFiles.addElement(f);
	}

	void addNewFile(SftpFile f) {
		newFiles.addElement(f);
	}

	void addFailedTransfer(SftpFile f, SftpStatusException ex) {
		failedTransfers.put(f, ex);
	}

	void addUpdatedFile(SftpFile f) {
		updatedFiles.addElement(f);
	}

	void addDeletedFile(SftpFile f) {
		deletedFiles.addElement(f);
	}

	void addUnchangedFile(SftpFile f) {
		unchangedFiles.addElement(f);
	}

	/**
	 * Returns a list of new files that will be transfered in the directory
	 * operation
	 * 
	 * @return Vector
	 */
	public Vector getNewFiles() {
		return newFiles;
	}

	/**
	 * Returns a list of files that will be updated in the directory operation
	 * 
	 * @return Vector
	 */
	public Vector getUpdatedFiles() {
		return updatedFiles;
	}

	/**
	 * Returns the list of files that will not be changed during the directory
	 * operation
	 * 
	 * @return Vector
	 */
	public Vector getUnchangedFiles() {
		return unchangedFiles;
	}

	/**
	 * When synchronizing directories, this method will return a list of files
	 * that will be deleted becasue they no longer exist at the source location.
	 * 
	 * @return Vector
	 */
	public Vector getDeletedFiles() {
		return deletedFiles;
	}

	/**
	 * Returns a Hashtable of files and exceptions.
	 * 
	 * @return Vector
	 */
	public Hashtable getFailedTransfers() {
		return failedTransfers;
	}

	/**
	 * Determine whether the operation contains a file.
	 * 
	 * @param f
	 * @return boolean
	 */
	public boolean containsFile(AbstractFile f) {
		return unchangedFiles.contains(f) || newFiles.contains(f)
				|| updatedFiles.contains(f) || deletedFiles.contains(f)
				|| recursedDirectories.contains(f)
				|| failedTransfers.containsKey(f);
	}

	/**
	 * Determine whether the directory operation contains an SftpFile
	 * 
	 * @param f
	 * @return boolean
	 */
	public boolean containsFile(SftpFile f) {
		return unchangedFiles.contains(f) || newFiles.contains(f)
				|| updatedFiles.contains(f) || deletedFiles.contains(f)
				|| recursedDirectories.contains(f.getAbsolutePath())
				|| failedTransfers.containsKey(f);
	}

	/**
	 * Add the contents of another directory operation. This is used to record
	 * changes when recuring through directories.
	 * 
	 * @param op
	 * @param f
	 */
	public void addDirectoryOperation(DirectoryOperation op, AbstractFile f) {
		addAll(op.getUpdatedFiles(), updatedFiles);
		addAll(op.getNewFiles(), newFiles);
		addAll(op.getUnchangedFiles(), unchangedFiles);
		addAll(op.getDeletedFiles(), deletedFiles);

		Object obj;
		for (Enumeration e = op.failedTransfers.keys(); e.hasMoreElements();) {
			obj = e.nextElement();
			failedTransfers.put(obj, op.failedTransfers.get(obj));
		}

		recursedDirectories.addElement(f);
	}

	void addAll(Vector source, Vector dest) {
		for (Enumeration e = source.elements(); e.hasMoreElements();) {
			dest.addElement(e.nextElement());
		}
	}

	/**
	 * Get the total number of new and changed files to transfer
	 * 
	 * @return int
	 */
	public int getFileCount() {
		return newFiles.size() + updatedFiles.size();
	}

	/**
	 * Add the contents of another directory operation. This is used to record
	 * changes when recuring through directories.
	 * 
	 * @param op
	 * @param file
	 */
	public void addDirectoryOperation(DirectoryOperation op, String file) {
		addAll(op.getUpdatedFiles(), updatedFiles);
		addAll(op.getNewFiles(), newFiles);
		addAll(op.getUnchangedFiles(), unchangedFiles);
		addAll(op.getDeletedFiles(), deletedFiles);

		Object obj;
		for (Enumeration e = op.failedTransfers.keys(); e.hasMoreElements();) {
			obj = e.nextElement();
			failedTransfers.put(obj, op.failedTransfers.get(obj));
		}

		recursedDirectories.addElement(file);
	}

	/**
	 * Get the total number of bytes that this operation will transfer
	 * 
	 * @return long
	 * @throws IOException 
	 * @throws PermissionDeniedException 
	 */
	public long getTransferSize() throws SftpStatusException, SshException, IOException, PermissionDeniedException {

		Object obj;
		long size = 0;
		SftpFile sftpfile;
		AbstractFile file;
		for (Enumeration e = newFiles.elements(); e.hasMoreElements();) {
			obj = e.nextElement();
			if (obj instanceof AbstractFile) {
				file = (AbstractFile) obj;
				if (file.isFile()) {
					size += file.length();
				}
			} else if (obj instanceof SftpFile) {
				sftpfile = (SftpFile) obj;
				if (sftpfile.attributes().isFile()) {
					size += sftpfile.attributes().size().longValue();
				}
			}
		}
		for (Enumeration e = updatedFiles.elements(); e.hasMoreElements();) {
			obj = e.nextElement();

			if (obj instanceof AbstractFile) {
				file = (AbstractFile) obj;
				if (file.isFile()) {
					size += file.length();
				}
			} else if (obj instanceof SftpFile) {
				sftpfile = (SftpFile) obj;
				if (sftpfile.attributes().isFile()) {
					size += sftpfile.attributes().size().longValue();
				}
			}
		}

		// Add a value for deleted files??

		return size;
	}

}
