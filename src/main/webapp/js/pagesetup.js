function parseJwt (token) {
    var base64Url = token.split('.')[1];
    var base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    var jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function(c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));

    return JSON.parse(jsonPayload);
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
}

var User = {
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

}

function uploadPost() {

}

var PostController = {
    uploadPost: function(body, pictureUrl){

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
        .then(function(result) {
            console.log("Uplaoded Post!" + result);
        })
    }
}

var SettingsBar = {
    
}

var SettingsBarView = {
    view: function() {
        return m('div', {class: 'd-flex flex-wrap align-items-center justify-content-center justify-content-md-between py-3 mb-4 border-bottom border-top border-left'}, [
            m('div', {class: 'col'}, [
                m('div',{
                    id: 'g_id_onload',
                    'data-client_id': '477502282441-pqpq69gi50k2tccp2ps7kqs5fjqdc1t9.apps.googleusercontent.com',
                    'data-callback': 'handleCredentialResponse',
                }),
                m('div', {
                    class: 'g_id_signin',
                    'data-type': 'standard'
                })    
            ]),
            m('a', {href: 'https://github.com/happy44300/tinygram/tree/main/src/main/webapp/html', class: 'col'}, 'TinyGram GitHub'),
            //m('div', {class: 'col img-max'}, m("img",{class:"card-img-top rounded float-left mx-auto d-block mb-2 float-md-left mr-md-4 img-thumbnail",src:'https://pixabay.com/fr/images/download/instagram-1581266_640.jpg'}))
        ])
    }
}

var Timeline = {
    posts: [],
    followedPosts: [],

    addPost: function (imgUrl, bodyText) {
        this.posts.push(
            m("div", {class:"card my-3",style:"width: 18rem;"},[
                m("img",{class:"card-img-top",src:imgUrl}),
                m("div", {class:"card-body"},
                    m("p",{class:"card-text"}, bodyText ),
                    m("a",{href:"#",class:"btn btn-primary"},"Like"))
        ]));
    },

    loadPosts: function(){
        console.log("hello");
        for(let i = 0; i < 5; i++){
            Timeline.addPost("https://cdn.pixabay.com/photo/2022/09/26/04/24/swan-7479623_960_720.jpg", `Goose ${i}`);
        }
    },
    
    loadFollowedPosts: function() {
        
    },
    displayPost: function() {

    },  
}

var TimelineView = {
    oninit: Timeline.loadPosts,
    view: function() {
        return m('div', {class: 'timeline'}, Timeline.posts)
    }
}

var Prompt = {
    content: '',
    addPictureToPost: function() {

    },
    addVideoToPost: function() {

    },
    sendPost: function() {

    }
}

var PromptView = {
    isPromptEnabled: true,
    view: function(){

        return m('div', {class: 'container mt-10'}, [
            m("textarea[placeholder=Write a new post]", {
                value: Prompt.content,
                disabled: PromptView.isPromptEnabled,
                rows: 3,
                oninput: function (e) {
                    Prompt.content=e.target.value
                },
            }),
            m('button', {
                onclick: function(e){
                    PostController.uploadPost(Prompt.content, "");
                }
            })
        ])
    },
    enablePrompt: function(){
        PromptView.isPromptEnabled = false;
        m.redraw();
    }
}

var MainView = {
    view: function() {
        return m('div', {class: 'container text-center'}, [
            m("div", {class: 'row container'}, m(SettingsBarView)),
            m('div',{class: 'row'}, [
                m("div", {class: 'col'}, m(PromptView)),
                m("div", {class: 'col'}, m(TimelineView))
            ])
        ])
    }
}

m.mount(document.body, MainView)	