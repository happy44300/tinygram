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
        return m('div', {/*Specify Attributes*/}, [
            m('div', {/*Specify Attributes*/}, [
                m('div',{
                    id: 'g_id_onload',
                    'data-client_id': '357851039901-acjg8a6nqa9asa4kog7da5fogtcup52p.apps.googleusercontent.com',
                    'data-callback': 'handleCredentialResponse',
                }),
                m('div', {
                    class: 'g_id_signin',
                    'data-type': 'standard'
                })    
            ]),
            m('a', {href: 'https://github.com/happy44300/tinygram/tree/main/src/main/webapp/html'/*Specify Attributes*/}, 'TinyGram GitHub'),

        ])
    }
}

var Timeline = {
    posts: [],
    followedPosts: [],
    loadPosts: function(){

    },
    loadFollowedPosts: function() {

    },
    displayPost: function() {

    },  
}

var TimelineView = {
    view: function() {
        return m('div', {class: 'timeline'}, Timeline.posts)
    }
}

var Prompt = {
    defaultContent: 'Write a new post',
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
        return m("input[type=text][placeholder=Prompt.defaultContent]", {
            value: Prompt.content,
            oninput: function (e) {
            Prompt.content=e.target.value
            },
        })
    }
}

var MainView = {
    view: function() {
        return m('div', {class:'container'}, [
            m("div", {}, "HELLOOOOOOOOOOOOOOOOOO"),
            m("div", {}, m(SettingsBarView)),
            m('div',{}, [
                m("div", {}, m(PromptView)),
                m("div", {}, m(TimelineView))
            ])
        ])
    }
 }

m.mount(document.body, MainView)	