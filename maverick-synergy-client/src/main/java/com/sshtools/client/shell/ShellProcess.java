package com.sshtools.client.shell;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.common.ssh.SshIOException;

public class ShellProcess {

	private final ExpectShell shell;
	private final ShellInputStream in;
	private final BufferedInputStream bin;
	private final OutputStream out;
	
	ShellProcess(ExpectShell shell, ShellInputStream in, OutputStream out) {
		this.shell = shell;
		this.in = in;
		this.bin = new BufferedInputStream(in);
		this.out = out;
	}

	public void mark(int readlimit) {
		bin.mark(readlimit);
	}
	
	public void reset() throws IOException {
		bin.reset();
	}
	
	/**
	 * Returns an InputStream that will contain only the output from the executed ShellProcess. Use 
	 * this for basic access to command output, if you use the expect methods DO NOT use this stream.
	 * @return
	 */
	public InputStream getInputStream() {
		return bin;
	}

	public OutputStream getOutputStream() throws SshIOException {
		return out;
	}

	public int getExitCode() {
		return in.getExitCode();
	}

	public boolean hasSucceeded() {
		return in.hasSucceeded();
	}

	public boolean isActive() {
		return in.isActive();
	}
	
	public void clearOutput() {
		in.clearOutput();
	}

	public String getCommandOutput() {
		return in.getCommandOutput();
	}

	public ExpectShell getShell() {
		return shell;
	}
	
	public ShellProcess drain() throws IOException {
		while(in.isActive() && bin.read() > -1 && !shell.isClosed());
		return this;
	}


}
