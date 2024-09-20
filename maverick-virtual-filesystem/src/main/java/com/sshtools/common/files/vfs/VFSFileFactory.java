package com.sshtools.common.files.vfs;

/*-
 * #%L
 * Virtual File System
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
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;

import com.sshtools.common.events.Event;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;

public class VFSFileFactory implements AbstractFileFactory<VFSFile> {
	
	private FileSystemOptions opts;
	private FileObject defaultPath;
	private boolean useURI = true;
	private FileSystemManager manager;
	private String defaultDirectory;

	public VFSFileFactory() throws FileNotFoundException {
		this((FileSystemManager) null);
	}
	
	public VFSFileFactory(String defaultDirectory) throws FileNotFoundException {
		this((FileSystemManager) null, defaultDirectory);
	}

	public VFSFileFactory(FileSystemOptions opts) throws FileNotFoundException {
		this((FileSystemManager) null, opts, null);
	}

	public VFSFileFactory(FileSystemOptions opts, String defaultDirectory) throws FileNotFoundException {
		this((FileSystemManager) null, opts, defaultDirectory);
	}

	public VFSFileFactory(FileSystemManager manager) throws FileNotFoundException {
		this(manager, new FileSystemOptions(), null);
	}

	public VFSFileFactory(FileSystemManager manager, String defaultDirectory) throws FileNotFoundException {
		this(manager, new FileSystemOptions(), defaultDirectory);
	}

	public VFSFileFactory(FileSystemManager manager, FileSystemOptions opts) throws FileNotFoundException {
		this(manager, opts, null);
	}

	public VFSFileFactory(FileSystemManager manager, FileSystemOptions opts, String defaultDirectory)
			throws FileNotFoundException {
		this.opts = opts;
		this.defaultDirectory = defaultDirectory;
		try {
			if (manager == null) {
				manager = VFS.getManager();
			}
		} catch (FileSystemException fse) {
			throw new FileNotFoundException("Could not obtain VFS manager.");
		}
		this.manager = manager;
		if (defaultDirectory == null) {
			try {
				defaultPath = manager.resolveFile(System.getProperty("maverick.vfsDefaultPath", new File(".").getAbsolutePath()));
		 	} catch (FileSystemException e) {
				if(Log.isDebugEnabled()) {
					Log.debug("Unable to determine default path", e);
				}
				throw new FileNotFoundException(
						"Unable to determine a default path. Pass a AbstractFileHomeFactory instance into this constructor of VFSFileFactory");
			}
		}
	}

	public FileSystemManager getFileSystemManager() {
		return manager;
	}

	public boolean isReturnURIForPath() {
		return useURI;
	}

	public void setReturnURIForPath(boolean useURI) {
		this.useURI = useURI;
	}

	public VFSFile getFile(String parent, String path) throws PermissionDeniedException, IOException {
		FileObject obj;
		try {
			obj = manager.resolveFile(parent, opts);
			obj = obj.resolveFile(path);
			return new VFSFile(obj, this);
		} catch (FileSystemException e) {
			try {
				FileObject alt;
				if (defaultDirectory == null) {
					alt = manager.resolveFile(defaultPath, parent);
				} else {
					alt = manager.resolveFile(manager.resolveFile(defaultDirectory, opts), parent);
				}
				alt = alt.resolveFile(path);
				return new VFSFile(alt, this);
			} catch (Exception e1) {
				if(Log.isDebugEnabled()) {
					Log.debug("Unable to resolve file " + path, e1);
				}
				throw new FileNotFoundException("Path " + path + " was not found");
			}
		}
	}

	public VFSFile getFile(String path) throws PermissionDeniedException, IOException {
		FileObject obj;
		try {
			obj = manager.resolveFile(path, opts);
		} catch (FileSystemException e) {
			try {
				if (defaultDirectory == null) {
					obj = manager.resolveFile(defaultPath, path);
				} else {
					obj = manager.resolveFile(manager.resolveFile(defaultDirectory, opts), path);
				}
			} catch (Exception e1) {
				if(Log.isDebugEnabled()) {
					Log.debug("Unable to resolve file " + path, e1);
				}
				throw new FileNotFoundException("Path " + path + " was not found");
			}
		}
		return new VFSFile(obj, this);
	}

	public Event populateEvent(Event evt) {
		return evt;
	}

	public void setDefaultPath(String path) throws FileNotFoundException {
		try {
			defaultPath = manager.resolveFile(path, opts);
		} catch (FileSystemException e) {
			if(Log.isDebugEnabled()) {
				Log.debug("Unable to set default path", e);
			}
			throw new FileNotFoundException(
					"Unable to determine a default path. Set AbstractFileHomeFactory instance on VFSFileFactory");
		}
	}

	public VFSFile getDefaultPath() throws PermissionDeniedException, IOException {
		return getFile("");
	}
}
