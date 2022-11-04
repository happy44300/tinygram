function parseJwt (token) {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function (c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));

    return JSON.parse(jsonPayload);
}

const User = {
    credential: "",
    FullName: "",
    GivenName: "",
    FamilyName: "",
    ImageURL: "",
    Email: "",

    isLogged: false,

    set: function(credential, FullName, GivenName, FamilyName, ImageURL, Email){
        User.isLogged = true;
        User.credential = credential;
        User.FullName = FullName;
        User.GivenName = GivenName;
        User.FamilyName = FamilyName;
        User.ImageURL = ImageURL;
        User.Email = Email;

        PromptView.enablePrompt();
    }

};

function handleCredentialResponse(response) {
    const responsePayload = parseJwt(response.credential);

    console.log("ID: " + responsePayload.sub);
    console.log('Full Name: ' + responsePayload.name);
    console.log('Given Name: ' + responsePayload.given_name);
    console.log('Family Name: ' + responsePayload.family_name);
    console.log("Image URL: " + responsePayload.picture);
    console.log("Email: " + responsePayload.email);

    console.log("Identified!");

    User.set(response.credential,
        responsePayload.name,
        responsePayload.given_name,
        responsePayload.family_name,
        responsePayload.family_name,
        responsePayload.picture,
        responsePayload.email);

    //Send ask server to register client
    m.request({
        method: "POST",
        url: "_ah/api/tinygram/v1/register?access_token=" + encodeURIComponent(User.credential),
    }).then(function (res) {
        console.log(res);
    })


}


const PromptView = {
    isPromptDisabled: true,
    view: function(){

        return m('div', {class: 'form my-3 mx-auto border-0', id:"myform", style:"width: 50%"},[

            m("button", {
                "class":"btn btn-secondary",
                "id":"option1",
                "checked":"checked",
                onclick: function (e) {
                    Timeline.clear();
                    Timeline.getDefaultPosts()
                }},"Display All"),

            m("button", {
                "class":"btn btn-secondary",
                "id":"option2",
                onclick: function (e) {
                    Timeline.clear();
                    Timeline.loadFollowedPosts();
                }}, "Display followed"),

            m("div",{class:"form-group"},[
                m("label",{for:"postUrl"},"Image Url"),
                m("input",{
                    class:"form-control", id:"postUrl", type:"url", placeholder:"Enter image url",
                    value: Prompt.imageUrl,
                    disabled: PromptView.isPromptDisabled,
                    oninput: function (e) {
                        Prompt.imageUrl=e.target.value
                    }
                }),
                m("label",{for:"postForm"},"Post Title"),
                m("textarea[placeholder=Write a new post]", {
                    value: Prompt.title,
                    disabled: PromptView.isPromptDisabled,
                    class:"form-control",
                    rows: 3,
                    oninput: function (e) {
                        Prompt.title=e.target.value
                    }
                }),
            ]),
            m('button', {
                type:"submit",
                class:"btn btn-block btn-primary mx-auto ",
                style:"text-center",
                onclick: function(e){
                    PostController.uploadPost(Prompt.title, Prompt.imageUrl);

                    //TODO: Empty prompt once post is uploaded
                }
            },"Post")
        ])
    },
    enablePrompt: function(){
        PromptView.isPromptDisabled = false;
        m.redraw();
    },
    disablePrompt: function(){
        PromptView.isPromptDisabled = true;
        m.redraw();
    }
};



const PostController = {
    uploadPost: function (body, pictureUrl) {

        let post = {
            'owner': "",
            'body': body,
            'pictureUrl': pictureUrl
        }

        return m.request({
            method: "POST",
            url: "_ah/api/tinygram/v1/publishPost?access_token=" + encodeURIComponent(User.credential),
            params: post
        })
            .then(function (result) {
                console.log("Uploaded Post!");
                console.log(result);

                //Updates the current timeline to show the post just uploaded
                Timeline.addPost(result.properties, result.key.name);
            })
    },

    followPost : function (post){

    }

};


const SettingsBarView = {
    view: function () {
        return m('nav', {class: 'navbar sticky-top navbar-light bg-light'}, [
            m('div', {class: 'container-fluid'}, [
                m('a', {
                    href: '#',
                    class: "navbar-brand"
                }, m("img", {
                    src:"static/logo.svg",
                    width:"230",height:"40"}
                )),
                m("div", {class:"navbar-brand"},[
                    m('div', {
                        id: 'g_id_onload',
                        'data-client_id': '477502282441-pqpq69gi50k2tccp2ps7kqs5fjqdc1t9.apps.googleusercontent.com',
                        'data-callback': 'handleCredentialResponse',
                    }),
                    m('div', {
                        class: 'g_id_signin',
                        'data-type': 'standard'
                    })
                ])
            ]),
        ])
    }
};

