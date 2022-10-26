package api;
import com.google.api.server.spi.config.*;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.ThreadManager;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.users.User;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import dto.PostMessage;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;


@Api(name = "tinygram",
     version = "v1",
     audiences = "477502282441-pqpq69gi50k2tccp2ps7kqs5fjqdc1t9.apps.googleusercontent.com",
  	 clientIds = {"477502282441-pqpq69gi50k2tccp2ps7kqs5fjqdc1t9.apps.googleusercontent.com"},
     namespace =
     @ApiNamespace(
		   ownerDomain = "tinygram-365009.ew.r.appspot.com",
		   ownerName = "tinygram-365009.ew.r.appspot.com")
     )
public class TinygramEndpoint {

	private static final UnauthorizedException INVALID_CREDENTIALS = new UnauthorizedException("Invalid credentials");
	private static final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    private static final Random rng = new Random();

    private static final int FOLLOWER_SHARD_NUMBER = 2;

	@ApiMethod(name = "GetPost", httpMethod = HttpMethod.GET)
	public CollectionResponse<Entity> GetPost(@Nullable @Named("next") String cursorString) throws UnauthorizedException {

        Query query = new Query("Post").addSort("date", SortDirection.DESCENDING);


        PreparedQuery preparedQuery = datastore.prepare(query);

        FetchOptions fetchOptions = FetchOptions.Builder.withLimit(1);

        if (cursorString != null) {
        fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
        }

        QueryResultList<Entity> results = preparedQuery.asQueryResultList(fetchOptions);
        cursorString = results.getCursor().toWebSafeString();

        return CollectionResponse.<Entity>builder().setItems(results).setNextPageToken(cursorString).build();
	}

    @ApiMethod(name = "register", httpMethod = HttpMethod.POST)
    public Boolean register(User user) throws UnauthorizedException{
        //Create user entity so that other user can start following
        if (user == null) {
			throw INVALID_CREDENTIALS;
		}

        Key key = KeyFactory.createKey("User", user.getEmail()+":"+"user");


        try {
            datastore.get(key);
            return false;
        } catch (EntityNotFoundException e) {
            createUserEntity(user);
            return true;
        }

    }

    private static void createUserEntity(User user){
         //Hotspot can occur if users have close email
         Entity userEntity = new Entity("User",user.getEmail()+":"+"user");

         userEntity.setProperty("owner", user.getEmail());
         userEntity.setProperty("followerShardAmount", FOLLOWER_SHARD_NUMBER);
 
         Transaction txn = datastore.beginTransaction();
 
         try {
   
             datastore.put(userEntity);
             txn.commit();
         }catch(Exception e){
             e.printStackTrace();
         }finally {
             if(txn.isActive()){
                 txn.rollback();
             }
         }
 
         Transaction transaction = datastore.beginTransaction();
         try {
             for (int i = 0; i < FOLLOWER_SHARD_NUMBER; i++) { 
                 Entity shard = new Entity("followerShard",user.getEmail()+":"+"shard_"+ i);
                 HashSet<String> followerAccount = new HashSet<String>();
                 followerAccount.add("");
                 shard.setProperty("shardedFollowerList", followerAccount);
                 datastore.put(shard);
             }
             transaction.commit();
         }catch(Exception e){
             e.printStackTrace();
         }finally {
             if(transaction.isActive()){
                 transaction.rollback();
             }
         }
 
    }

    @ApiMethod(name = "publishPost", httpMethod = HttpMethod.POST)
	public Entity publishPost(User user, PostMessage post) throws UnauthorizedException {
        if (user == null) {
			throw INVALID_CREDENTIALS;
		}
        if(post.body == null && post.pictureUrl == null){
            throw new InvalidParameterException("post body and picture are null");
        }

		Entity postEntity = new Entity("Post",Long.MAX_VALUE-(new Date()).getTime()+":"+user.getEmail());
		postEntity.setProperty("url", post.pictureUrl);
		postEntity.setProperty("body", post.body);
        postEntity.setProperty("owner", user.getEmail());
		postEntity.setProperty("likec", 0);
		postEntity.setProperty("date", new Date());


        HashSet<String> likeaccounts = new HashSet<String>();
        
        //IDK why this is needed but if i don't write it then likeaccounts is considered null
        likeaccounts.add("");
        
        postEntity.setProperty("likeaccounts", likeaccounts);

		System.out.println("Yeah:"+ post);

		Transaction txn = datastore.beginTransaction();
		datastore.put(postEntity);
		txn.commit();

		return postEntity;
	}

    @ApiMethod(name = "follow", httpMethod = HttpMethod.POST)
	public void follow(User user, @Named("userToFollow") String userToFollowEmail ) throws UnauthorizedException {
        if (user == null) {
			throw INVALID_CREDENTIALS;
		}

        System.out.println("you made it : " + userToFollowEmail);

        try{

            Key userKey = KeyFactory.createKey("User", userToFollowEmail+":"+"user");
            Entity userEntity = datastore.get(userKey);

            //Can return way to many key
            Query query = new Query("followerShard")
            .setFilter(new FilterPredicate("shardedFollowerList", FilterOperator.EQUAL, user.getEmail()))
            .setKeysOnly();
            
            PreparedQuery preparedQuery = datastore.prepare(query);

            FetchOptions fetchOptions = FetchOptions.Builder.withLimit(2);
            
            QueryResultList<Entity> results = preparedQuery.asQueryResultList(fetchOptions);

            if(results == null){
                addFollower(user, userToFollowEmail, (int) userEntity.getProperty("followerShardAmount"));
            }
            

        }catch(EntityNotFoundException e){
            System.err.println("User not found, check key");
            e.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
	}

    /**
     * SLOW AF
     * @param user
     * @param userToFollowKey
     */
    private static void addFollower(User user, String userToFollowBaseKey, int numberOfShard ){
        Transaction transaction = datastore.beginTransaction();

        try {

            Key shard_key = KeyFactory.createKey("followerShard", userToFollowBaseKey+":"+"shard_"+ rng.nextInt(numberOfShard));
            Entity shardEntity = datastore.get(shard_key);

            HashSet<String> follower = (HashSet<String>) shardEntity.getProperty("shardedFollowerList");

            follower.add(user.getEmail());

            //HACK: possible bug since set property was not called
            datastore.put(shardEntity);
            transaction.commit();

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(transaction.isActive()){
                transaction.rollback();
            }
        }
    }

    @ApiMethod(name = "likePost", httpMethod = HttpMethod.POST)
	public Object likePost(User user, @Named("postid") String postid ) throws UnauthorizedException {
        if (user == null) {
			throw INVALID_CREDENTIALS;
		}

        Long likes = null;

        System.out.println("you made it : " + postid);

        Transaction transaction = datastore.beginTransaction();

        try{

            Key postKey = KeyFactory.createKey("Post", postid);
            Entity post = datastore.get(postKey);


            HashSet<String> usersWhoLiked = (HashSet<String>) post.getProperty("likeaccounts");

            Long currentLikesCount = (Long) post.getProperty("likec");

            likes = currentLikesCount;
    
            if(!usersWhoLiked.contains(user.toString())){
                
                post.setProperty("likec", currentLikesCount+1);
                likes = likes + 1;
                usersWhoLiked.add(user.toString());

            }

            datastore.put(post);
            transaction.commit();

        }catch(Exception e){
            e.printStackTrace();
        }finally {
            if(transaction.isActive()){
                transaction.rollback();
            }
        }
        
        return likes;
	}

}