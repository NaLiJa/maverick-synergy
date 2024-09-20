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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.RandomAccessContent;
import org.apache.commons.vfs2.util.RandomAccessMode;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileImpl;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpFileAttributes.SftpFileAttributesBuilder;

public class VFSFile extends AbstractFileImpl<VFSFile> {

	FileObject file;
	FileSystemOptions opts;

	public VFSFile(FileObject file, VFSFileFactory fileFactory) {
		super(fileFactory);
		this.file = file;
	}
	
	public VFSFile(String path, VFSFileFactory fileFactory) throws IOException {
		super(fileFactory);
		this.file = fileFactory.getFileSystemManager().resolveFile(path);
	}

	public VFSFile(String path, String defaultPath, VFSFileFactory fileFactory, FileSystemOptions opts)
			throws IOException {
		super(fileFactory);
		this.file = fileFactory.getFileSystemManager().resolveFile(path, opts);
		this.opts = opts;
	}

	public FileObject getFileObject() {
		return file;
	}

	public AbstractFile getParentFile() throws IOException {
		return new VFSFile(file.getParent(), (VFSFileFactory) fileFactory);
	}
	
	public boolean exists() throws IOException {
		return file.exists();
	}

	public boolean createFolder() throws PermissionDeniedException, IOException {
		if(!file.exists()) {
			file.createFolder();
			return file.exists();
		}
		return false;
	}

	public long lastModified() throws IOException {
		return file.getContent().getLastModifiedTime();
	}

	public String getName() {
		return file.getName().getBaseName();
	}

	public long length() throws IOException {
		if (file.getType() == FileType.FILE) {
			return file.getContent().getSize();
		} else {
			return 0;
		}
	}

	public SftpFileAttributes getAttributes() throws IOException {

		if(!exists()) {
			throw new FileNotFoundException();
		}
		var bldr = SftpFileAttributesBuilder.ofType(getFileType(file), "UTF-8");
		if (!isDirectory())
			bldr.withSize(length());
		
		bldr.withLastModifiedTime(lastModified());
		bldr.withLastAccessTime(lastModified());
		
		var permBldr = PosixPermissionsBuilder.create();
		if(isReadable())
			permBldr.withPermissions(PosixFilePermission.OWNER_READ);
		if(isWritable())
			permBldr.withPermissions(PosixFilePermission.OWNER_WRITE);
		if(isDirectory())
			permBldr.withPermissions(PosixFilePermission.OWNER_EXECUTE);
		bldr.withPermissions(permBldr.build());


		try {
			for (var name : file.getContent().getAttributeNames()) {
				var attribute = file.getContent().getAttribute(name);
				bldr.addExtendedAttribute(name,
						attribute == null ? new byte[] {} : String.valueOf(attribute).getBytes());

				if (name.equals("uid")) {
					bldr.withUidOrUsername((String) attribute);
				} else if (name.equals("gid")) {
					bldr.withGidOrGroup((String) attribute);
				} else if (name.equals("accessedTime")) {
					bldr.withLastAccessTime((Long)attribute);
				} 
			}
		} catch (Exception e) {

		}

		return bldr.build();
	}

	private int getFileType(FileObject file) throws FileSystemException {
		
		try {
			for (String name : file.getContent().getAttributeNames()) {
				Object attribute = file.getContent().getAttribute(name);

				if (name.equals("link")
						&& Boolean.TRUE.equals(attribute)) {
					return SftpFileAttributes.SSH_FILEXFER_TYPE_SYMLINK;
				} else if (name.equals("block")
						&& Boolean.TRUE.equals(attribute)) {
					return SftpFileAttributes.SSH_FILEXFER_TYPE_BLOCK_DEVICE;
				} else if(name.equals("character")
						&& Boolean.TRUE.equals(attribute)) {
					return SftpFileAttributes.SSH_FILEXFER_TYPE_CHAR_DEVICE;
				} else if(name.equals("socket")
						&& Boolean.TRUE.equals(attribute)) {
					return SftpFileAttributes.SSH_FILEXFER_TYPE_SOCKET;
				} else if(name.equals("fifo")
						&& Boolean.TRUE.equals(attribute)) {
					return SftpFileAttributes.SSH_FILEXFER_TYPE_FIFO;
				} else if(name.equals("pipe")
						&& Boolean.TRUE.equals(attribute)) {
					return SftpFileAttributes.SSH_FILEXFER_TYPE_SPECIAL;
				}
			}
		} catch (Exception e) {
		}
		
		switch (file.getType()) {
		case FILE:
			return SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR;
		case FOLDER:
			return SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY;
		default:
			return SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN;
		}
	}

	public boolean isHidden() throws IOException {
		return file.isHidden();
	}

	public boolean isDirectory() throws IOException {
		return file.getType() == FileType.FOLDER;
	}

	public List<AbstractFile> getChildren() throws IOException,
			PermissionDeniedException {

		List<AbstractFile> children = new ArrayList<AbstractFile>();
		for (FileObject f : file.getChildren()) {
			children.add(new VFSFile(f, (VFSFileFactory) fileFactory));
		}
		return children;
	}

