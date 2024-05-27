package com.sshtools.server.vsession;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.SessionChannelServer;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.synergy.ssh.TerminalModes;

public class VirtualConsole {

	Environment env;
	Terminal terminal;
	LineReader reader;
	SshConnection con;
	SessionChannelServer channel;
	Msh shell;
	AbstractFile cwd;
	AbstractFileFactory<?> fileFactory;
	TerminalModes modes;
	
	static ThreadLocal<VirtualConsole> threadConsoles = new ThreadLocal<>();
	
	public VirtualConsole(SessionChannelServer channel, Environment env, Terminal terminal, LineReader reader, Msh shell, TerminalModes modes) throws IOException, PermissionDeniedException {
		this.channel = channel;
		this.con = channel.getConnection();
		this.env = env;
		this.terminal = terminal;
		this.reader = reader;
		this.shell = shell;
		this.fileFactory = getContext().getPolicy(FileSystemPolicy.class)
				.getFileFactory().getFileFactory(con);
		this.modes = modes;
	}
	
	public TerminalModes getPseudoTerminalModes() {
		return modes;
	}
	
	public SshConnection getConnection() {
		return con;
	}
	
	public Environment getEnvironment() {
		return env;
	}
	
	public Terminal getTerminal() {
		return terminal;
	}
	
	public LineReader getLineReader() {
		return reader;
	}
	
	public void clear() {
		terminal.puts(Capability.clear_screen);
        terminal.flush();
	}
	
	public void print(char ch) {
		terminal.writer().print(ch);
		terminal.writer().flush();
	}
	
	public void print(String str) {
		terminal.writer().print(str);
		terminal.writer().flush();
	}
	
	public void println(String str) {
		terminal.writer().print(str);
		terminal.writer().print("\r\n");
		terminal.writer().flush();
	}
	
	public void println() {
		terminal.writer().print("\r\n");
		terminal.writer().flush();
	}
	
	public String readLine(String prompt) {
		return reader.readLine(prompt);
	}
	
	public String readLine(String prompt, Character mask) {
		return reader.readLine(prompt, mask);
	}

	public Context getContext() {
		return con.getContext();
	}

	public String readLine() {
		return reader.readLine();
	}

	public SessionChannelServer getSessionChannel() {
		return channel;
	}

	public History getHistory() {
		return reader.getHistory();
	}

	public void destroy() {

	}

	public void print(Throwable e) {
		e.printStackTrace(terminal.writer());
		terminal.writer().flush();
		
	}

	public Msh getShell() {
		return shell;
	}

	public void setCurrentDirectory(String currentDirectory) throws IOException, PermissionDeniedException {
		if(Objects.isNull(cwd)) {
			cwd = fileFactory.getFile((String)env.getOrDefault("HOME", "/"));
			if(!cwd.exists()) {
				cwd.createFolder();
				if(!cwd.exists()) {
					throw new FileNotFoundException(String.format("User directory %s does not exist", cwd.getName()));
				}
			} else {
				if(!cwd.isDirectory()) {
					throw new IOException(String.format("%s is not a directory", cwd.getName()));
				}
			}
		} 
		
		AbstractFile file = cwd.resolveFile(currentDirectory);
		
		if(!file.isDirectory()) {
			throw new IOException(String.format("%s is not a directory", file.getName()));
		}
		
		this.cwd = file;
		env.put("CWD", currentDirectory);
	}
	
	public AbstractFile getCurrentDirectory() throws IOException, PermissionDeniedException {
		if(Objects.isNull(cwd)) {
			setCurrentDirectory("");
		}
		return cwd;
	}

	public AbstractFileFactory<?> getFileFactory() {
		return fileFactory;
	}
	
	public static VirtualConsole getCurrentConsole() {
		return threadConsoles.get();
	}

	public static void setCurrentConsole(VirtualConsole console) {
		threadConsoles.set(console);
	}

	public static void clearCurrentConsole() {
		threadConsoles.remove();
	}
	
}
