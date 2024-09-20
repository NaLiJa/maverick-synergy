package com.sshtools.agent.provider.namedpipes;

/*-
 * #%L
 * Named Pipes Key Agent Provider
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

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;

public class NamedPipeClient extends AbstractNamedPipe {

	NamedPipeSession session;
	
	public NamedPipeClient(String pipeName) throws IOException {
		super(pipeName);
		
		HANDLE hPipe = assertValidHandle("CreateFile", Kernel32.INSTANCE.CreateFile(getPath(),
                WinNT.GENERIC_READ | WinNT.GENERIC_WRITE,
                0,
                null,
                WinNT.OPEN_EXISTING,
                0,
                null));
		
		this.session = new NamedPipeSession(hPipe);
	}
	
	public InputStream getInputStream() {
		return session.getInputStream();
	}
	
	public OutputStream getOutputStream() {
		return session.getOutputStream();
	}
	
	public void close() throws IOException {
		session.close();
	}
}
