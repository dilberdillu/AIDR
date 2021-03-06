package qa.qcri.aidr.manager.dto;

import java.io.Serializable;
import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class FetcherRequestDTO implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private String collectionName;
    private String collectionCode;
    private String toTrack;
    private String toFollow;
    private String consumerKey;
    private String consumerSecret;
    private String accessToken;
    private String accessTokenSecret;
    private String geoLocation, geoR;
    private String languageFilter;
    private boolean saveMediaEnabled;
	private Date lastExecutionTime;
	private int fetchInterval;
	private int fetchFrom;
	private String provider;
    
    public FetcherRequestDTO() {}		
    
    public String getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(String geoLocation) {
        this.geoLocation = geoLocation;
    }

    public String getGeoR() {
		return geoR;
	}

	public void setGeoR(String geoR) {
		this.geoR = geoR;
	}

	public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getCollectionCode() {
        return collectionCode;
    }

    public void setCollectionCode(String collectionCode) {
        this.collectionCode = collectionCode;
    }

    public String getToTrack() {
        return toTrack;
    }

    public void setToTrack(String toTrack) {
        this.toTrack = toTrack;
    }

    public String getToFollow() {
        return toFollow;
    }

    public void setToFollow(String toFollow) {
        this.toFollow = toFollow;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessTokenSecret() {
        return accessTokenSecret;
    }

    public void setAccessTokenSecret(String accessTokenSecret) {
        this.accessTokenSecret = accessTokenSecret;
    }

    public String getLanguageFilter() {
        return languageFilter;
    }

    public void setLanguageFilter(String languageFilter) {
        this.languageFilter = languageFilter;
    }

	public boolean isSaveMediaEnabled() {
		return saveMediaEnabled;
	}

	public void setSaveMediaEnabled(boolean saveMediaEnabled) {
		this.saveMediaEnabled = saveMediaEnabled;
	}

	public Date getLastExecutionTime() {
		return lastExecutionTime;
	}

	public void setLastExecutionTime(Date lastExecutionTime) {
		this.lastExecutionTime = lastExecutionTime;
	}

	public int getFetchInterval() {
		return fetchInterval;
	}

	public void setFetchInterval(int fetchInterval) {
		this.fetchInterval = fetchInterval;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public int getFetchFrom() {
		return fetchFrom;
	}

	public void setFetchFrom(int fetchFrom) {
		this.fetchFrom = fetchFrom;
	}
}
