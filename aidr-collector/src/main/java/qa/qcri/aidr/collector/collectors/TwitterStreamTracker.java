package qa.qcri.aidr.collector.collectors;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import qa.qcri.aidr.collector.beans.CollectionTask;
import qa.qcri.aidr.collector.beans.TwitterCollectionTask;
import qa.qcri.aidr.collector.utils.CollectorConfigurator;
import qa.qcri.aidr.collector.utils.CollectorConfigurationProperty;
import qa.qcri.aidr.common.redis.LoadShedder;
import twitter4j.FilterQuery;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * This class is responsible for managing all the resources associated with the
 * thread which pulls tweets for the given collection task.
 * 
 */
public class TwitterStreamTracker implements Closeable {

	private static Logger logger = Logger.getLogger(TwitterStreamTracker.class.getName());
	
	private static CollectorConfigurator configProperties = CollectorConfigurator.getInstance();
	
	private TwitterStream twitterStream;
	private FilterQuery query;
	private JedisPublisher publisherJedis;

	public TwitterStreamTracker(TwitterCollectionTask task) throws ParseException{

		logger.info("Waiting to aquire Jedis connection for collection " + task.getCollectionCode());
		this.query = task2query(task);
		Configuration config = task2configuration(task);
		this.publisherJedis = JedisPublisher.newInstance();
		logger.info("Jedis connection acquired for collection " + task.getCollectionCode());

		String channelName = configProperties.getProperty(CollectorConfigurationProperty.COLLECTOR_CHANNEL) + "." + task.getCollectionCode();
		LoadShedder shedder = new LoadShedder(
				Integer.parseInt(configProperties.getProperty(CollectorConfigurationProperty.PERSISTER_LOAD_LIMIT)),
				Integer.parseInt(configProperties.getProperty(CollectorConfigurationProperty.PERSISTER_LOAD_CHECK_INTERVAL_MINUTES)), 
				true,channelName);
		TwitterStatusListener listener = new TwitterStatusListener(task, channelName);
		listener.addFilter(new ShedderFilter(channelName, shedder));
		if ("strict".equals(task.getGeoR())) {
			listener.addFilter(new StrictLocationFilter(task));
			logger.info("Added StrictLocationFilter for collection = " + task.getCollectionCode() + ", BBox: " + task.getGeoLocation());
		}
		
		// Added by koushik
		if (task.isToFollowAvailable()) {
			listener.addFilter(new FollowFilter(task));
			logger.info("Added FollowFilter for collection = " + task.getCollectionCode() + ", toFollow: " + task.getToFollow());
		}
		
		if (task.isToTrackAvailable() && (task.isGeoLocationAvailable() || task.isToFollowAvailable())) {
			// New default behavior: filter tweets received from geolocation and/or followed users using tracked keywords
			// Note: this override the default and old behavior of ORing the filter conditions by Twitter
			listener.addFilter(new TrackFilter(task));
			logger.info("Added TrackFilter for collection = " + task.getCollectionCode() + ", toTrack: " + task.getToTrack());
		}
		listener.addPublisher(publisherJedis);
		long threhold = Long.parseLong(configProperties.getProperty(CollectorConfigurationProperty.COLLECTOR_REDIS_COUNTER_UPDATE_THRESHOLD));
		String cacheKey = task.getCollectionCode();
		listener.addPublisher(new StatusPublisher(cacheKey, threhold));

		twitterStream = new TwitterStreamFactory(config).getInstance();
		twitterStream.addListener(listener);
		twitterStream.addConnectionLifeCycleListener(listener);
	}
	
	/**
	 * This method internally creates a thread which manipulates TwitterStream
	 * and calls these adequate listener methods continuously
	 */
	public void start() {
		twitterStream.filter(query);
	}

	public void close() throws IOException {
		twitterStream.cleanUp();
		twitterStream.shutdown();
		publisherJedis.close();
		logger.info("AIDR-Fetcher: Collection stopped which was tracking " + query);
	}
	
	private static Configuration task2configuration(CollectionTask task) {
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
		configurationBuilder.setDebugEnabled(false)
		        .setJSONStoreEnabled(true)
		        .setOAuthConsumerKey(configProperties.getProperty(CollectorConfigurationProperty.TWITTER_CONSUMER_KEY))
		        .setOAuthConsumerSecret(configProperties.getProperty(CollectorConfigurationProperty.TWITTER_CONSUMER_SECRET))
		        .setOAuthAccessToken(task.getAccessToken())
		        .setOAuthAccessTokenSecret(task.getAccessTokenSecret());
		Configuration configuration = configurationBuilder.build();
		return configuration;
	}
	
	/*default*/ static FilterQuery task2query(TwitterCollectionTask collectionTask) throws NumberFormatException {
		FilterQuery query;
		query = new FilterQuery();
		String toTrack = collectionTask.getToTrack();
		if (toTrack != null && !toTrack.isEmpty())
			query.track(toTrack.split(","));

		String toFollow = collectionTask.getToFollow();
		if (toFollow != null && !toFollow.isEmpty()) {
			List<String> list = Arrays.asList(toFollow.split(","));
			// TODO: Java 8 update. Replace the following block with one single line
			// query.follow(list.stream().mapToLong(Long::parseLong).toArray());
			long[] tmp = new long[list.size()];
			for (int i=0; i<list.size(); ++i) {
				long val = Long.parseLong(list.get(i));
				tmp[i] = val;
				//System.out.println("Will follow twitter user ID: " + tmp[i]);
			}
			query.follow(tmp);
			// End of Java 8 Update
		}

		String locations = collectionTask.getGeoLocation();
		if (locations != null && !locations.isEmpty()) {
			List<String> list = Arrays.asList(locations.split(","));
			// TODO: Java 8 update. Replace the following block with one single line
			// double[] flat = list.stream().mapToDouble(Double::parseDouble).toArray();
			double[] flat = new double[list.size()];
			for (int i=0; i<list.size(); ++i) {
				double val = Double.parseDouble(list.get(i));
				flat[i] = val;
			}
			// End of Java 8 Update
			assert flat.length % 4 == 0;
			double[][] square = new double[flat.length / 2][2];
			for (int i = 0; i < flat.length; i = i + 2) {
				// Read 2 elements at a time, into each 2-element sub-array
				// of 'locations'
				square[i / 2][0] = flat[i];
				square[i / 2][1] = flat[i + 1];
			}
			query.locations(square);
		}

		String language = collectionTask.getLanguageFilter();
		if (language != null && !language.isEmpty())
			query.language(language.split(","));
		return query;
	}

}
