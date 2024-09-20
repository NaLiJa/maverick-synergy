package com.sshtools.common.sftp;

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

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.util.UnsignedInteger64;

public class Multipart {

	AbstractFile targetFile;
	String transaction;
	String partIdentifier;
	UnsignedInteger64 startPosition;
	UnsignedInteger64 length;
	
	public String getTransactionUUID() {
		return transaction;
	}

	public void setTransaction(String transaction) {
		this.transaction = transaction;
	}
	
	public UnsignedInteger64 getStartPosition() {
		return startPosition;
	}
	
	public void setStartPosition(UnsignedInteger64 startPosition) {
		this.startPosition = startPosition;
	}
	
	public UnsignedInteger64 getLength() {
		return length;
	}
	
	public void setLength(UnsignedInteger64 length) {
		this.length = length;
	}

	public String getPartIdentifier() {
		return partIdentifier;
	}

	public void setPartIdentifier(String partIdentifier) {
		this.partIdentifier = partIdentifier;
	}

	public AbstractFile getTargetFile() {
		return targetFile;
	}
	
	public void setTargetFile(AbstractFile targetFile) {
		this.targetFile = targetFile;
	}
}
