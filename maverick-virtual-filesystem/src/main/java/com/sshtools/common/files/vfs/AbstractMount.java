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

import java.util.HashMap;
import java.util.Map;

import com.sshtools.common.util.FileUtils;

public class AbstractMount {

	private Map<String, Object> attributes = new HashMap<String, Object>();

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof AbstractMount)) {
			return false; 
		}

		return ((AbstractMount) obj).mount.equals(mount)
				&& ((AbstractMount) obj).path.equals(path);
	}

	@Override
	public int hashCode() {
		return mount.hashCode();
	}

	protected String mount;
	protected String path;
	private boolean filesystemRoot;
	private boolean isDefault;
	protected boolean isImaginary;
	protected String username = "unknown";
	protected String group = "unknown";
	protected int uid = 0;
	protected int gid = 0;
	
	protected AbstractMount(String mount, String path) {
		this(mount, path, false, false);
	}

	protected AbstractMount(String mount, String path, boolean isDefault,
			boolean isImaginary) {

		this.filesystemRoot = mount.equals("/");
		this.mount = mount.equals("/") ? mount : FileUtils.removeTrailingSlash(mount);
		this.path = FileUtils.removeTrailingSlash(path);
		this.isDefault = isDefault;
		this.isImaginary = isImaginary;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.maverick.sshd.sftp.FileSystemMountI#getMount()
	 */
	public String getMount() {
		return mount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.maverick.sshd.sftp.FileSystemMountI#getRoot()
	 */
	public String getRoot() {
		return path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.maverick.sshd.sftp.FileSystemMountI#isFilesystemRoot()
	 */
	public boolean isFilesystemRoot() {
		return filesystemRoot;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.maverick.sshd.sftp.FileSystemMountI#isDefault()
	 */
	public boolean isDefault() {
		return isDefault;
	}

	public boolean isImaginary() {
		return isImaginary;
	}

	public Object getAttribute(String key, Object defaultValue) {
		if (attributes.containsKey(key)) {
			return attributes.get(key);
		} else {
			return defaultValue;
		}
	}

	public void setAttribute(String key, Object value) {
		attributes.put(key, value);
	}
	
	public String toString() {
		return getMount() + " on " + getRoot();
	}

	public String getUsername() {
		return username;
	}

	public String getGroup() {
		return group;
	}

	public int getUid() {
		return uid;
	}

	public int getGid() {
		return gid;
	}
}
