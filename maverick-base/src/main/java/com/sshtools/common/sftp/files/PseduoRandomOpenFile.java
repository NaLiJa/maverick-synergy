package com.sshtools.common.sftp.files;

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
import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.OpenFile;
import com.sshtools.common.util.UnsignedInteger32;

public class PseduoRandomOpenFile implements OpenFile { 
		AbstractFile f;
		UnsignedInteger32 flags;
		long filePointer;
		boolean textMode = false;
		InputStream in;
		OutputStream out;
		boolean closed;
		byte[] handle; 
		
		public PseduoRandomOpenFile(AbstractFile f, UnsignedInteger32 flags, byte[] handle) throws IOException, PermissionDeniedException {
			this.f = f;
			this.flags = flags;
			this.textMode = (flags.intValue() & AbstractFileSystem.OPEN_TEXT) != 0;
			if (isTextMode() && Log.isDebugEnabled()) {
				Log.debug(f.getName() + " is being opened in TEXT mode");
			}
		}
		
		@Override
		public byte[] getHandle() {
			return handle;
		}

		public boolean isTextMode() {
			return textMode;
		}

		public void close() throws IOException {
			if (in != null) {
				try {
					in.close();
				} finally {
					in = null;
				}
			}
			if (out != null) {
				try {
					out.close();
				} finally {
					out = null;
				}
			}
			closed = true;
		}

		public int read(byte[] buf, int off, int len) throws IOException, PermissionDeniedException {
			if(closed) {
				return -1;
			}
			if (filePointer == -1)
				return -1;
			InputStream in = getInputStream();
			int count = 0;
			while (count < len) {
				int r = in.read(buf, off + count, len - count);
				if (r == -1) {
					if (count == 0) {
						filePointer = -1;
						return -1;
					} else {
						return count;
					}
				} else {
					filePointer += r;
					count += r;
				}
			}
			return count;
		}

		public void write(byte[] buf, int off, int len) throws IOException, PermissionDeniedException {
			if(closed) {
				throw new IOException("File has been closed.");
			}
			if (filePointer == -1)
				throw new IOException("File is EOF");
			OutputStream out = getOutputStream();
			out.write(buf, off, len);
			filePointer += len;
		}

		private OutputStream getOutputStream() throws IOException, PermissionDeniedException {
			if(closed) {
				throw new IOException("File has been closed [getOutputStream].");
			}
			if (out == null)
				out = f.getOutputStream();
			return out;
		}

		private InputStream getInputStream() throws IOException, PermissionDeniedException {
			if(closed) {
				throw new IOException("File has been closed [getInputStream].");
			}
			if (in == null)
				in = f.getInputStream();
			return in;
		}

		public void seek(long longValue) throws IOException {
			if(closed) {
				throw new IOException("File has been closed [getOutputStream].");
			}
			filePointer = -1;
			return;
		}

		public AbstractFile getFile() {
			return f;
		}

		public UnsignedInteger32 getFlags() {
			return flags;
		}

		public long getFilePointer() throws IOException {
			if(closed) {
				throw new IOException("File has been closed [getFilePointer].");
			}
			return  filePointer;
		}

		@Override
		public void processEvent(Event evt) {
			evt.addAttribute(EventCodes.ATTRIBUTE_ABSTRACT_FILE, f);

			if(in!=null) {
				evt.addAttribute(EventCodes.ATTRIBUTE_ABSTRACT_FILE_INPUTSTREAM, in);
			}
			if(out!=null) {
				evt.addAttribute(EventCodes.ATTRIBUTE_ABSTRACT_FILE_OUTPUTSTREAM, out);
			}
		}
	}
