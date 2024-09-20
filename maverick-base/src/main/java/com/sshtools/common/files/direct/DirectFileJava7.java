package com.sshtools.common.files.direct;

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

import java.io.File;
import java.io.IOException;

import com.sshtools.common.files.AbstractFileFactory;

/**
 * Deprecated, use {@link NioFile} and {@link NioFileFactory} instead.
 */
@Deprecated(since = "3.1.0", forRemoval = true)
public class DirectFileJava7 extends DirectFile {

	public DirectFileJava7(String path, AbstractFileFactory<DirectFile> fileFactory, File homeDir) throws IOException {
		super(path, fileFactory, homeDir);
	}
	
}
