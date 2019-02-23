/*
 * Copyright (c) 2019 Hugo Dupanloup (Yeregorix)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.smoofyuniverse.common.ore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.smoofyuniverse.common.app.App;
import net.smoofyuniverse.common.download.ConnectionConfiguration;
import net.smoofyuniverse.common.ore.adapter.InstantAdapter;
import net.smoofyuniverse.common.ore.object.VersionInfo;
import net.smoofyuniverse.common.task.listener.IncrementalListener;
import net.smoofyuniverse.common.task.listener.IncrementalListenerProvider;
import net.smoofyuniverse.common.util.IOUtil;
import net.smoofyuniverse.logger.core.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.List;

public class OreAPI {
	public static final URL URL_BASE;
	public static final Gson GSON = new GsonBuilder().registerTypeAdapter(Instant.class, new InstantAdapter()).create();
	private static final Logger logger = App.getLogger("OreAPI");
	private static final Type versionInfoList = new TypeToken<List<VersionInfo>>() {}.getType();

	public static List<VersionInfo> getProjectVersions(IncrementalListenerProvider p, String projectId, String... channels) throws Exception {
		return getProjectVersions(p, projectId, 0, 10, channels);
	}

	public static List<VersionInfo> getProjectVersions(IncrementalListenerProvider p, String projectId, int offset, int limit, String... channels) throws Exception {
		return getProjectVersions(App.get().getConnectionConfig(), p, projectId, offset, limit, channels);
	}

	public static List<VersionInfo> getProjectVersions(ConnectionConfiguration config, IncrementalListenerProvider p, String projectId, int offset, int limit, String... channels) throws Exception {
		String suffix = "projects/" + projectId + "/versions?offset=" + offset + "&limit=" + limit;
		if (channels.length != 0)
			suffix += "&channels=" + String.join(",", channels);

		HttpURLConnection co = null;
		try {
			co = App.get().getConnectionConfig().openHttpConnection(IOUtil.appendSuffix(URL_BASE, suffix));
			co.connect();
			if (co.getResponseCode() / 100 != 2)
				throw new IOException("Bad response code: " + co.getResponseCode());

			long expected;
			try {
				expected = Long.parseLong(co.getHeaderField("Content-Length"));
			} catch (NumberFormatException e) {
				expected = -1;
			}

			IncrementalListener l = p.provide(expected);

			try (Reader in = new InputStreamReader(co.getInputStream()) {

				@Override
				public int read() throws IOException {
					if (l.isCancelled())
						return -1;

					int r = super.read();
					if (r != -1)
						l.increment(1);
					return r;
				}

				@Override
				public int read(char[] cbuf, int offset, int length) throws IOException {
					if (l.isCancelled())
						return -1;

					int c = super.read(cbuf, offset, length);
					if (c != -1)
						l.increment(c);
					return c;
				}
			}) {
				return GSON.fromJson(in, versionInfoList);
			}
		} finally {
			if (co != null)
				co.disconnect();
		}
	}

	static {
		try {
			URL_BASE = new URL("https://ore.spongepowered.org/api/v1/");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
