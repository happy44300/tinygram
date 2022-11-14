package api;
import com.google.api.server.spi.config.*;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.users.User;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import dto.PostMessage;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


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
    private static final int LIKER_SHARD_NUMBER = 2;

    @ApiMethod(name = "retrievePostsFromFollowedSenders", httpMethod = HttpMethod.GET)
    public CollectionResponse<Entity> retrievePostsFromFollowedSenders(User user, @Nullable @Named("next") String cursorString) throws UnauthorizedException {

        if (user == null) {
			throw INVALID_CREDENTIALS;
		}

        System.out.println("next: " + cursorString);

        Query query = new Query("ReceiverShard")
            .setFilter(new FilterPredicate("receiversList", FilterOperator.EQUAL, user.getEmail()))
            .setKeysOnly();
        
        PreparedQuery preparedQuery = datastore.prepare(query);
        FetchOptions fetchOptions = FetchOptions.Builder.withLimit(1); // for the cursor demo, we set the limit to 1

        if (cursorString != null) {
            fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
        }


        QueryResultList<Entity> results = preparedQuery.asQueryResultList(fetchOptions);

        System.out.println("key " + results.toString());

        List<Key> postKeys = results.stream()
            .map(Entity::getParent)
            .collect(Collectors.toList());

        cursorString = results.getCursor().toWebSafeString();
        
        Collection<Entity> posts = datastore.get(postKeys).values();
        System.out.println("Post " + posts.toString());

        return CollectionResponse.<Entity>builder()
            .setItems(posts)
            .setNextPageToken(cursorString).build();
    }

	@ApiMethod(name = "retrievePost", httpMethod = HttpMethod.GET)
	public CollectionResponse<Entity> retrievePost(@Nullable @Named("next") String cursorString){

        Query query = new Query("Post").addSort("date", SortDirection.DESCENDING);


        PreparedQuery preparedQuery = datastore.prepare(query);

        FetchOptions fetchOptions = FetchOptions.Builder.withLimit(1);

        if (cursorString != null) {
            fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
        }

        QueryResultList<Entity> results = preparedQuery.asQueryResultList(fetchOptions);
        cursorString = results.getCursor().toWebSafeString();

        List<Object> resultsWithLikes = new ArrayList<>();
        resultsWithLikes.add(results);
        resultsWithLikes.add(8);

        System.out.println(results);


        Long likesCount = Long.valueOf(0);

        for(Entity result: results){
            Long likerShardsAmount = (Long) result.getProperty("likerShardsAmount");

            for(int i = 0; i < likerShardsAmount; i++){
                Key shardKey = KeyFactory.createKey(result.getKey(), "LikerShard", ":shard_"+i);
                try {
                    Entity shard = datastore.get(shardKey);
                    likesCount = likesCount + (Long) shard.getProperty("likes");
                } catch(Exception e){
                    e.printStackTrace();
                    System.out.println("Issue retrieving like counter value from a like shard of a post. Post in question : " + result);
                }
            }

            result.setProperty("likec", likesCount);
        }
        

        return CollectionResponse.<Entity>builder().setItems(results).setNextPageToken(cursorString).build();
	}

    @ApiMethod(name = "register", httpMethod = HttpMethod.POST)
    public Object register(User user) throws UnauthorizedException{
        //Create user entity so that other user can start following
        if (user == null) {
			throw INVALID_CREDENTIALS;
		}

        Key key = KeyFactory.createKey("User", user.getEmail()+":"+"user");


        try {
            datastore.get(key);
            return "User already registered in db";
        } catch (EntityNotFoundException e) {
            createUserEntity(user);
            return "User registered";
        }

    }

    private static void createUserEntity(User user){
         //Hotspot can occur if users have close email
         Entity userEntity = new Entity("User", user.getEmail()+":"+"user");

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
       
      
             for (int i = 0; i < FOLLOWER_SHARD_NUMBER; i++) {

                Transaction transaction = datastore.beginTransaction();;

                try {
                 Key parentKey = KeyFactory.createKey("User", user.getEmail()+":"+"user");
                 Key shardKey = KeyFactory.createKey(parentKey, "followerShard", ":shard_"+ i);
                 Entity shard = new Entity(shardKey);

                 System.out.println(shardKey);

                 HashSet<String> followerAccount = new HashSet<String>();
                 followerAccount.add("");

                 shard.setProperty("shardedFollowerList", followerAccount);
                 datastore.put(shard);
                 transaction.commit();
                }catch(Exception e){
                    e.printStackTrace();
                }finally {
                    if(transaction.isActive()){
                        transaction.rollback();
                    }
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
        Key postKey = KeyFactory.createKey("Post", Long.MAX_VALUE-(new Date()).getTime()+":"+user.getEmail());
		Entity postEntity = new Entity(postKey);
		postEntity.setProperty("url", post.pictureUrl);
		postEntity.setProperty("body", post.body);
        postEntity.setProperty("owner", user.getEmail());
        postEntity.setProperty("likec", 0);
        postEntity.setProperty("likerShardsAmount", LIKER_SHARD_NUMBER);
		postEntity.setProperty("date", new Date());

		System.out.println("Yeah:"+ post);

        Key userKey = KeyFactory.createKey("User", user.getEmail()+":user");
        try {
            Entity userEntity = datastore.get(userKey);

            Long numberOfShards = (Long) userEntity.getProperty("followerShardAmount");

            System.out.println(userEntity);
            System.out.println(userKey);
            System.out.println(KeyFactory.createKey(userKey, "followerShard", ":shard_"+numberOfShards));
            System.out.println(datastore.get(KeyFactory.createKey(userKey, "followerShard", ":shard_"+1)));
            

            Query query = new Query("followerShard")
            .setFilter(CompositeFilterOperator.and(
                new FilterPredicate("__key__", FilterOperator.LESS_THAN_OR_EQUAL, KeyFactory.createKey(userKey, "followerShard", ":shard_"+numberOfShards)),
                new FilterPredicate("__key__", FilterOperator.GREATER_THAN_OR_EQUAL, KeyFactory.createKey(userKey, "followerShard", ":shard_0"))));
        
            PreparedQuery preparedQuery = datastore.prepare(query);
            FetchOptions fetchOptions = FetchOptions.Builder.withLimit(numberOfShards.intValue());
            QueryResultList<Entity> results = preparedQuery.asQueryResultList(fetchOptions);

            List<Entity> shardedReceviersEntities = new ArrayList<>(numberOfShards.intValue());

            for(int i = 0; i < numberOfShards; i++){
                Key receiversShardKey = KeyFactory.createKey(postKey, "ReceiverShard", ":shard_"+i);
                Entity shardedReceiversListEntity = new Entity(receiversShardKey);

                shardedReceiversListEntity.setProperty("receiversList", results.get(i).getProperty("shardedFollowerList"));

                shardedReceviersEntities.add(shardedReceiversListEntity);
            }
            
            List<Entity> shardedLikersEntities = new ArrayList<>(numberOfShards.intValue());

            for(int i = 0; i < LIKER_SHARD_NUMBER; i++){
                Key likersShardKey = KeyFactory.createKey(postKey, "LikerShard", ":shard_"+i);
                Entity shardedLikersListEntity = new Entity(likersShardKey);


                HashSet<String> emptyLikersList = new HashSet<String>();
                emptyLikersList.add("");
                
                shardedLikersListEntity.setProperty("shardedLikerList", emptyLikersList);
                shardedLikersListEntity.setProperty("likes", 0);

                shardedLikersEntities.add(shardedLikersListEntity);
            }

            Transaction txn = datastore.beginTransaction();
            datastore.put(postEntity);
            datastore.put(shardedReceviersEntities);
            datastore.put(shardedLikersEntities);
		    txn.commit();

            return postEntity;

        } catch (EntityNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        

		System.out.println("Uploading post didn't work!");

		return null;
	}

    @ApiMethod(name = "follow", httpMethod = HttpMethod.POST)
	public void follow(User user, @Named("userToFollow") String userToFollowEmail ) throws UnauthorizedException {
        if (user == null) {
			throw INVALID_CREDENTIALS;
		}

        System.out.println("user:"+ user.getEmail() +" followed: " + userToFollowEmail);

        try{
            Key userKey = KeyFactory.createKey("User", userToFollowEmail+":"+"user");
            Entity userEntity = datastore.get(userKey);

            Long numberOfShards = (Long) userEntity.getProperty("followerShardAmount");

            Query query = new Query("followerShard")
            .setFilter(CompositeFilterOperator.and(
                (CompositeFilterOperator.and(
                    //range search
                    new FilterPredicate("__key__", FilterOperator.LESS_THAN_OR_EQUAL, KeyFactory.createKey("followerShard", userToFollowEmail+":shard_"+numberOfShards)),
                    new FilterPredicate("__key__", FilterOperator.GREATER_THAN_OR_EQUAL, KeyFactory.createKey("followerShard", userToFollowEmail+":shard_0")))),
                new FilterPredicate("shardedFollowerList", FilterOperator.EQUAL, user.getEmail())))
            .setKeysOnly();

            PreparedQuery preparedQuery = datastore.prepare(query);

            FetchOptions fetchOptions = FetchOptions.Builder.withLimit(numberOfShards.intValue());
            
            QueryResultList<Entity> results = preparedQuery.asQueryResultList(fetchOptions);

            System.out.println(results);

            if(results.isEmpty()){
                addFollower(user, userToFollowEmail, numberOfShards.intValue());
            }
            

        }catch(EntityNotFoundException e){
            System.err.println("User not found, check key");
            e.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }
	}

    /**
     * Add folower
     * @param user
     * @param userToFollowKey
     */
    private static void addFollower(User user, String userToFollowBaseKey, int numberOfShards ){
        Transaction transaction = datastore.beginTransaction();

        try {

            Key userToFollowKey = KeyFactory.createKey("User", userToFollowBaseKey+":user"); 

            
            Key shard_key = KeyFactory.createKey(userToFollowKey, "followerShard", ":shard_"+ rng.nextInt(numberOfShards));
            Entity shardEntity = datastore.get(shard_key);

            List<String> follower = (ArrayList<String>) shardEntity.getProperty("shardedFollowerList");

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

        System.out.println("user:"+ user.getEmail() +" liked: " + postid);

        Transaction transaction = datastore.beginTransaction();

        try{

            Key postKey = KeyFactory.createKey("Post", postid);
 
            Query query = new Query("LikerShard")
            .setFilter(CompositeFilterOperator.and(
                (CompositeFilterOperator.and(
                    new FilterPredicate("__key__", FilterOperator.LESS_THAN_OR_EQUAL, KeyFactory.createKey(postKey, "LikerShard", ":shard_"+LIKER_SHARD_NUMBER)),
                    new FilterPredicate("__key__", FilterOperator.GREATER_THAN_OR_EQUAL, KeyFactory.createKey(postKey, "LikerShard", ":shard_0")))),
                new FilterPredicate("shardedLikerList", FilterOperator.EQUAL, user.getEmail())))
            .setKeysOnly();

            PreparedQuery preparedQuery = datastore.prepare(query);

            FetchOptions fetchOptions = FetchOptions.Builder.withLimit(LIKER_SHARD_NUMBER);
            
            QueryResultList<Entity> results = preparedQuery.asQueryResultList(fetchOptions);


            if(results.isEmpty()){
                Key shardKey = KeyFactory.createKey(postKey, "LikerShard", ":shard_"+ rng.nextInt(LIKER_SHARD_NUMBER));
                Entity shardEntity = datastore.get(shardKey);

                List<String> likers = (ArrayList<String>) shardEntity.getProperty("shardedLikerList");

                if(likers == null) {//This shard contains an empty list, no one who liked has been registered in this shard yet
                    likers = new ArrayList<String>();
                }

                likes = (Long) shardEntity.getProperty("likes") + 1;
                likers.add(user.getEmail());
                shardEntity.setProperty("likes", likes);
                shardEntity.setProperty("shardedLikerList", likers);

                //HACK: possible bug since set property was not called
                datastore.put(shardEntity);
            }
            
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