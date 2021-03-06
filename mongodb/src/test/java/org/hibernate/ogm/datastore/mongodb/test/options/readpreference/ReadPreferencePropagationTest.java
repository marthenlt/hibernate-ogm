/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.mongodb.test.options.readpreference;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.mongodb.utils.MockMongoClientBuilder.mockClient;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.DocumentStoreProperties;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.utils.MockMongoClientBuilder.MockMongoClient;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.After;
import org.junit.Test;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;

/**
 * Tests that the configured read preference is applied when performing operations against MongoDB.
 *
 * @author Gunnar Morling
 */
public class ReadPreferencePropagationTest {

	private OgmSessionFactory sessions;

	@After
	public void closeSessionFactory() {
		sessions.close();
	}

	@Test
	public void shouldApplyConfiguredReadPreferenceForGettingTuple() {
		// given an empty database
		MockMongoClient mockClient = mockClient().build();
		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ) );

		final Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when getting a golf player
		session.get( GolfPlayer.class, 1L );

		transaction.commit();
		session.close();

		// then expect a findOne() call with the configured read preference
		verify( mockClient.getCollection( "GolfPlayer" ) ).findOne( any( DBObject.class ), any( DBObject.class ), eq( ReadPreference.secondaryPreferred() ) );
	}

	@Test
	public void shouldApplyConfiguredReadPreferenceForGettingEmbeddedAssociation() {
		// given a persisted player with one associated golf course
		BasicDBObject player = getPlayer();
		player.put( "playedCourses", getPlayedCoursesAssociationEmbedded() );

		MockMongoClient mockClient = mockClient()
				.insert( "GolfPlayer", player )
				.insert( "GolfCourse", getGolfCourse() )
				.build();

		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ) );

		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when getting the golf player
		GolfPlayer ben = (GolfPlayer) session.get( GolfPlayer.class, 1L );
		List<GolfCourse> playedCourses = ben.getPlayedCourses();
		assertThat( playedCourses ).onProperty( "id" ).containsExactly( 1L );

		transaction.commit();
		session.close();

		// then expect a findOne() call for the entity and one for the association  with the configured read preferences
		// TODO Ideally only one fetch would be required, see OGM-469
		verify( mockClient.getCollection( "GolfPlayer" ) ).findOne( any( DBObject.class ), any( DBObject.class ), eq( ReadPreference.secondaryPreferred() ) );
		verify( mockClient.getCollection( "GolfPlayer" ) ).findOne( any( DBObject.class ), any( DBObject.class ), eq( ReadPreference.primaryPreferred() ) );
	}

	@Test
	public void shouldApplyConfiguredReadPreferenceForGettingAssociationStoredAsAssociation() {
		// given a persisted player with one associated golf course
		MockMongoClient mockClient = mockClient()
				.insert( "GolfPlayer", getPlayer() )
				.insert( "GolfCourse", getGolfCourse() )
				.insert( "Associations", getPlayedCoursesAssociationAsDocument() )
				.build();

		setupSessionFactory( new MongoDBDatastoreProvider( mockClient.getClient() ), AssociationStorageType.ASSOCIATION_DOCUMENT );

		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		// when getting the golf player
		GolfPlayer ben = (GolfPlayer) session.get( GolfPlayer.class, 1L );
		List<GolfCourse> playedCourses = ben.getPlayedCourses();
		assertThat( playedCourses ).onProperty( "id" ).containsExactly( 1L );

		transaction.commit();
		session.close();

		// then expect a findOne() call for the entity and one for the association  with the configured read preference
		verify( mockClient.getCollection( "GolfPlayer" ) ).findOne( any( DBObject.class ), any( DBObject.class ), eq( ReadPreference.secondaryPreferred() ) );
		verify( mockClient.getCollection( "Associations" ) ).findOne( any( DBObject.class ), any( DBObject.class ), eq( ReadPreference.primaryPreferred() ) );
		verifyNoMoreInteractions( mockClient.getCollection( "GolfPlayer" ) );
	}

	private Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { GolfPlayer.class, GolfCourse.class };
	}

	private void setupSessionFactory(MongoDBDatastoreProvider provider) {
		setupSessionFactory( provider, null );
	}

	private void setupSessionFactory(MongoDBDatastoreProvider provider, AssociationStorageType associationStorage) {
		OgmConfiguration configuration = TestHelper.getDefaultTestConfiguration( getAnnotatedClasses() );

		configuration.getProperties().put( OgmProperties.DATASTORE_PROVIDER, provider );

		if ( associationStorage != null ) {
			configuration.getProperties().put( DocumentStoreProperties.ASSOCIATIONS_STORE, associationStorage );
		}

		sessions = configuration.buildSessionFactory();
	}

	private BasicDBObject getGolfCourse() {
		BasicDBObject bepplePeach = new BasicDBObject();
		bepplePeach.put( "_id", 1L );
		bepplePeach.put( "name", "Bepple Peach" );
		return bepplePeach;
	}

	private BasicDBObject getPlayer() {
		BasicDBObject golfPlayer = new BasicDBObject();
		golfPlayer.put( "_id", 1L );
		golfPlayer.put( "name", "Ben" );
		golfPlayer.put( "handicap", 0.1 );
		return golfPlayer;
	}

	private BasicDBList getPlayedCoursesAssociationEmbedded() {
		BasicDBObject bepplePeachRef = new BasicDBObject();
		bepplePeachRef.put( "playedCourses_id", 1L );

		BasicDBList playedCourses = new BasicDBList();
		playedCourses.add( bepplePeachRef );

		return playedCourses;
	}

	private BasicDBObject getPlayedCoursesAssociationAsDocument() {
		BasicDBObject id = new BasicDBObject();
		id.put( "golfPlayer_id", 1L );
		id.put( "table", "GolfPlayer_GolfCourse" );

		BasicDBObject row = new BasicDBObject();
		row.put( "playedCourses_id", 1L );

		BasicDBList rows = new BasicDBList();
		rows.add( row );

		BasicDBObject association = new BasicDBObject();
		association.put( "_id", id );
		association.put( "rows", rows );
		return association;
	}
}
