/*
 *	Copyright 2012 Takehito Tanabe (dateofrock at gmail dot com)
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package com.dateofrock.simpledbmapper.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * 
 * 
 * @author Takehito Tanabe (dateofrock at gmail dot com)
 */
public class IOUtils {

	public static String readString(InputStream input, String encoding) {
		InputStreamReader reader = null;
		StringWriter writer = null;
		try {
			reader = new InputStreamReader(input, encoding);
			writer = new StringWriter();
			int c;
			while ((c = reader.read()) != -1) {
				writer.write(c);
			}
			return writer.toString();
		} catch (Exception e) {
			throw new IOUtilsRuntimeException("Failed to read InputStream", e);
		} finally {
			IOUtils.closeQuietly(reader);
			IOUtils.closeQuietly(writer);
			IOUtils.closeQuietly(input);
		}
	}

	public static byte[] readBytes(InputStream input) {
		ByteArrayOutputStream byteout = new ByteArrayOutputStream();
		BufferedOutputStream bufout = new BufferedOutputStream(byteout);
		int c;
		try {
			while ((c = input.read()) != -1) {
				bufout.write(c);
			}
		} catch (IOException e) {
			throw new IOUtilsRuntimeException("Failed to read InputStream", e);
		} finally {
			IOUtils.closeQuietly(bufout);
			IOUtils.closeQuietly(byteout);
			IOUtils.closeQuietly(input);
		}
		return byteout.toByteArray();
	}

	public static void closeQuietly(OutputStream output) {
		if (output != null) {
			try {
				output.close();
			} catch (IOException ignore) {
			}
		}
	}

	public static void closeQuietly(InputStream instr) {
		if (instr != null) {
			try {
				instr.close();
			} catch (IOException ignore) {
			}
		}
	}

	public static void closeQuietly(Reader reader) {
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException ignore) {
			}
		}
	}

	public static void closeQuietly(Writer writer) {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException ignore) {
			}
		}

	}

}
