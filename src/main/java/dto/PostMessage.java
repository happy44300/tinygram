package dto;


public class PostMessage{
    public String owner;
	public String body;
	public String url;

	@Override
	public String toString() {
		return  "Owner: "+ owner + "\n"+
				"body: " + body  + "\n"+
				"url: "  + url;
	}
}