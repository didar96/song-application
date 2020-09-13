package com.csc301.profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus addSong(String songId, String songName) {
		
		Session session = driver.session();
        
		session.run("CREATE (n:song {songName: $songName, songId: $songId})", 
				parameters("songName", songName, "songId", songId));
        
        DbQueryStatus dbQuery = new DbQueryStatus("Song added to database.", DbQueryExecResult.QUERY_OK);
		
		return dbQuery;
	}
	
	@Override
	public DbQueryStatus likeSong(String userName, String songId) {

		Session session = driver.session();
		DbQueryStatus dbQuery = null;
		StatementResult exists = session.run("RETURN EXISTS( (:playlist {plName: $plName})-[:includes]->(:song{songId: $songId}) )",
				parameters("plName", userName+"-favourites", "songId", songId));
		if(exists.single().get(0).asBoolean()) { 
			dbQuery = new DbQueryStatus(userName + " already likes the song#" + songId, DbQueryExecResult.QUERY_ERROR_GENERIC);
		} else {
			session.run("MATCH (n:playlist), (m:song) "
	        		+ "WHERE n.plName = $plName "
	        		+ "AND m.songId = $songId CREATE UNIQUE (n)-[r:includes]->(m)", 
	        		parameters("plName", userName + "-favourites", "songId", songId));
		
			dbQuery = new DbQueryStatus(userName + " succesfully liked song#" + songId, DbQueryExecResult.QUERY_OK);
		}
		return dbQuery;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		
		DbQueryStatus dbQuery = null;
		Session session = driver.session();
		StatementResult exists = session.run("RETURN EXISTS( (:playlist {plName: $plName})-[:includes]->(:song {songId: $songId}) )",
				parameters("plName", userName+"-favourites", "songId", songId));
		if(!exists.single().get(0).asBoolean()) { 
			dbQuery = new DbQueryStatus(userName + " doesn't already like the song#" + songId, DbQueryExecResult.QUERY_ERROR_GENERIC);
		} else {
			session.run("MATCH (n:playlist), (m:song) "
	        		+ "WHERE n.plName = $plName "
	        		+ "AND m.songId = $songId MATCH(n)-[r:includes]->(m) DELETE r", 
	        		parameters("plName", userName + "-favourites", "songId", songId));
			dbQuery = new DbQueryStatus(userName + " succesfully unliked song#" + songId, DbQueryExecResult.QUERY_OK);
		}
		return dbQuery;
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		
		Session session = driver.session();
		
		session.run("MATCH (n:song {songId: $songId})" + 
				"DETACH DELETE n", parameters ("songId", songId));
		
		DbQueryStatus dbQuery = new DbQueryStatus("Song deleted from database.", DbQueryExecResult.QUERY_OK);
		
		return dbQuery;
	}
}
