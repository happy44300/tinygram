function parseJwt (token) {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function (c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));

    return JSON.parse(jsonPayload);
}

const PromptView = {
    isPromptEnabled: true,
    view: function(){

        return m('div', {class: 'card my-3 mx-auto border-0', style:"width: 50%"},
            m("label",{for:"postForm", class:"form-label"},[
            m("textarea[placeholder=Write a new post]", {
                value: Prompt.content,
                disabled: PromptView.isPromptEnabled,
                class:"form-control",
                rows: 3,
                id:"postForm",
                oninput: function (e) {
                    Prompt.content=e.target.value
                },
            }),
            m('button', {
                type:"submit",
                class:"btn btn-block btn-primary w-100",
                style:"auto",
                onclick: function(e){
                    PostController.uploadPost(Prompt.content, "");
                }
            },"Post")
        ]))
    },
    enablePrompt: function(){
        PromptView.isPromptEnabled = false;
        m.redraw();
    }
};
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
}


function uploadPost() {

}

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
                console.log("Uploaded Post!" + result);
            })
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

    addPost: function (imgUrl, bodyText) {
        this.posts.push(
            m("div", {class: "card my-3 mx-auto", style: "width: 50%;"}, [
                m("img", {class: "card-static_dir-top", src: imgUrl}),
                m("div", {class: "card-body"},
                    m("p", {class: "card-text"}, bodyText),
                    m("a", {href: "#", class: "btn btn-danger w-100"}, "Like"))
            ]));
    },

    loadPosts: function () {
        console.log("hello");
        for (let i = 0; i < 5; i++) {
            Timeline.addPost("https://cdn.pixabay.com/photo/2022/09/26/04/24/swan-7479623_960_720.jpg", `Goose ${i}`);
        }
    },

    loadFollowedPosts: function () {

    },
    displayPost: function () {

    },
};

const TimelineView = {
    oninit: Timeline.loadPosts,
    view: function () {
        return m('div', {class: 'timeline'}, Timeline.posts)
    }
};

const Prompt = {
    content: '',
    addPictureToPost: function () {

    },

    sendPost: function () {

    }
};


const MainView = {
    view: function () {
        return m("div", {class: 'row align-items-start'},[
        m(SettingsBarView),
            m('div', {class: 'container text-center'}, [

                m('div', {class: 'col'}, [
                    m("div", {class: 'justify-content-center'}, m(PromptView)),
                    m("div", {class: 'justify-content-center'}, m(TimelineView))
                ])
            ])
        ])
    }
};

m.mount(document.body, MainView)	