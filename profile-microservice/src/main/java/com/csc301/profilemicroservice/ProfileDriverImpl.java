package com.csc301.profilemicroservice;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;

import com.csc301.profilemicroservice.DbQueryExecResult;
import com.csc301.profilemicroservice.DbQueryStatus;

import org.neo4j.driver.v1.Transaction;
import static org.neo4j.driver.v1.Values.parameters;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
		}
	}
	
	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
		
		Session session = driver.session();
        
		DbQueryStatus dbQuery = null;
		if (userName == null || userName == "" || fullName == "" || fullName == null 
				|| password == "" || password == null) {
			dbQuery = new DbQueryStatus("Please fill in the missing fields", DbQueryExecResult.QUERY_ERROR_GENERIC);
		} else { 
			session.run("CREATE (n:profile {userName: $userName, fullName: $fullName, password: $password})", 
					parameters("userName", userName, "fullName", fullName, "password", password));
	        session.run("CREATE (m:playlist {plName: $plName})", parameters("plName", userName + "-favourites"));
	        session.run("MATCH (n:profile), (m:playlist) "
	        		+ "WHERE n.userName = $userName "
	        		+ "AND m.plName = $plName CREATE (n)-[r:created]->(m)", 
	        		parameters("userName", userName, "plName", userName + "-favourites"));
	        
	        dbQuery = new DbQueryStatus("Profile added to database.", DbQueryExecResult.QUERY_OK);
		}
		return dbQuery;
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		
		Session session = driver.session();
		DbQueryStatus dbQuery = null;
		
		if(userName == null || userName == "" || frndUserName == "" || frndUserName == null) {
			dbQuery = new DbQueryStatus("Please fill in the missing fields", DbQueryExecResult.QUERY_ERROR_GENERIC);
		} else {
			StatementResult exists = session.run("RETURN EXISTS( (:profile {userName: $userName})-[:follows]->(:profile {userName: $frndUserName}) )",
					parameters("userName", userName, "frndUserName", frndUserName));
			if(exists.single().get(0).asBoolean()) { 
				dbQuery = new DbQueryStatus(userName + " already follows " + frndUserName, DbQueryExecResult.QUERY_ERROR_GENERIC);
			} else {
				session.run("MATCH (n:profile), (m: profile) WHERE n.userName = $userName AND "
						+ "m.userName = $frndUserName CREATE (n)-[r:follows]->(m)", 
						parameters("userName", userName, "frndUserName", frndUserName));
				 dbQuery = new DbQueryStatus(userName + " successfully followed " + frndUserName, DbQueryExecResult.QUERY_OK);
			}
		}
		return dbQuery;
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		
		Session session = driver.session();
		DbQueryStatus dbQuery = null;
		
		if(userName == null || userName == "" || frndUserName == "" || frndUserName == null) {
			dbQuery = new DbQueryStatus("Please fill in the missing fields", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		
		StatementResult exists = session.run("RETURN EXISTS( (:profile {userName: $userName})-[:follows]->(:profile {userName: $frndUserName}) )",
				parameters("userName", userName, "frndUserName", frndUserName));
		if(!exists.single().get(0).asBoolean()) { 
			dbQuery = new DbQueryStatus(userName + " doesn't already follow " + frndUserName, DbQueryExecResult.QUERY_ERROR_GENERIC);
		} else {
			session.run("MATCH(n:profile), (m:profile) WHERE n.userName = $userName AND "
					+ "m.userName = $frndUserName MATCH(n)-[r:follows]->(m) DELETE r", 
					parameters("userName", userName, "frndUserName", frndUserName));
			dbQuery = new DbQueryStatus(userName + " successfully unfollowed " + frndUserName, DbQueryExecResult.QUERY_OK);
		}
		return dbQuery;
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {

		Session session = driver.session();
		DbQueryStatus dbQuery = null;
		StatementResult result = session.run("MATCH (a:profile {userName: $userName})-[:follows]->(profile) RETURN profile.userName", 
				parameters ("userName", userName));
		
		HashMap<String, List<String>> data = new HashMap<String, List<String>>();
		
		while(result.hasNext()) {
            Record record = result.next();
            String currUsername = record.get(0).asString();
            StatementResult result_playlist = session.run("MATCH (a:profile {userName: $userName})-[:created]->(playlist) RETURN playlist.plName", 
            						parameters ("userName", currUsername));
            String playlist = result_playlist.single().get(0).asString();
            StatementResult result_songs = session.run("MATCH (a:playlist {plName: $plName})-[:includes]->(song) RETURN song.songName", 
					parameters ("plName", playlist));
            List<String> songsList = new ArrayList<String>();
            while(result_songs.hasNext()) { 
            	Record songs_record = result_songs.next();
            	songsList.add(songs_record.get(0).asString());
            }
            data.put(currUsername, songsList);
        }
		if(data.isEmpty()) {
			dbQuery = new DbQueryStatus("Username not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		} else {
			dbQuery = new DbQueryStatus("Succesfully retrieved song list of following", DbQueryExecResult.QUERY_OK);
			dbQuery.setData(data);
		}
		
		return dbQuery;
	}
}
