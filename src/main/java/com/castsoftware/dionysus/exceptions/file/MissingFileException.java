/*
 * Copyright (C) 2020  Hugo JOBY
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty ofnMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNUnLesser General Public License v3 for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public v3 License along with this library; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.castsoftware.dionysus.exceptions.file;

import com.castsoftware.dionysus.exceptions.ExtensionException;

/**
 * The <code>FileNotFound</code> is thrown when the procedure can't access a file because it doesn't
 * exist, or the path resolution failed. FileNotFound
 */
public class MissingFileException extends ExtensionException {

  private static final long serialVersionUID = 1676506597566629385L;
  private static final String MESSAGE_PREFIX = "Error, file not found : ";
  private static final String CODE_PREFIX = "FIL_NF_";

  public MissingFileException(String path, Throwable cause, String code) {
    super(MESSAGE_PREFIX.concat(path), cause, CODE_PREFIX.concat(code));
  }

  public MissingFileException(String message, String path, String code) {
    super(
        MESSAGE_PREFIX.concat(message).concat(". Path : ").concat(path), CODE_PREFIX.concat(code));
  }
}
