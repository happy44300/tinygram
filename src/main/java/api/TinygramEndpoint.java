package api;
import com.google.api.server.spi.config.*;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.ThreadManager;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.users.User;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.Query.SortDirection;
import dto.PostMessage;

import java.security.InvalidParameterException;
import java.util.Date;
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

	@ApiMethod(name = "GetPost", httpMethod = HttpMethod.GET)
	public CollectionResponse<Entity> GetPost(User user,@Nullable @Named("next") String cursorString) throws UnauthorizedException {

        Query query = new Query("Post").addSort("date", SortDirection.DESCENDING);

        if (user != null) {
            query.setFilter(new Query.FilterPredicate("owner", Query.FilterOperator.EQUAL, user.getUserId()));
        }


        PreparedQuery preparedQuery = datastore.prepare(query);

        FetchOptions fetchOptions = FetchOptions.Builder.withLimit(1);

        if (cursorString != null) {
        fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
        }

        QueryResultList<Entity> results = preparedQuery.asQueryResultList(fetchOptions);
        cursorString = results.getCursor().toWebSafeString();

        return CollectionResponse.<Entity>builder().setItems(results).setNextPageToken(cursorString).build();
	}

    @ApiMethod(name = "publishPost", httpMethod = HttpMethod.POST)
	public Entity  publishPost(User user, PostMessage post) throws UnauthorizedException {
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

		System.out.println("Yeah:"+ post);

		Transaction txn = datastore.beginTransaction();
		datastore.put(postEntity);
		txn.commit();

		return postEntity;
	}

    @ApiMethod(name = "follow", httpMethod = HttpMethod.POST)
	public void follow(User user, @Named("userToFollow") String userToFollow ) throws UnauthorizedException {
        if (user == null) {
			throw INVALID_CREDENTIALS;
		}
        //TODO: add body
	}

    @ApiMethod(name = "like", httpMethod = HttpMethod.POST)
	public void like(User user, @Named("postid") String postid ) throws UnauthorizedException {
        if (user == null) {
			throw INVALID_CREDENTIALS;
		}

        Thread th;

        th = ThreadManager.createThreadForCurrentRequest(new Runnable() {
            public void run(){
                Transaction transaction = datastore.beginTransaction();

                try{

                    //STEP 1 : Access the post
                    Key postKey = KeyFactory.stringToKey(postid);
                    Entity post = datastore.get(postKey);

                    //STEP 2 : Increase the likes counter of the post
                    int currentLikesCount = (int) post.getProperty("likec");
                    post.setProperty("likec", currentLikesCount+1);

                    //STEP 3 : Close the transaction
                    datastore.put(post);
                    transaction.commit();

                }catch(Exception e){
                    e.printStackTrace();
                }finally {
                    if(transaction.isActive()){
                        transaction.rollback();
                    }
                }
            }
        });

        th.start();

        try {
            //Unsure about the way to use this
            th.join();
        } catch (Exception e) {
            //TODO: handle exception
        }
        
	}

}