const Timeline = {
    posts: [],
    followedPosts: [],
    next: "",
    addPost: function (post, postid) {
        this.posts.push(
            m("div", {class: "card my-3 mx-auto", id:"post", style: "width: 50%;"}, [
                m("img", {class: "card-static_dir-top", src: post.url}),
                m("div", {class: "card-body"},
                    m("p", {class: "card-text"}, post.body),
                    m("button", {href: "", type:"button", class: "btn btn-danger w-100", onclick: function(e){

                            if(!User.isLogged){return;}
                            m.request({
                                method: "POST",
                                url: "_ah/api/tinygram/v1/likePost/" + encodeURIComponent(postid) + "?access_token=" + encodeURIComponent(User.credential)
                            })
                                .then(function(result){
                                    console.log("number of likes" + result)
                                    document.getElementById(postid).innerHTML = result;
                                });
                        }}, "Like"),
                    m("button", {href: "", type:"button", class: "btn btn-primary", onclick: function(e){
                            if(!User.isLogged){return;}
                            console.log("bruh");
                            m.request({
                                method: "POST",
                                url: "_ah/api/tinygram/v1/follow/" + encodeURIComponent(post.owner) + "?access_token=" + encodeURIComponent(User.credential),
                            })
                        }}, "Follow post author"),
                    m("div",{class: "", id:postid},`Like: ${post.likec}`))
            ])
        );
    },
    getDefaultPosts: function () {

        m.request({
            method:"GET",
            url:"_ah/api/tinygram/v1/GetPost",
            params:{"next": Timeline.next}

        }).then(function(result) {
            console.log(result)

            if(result == null){
                return;
            }

            if(result.hasOwnProperty("items")){
                for(let item of result.items){
                    console.log("item from get! :" + item);
                    console.log("item from get, to string()! :" + JSON.stringify(item));
                    let entity = item.properties;

                    console.log("entity from get! :" + entity);
                    console.log("entity from get, to string()! :" + JSON.stringify(entity));

                    console.log("url?" + entity.url);
                    Timeline.addPost(entity, item.key.name);
                }
            }
            Timeline.next=result.nextPageToken;
        })
    },

    clear: function () {
        Timeline.posts = [];
        Timeline.next = "";
        m.redraw()
    },

    loadFollowedPosts: function () {
        console.log("loading followed posts...");
        m.request({
            method:"GET",
            url:"_ah/api/tinygram/v1/getPostFromFollowedSenders" + "?access_token=" + encodeURIComponent(User.credential),
            params:{"next": encodeURIComponent(Timeline.next)}

        }).then(function(result) {
            console.log(result)

            if(result == null){
                return;
            }

            if(result.hasOwnProperty("items")){
                for(let item of result.items){
                    console.log("item from get! :" + item);
                    console.log("item from get, to string()! :" + JSON.stringify(item));
                    let entity = item.properties;

                    console.log("entity from get! :" + entity);
                    console.log("entity from get, to string()! :" + JSON.stringify(entity));

                    console.log("url?" + entity.url);
                    Timeline.addPost(entity, item.key.name);
                }
            }
            Timeline.next=result.nextPageToken;
        })
    },
};

const TimelineView = {
    oninit: Timeline.getDefaultPosts,
    view: function () {
        return m('div', {class: 'text-center'}, [Timeline.posts,
            m('button', {
                type:"button",
                class:"btn btn-block btn-primary mx-auto",
                style:"auto",
                onclick: function(e){
                    Timeline.getDefaultPosts();
                }
            },"Load more post")
        ])
    }
};

const Prompt = {
    title: '',
    imageUrl:"",
};


const MainView = {
    view: function () {
        return m("div", {class: 'row align-items-start'},[
            m(SettingsBarView),
            m('div', {class: 'container'}, [

                m('div', {class: 'col'}, [
                    m("div", {class: ''}, m(PromptView)),
                    m("div", {class: ''}, m(TimelineView))
                ])
            ])
        ])
    }
};

m.mount(document.body, MainView);


