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
}


var SettingsBar = {
    
}

var SettingsBarView = {
    view: function() {
        return m('div', {}, [
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

        ])
    }
}

var Timeline = {
    posts: [],
    followedPosts: [],

    addPost: function (imgUrl, bodyText) {
        this.posts.push(
            m("div", {class:"card",style:"width: 18rem;"},[
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
    view: function(){
        return m("input[type=text][placeholder=Write a new post]", {
            value: Prompt.content,
            oninput: function (e) {
            Prompt.content=e.target.value
            },
        })
    }
}

var MainView = {
    view: function() {
        return m('div', {class: 'container text-center'}, [
            m("div", {class: 'row'}, m(SettingsBarView)),
            m('div',{class: 'row'}, [
                m("div", {class: 'col'}, m(PromptView)),
                m("div", {class: 'col'}, m(TimelineView))
            ])
        ])
    }
}

m.mount(document.body, MainView)	