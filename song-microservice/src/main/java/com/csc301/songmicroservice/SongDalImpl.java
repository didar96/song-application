package com.csc301.songmicroservice;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.mongodb.BasicDBObject;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		String songName = songToAdd.getSongName();
		String songArtistFullName = songToAdd.getSongArtistFullName();
		String songAlbum = songToAdd.getSongAlbum();
		long songAmountFavourites = songToAdd.getSongAmountFavourites();
		
		DbQueryStatus dbQuery = null;
		if (songName == null || songName == "" || songArtistFullName == "" || songArtistFullName == null || songAlbum == null || songAlbum == "") {
			dbQuery = new DbQueryStatus("Please fill in the missing fields.", DbQueryExecResult.QUERY_ERROR_GENERIC);
		} else { 
			JSONObject document = new JSONObject(); 
			document.put("songName", songName);
			document.put("songArtistFullName", songArtistFullName);
			document.put("songAlbum", songAlbum);
			document.put("songAmountFavourites", songAmountFavourites);
			
			Document doc = Document.parse(document.toString());
			
			db.getCollection("songs").insertOne(doc);
			
			String id = doc.get("_id").toString();
			songToAdd.setId(new ObjectId(id));
	        doc.replace("_id", id);
	        
			dbQuery = new DbQueryStatus("Song added to database.", DbQueryExecResult.QUERY_OK);
			dbQuery.setData(doc);
		}
		
		return dbQuery;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		
		BasicDBObject query = new BasicDBObject();
        query.put("_id", new ObjectId(songId));
        
        DbQueryStatus dbQuery = null;
        if(songId == null) {
        	dbQuery = new DbQueryStatus("Please insert an appropriate songId", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        } else {
        	Document doc = db.getCollection("songs").find(query).first();
        	doc.replace("_id", songId);
	        dbQuery = new DbQueryStatus("Song found from database with given id.", DbQueryExecResult.QUERY_OK);
			dbQuery.setData(doc);
        }
		return dbQuery;
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		BasicDBObject query = new BasicDBObject();
        query.put("_id", new ObjectId(songId));
        
        DbQueryStatus dbQuery = null;
        if(songId == null) { 
        	dbQuery = new DbQueryStatus("Please insert an appropriate songId", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        } else {
	        Document doc = db.getCollection("songs").find(query).first();
	        String title = doc.get("songName").toString();
	       
	        dbQuery = new DbQueryStatus("Song title found from database with given id.", DbQueryExecResult.QUERY_OK);
			dbQuery.setData(title);
        }
		
		return dbQuery;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		BasicDBObject query = new BasicDBObject();
        query.put("_id", new ObjectId(songId));

        DbQueryStatus dbQuery = null;
        if(songId == null) { 
        	dbQuery = new DbQueryStatus("Please insert an appropriate songId", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        } else {
	        Document doc = db.getCollection("songs").find(query).first();
	        db.getCollection("songs").deleteOne(doc);
	        
	        dbQuery = new DbQueryStatus("Song deleted from given id.", DbQueryExecResult.QUERY_OK);
        }
        return dbQuery;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		
		BasicDBObject query = new BasicDBObject();
        query.put("_id", new ObjectId(songId));
        
        Document doc = db.getCollection("songs").find(query).first();
        
        Integer songFav = (Integer) doc.get("songAmountFavourites");
        if (shouldDecrement == true) {
        	if (songFav != 0) {
        		songFav -= 1;
        	}
        } else {
        	songFav += 1;
        }
        
        Bson updatedValue = new Document("songAmountFavourites", songFav);
        Bson updatedOp = new Document("$set", updatedValue);
        db.getCollection("songs").updateOne(doc, updatedOp);
        
        DbQueryStatus dbQuery = new DbQueryStatus("Song favourites updated.", DbQueryExecResult.QUERY_OK);
        return dbQuery;
	}
}