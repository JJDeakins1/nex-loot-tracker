package com.nexloottracker;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import net.runelite.http.api.chat.Duels;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Shares dry-streak snapshots for {@code !nexdry} chat commands.
 * Uses RuneLite's chat/duels storage the same way ToB QoL does for {@code !tobdry}.
 */
@Slf4j
public class NexChatClient
{
	private final OkHttpClient client;
	private final HttpUrl apiBase;
	private final Gson gson;

	@Inject
	private NexChatClient(OkHttpClient client, @Named("runelite.api.base") HttpUrl apiBase, Gson gson)
	{
		this.client = client;
		this.apiBase = apiBase;
		this.gson = gson;
	}

	public boolean submitDryStreak(String username, DryStreakStats stats) throws IOException
	{
		int lastItemId = 0;
		if (stats.getLastPersonalItem() != null && !stats.getLastPersonalItem().isEmpty())
		{
			NexUniques unique = NexUniques.fromName(stats.getLastPersonalItem());
			if (unique != null)
			{
				lastItemId = unique.getItemId();
			}
		}

		HttpUrl url = apiBase.newBuilder()
			.addPathSegment("chat")
			.addPathSegment("duels")
			.addQueryParameter("name", username)
			.addQueryParameter("wins", Integer.toString(stats.getPersonalDry()))
			.addQueryParameter("losses", Integer.toString(stats.getTeamDry()))
			.addQueryParameter("winningStreak", Integer.toString(lastItemId))
			.addQueryParameter("losingStreak", "0")
			.build();

		Request request = new Request.Builder()
			.post(RequestBody.create(null, new byte[0]))
			.url(url)
			.build();

		try (Response response = client.newCall(request).execute())
		{
			return response.isSuccessful();
		}
	}

	public DryStreakStats getDryStreak(String username) throws IOException
	{
		HttpUrl url = apiBase.newBuilder()
			.addPathSegment("chat")
			.addPathSegment("duels")
			.addQueryParameter("name", username)
			.build();

		Request request = new Request.Builder()
			.url(url)
			.build();

		try (Response response = client.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				throw new IOException("unable to lookup nex dry streak");
			}

			InputStream in = response.body().byteStream();
			Duels duels = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Duels.class);

			String lastItem = "";
			if (duels.getWinningStreak() != 0)
			{
				NexUniques unique = NexUniques.fromItemId(duels.getWinningStreak());
				if (unique != null)
				{
					lastItem = unique.getName();
				}
			}

			return new DryStreakStats(duels.getWins(), duels.getLosses(), lastItem);
		}
		catch (JsonParseException ex)
		{
			throw new IOException(ex);
		}
	}
}
