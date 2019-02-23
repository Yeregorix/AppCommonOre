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

package net.smoofyuniverse.common.ore.object;

import com.google.gson.JsonParser;
import net.smoofyuniverse.common.app.App;
import net.smoofyuniverse.common.download.ConnectionConfiguration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.List;

public class VersionInfo {
	public String name, pluginId, md5, href, author;
	public List<DependencyInfo> dependencies;
	public List<TagInfo> tags;
	public ChannelInfo channel;
	public Instant createdAt;
	public int id, fileSize, downloads;
	public boolean staffApproved;

	public String getApiVersion() {
		for (DependencyInfo d : this.dependencies) {
			if (d.pluginId.equals("spongeapi"))
				return d.version;
		}
		return null;
	}

	public HttpURLConnection openDownloadConnection() throws IOException {
		return openDownloadConnection(App.get().getConnectionConfig());
	}

	public HttpURLConnection openDownloadConnection(ConnectionConfiguration cfg) throws IOException {
		URL url = new URL("https://ore.spongepowered.org/api/projects/" + this.pluginId + "/versions/" + this.name + "/download");
		if (this.staffApproved)
			return cfg.openHttpConnection(url);

		try (InputStreamReader in = new InputStreamReader(cfg.openStream(url))) {
			url = new URL(new JsonParser().parse(in).getAsJsonObject().get("post").getAsString());
		}

		HttpURLConnection co = cfg.openHttpConnection(url);
		co.setRequestMethod("POST");
		co.setInstanceFollowRedirects(true);
		return co;
	}
}