	public boolean isFile() throws IOException {
		return file.getType() == FileType.FILE;
	}

	public String getAbsolutePath() throws IOException,
			PermissionDeniedException {
		if (!((VFSFileFactory) getFileFactory()).isReturnURIForPath()) {
			return file.getName().getPath();
		} else {
			return file.getName().getURI();
		}
	}

	@Override
	public void copyFrom(AbstractFile src) throws IOException,
			PermissionDeniedException {
		if (src instanceof VFSFile) {
			file.copyFrom(((VFSFile) src).file, new AllFileSelector());
		} else {
			super.copyFrom(src);
		}
	}

	public boolean isReadable() throws IOException {
		return file.isReadable();
	}

	public boolean isWritable() throws IOException {
		return file.isWriteable();
	}

	public boolean createNewFile() throws PermissionDeniedException,
			IOException {
		if(!file.exists()) {
			file.createFile();
			return file.exists();
		}
		return false;
	}

	public void truncate() throws PermissionDeniedException, IOException {
		OutputStream out = file.getContent().getOutputStream();
		out.close();
	}

	public InputStream getInputStream() throws IOException {
		return file.getContent().getInputStream();
	}

	public OutputStream getOutputStream() throws IOException {
		return file.getContent().getOutputStream();
	}

	public boolean delete(boolean recurse) throws IOException {
		if (recurse) {
			file.delete(new AllFileSelector());
			return true;
		}
		return file.delete();
	}

	public void moveTo(AbstractFile target) throws IOException,
			PermissionDeniedException {
		if (target instanceof VFSFile) {
			file.moveTo(((VFSFile) target).file);
		} else {
			super.moveTo(target);
		}

	}

	public void setAttributes(SftpFileAttributes attrs) throws IOException {
		
		file.getContent().setLastModifiedTime(attrs.lastModifiedTime().toMillis());

		// For SSH it's easy
		if (file.getFileSystem().getRootName().getScheme().equals("sftp")) {
			var username = attrs.bestUsernameOr();
			if (username.isPresent()) {
				file.getContent().setAttribute("uid", username.get());
			}
			var group = attrs.bestGroupOr();
			if (group.isPresent()) {
				file.getContent().setAttribute("gid", group.get());
			}
			file.getContent().setAttribute("permissions", attrs.permissions().asInt());
		}

	}

	public String getCanonicalPath() throws IOException,
			PermissionDeniedException {
		return file.getName().getURI();
	}

	public boolean supportsRandomAccess() {
		return file.getFileSystem()
				.hasCapability(Capability.RANDOM_ACCESS_READ)
				|| file.getFileSystem().hasCapability(
						Capability.RANDOM_ACCESS_WRITE);
	}

	public AbstractFileRandomAccess openFile(boolean writeAccess)
			throws IOException {
		return new VFSFileRandomAccess(file.getContent()
				.getRandomAccessContent(
						writeAccess ? RandomAccessMode.READWRITE
								: RandomAccessMode.READ));
	}

	class VFSFileRandomAccess implements AbstractFileRandomAccess {

		RandomAccessContent randomAccessContent;

		public VFSFileRandomAccess(RandomAccessContent randomAccessContent) {
			this.randomAccessContent = randomAccessContent;
		}

		public int read(byte[] buf, int off, int len) throws IOException {

			long length = Math.min(randomAccessContent.length()
					- randomAccessContent.getFilePointer(), len);

			if (length <= 0) {
				return -1;
			}

			randomAccessContent.readFully(buf, off, (int) length);
			return (int) length;
		}

		public void write(byte[] buf, int off, int len) throws IOException {
			randomAccessContent.write(buf, off, len);
		}

		public void setLength(long length) throws IOException {
			long pos = randomAccessContent.getFilePointer();
			if (length > pos) {
				randomAccessContent.seek(pos - 1);
				randomAccessContent.write(0);
				randomAccessContent.seek(pos);
			}
		}

		public void seek(long position) throws IOException {
			randomAccessContent.seek(position);
		}

		public void close() throws IOException {
			randomAccessContent.close();

		}

		public long getFilePointer() throws IOException {
			return randomAccessContent.getFilePointer();
		}

		@Override
		public int read() throws IOException {
			return randomAccessContent.readByte() & 0xFF;
		}

	}

	public void refresh() {
		try {
			file.refresh();
		} catch (FileSystemException e) {
			Log.error("Failed to refresh.", e);
		}
	}

	public AbstractFile resolveFile(String child) throws IOException,
			PermissionDeniedException {
		return new VFSFile(file.resolveFile(child),
				(VFSFileFactory) fileFactory);
	}

	@Override
	protected int doHashCode() {
		return file.hashCode();
	}

	@Override
	protected boolean doEquals(Object obj) {
		if(obj instanceof VFSFile) {
			VFSFile f2 = (VFSFile) obj;
			return file.equals(f2.file);
		}
		return false;
	}

	@Override
	public String readSymbolicLink() throws IOException, PermissionDeniedException {
		throw new UnsupportedOperationException();
	}

}
