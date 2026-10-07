package com.triglav.clan.share;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.triglav.clan.net.ApiClient;
import com.triglav.clan.net.KeyStore;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** "Povej klanu": a screenshot and a short line to POST /api/plugin/share/:key, which posts them to Discord. */
@Slf4j
@Singleton
public class ShareClient
{
	private static final MediaType PNG = MediaType.parse("image/png");
	public static final int MAX_TEXT = 200;

	private final OkHttpClient httpClient;
	private final KeyStore keyStore;
	private final ScheduledExecutorService executor;

	@Inject
	private ShareClient(OkHttpClient httpClient, KeyStore keyStore, ScheduledExecutorService executor)
	{
		this.httpClient = httpClient;
		this.keyStore = keyStore;
		this.executor = executor;
	}

	/** @param png screenshot bytes, or null to share the text alone */
	public void share(String text, byte[] png, Consumer<String> onResult)
	{
		final String key = keyStore.ingestKey();
		if (key == null)
		{
			onResult.accept("najprej poveži račun.");
			return;
		}

		executor.execute(() ->
		{
			final HttpUrl url = HttpUrl.parse(ApiClient.siteUrl() + "/api/plugin/share/" + key);
			if (url == null)
			{
				return;
			}

			final MultipartBody.Builder form = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("text", text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text);
			if (png != null)
			{
				form.addFormDataPart("file", "screenshot.png", RequestBody.create(PNG, png));
			}

			try (Response response = httpClient.newCall(new Request.Builder().url(url).post(form.build()).build()).execute())
			{
				if (response.isSuccessful())
				{
					onResult.accept("poslano klanu.");
					return;
				}

				final String raw = response.body() == null ? "" : response.body().string();
				final JsonObject json = raw.isEmpty() ? new JsonObject() : new JsonParser().parse(raw).getAsJsonObject();
				onResult.accept(json.has("error") ? json.get("error").getAsString() : "ni bilo mogoče poslati (HTTP " + response.code() + ").");
			}
			catch (IOException | RuntimeException e)
			{
				log.warn("Share failed", e);
				onResult.accept("ni bilo mogoče poslati — stran ni dosegljiva.");
			}
		});
	}
}
