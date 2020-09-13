package com.csc301.profilemicroservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.csc301.profilemicroservice.Utils;
import com.csc301.profilemicroservice.DbQueryStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class ProfileController {
	public static final String KEY_USER_NAME = "userName";
	public static final String KEY_USER_FULLNAME = "fullName";
	public static final String KEY_USER_PASSWORD = "password";

	@Autowired
	private final ProfileDriverImpl profileDriver;

	@Autowired
	private final PlaylistDriverImpl playlistDriver;

	OkHttpClient client = new OkHttpClient();

	public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
		this.profileDriver = profileDriver;
		this.playlistDriver = playlistDriver;
	}

	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addProfile(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));
		
		String userName = params.get("userName");
		String fullName = params.get("fullName");
		String password = params.get("password");
		

		DbQueryStatus dbQueryStatus = profileDriver.createUserProfile(userName, fullName, password);
		
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		
		return response;
	}

	@RequestMapping(value = "/followFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> followFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = profileDriver.followFriend(userName, friendUserName);
		
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		
		return response;
	}

	@RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getAllFriendFavouriteSongTitles(@PathVariable("userName") String userName,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = profileDriver.getAllSongFriendsLike(userName);
		
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		
		return response;
	}


	@RequestMapping(value = "/unfollowFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unfollowFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		DbQueryStatus dbQueryStatus = profileDriver.unfollowFriend(userName, friendUserName);
		
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		
		return response;
	}
	
	@RequestMapping(value = "/addSong/{songId}/{songName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> addSong(@PathVariable("songId") String songId,
			@PathVariable ("songName") String songName, HttpServletRequest request) {
		
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = playlistDriver.addSong(songId, songName);
		
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		
		return response;
	}


	@RequestMapping(value = "/likeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> likeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) throws IOException {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		DbQueryStatus dbQueryStatus = playlistDriver.likeSong(userName, songId);
		
		if(!(dbQueryStatus.getdbQueryExecResult() == (DbQueryExecResult.QUERY_ERROR_GENERIC))) {
			Request request1 = new Request.Builder()
					.url(String.format("http://localhost:3001/updateSongFavouritesCount/%s?shouldDecrement=false", songId))
					.method("PUT", okhttp3.RequestBody.create(null,new byte[0])).build();
			Response execResponse = client.newCall(request1).execute();
		}
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		
		return response;
	}

	@RequestMapping(value = "/unlikeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unlikeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) throws IOException {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		Request request1 = new Request.Builder()
				.url(String.format("http://localhost:3001/updateSongFavouritesCount/%s?shouldDecrement=true", songId))
				.method("PUT", okhttp3.RequestBody.create(null,new byte[0])).build();
		
		Response execResponse = client.newCall(request1).execute();
		DbQueryStatus dbQueryStatus = playlistDriver.unlikeSong(userName, songId);
		
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		
		return response;
	}

	@RequestMapping(value = "/deleteAllSongsFromDb/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> deleteAllSongsFromDb(@PathVariable("songId") String songId,
			HttpServletRequest request) throws IOException {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = playlistDriver.deleteSongFromDb(songId);
		
		response.put("message", dbQueryStatus.getMessage());
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}
}